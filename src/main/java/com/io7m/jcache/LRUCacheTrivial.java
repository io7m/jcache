/*
 * Copyright © 2014 <code@io7m.com> http://io7m.com
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

package com.io7m.jcache;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import com.io7m.jaux.Constraints;
import com.io7m.jaux.Constraints.ConstraintError;

/**
 * A mindlessly simple LRU cache; the oldest objects are evicted from the
 * cache first.
 * 
 * @param <K>
 *          The type of keys
 * @param <V>
 *          The type of cached values
 * @param <E>
 *          The type of exceptions raised during loading
 */

public final class LRUCacheTrivial<K, V, E extends Throwable> implements
  LRUCache<K, V, E>
{
  @Immutable private static final class CachedValue<V>
  {
    private final long       size;
    private final long       time;
    private final @Nonnull V value;

    CachedValue(
      final @Nonnull V in_value,
      final long in_time,
      final long in_size)
    {
      this.value = in_value;
      this.time = in_time;
      this.size = in_size;
    }

    public long getSize()
    {
      return this.size;
    }

    public long getTime()
    {
      return this.time;
    }

    public @Nonnull V getValue()
    {
      return this.value;
    }
  }

  /**
   * Construct a new <tt>LRUCache</tt>.
   * 
   * @param loader
   *          The class that will load instances when given keys
   * @param config
   *          The cache configuration
   * @return A new cache instance
   * @throws ConstraintError
   *           Iff the loader or configuration are <tt>null</tt>
   * 
   * @param <K>
   *          The type of keys
   * @param <V>
   *          The type of cached values
   * @param <E>
   *          The type of exceptions raised by the loader
   */

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

  private final @Nonnull LRUCacheConfig         config;
  private @CheckForNull LUCacheEvents<K, V>     events;
  private long                                  gets;
  private final @Nonnull Map<K, CachedValue<V>> items;
  private final @Nonnull LUCacheLoader<K, V, E> loader;
  private final @Nonnull NavigableMap<Long, K>  time_items;
  private long                                  used;

  private LRUCacheTrivial(
    final @Nonnull LUCacheLoader<K, V, E> in_loader,
    final @Nonnull LRUCacheConfig in_config)
    throws ConstraintError
  {
    this.loader = Constraints.constrainNotNull(in_loader, "Loader");
    this.config = Constraints.constrainNotNull(in_config, "Configuration");
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
  {
    final Entry<Long, K> eleast = this.time_items.firstEntry();
    assert eleast != null;
    final CachedValue<V> old_cached = this.items.get(eleast.getValue());
    this.cacheRemove(eleast.getValue(), old_cached);
  }

  private void cacheEvictOldestItems(
    final long added_size,
    final long maximum)
  {
    assert added_size <= maximum;

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
        throw LUCacheException.errorLoaderReturnedNull(key);
      }

      final long size = this.loader.luCacheSizeOf(new_value);
      this.eventObjectLoaded(key, new_value, size);

      if (size < 1) {
        throw LUCacheException.errorObjectTooSmall(key, size);
      }

      final long maximum = this.config.getMaximumCapacity();
      if (size > maximum) {
        throw LUCacheException.errorObjectTooLarge(key, size, maximum);
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
    this.time_items.remove(Long.valueOf(v.getTime()));
    return this.cachePut(key, v.getValue(), v.getSize());
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
    final @Nonnull K key,
    final @Nonnull CachedValue<V> existing)
  {
    this.eventObjectEvicted(key, existing);
    try {
      this.loader.luCacheClose(existing.getValue());
    } catch (final Throwable x) {
      this.eventObjectCloseError(key, existing, x);
    }
    this.time_items.remove(Long.valueOf(existing.getTime()));
    this.items.remove(key);
    this.used -= existing.getSize();
  }

  private void eventObjectCloseError(
    final @Nonnull K key,
    final @Nonnull CachedValue<V> existing,
    final @Nonnull Throwable x)
  {
    if (this.events != null) {
      try {
        this.events.luCacheEventObjectCloseError(
          key,
          existing.getValue(),
          existing.getSize(),
          x);
      } catch (final Throwable _) {
        // Ignore
      }
    }
  }

  private void eventObjectEvicted(
    final K key,
    final CachedValue<V> existing)
  {
    if (this.events != null) {
      try {
        this.events.luCacheEventObjectEvicted(
          key,
          existing.getValue(),
          existing.getSize());
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
        this.events.luCacheEventObjectRetrieved(
          key,
          cv.getValue(),
          cv.getSize());
      } catch (final Throwable _) {
        // Ignore
      }
    }
  }

  @Override public @Nonnull LRUCacheConfig lruCacheConfiguration()
  {
    return this.config;
  }

  @Override public void luCacheDelete()
  {
    while (this.items.size() > 0) {
      this.cacheEvictOldest();
    }

    assert this.time_items.size() == 0;
    assert this.items.size() == 0;
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
    return cv.getValue();
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
