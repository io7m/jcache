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

import java.math.BigInteger;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import com.io7m.jaux.Constraints;
import com.io7m.jaux.Constraints.ConstraintError;

/**
 * A trivial implementation of the {@link PCache} interface.
 * 
 * @param <K>
 *          The type of keys
 * @param <V>
 *          The type of cached values
 * @param <E>
 *          The type of exceptions raised during loading
 */

public final class PCacheTrivial<K, V, E extends Throwable> implements
  PCache<K, V, E>
{
  @Immutable private static final class CachedValue<V>
  {
    private final @Nonnull BigInteger size;
    private final @Nonnull BigInteger time;
    private final @Nonnull V          value;

    public CachedValue(
      final @Nonnull V in_value,
      final @Nonnull BigInteger in_time,
      final @Nonnull BigInteger in_size)
    {
      this.value = in_value;
      this.time = in_time;
      this.size = in_size;
    }

    public @Nonnull BigInteger getSize()
    {
      return this.size;
    }

    public @Nonnull BigInteger getTime()
    {
      return this.time;
    }

    public @Nonnull V getValue()
    {
      return this.value;
    }
  }

  /**
   * Construct a new <tt>PCache</tt>.
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
    PCache<K, V, E>
    newCache(
      final @Nonnull JCacheLoader<K, V, E> loader,
      final @Nonnull PCacheConfig config)
      throws ConstraintError
  {
    return new PCacheTrivial<K, V, E>(loader, config);
  }

  private final @Nonnull PCacheConfig                     config;
  private @CheckForNull JCacheEvents<K, V>                events;
  private final @Nonnull Set<K>                           item_removals;
  private final @Nonnull Map<K, CachedValue<V>>           items;
  private final @Nonnull NavigableMap<BigInteger, Set<K>> items_by_time;
  private final @Nonnull JCacheLoader<K, V, E>            loader;
  private boolean                                         period;
  private @Nonnull BigInteger                             time;
  private @Nonnull BigInteger                             used;

  private PCacheTrivial(
    final @Nonnull JCacheLoader<K, V, E> in_loader,
    final @Nonnull PCacheConfig in_config)
    throws ConstraintError
  {
    this.loader = Constraints.constrainNotNull(in_loader, "Loader");
    this.config = Constraints.constrainNotNull(in_config, "Configuration");
    this.items = new HashMap<K, CachedValue<V>>();
    this.item_removals = new HashSet<K>();
    this.items_by_time = new TreeMap<BigInteger, Set<K>>();
    this.used = BigInteger.ZERO;
    this.time = BigInteger.ZERO;
    this.period = false;
    this.events = null;
  }

  private @Nonnull CachedValue<V> cacheAdd(
    final @Nonnull K key,
    final @Nonnull V new_value,
    final @Nonnull BigInteger size)
  {
    this.used = this.used.add(size);
    return this.cachePut(key, new_value, size);
  }

  private void cacheEvictItems()
  {
    this.cacheEvictItemsByAgeIfNecessary();
    this.cacheEvictItemsBySizeIfNecessary();
  }

  private void cacheEvictItemsByAgeIfNecessary()
  {
    if (this.config.getMaximumAge().compareTo(BigInteger.ZERO) > 0) {
      final BigInteger minimum =
        this.time.subtract(this.config.getMaximumAge());
      final NavigableMap<BigInteger, Set<K>> head =
        this.items_by_time.headMap(minimum, true);
      for (final Entry<BigInteger, Set<K>> e : head.entrySet()) {
        this.item_removals.addAll(e.getValue());
      }
      this.cachePerformRemovals();
    }
  }

  private void cacheEvictItemsBySizeIfNecessary()
  {
    final BigInteger maximum = this.config.getMaximumSize();
    if (maximum.compareTo(BigInteger.ZERO) > 0) {
      while (this.used.compareTo(maximum) > 0) {
        final Set<K> keys =
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
      JCacheException,
      ConstraintError
  {
    boolean failed = true;
    V new_value = null;

    this.checkOverflow();

    try {
      new_value = this.loader.cacheValueLoad(key);
      if (new_value == null) {
        throw JCacheException.errorLoaderReturnedNull(key);
      }

      final BigInteger size = this.loader.cacheValueSizeOf(new_value);
      this.eventObjectLoaded(key, new_value, size);

      if (size.compareTo(BigInteger.ONE) < 0) {
        throw JCacheException.errorObjectTooSmall(key, size);
      }

      final BigInteger maximum = this.config.getMaximumSize();
      if (maximum.compareTo(BigInteger.ZERO) > 0) {
        if (size.compareTo(maximum) > 0) {
          throw JCacheException.errorObjectTooLarge(key, size, maximum);
        }
      }

      failed = false;
      return this.cacheAdd(key, new_value, size);
    } finally {
      if (failed) {
        if (new_value != null) {
          this.loader.cacheValueClose(new_value);
        }
      }
    }
  }

  void checkOverflow()
    throws JCacheException
  {
    if (this.items.size() == Integer.MAX_VALUE) {
      throw JCacheException.errorInternalCacheOverflow(this.items.size());
    }
  }

  private @Nonnull CachedValue<V> cacheGetReplace(
    final @Nonnull K key)
  {
    final CachedValue<V> v = this.items.get(key);
    this.cacheTimeRemoveKey(key, v.getTime());
    return this.cachePut(key, v.getValue(), v.getSize());
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
    final @Nonnull BigInteger size)
  {
    final CachedValue<V> cv = new CachedValue<V>(value, this.time, size);
    this.items.put(key, cv);

    Set<K> keys;
    if (this.items_by_time.containsKey(this.time)) {
      keys = this.items_by_time.get(this.time);
    } else {
      keys = new HashSet<K>();
      this.items_by_time.put(this.time, keys);
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
      this.loader.cacheValueClose(existing.getValue());
    } catch (final Throwable x) {
      this.eventObjectCloseError(key, existing, x);
    }

    this.cacheTimeRemoveKey(key, existing.getTime());
    this.items.remove(key);
    this.used = this.used.subtract(existing.getSize());
  }

  private void cacheTimeRemoveKey(
    final @Nonnull K key,
    final @Nonnull BigInteger t)
  {
    final Set<K> time_keys = this.items_by_time.get(t);
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
        this.events.cacheEventValueCloseError(
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
    final @Nonnull K key,
    final @Nonnull CachedValue<V> existing)
  {
    if (this.events != null) {
      try {
        this.events.cacheEventValueEvicted(
          key,
          existing.getValue(),
          existing.getSize());
      } catch (final Throwable _) {
        // Ignore
      }
    }
  }

  private void eventObjectLoaded(
    final @Nonnull K key,
    final @Nonnull V new_value,
    final @Nonnull BigInteger size)
  {
    if (this.events != null) {
      try {
        this.events.cacheEventValueLoaded(key, new_value, size);
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
        this.events
          .cacheEventValueRetrieved(key, cv.getValue(), cv.getSize());
      } catch (final Throwable _) {
        // Ignore
      }
    }
  }

  @Override public void cacheDelete()
  {
    this.item_removals.addAll(this.items.keySet());
    this.cachePerformRemovals();
  }

  @Override public void cacheEventsSubscribe(
    final @Nonnull JCacheEvents<K, V> e)
    throws ConstraintError
  {
    this.events = Constraints.constrainNotNull(e, "Events");
  }

  @Override public void cacheEventsUnsubscribe()
  {
    this.events = null;
  }

  @Override public boolean cacheIsCached(
    final @Nonnull K key)
    throws ConstraintError
  {
    Constraints.constrainNotNull(key, "Key");
    return this.items.containsKey(key);
  }

  @Override public @Nonnull BigInteger cacheItemCount()
  {
    return BigInteger.valueOf(this.items.size());
  }

  @Override public @Nonnull BigInteger cacheSize()
  {
    return this.used;
  }

  @Override public V cacheGetPeriodic(
    final @Nonnull K key)
    throws ConstraintError,
      E,
      JCacheException
  {
    Constraints.constrainNotNull(key, "Key");
    Constraints.constrainArbitrary(this.period, "Period has begun");

    final CachedValue<V> cv = this.pcCacheGetActual(key);
    this.eventObjectRetrieved(key, cv);
    return cv.getValue();
  }

  private @Nonnull CachedValue<V> pcCacheGetActual(
    final @Nonnull K key)
    throws ConstraintError,
      E,
      JCacheException
  {
    if (this.cacheIsCached(key)) {
      return this.cacheGetReplace(key);
    }

    return this.cacheGetAddingNew(key);
  }

  @Override public void cachePeriodEnd()
    throws ConstraintError
  {
    Constraints.constrainArbitrary(
      this.period == true,
      "Period has not already ended");
    this.period = false;

    this.cacheEvictItems();
  }

  @Override public void cachePeriodStart()
    throws ConstraintError
  {
    Constraints.constrainArbitrary(
      this.period == false,
      "Period has not already begun");
    this.period = true;
    this.time = this.time.add(BigInteger.ONE);
  }
}
