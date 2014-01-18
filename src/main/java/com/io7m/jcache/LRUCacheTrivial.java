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
    private final @Nonnull BigInteger size;
    private final @Nonnull BigInteger time;
    private final @Nonnull V          value;

    CachedValue(
      final @Nonnull V in_value,
      final @Nonnull BigInteger in_time,
      final @Nonnull BigInteger in_size)
    {
      this.value = in_value;
      this.time = in_time;
      this.size = in_size;
    }

    public BigInteger getSize()
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
      final @Nonnull JCacheLoader<K, V, E> loader,
      final @Nonnull LRUCacheConfig config)
      throws ConstraintError
  {
    return new LRUCacheTrivial<K, V, E>(loader, config);
  }

  private final @Nonnull LRUCacheConfig              config;
  private @CheckForNull JCacheEvents<K, V>           events;
  private @Nonnull BigInteger                        gets;
  private final @Nonnull Map<K, CachedValue<V>>      items;
  private final @Nonnull JCacheLoader<K, V, E>       loader;
  private final @Nonnull NavigableMap<BigInteger, K> time_items;
  private @Nonnull BigInteger                        used;

  private LRUCacheTrivial(
    final @Nonnull JCacheLoader<K, V, E> in_loader,
    final @Nonnull LRUCacheConfig in_config)
    throws ConstraintError
  {
    this.loader = Constraints.constrainNotNull(in_loader, "Loader");
    this.config = Constraints.constrainNotNull(in_config, "Configuration");
    this.items = new HashMap<K, CachedValue<V>>();
    this.time_items = new TreeMap<BigInteger, K>();
    this.used = BigInteger.ZERO;
    this.gets = BigInteger.ZERO;
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
    final @Nonnull JCacheEvents<K, V> e)
    throws ConstraintError
  {
    this.events = Constraints.constrainNotNull(e, "Events");
  }

  @Override public void cacheEventsUnsubscribe()
  {
    this.events = null;
  }

  private void cacheEvictOldest()
  {
    final Entry<BigInteger, K> eleast = this.time_items.firstEntry();
    assert eleast != null;
    final CachedValue<V> old_cached = this.items.get(eleast.getValue());
    this.cacheRemove(eleast.getValue(), old_cached);
  }

  private void cacheEvictOldestItems(
    final @Nonnull BigInteger added_size,
    final @Nonnull BigInteger maximum)
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

  private @Nonnull CachedValue<V> cacheGetActual(
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

  private @Nonnull CachedValue<V> cacheGetAddingNew(
    final @Nonnull K key)
    throws E,
      JCacheException,
      ConstraintError
  {
    boolean failed = true;
    V new_value = null;

    this.cacheCheckOverflow();

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

  @Override public @Nonnull V cacheGetLU(
    final @Nonnull K key)
    throws ConstraintError,
      E,
      JCacheException
  {
    Constraints.constrainNotNull(key, "Key");

    final CachedValue<V> cv = this.cacheGetActual(key);
    this.eventObjectRetrieved(key, cv);
    return cv.getValue();
  }

  private @Nonnull CachedValue<V> cacheGetReplace(
    final @Nonnull K key)
  {
    final CachedValue<V> v = this.items.get(key);
    this.time_items.remove(v.getTime());
    return this.cachePut(key, v.getValue(), v.getSize());
  }

  private void cacheIncrementGets()
  {
    this.gets = this.gets.add(BigInteger.ONE);
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

  private @Nonnull CachedValue<V> cachePut(
    final @Nonnull K key,
    final @Nonnull V new_value,
    final @Nonnull BigInteger size)
  {
    this.cacheIncrementGets();
    final CachedValue<V> cv = new CachedValue<V>(new_value, this.gets, size);
    this.items.put(key, cv);
    this.time_items.put(this.gets, key);
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
    this.time_items.remove(existing.getTime());
    this.items.remove(key);
    this.used = this.used.subtract(existing.getSize());
  }

  @Override public BigInteger cacheSize()
  {
    return this.used;
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

  @Override public @Nonnull LRUCacheConfig lruCacheConfiguration()
  {
    return this.config;
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
