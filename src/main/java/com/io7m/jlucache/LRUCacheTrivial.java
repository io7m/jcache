/*
 * Copyright © 2013 <code@io7m.com> http://io7m.com
 * 
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR
 * IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package com.io7m.jlucache;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import com.io7m.jaux.Constraints;
import com.io7m.jaux.Constraints.ConstraintError;
import com.io7m.jlucache.LUCacheException.Code;

/**
 * A mindlessly simple LRU cache; the oldest objects are evicted from the
 * cache first.
 */

public final class LRUCacheTrivial<K, V, E extends Throwable> implements
  LUCache<K, V, E>,
  LUCacheEventsSubscription<K, V, E>
{
  @Immutable private static final class CachedValue<V>
  {
    final @Nonnull V value;
    final long       time;
    final long       size;

    CachedValue(
      final @Nonnull V value,
      final long time,
      final long size)
    {
      this.value = value;
      this.time = time;
      this.size = size;
    }
  }

  public static @Nonnull
    <K, V, E extends Throwable>
    LRUCacheTrivial<K, V, E>
    newCache(
      final @Nonnull LUCacheLoader<K, V, E> loader,
      final @Nonnull LRUCacheConfig config)
      throws ConstraintError
  {
    return new LRUCacheTrivial<K, V, E>(loader, config);
  }

  private final @Nonnull LUCacheLoader<K, V, E>     loader;
  private final @Nonnull LRUCacheConfig             config;
  private final @Nonnull HashMap<K, CachedValue<V>> items;
  private final @Nonnull TreeMap<Long, K>           time_items;
  private long                                      used;
  private long                                      gets;
  private @CheckForNull LUCacheEvents<K, V>         events;

  private LRUCacheTrivial(
    final @Nonnull LUCacheLoader<K, V, E> loader,
    final @Nonnull LRUCacheConfig config)
    throws ConstraintError
  {
    this.loader = Constraints.constrainNotNull(loader, "Loader");
    this.config = Constraints.constrainNotNull(config, "Configuration");
    this.items = new HashMap<K, CachedValue<V>>();
    this.time_items = new TreeMap<Long, K>();
    this.used = 0;
    this.gets = 0;
    this.events = null;
  }

  private @Nonnull CachedValue<V> cacheAdd(
    final @Nonnull K key,
    final @Nonnull V new_value,
    final long size)
  {
    this.used += size;
    return this.cachePut(key, new_value, size);
  }

  private void cacheEvictOldest()
    throws E
  {
    final Entry<Long, K> eleast = this.time_items.firstEntry();
    assert eleast != null;
    final CachedValue<V> old_cached = this.items.get(eleast.getValue());
    this.cacheRemove(eleast.getKey(), eleast.getValue(), old_cached);
  }

  private void cacheEvictOldestItems(
    final long added_size,
    final long maximum)
    throws E
  {
    assert added_size < maximum;

    /**
     * Because objects larger than the capacity cannot be inserted into the
     * map, at least one object must exist if <code>used</code> is nonzero.
     */

    for (;;) {
      if ((this.used + added_size) <= maximum) {
        break;
      }
      this.cacheEvictOldest();
    }

    assert (this.used + added_size) <= maximum;
  }

  private @Nonnull CachedValue<V> cacheGetAddingNew(
    final @Nonnull K key)
    throws E,
      LUCacheException,
      ConstraintError
  {
    boolean failed = true;
    V new_value = null;

    try {
      new_value = this.loader.luCacheLoadFrom(key);
      if (new_value == null) {
        throw this.errorLoaderReturnedNull(key);
      }

      final long size = this.loader.luCacheSizeOf(new_value);
      this.eventObjectLoaded(key, new_value, size);

      if (size < 1) {
        throw this.errorObjectTooSmall(key, size);
      }

      final long maximum = this.config.getMaximumCapacity();
      if (size > maximum) {
        throw this.errorObjectTooLarge(key, size, maximum);
      }

      this.cacheEvictOldestItems(size, maximum);

      failed = false;
      return this.cacheAdd(key, new_value, size);
    } finally {
      if (failed) {
        if (new_value != null) {
          this.loader.luCacheClose(new_value);
        }
      }
    }
  }

  private @Nonnull CachedValue<V> cacheGetReplace(
    final @Nonnull K key)
  {
    final CachedValue<V> v = this.items.get(key);
    this.time_items.remove(Long.valueOf(v.time));
    return this.cachePut(key, v.value, v.size);
  }

  private @Nonnull CachedValue<V> cachePut(
    final @Nonnull K key,
    final @Nonnull V new_value,
    final long size)
  {
    this.gets += 1;
    final CachedValue<V> cv = new CachedValue<V>(new_value, this.gets, size);
    this.items.put(key, cv);
    this.time_items.put(Long.valueOf(this.gets), key);
    return cv;
  }

  private void cacheRemove(
    final @Nonnull Long time,
    final @Nonnull K key,
    final @Nonnull CachedValue<V> existing)
    throws E
  {
    this.eventObjectEvicted(key, existing);
    this.loader.luCacheClose(existing.value);
    this.time_items.remove(time);
    this.items.remove(key);
    this.used -= existing.size;
  }

  private @Nonnull LUCacheException errorLoaderReturnedNull(
    final @Nonnull K key)
    throws ConstraintError
  {
    final StringBuilder m = new StringBuilder();
    m.append("Loader returned null for '");
    m.append(key);
    m.append("'");
    return new LUCacheException(
      Code.LUCACHE_LOADER_RETURNED_NULL,
      m.toString());
  }

  private @Nonnull LUCacheException errorObjectTooLarge(
    final @Nonnull K key,
    final long size,
    final long maximum)
    throws ConstraintError
  {
    final StringBuilder m = new StringBuilder();
    m.append("Object for '");
    m.append(key);
    m.append("' is of size ");
    m.append(size);
    m.append(", which is too large for a cache with maximum capacity of ");
    m.append(maximum);
    return new LUCacheException(Code.LUCACHE_OBJECT_TOO_LARGE, m.toString());
  }

  private @Nonnull LUCacheException errorObjectTooSmall(
    final @Nonnull K key,
    final long size)
    throws ConstraintError
  {
    final StringBuilder m = new StringBuilder();
    m.append("Object for '");
    m.append(key);
    m.append("' is of size ");
    m.append(size);
    m.append(", which is too small: must be at least 1");
    return new LUCacheException(Code.LUCACHE_OBJECT_TOO_SMALL, m.toString());
  }

  private void eventObjectEvicted(
    final K key,
    final CachedValue<V> existing)
  {
    if (this.events != null) {
      try {
        this.events.luCacheEventObjectEvicted(
          key,
          existing.value,
          existing.size);
      } catch (final Throwable _) {
        // Ignore
      }
    }
  }

  private void eventObjectLoaded(
    final K key,
    final V new_value,
    final long size)
  {
    if (this.events != null) {
      try {
        this.events.luCacheEventObjectLoaded(key, new_value, size);
      } catch (final Throwable _) {
        // Ignore
      }
    }
  }

  private void eventObjectRetrieved(
    final @Nonnull K key,
    final @Nonnull CachedValue<V> cv)
  {
    if (this.events != null) {
      try {
        this.events.luCacheEventObjectRetrieved(key, cv.value, cv.size);
      } catch (final Throwable _) {
        // Ignore
      }
    }
  }

  @Override public @Nonnull LRUCacheConfig luCacheConfiguration()
  {
    return this.config;
  }

  @Override public void luCacheEventsSubscribe(
    final @Nonnull LUCacheEvents<K, V> e)
    throws ConstraintError
  {
    this.events = Constraints.constrainNotNull(e, "Events");
  }

  @Override public void luCacheEventsUnsubscribe()
  {
    this.events = null;
  }

  @Override public @Nonnull V luCacheGet(
    final @Nonnull K key)
    throws ConstraintError,
      E,
      LUCacheException
  {
    Constraints.constrainNotNull(key, "Key");

    final CachedValue<V> cv = this.luCacheGetActual(key);
    this.eventObjectRetrieved(key, cv);
    return cv.value;
  }

  private @Nonnull CachedValue<V> luCacheGetActual(
    final @Nonnull K key)
    throws ConstraintError,
      E,
      LUCacheException
  {
    if (this.luCacheIsCached(key)) {
      return this.cacheGetReplace(key);
    }

    return this.cacheGetAddingNew(key);
  }

  @Override public boolean luCacheIsCached(
    final @Nonnull K key)
    throws ConstraintError
  {
    Constraints.constrainNotNull(key, "Key");
    return this.items.containsKey(key);
  }

  @Override public long luCacheItems()
  {
    return this.items.size();
  }

  @Override public long luCacheSize()
  {
    return this.used;
  }

  @Override public String toString()
  {
    final StringBuilder builder = new StringBuilder();
    builder.append("[LRUCacheTrivial ");

    builder.append("[size ");
    builder.append(this.used);
    builder.append("]");

    builder.append("]");
    return builder.toString();
  }
}
