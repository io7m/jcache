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
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import com.io7m.jaux.Constraints;
import com.io7m.jaux.Constraints.ConstraintError;
import com.io7m.jaux.UnreachableCodeException;
import com.io7m.jaux.functional.Function;
import com.io7m.jaux.functional.Unit;

/**
 * A trivial implementation of a borrowing LRU cache; the oldest
 * non-<i>borrowed</i> objects are evicted from the cache first.
 * 
 * @param <K>
 *          The type of keys
 * @param <V>
 *          The type of cached values
 * @param <E>
 *          The type of exceptions raised during loading
 */

public final class BLUCacheTrivial<K, V, E extends Throwable> implements
  BLUCache<K, V, E>
{
  @Immutable private static final class CachedValue<V>
  {
    private final @Nonnull BigInteger size;
    private final @Nonnull V          value;

    CachedValue(
      final @Nonnull V in_value,
      final @Nonnull BigInteger in_size)
    {
      this.value = in_value;
      this.size = in_size;
    }

    public BigInteger getSize()
    {
      return this.size;
    }

    public @Nonnull V getValue()
    {
      return this.value;
    }
  }

  @Immutable private static final class ExtendedKey<K>
  {
    private final @Nonnull K          key;
    private final @Nonnull BigInteger serial;

    ExtendedKey(
      final @Nonnull K in_key,
      final @Nonnull BigInteger in_serial)
    {
      this.key = in_key;
      this.serial = in_serial;
    }

    @Override public boolean equals(
      final Object obj)
    {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (this.getClass() != obj.getClass()) {
        return false;
      }
      final ExtendedKey<?> other = (ExtendedKey<?>) obj;
      return this.key.equals(other.key) && this.serial.equals(other.serial);
    }

    @Nonnull K getKey()
    {
      return this.key;
    }

    @Nonnull BigInteger getSerial()
    {
      return this.serial;
    }

    @Override public int hashCode()
    {
      final int prime = 31;
      int result = 1;
      result = (prime * result) + this.key.hashCode();
      result = (prime * result) + this.serial.hashCode();
      return result;
    }

    @Override public String toString()
    {
      final StringBuilder builder = new StringBuilder();
      builder.append("[ExtendedKey ");
      builder.append(this.key);
      builder.append(" ");
      builder.append(this.serial);
      builder.append("]");
      return builder.toString();
    }
  }

  private final class Receipt implements BLUCacheReceipt<K, V>
  {
    private final @Nonnull ExtendedKey<K> key;
    private final @Nonnull BigInteger     size;
    private boolean                       valid;
    private final @Nonnull V              value;

    Receipt(
      final @Nonnull ExtendedKey<K> in_key,
      final @Nonnull V in_value,
      final @Nonnull BigInteger in_size)
    {
      this.key = in_key;
      this.value = in_value;
      this.size = in_size;
      this.valid = true;
    }

    @Override public boolean equals(
      final Object obj)
    {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (this.getClass() != obj.getClass()) {
        return false;
      }
      @SuppressWarnings("unchecked") final Receipt other = (Receipt) obj;
      return (this.key.equals(other.key))
        && (this.size.equals(other.size))
        && (this.valid == other.valid)
        && (this.value.equals(other.value));
    }

    @Override public K getKey()
    {
      return this.key.getKey();
    }

    @Nonnull ExtendedKey<K> getKeyExtended()
    {
      return this.key;
    }

    @Nonnull BigInteger getSize()
    {
      return this.size;
    }

    @Override public V getValue()
    {
      return this.value;
    }

    @Override public int hashCode()
    {
      final int prime = 31;
      int result = 1;
      result = (prime * result) + this.key.hashCode();
      result = (prime * result) + this.size.hashCode();
      result = (prime * result) + (this.valid ? 1231 : 1237);
      result = (prime * result) + this.value.hashCode();
      return result;
    }

    void invalidate()
    {
      this.valid = false;
    }

    boolean isValid()
    {
      return this.valid;
    }

    @Override public void returnToCache()
      throws ConstraintError
    {
      BLUCacheTrivial.this.cacheReturnReceipt(this);
    }

    @Override public String toString()
    {
      final StringBuilder builder = new StringBuilder();
      builder.append("[Receipt ");
      builder.append(this.key);
      builder.append(" value=");
      builder.append(this.value);
      builder.append(" size=");
      builder.append(this.size);
      builder.append("]");
      return builder.toString();
    }
  }

  /**
   * Construct a new <tt>BRUCache</tt>.
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
    BLUCacheTrivial<K, V, E>
    newCache(
      final @Nonnull JCacheLoader<K, V, E> loader,
      final @Nonnull BLUCacheConfig config)
      throws ConstraintError
  {
    return new BLUCacheTrivial<K, V, E>(loader, config);
  }

  private final @Nonnull BLUCacheConfig                           config;
  private @CheckForNull JCacheEvents<K, V>                        events;
  private @Nonnull BigInteger                                     gets;
  private final @Nonnull Map<ExtendedKey<K>, CachedValue<V>>      items;
  private final @Nonnull Map<K, NavigableSet<BigInteger>>         items_available;
  private final @Nonnull Map<K, NavigableSet<BigInteger>>         items_borrowed;
  private final @Nonnull NavigableMap<BigInteger, ExtendedKey<K>> items_timed;
  private final @Nonnull JCacheLoader<K, V, E>                    loader;
  private @Nonnull BigInteger                                     used;

  private BLUCacheTrivial(
    final @Nonnull JCacheLoader<K, V, E> in_loader,
    final @Nonnull BLUCacheConfig in_config)
    throws ConstraintError
  {
    this.loader = Constraints.constrainNotNull(in_loader, "Loader");
    this.config = Constraints.constrainNotNull(in_config, "Configuration");

    this.events = null;
    this.items = new HashMap<ExtendedKey<K>, CachedValue<V>>();
    this.items_borrowed = new HashMap<K, NavigableSet<BigInteger>>();
    this.items_available = new HashMap<K, NavigableSet<BigInteger>>();
    this.items_timed = new TreeMap<BigInteger, ExtendedKey<K>>();

    this.gets = BigInteger.ZERO;
    this.used = BigInteger.ZERO;
  }

  @Override public Receipt bluCacheGet(
    final @Nonnull K key)
    throws ConstraintError,
      E,
      JCacheException
  {
    Constraints.constrainNotNull(key, "Key");

    final Receipt r = this.cacheGetActual(key);
    this.eventObjectRetrieved(key, r);
    return r;
  }

  private void cacheCheckBorrowingLimit(
    final @Nonnull K key)
    throws JCacheException
  {
    if (this.items_borrowed.containsKey(key)) {
      final NavigableSet<BigInteger> serials = this.items_borrowed.get(key);
      assert serials.size() < Integer.MAX_VALUE;

      final BigInteger next_size =
        BigInteger.valueOf(serials.size()).add(BigInteger.ONE);
      final BigInteger maximum = this.config.getMaximumBorrowsPerKey();
      if (next_size.compareTo(maximum) > 0) {
        throw JCacheException.tooManyBorrows(key);
      }
    }
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
    for (final ExtendedKey<K> k : this.items.keySet()) {
      final CachedValue<V> v = this.items.get(k);
      this.cacheValueDelete(k, v);
    }

    this.used = BigInteger.ZERO;
    this.gets = BigInteger.ZERO;
    this.items.clear();
    this.items_available.clear();
    this.items_borrowed.clear();
    this.items_timed.clear();
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
    assert this.items_timed.isEmpty() == false;
    assert this.items_available.isEmpty() == false;

    /**
     * Attempt to find the oldest available item.
     */

    final Iterator<BigInteger> iter = this.items_timed.keySet().iterator();
    while (iter.hasNext()) {
      final BigInteger time = iter.next();
      final ExtendedKey<K> key = this.items_timed.get(time);

      /**
       * If the value associated with the given extended key is available,
       * delete it.
       */

      if (this.items_available.containsKey(key.getKey())) {
        final NavigableSet<BigInteger> serials =
          this.items_available.get(key.getKey());
        assert serials.contains(key.getSerial());

        MapSet.mapSetRemove(
          this.items_available,
          key.getKey(),
          key.getSerial());

        final CachedValue<V> existing = this.items.get(key);
        this.cacheValueDelete(key, existing);
        this.cacheSizeDecrease(existing.getSize());
        this.items.remove(key);

        iter.remove();
        return;
      }
    }

    /**
     * There were available values, so one must have been removed.
     */

    throw new UnreachableCodeException();
  }

  private void cacheEvictOldestItems(
    final @Nonnull BigInteger added_size,
    final @Nonnull BigInteger maximum)
  {
    assert added_size.compareTo(maximum) <= 0;

    for (;;) {

      /**
       * If there are available items, then it must be possible to shrink the
       * cache to bring the total size closer to the maximum.
       */

      if (this.items_available.isEmpty() == false) {
        final BigInteger current = this.used.add(added_size);
        if (current.compareTo(maximum) <= 0) {
          break;
        }
        this.cacheEvictOldest();
      } else {
        break;
      }
    }
  }

  private @Nonnull Receipt cacheGetActual(
    final @Nonnull K key)
    throws ConstraintError,
      E,
      JCacheException
  {
    if (this.cacheIsAvailable(key)) {
      return this.cachePutExistingAvailable(key);
    }

    return this.cacheGetNew(key);
  }

  private @Nonnull Receipt cacheGetNew(
    final @Nonnull K key)
    throws E,
      JCacheException,
      ConstraintError
  {
    assert this.items_available.containsKey(key) == false;

    boolean failed = true;
    V new_value = null;

    this.cacheCheckBorrowingLimit(key);
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
      return this.cachePutAddNew(key, new_value, size);
    } finally {
      if (failed) {
        if (new_value != null) {
          this.loader.cacheValueClose(new_value);
        }
      }
    }
  }

  private void cacheIncrementGets()
  {
    this.gets = this.gets.add(BigInteger.ONE);
  }

  @Override public boolean cacheIsAvailable(
    final @Nonnull K key)
    throws ConstraintError
  {
    Constraints.constrainNotNull(key, "Key");
    return this.items_available.containsKey(key);
  }

  @Override public boolean cacheIsBorrowed(
    final @Nonnull K key)
    throws ConstraintError
  {
    Constraints.constrainNotNull(key, "Key");
    return this.items_borrowed.containsKey(key);
  }

  @Override public boolean cacheIsCached(
    final @Nonnull K key)
    throws ConstraintError
  {
    return this.items_available.containsKey(key)
      || this.items_borrowed.containsKey(key);
  }

  @Override public @Nonnull BigInteger cacheItemCount()
  {
    return BigInteger.valueOf(this.items.size());
  }

  private void cacheKeyTimeTouch(
    final @Nonnull BigInteger new_time,
    final @Nonnull ExtendedKey<K> key)
  {
    assert this.items_timed.containsKey(new_time) == false;
    this.items_timed.remove(key.getSerial());
    this.items_timed.put(new_time, key);
  }

  private void cacheMarkAvailable(
    final @Nonnull K k,
    final @Nonnull BigInteger s)
  {
    assert this.items_borrowed.containsKey(k);
    final NavigableSet<BigInteger> serials = this.items_borrowed.get(k);
    assert serials.contains(s);

    MapSet.mapSetRemove(this.items_borrowed, k, s);
    MapSet.mapSetAdd(
      this.items_available,
      new Function<Unit, NavigableSet<BigInteger>>() {
        @Override public NavigableSet<BigInteger> call(
          final @Nonnull Unit x)
        {
          return new TreeSet<BigInteger>();
        }
      },
      k,
      s);
  }

  private void cacheMarkBorrowed(
    final @Nonnull ExtendedKey<K> key)
  {
    if (this.items_borrowed.containsKey(key.getKey())) {
      final NavigableSet<BigInteger> serials =
        this.items_borrowed.get(key.getKey());
      assert serials.contains(key.getSerial()) == false;
    }

    MapSet.mapSetRemove(this.items_available, key.getKey(), key.getSerial());
    MapSet.mapSetAdd(
      this.items_borrowed,
      new Function<Unit, NavigableSet<BigInteger>>() {
        @Override public NavigableSet<BigInteger> call(
          final @Nonnull Unit x)
        {
          return new TreeSet<BigInteger>();
        }
      },
      key.getKey(),
      key.getSerial());
  }

  private @Nonnull Receipt cachePut(
    final @Nonnull ExtendedKey<K> ext_key,
    final @Nonnull V new_value,
    final @Nonnull BigInteger size)
  {
    final CachedValue<V> cv = new CachedValue<V>(new_value, size);
    this.items.put(ext_key, cv);
    this.cacheMarkBorrowed(ext_key);
    this.cacheKeyTimeTouch(this.gets, ext_key);
    return new Receipt(ext_key, cv.getValue(), cv.getSize());
  }

  private @Nonnull Receipt cachePutAddNew(
    final @Nonnull K key,
    final @Nonnull V new_value,
    final @Nonnull BigInteger size)
  {
    this.cacheSizeIncrease(size);
    this.cacheIncrementGets();
    final ExtendedKey<K> ext_key = new ExtendedKey<K>(key, this.gets);
    return this.cachePut(ext_key, new_value, size);
  }

  private @Nonnull Receipt cachePutExistingAvailable(
    final @Nonnull K key)
  {
    assert this.items_available.containsKey(key);

    final BigInteger first_available = this.items_available.get(key).first();
    final ExtendedKey<K> ext_key = new ExtendedKey<K>(key, first_available);

    assert this.items.containsKey(ext_key);
    final CachedValue<V> old_value = this.items.get(ext_key);

    final V v = old_value.getValue();
    final BigInteger s = old_value.getSize();
    this.cacheIncrementGets();
    final Receipt receipt = this.cachePut(ext_key, v, s);

    this.eventObjectRetrieved(key, receipt);
    return receipt;
  }

  void cacheReturnReceipt(
    final @Nonnull Receipt r)
    throws ConstraintError
  {
    Constraints.constrainNotNull(r, "Receipt");
    Constraints.constrainArbitrary(r.isValid(), "Receipt is valid");

    final ExtendedKey<K> ext = r.getKeyExtended();
    try {
      final K k = r.getKey();
      final BigInteger s = ext.getSerial();
      this.cacheMarkAvailable(k, s);

      this.cacheEvictOldestItems(
        BigInteger.ZERO,
        this.config.getMaximumCapacity());
    } finally {
      r.invalidate();
    }
  }

  @Override public @Nonnull BigInteger cacheSize()
  {
    return this.used;
  }

  private void cacheSizeDecrease(
    final BigInteger size)
  {
    this.used = this.used.subtract(size);
  }

  private void cacheSizeIncrease(
    final @Nonnull BigInteger size)
  {
    this.used = this.used.add(size);
  }

  private void cacheValueDelete(
    final ExtendedKey<K> key,
    final CachedValue<V> existing)
  {
    this.eventObjectEvicted(key.getKey(), existing);
    try {
      this.loader.cacheValueClose(existing.getValue());
    } catch (final Throwable x) {
      this.eventObjectCloseError(key.getKey(), existing, x);
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
    final @Nonnull Receipt receipt)
  {
    if (this.events != null) {
      try {
        this.events.cacheEventValueRetrieved(
          key,
          receipt.getValue(),
          receipt.getSize());
      } catch (final Throwable _) {
        // Ignore
      }
    }
  }
}
