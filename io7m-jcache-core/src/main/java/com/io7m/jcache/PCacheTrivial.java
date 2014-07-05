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

import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

/**
 * A trivial implementation of the {@link PCacheType} interface.
 *
 * @param <K>
 *          The type of keys
 * @param <TVIEW>
 *          The type of cached values, as visible to users of the cache
 * @param <TCACHE>
 *          The type of cached values, as visible to cache implementations
 * @param <E>
 *          The type of exceptions raised during loading
 */

public final class PCacheTrivial<K, TVIEW, TCACHE extends TVIEW, E extends Throwable> implements
  PCacheType<K, TVIEW, TCACHE, E>
{
  private static final class CachedValue<V>
  {
    private final BigInteger size;
    private final BigInteger time;
    private final V          value;

    public CachedValue(
      final V in_value,
      final BigInteger in_time,
      final BigInteger in_size)
    {
      this.value = in_value;
      this.time = in_time;
      this.size = in_size;
    }

    public BigInteger getSize()
    {
      return this.size;
    }

    public BigInteger getTime()
    {
      return this.time;
    }

    public V getValue()
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
   *
   * @param <K>
   *          The type of keys
   * @param <TVIEW>
   *          The type of cached values, as visible to users of the cache
   * @param <TCACHE>
   *          The type of cached values, as visible to cache implementations
   * @param <E>
   *          The type of exceptions raised by the loader
   */

  public static
    <K, TVIEW, TCACHE extends TVIEW, E extends Throwable>
    PCacheType<K, TVIEW, TCACHE, E>
    newCache(
      final JCacheLoaderType<K, TCACHE, E> loader,
      final PCacheConfig config)
  {
    return new PCacheTrivial<K, TVIEW, TCACHE, E>(loader, config);
  }

  private final PCacheConfig                     config;
  private @Nullable JCacheEventsType<K, TCACHE>  events;
  private final Set<K>                           item_removals;
  private final Map<K, CachedValue<TCACHE>>      items;
  private final NavigableMap<BigInteger, Set<K>> items_by_time;
  private final JCacheLoaderType<K, TCACHE, E>   loader;
  private boolean                                period;
  private BigInteger                             time;
  private BigInteger                             used;

  private PCacheTrivial(
    final JCacheLoaderType<K, TCACHE, E> in_loader,
    final PCacheConfig in_config)
  {
    this.loader = NullCheck.notNull(in_loader, "Loader");
    this.config = NullCheck.notNull(in_config, "Configuration");
    this.items = new HashMap<K, CachedValue<TCACHE>>();
    this.item_removals = new HashSet<K>();
    this.items_by_time = new TreeMap<BigInteger, Set<K>>();
    this.used = BigInteger.ZERO;
    this.time = BigInteger.ZERO;
    this.period = false;
    this.events = null;
  }

  private CachedValue<TCACHE> cacheAdd(
    final K key,
    final TCACHE new_value,
    final BigInteger size)
  {
    this.used = this.used.add(size);
    return this.cachePut(key, new_value, size);
  }

  @Override public void cacheDelete()
  {
    this.item_removals.addAll(this.items.keySet());
    this.cachePerformRemovals();
  }

  @Override public void cacheEventsSubscribe(
    final JCacheEventsType<K, TCACHE> e)
  {
    this.events = NullCheck.notNull(e, "Events");
  }

  @Override public void cacheEventsUnsubscribe()
  {
    this.events = null;
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

  private CachedValue<TCACHE> cacheGetAddingNew(
    final K key)
    throws E,
      JCacheException
  {
    boolean failed = true;
    TCACHE new_value = null;

    this.checkOverflow();

    try {
      new_value = this.loader.cacheValueLoad(key);
      this.checkLoaderReturnForNull(key, new_value);

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

  @Override public TCACHE cacheGetPeriodic(
    final K key)
    throws E,
      JCacheException
  {
    NullCheck.notNull(key, "Key");

    if (this.period == false) {
      throw new IllegalStateException("Period is not in progress");
    }

    final CachedValue<TCACHE> cv = this.pcCacheGetActual(key);
    this.eventObjectRetrieved(key, cv);
    return cv.getValue();
  }

  private CachedValue<TCACHE> cacheGetReplace(
    final K key)
  {
    final CachedValue<TCACHE> v = this.items.get(key);
    this.cacheTimeRemoveKey(key, v.getTime());
    return this.cachePut(key, v.getValue(), v.getSize());
  }

  @Override public boolean cacheIsCached(
    final K key)
  {
    NullCheck.notNull(key, "Key");
    return this.items.containsKey(key);
  }

  @Override public BigInteger cacheItemCount()
  {
    return BigInteger.valueOf(this.items.size());
  }

  private void cachePerformRemovals()
  {
    for (final K key : this.item_removals) {
      this.cacheRemove(key, this.items.get(key));
    }

    this.item_removals.clear();
  }

  @Override public void cachePeriodEnd()
  {
    if (this.period == false) {
      throw new IllegalStateException(
        "Period has already ended (or has not begun)");
    }

    this.period = false;
    this.cacheEvictItems();
  }

  @Override public void cachePeriodStart()
  {
    if (this.period == true) {
      throw new IllegalStateException("Period is already in progress");
    }

    this.period = true;
    this.time = this.time.add(BigInteger.ONE);
  }

  private CachedValue<TCACHE> cachePut(
    final K key,
    final TCACHE value,
    final BigInteger size)
  {
    final CachedValue<TCACHE> cv =
      new CachedValue<TCACHE>(value, this.time, size);
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
    final K key,
    final CachedValue<TCACHE> existing)
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

  @Override public BigInteger cacheSize()
  {
    return this.used;
  }

  private void cacheTimeRemoveKey(
    final K key,
    final BigInteger t)
  {
    final Set<K> time_keys = this.items_by_time.get(t);
    assert time_keys != null;
    assert time_keys.isEmpty() == false;
    time_keys.remove(key);
    if (time_keys.isEmpty()) {
      this.items_by_time.remove(t);
    }
  }

  private void checkLoaderReturnForNull(
    final K key,
    final @Nullable TCACHE new_value)
    throws JCacheException
  {
    if (new_value == null) {
      throw JCacheException.errorLoaderReturnedNull(key);
    }
  }

  void checkOverflow()
    throws JCacheException
  {
    if (this.items.size() == Integer.MAX_VALUE) {
      throw JCacheException.errorInternalCacheOverflow(this.items.size());
    }
  }

  private void eventObjectCloseError(
    final K key,
    final CachedValue<TCACHE> existing,
    final Throwable x)
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
    final K key,
    final CachedValue<TCACHE> existing)
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
    final K key,
    final TCACHE new_value,
    final BigInteger size)
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
    final K key,
    final CachedValue<TCACHE> cv)
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

  private CachedValue<TCACHE> pcCacheGetActual(
    final K key)
    throws E,
      JCacheException
  {
    if (this.cacheIsCached(key)) {
      return this.cacheGetReplace(key);
    }

    return this.cacheGetAddingNew(key);
  }
}
