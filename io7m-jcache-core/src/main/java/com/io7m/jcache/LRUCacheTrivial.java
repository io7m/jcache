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
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;

import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

/**
 * A mindlessly simple LRU cache; the oldest objects are evicted from the
 * cache first.
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

public final class LRUCacheTrivial<K, TVIEW, TCACHE extends TVIEW, E extends Throwable> implements
  LRUCacheType<K, TVIEW, TCACHE, E>
{
  private static final class CachedValue<V>
  {
    private final BigInteger size;
    private final BigInteger time;
    private final V          value;

    CachedValue(
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
   * Construct a new <tt>LRUCache</tt>.
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
    LRUCacheTrivial<K, TVIEW, TCACHE, E>
    newCache(
      final JCacheLoaderType<K, TCACHE, E> loader,
      final LRUCacheConfig config)
  {
    return new LRUCacheTrivial<K, TVIEW, TCACHE, E>(loader, config);
  }

  private LRUCacheConfig                        config;
  private @Nullable JCacheEventsType<K, TCACHE> events;
  private BigInteger                            gets;
  private final Map<K, CachedValue<TCACHE>>     items;
  private final JCacheLoaderType<K, TCACHE, E>  loader;
  private final NavigableMap<BigInteger, K>     time_items;
  private BigInteger                            used;

  private LRUCacheTrivial(
    final JCacheLoaderType<K, TCACHE, E> in_loader,
    final LRUCacheConfig in_config)
  {
    this.loader = NullCheck.notNull(in_loader, "Loader");
    this.config = NullCheck.notNull(in_config, "Configuration");
    this.items = new HashMap<K, CachedValue<TCACHE>>();
    this.time_items = new TreeMap<BigInteger, K>();
    this.used = BigInteger.ZERO;
    this.gets = BigInteger.ZERO;
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

  private void cacheCheckOverflow()
    throws JCacheException
  {
    if (this.items.size() == Integer.MAX_VALUE) {
      throw JCacheException.errorInternalCacheOverflow(this.items.size());
    }
  }

  @Override public void cacheDelete()
  {
    while (this.items.size() > 0) {
      this.cacheEvictOldest();
    }

    assert this.time_items.size() == 0;
    assert this.items.size() == 0;
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

  private void cacheEvictOldest()
  {
    final Entry<BigInteger, K> eleast = this.time_items.firstEntry();
    assert eleast != null;
    final CachedValue<TCACHE> old_cached = this.items.get(eleast.getValue());
    this.cacheRemove(eleast.getValue(), old_cached);
  }

  private void cacheEvictOldestItems(
    final BigInteger added_size,
    final BigInteger maximum)
  {
    assert added_size.compareTo(maximum) <= 0;

    /**
     * Because objects larger than the capacity cannot be inserted into the
     * map, at least one object must exist if <code>used</code> is nonzero.
     */

    for (;;) {
      final BigInteger current = this.used.add(added_size);
      if (current.compareTo(maximum) <= 0) {
        break;
      }
      this.cacheEvictOldest();
    }

    final BigInteger current = this.used.add(added_size);
    assert current.compareTo(maximum) <= 0;
  }

  private CachedValue<TCACHE> cacheGetActual(
    final K key)
    throws E,
      JCacheException
  {
    if (this.cacheIsCached(key)) {
      return this.cacheGetReplace(key);
    }

    return this.cacheGetAddingNew(key);
  }

  private CachedValue<TCACHE> cacheGetAddingNew(
    final K key)
    throws E,
      JCacheException
  {
    boolean failed = true;
    TCACHE new_value = null;

    this.cacheCheckOverflow();

    try {
      new_value = this.loader.cacheValueLoad(key);
      this.checkLoaderReturnForNull(key, new_value);

      final BigInteger size = this.loader.cacheValueSizeOf(new_value);
      this.eventObjectLoaded(key, new_value, size);

      if (size.compareTo(BigInteger.ONE) < 0) {
        throw JCacheException.errorObjectTooSmall(key, size);
      }

      final BigInteger maximum = this.config.getMaximumCapacity();
      if (size.compareTo(maximum) > 0) {
        throw JCacheException.errorObjectTooLarge(key, size, maximum);
      }

      this.cacheEvictOldestItems(size, maximum);

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

  @Override public TVIEW cacheGetLU(
    final K key)
    throws E,
      JCacheException
  {
    NullCheck.notNull(key, "Key");

    final CachedValue<TCACHE> cv = this.cacheGetActual(key);
    this.eventObjectRetrieved(key, cv);
    return cv.getValue();
  }

  private CachedValue<TCACHE> cacheGetReplace(
    final K key)
  {
    final CachedValue<TCACHE> v = this.items.get(key);
    this.time_items.remove(v.getTime());
    return this.cachePut(key, v.getValue(), v.getSize());
  }

  private void cacheIncrementGets()
  {
    this.gets = this.gets.add(BigInteger.ONE);
  }

  @Override public boolean cacheIsCached(
    final K key)
  {
    return this.items.containsKey(NullCheck.notNull(key, "Key"));
  }

  @Override public BigInteger cacheItemCount()
  {
    return BigInteger.valueOf(this.items.size());
  }

  private CachedValue<TCACHE> cachePut(
    final K key,
    final TCACHE new_value,
    final BigInteger size)
  {
    this.cacheIncrementGets();
    final CachedValue<TCACHE> cv =
      new CachedValue<TCACHE>(new_value, this.gets, size);
    this.items.put(key, cv);
    this.time_items.put(this.gets, key);
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
    this.time_items.remove(existing.getTime());
    this.items.remove(key);
    this.used = this.used.subtract(existing.getSize());
  }

  @Override public BigInteger cacheSize()
  {
    return this.used;
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

  @Override public LRUCacheConfig lruCacheConfiguration()
  {
    return this.config;
  }

  @Override public void lruCacheSetConfiguration(
    final LRUCacheConfig c)
  {
    this.config = NullCheck.notNull(c, "Configuration");
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
