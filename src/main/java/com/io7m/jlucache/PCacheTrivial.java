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
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import com.io7m.jaux.Constraints;
import com.io7m.jaux.Constraints.ConstraintError;

public final class PCacheTrivial<K, V, E extends Throwable> implements
  PCache<K, V, E>
{
  @Immutable private static final class CachedValue<V>
  {
    final long          size;
    final @Nonnull Long time;
    final @Nonnull V    value;

    public CachedValue(
      final @Nonnull V value,
      final @Nonnull Long time,
      final long size)
    {
      this.value = value;
      this.time = time;
      this.size = size;
    }
  }

  public static @Nonnull
    <K, V, E extends Throwable>
    PCache<K, V, E>
    newCache(
      final @Nonnull LUCacheLoader<K, V, E> loader,
      final @Nonnull PCacheConfig config)
      throws ConstraintError
  {
    return new PCacheTrivial<K, V, E>(loader, config);
  }

  private final @Nonnull PCacheConfig               config;
  private @CheckForNull LUCacheEvents<K, V>         events;
  private final @Nonnull HashSet<K>                 item_removals;
  private final @Nonnull HashMap<K, CachedValue<V>> items;
  private final @Nonnull TreeMap<Long, HashSet<K>>  items_by_time;
  private final @Nonnull LUCacheLoader<K, V, E>     loader;
  private boolean                                   period;
  private long                                      time;
  private long                                      used;

  private PCacheTrivial(
    final @Nonnull LUCacheLoader<K, V, E> loader,
    final @Nonnull PCacheConfig config)
    throws ConstraintError
  {
    this.loader = Constraints.constrainNotNull(loader, "Loader");
    this.config = Constraints.constrainNotNull(config, "Configuration");
    this.items = new HashMap<K, CachedValue<V>>();
    this.item_removals = new HashSet<K>();
    this.items_by_time = new TreeMap<Long, HashSet<K>>();
    this.used = 0;
    this.time = 0;
    this.period = false;
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

  private void cacheEvictItems()
  {
    this.cacheEvictItemsByAgeIfNecessary();
    this.cacheEvictItemsBySizeIfNecessary();
  }

  private void cacheEvictItemsByAgeIfNecessary()
  {
    if (this.config.getMaximumAge() > 0) {
      final Long minimum =
        Long.valueOf(this.time - this.config.getMaximumAge());
      final NavigableMap<Long, HashSet<K>> head =
        this.items_by_time.headMap(minimum, true);
      for (final Entry<Long, HashSet<K>> e : head.entrySet()) {
        this.item_removals.addAll(e.getValue());
      }
      this.cachePerformRemovals();
    }
  }

  private void cacheEvictItemsBySizeIfNecessary()
  {
    if (this.config.getMaximumSize() > 0) {
      while (this.used > this.config.getMaximumSize()) {
        final HashSet<K> keys =
          this.items_by_time.get(this.items_by_time.firstKey());
        assert keys.isEmpty() == false;
        final K k = keys.iterator().next();
        this.item_removals.add(k);
        this.cachePerformRemovals();
      }
    }
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

      final long maximum = this.config.getMaximumSize();
      if (maximum > 0) {
        if (size > maximum) {
          throw LUCacheException.errorObjectTooLarge(key, size, maximum);
        }
      }

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
    this.cacheTimeRemoveKey(key, v.time);
    return this.cachePut(key, v.value, v.size);
  }

  private void cachePerformRemovals()
  {
    for (final K key : this.item_removals) {
      this.cacheRemove(key, this.items.get(key));
    }

    this.item_removals.clear();
  }

  private @Nonnull CachedValue<V> cachePut(
    final @Nonnull K key,
    final @Nonnull V value,
    final long size)
  {
    final Long t = Long.valueOf(this.time);
    final CachedValue<V> cv = new CachedValue<V>(value, t, size);
    this.items.put(key, cv);

    HashSet<K> keys;
    if (this.items_by_time.containsKey(t)) {
      keys = this.items_by_time.get(t);
    } else {
      keys = new HashSet<K>();
      this.items_by_time.put(t, keys);
    }
    keys.add(key);

    return cv;
  }

  private void cacheRemove(
    final @Nonnull K key,
    final @Nonnull CachedValue<V> existing)
  {
    this.eventObjectEvicted(key, existing);
    try {
      this.loader.luCacheClose(existing.value);
    } catch (final Throwable x) {
      this.eventObjectCloseError(key, existing, x);
    }

    this.cacheTimeRemoveKey(key, existing.time);
    this.items.remove(key);
    this.used -= existing.size;
  }

  private void cacheTimeRemoveKey(
    final @Nonnull K key,
    final @Nonnull Long t)
  {
    final HashSet<K> time_keys = this.items_by_time.get(t);
    assert time_keys != null;
    assert time_keys.isEmpty() == false;
    time_keys.remove(key);
    if (time_keys.isEmpty()) {
      this.items_by_time.remove(t);
    }
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
          existing.value,
          existing.size,
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

  @Override public void luCacheDelete()
  {
    this.item_removals.addAll(this.items.keySet());
    this.cachePerformRemovals();
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

  @Override public V pcCacheGet(
    final @Nonnull K key)
    throws ConstraintError,
      E,
      LUCacheException
  {
    Constraints.constrainNotNull(key, "Key");
    Constraints.constrainArbitrary(this.period, "Period has begun");

    final CachedValue<V> cv = this.pcCacheGetActual(key);
    this.eventObjectRetrieved(key, cv);
    return cv.value;
  }

  private @Nonnull CachedValue<V> pcCacheGetActual(
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

  @Override public void pcPeriodEnd()
    throws ConstraintError
  {
    Constraints.constrainArbitrary(
      this.period == true,
      "Period has not already ended");
    this.period = false;

    this.cacheEvictItems();
  }

  @Override public void pcPeriodStart()
    throws ConstraintError
  {
    Constraints.constrainArbitrary(
      this.period == false,
      "Period has not already begun");
    this.period = true;
    this.time++;
  }
}
