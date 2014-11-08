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

import com.io7m.jfunctional.FunctionType;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;
import com.io7m.junreachable.UnreachableCodeException;

/**
 * A trivial implementation of a borrowing LRU cache; the oldest
 * non-<i>borrowed</i> objects are evicted from the cache first.
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

public final class BLUCacheTrivial<K, TVIEW, TCACHE extends TVIEW, E extends Throwable> implements
  BLUCacheType<K, TVIEW, TCACHE, E>
{
  private static final class CachedValue<V>
  {
    private final BigInteger size;
    private final V          value;

    CachedValue(
      final V in_value,
      final BigInteger in_size)
    {
      this.value = in_value;
      this.size = in_size;
    }

    public BigInteger getSize()
    {
      return this.size;
    }

    public V getValue()
    {
      return this.value;
    }
  }

  private static final class ExtendedKey<K>
  {
    private final K          key;
    private final BigInteger serial;

    ExtendedKey(
      final K in_key,
      final BigInteger in_serial)
    {
      this.key = in_key;
      this.serial = in_serial;
    }

    @Override public boolean equals(
      final @Nullable Object obj)
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

    K getKey()
    {
      return this.key;
    }

    BigInteger getSerial()
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

  private final class Receipt implements BLUCacheReceiptType<K, TVIEW>
  {
    private final ExtendedKey<K> key;
    private final BigInteger     size;
    private boolean              valid;
    private final TCACHE         value;

    Receipt(
      final ExtendedKey<K> in_key,
      final TCACHE in_value,
      final BigInteger in_size)
    {
      this.key = in_key;
      this.value = in_value;
      this.size = in_size;
      this.valid = true;
    }

    @Override public boolean equals(
      final @Nullable Object obj)
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

    ExtendedKey<K> getKeyExtended()
    {
      return this.key;
    }

    BigInteger getSize()
    {
      return this.size;
    }

    @Override public TVIEW getValue()
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
    BLUCacheTrivial<K, TVIEW, TCACHE, E>
    newCache(
      final JCacheLoaderType<K, TCACHE, E> loader,
      final BLUCacheConfig config)
  {
    return new BLUCacheTrivial<K, TVIEW, TCACHE, E>(loader, config);
  }

  private BLUCacheConfig                                 config;
  private @Nullable JCacheEventsType<K, TCACHE>          events;
  private BigInteger                                     gets;
  private final Map<ExtendedKey<K>, CachedValue<TCACHE>> items;
  private final Map<K, NavigableSet<BigInteger>>         items_available;
  private final Map<K, NavigableSet<BigInteger>>         items_borrowed;
  private final NavigableMap<BigInteger, ExtendedKey<K>> items_timed;
  private final JCacheLoaderType<K, TCACHE, E>           loader;
  private BigInteger                                     used;

  private BLUCacheTrivial(
    final JCacheLoaderType<K, TCACHE, E> in_loader,
    final BLUCacheConfig in_config)
  {
    this.loader = NullCheck.notNull(in_loader, "Loader");
    this.config = NullCheck.notNull(in_config, "Configuration");

    this.events = null;
    this.items = new HashMap<ExtendedKey<K>, CachedValue<TCACHE>>();
    this.items_borrowed = new HashMap<K, NavigableSet<BigInteger>>();
    this.items_available = new HashMap<K, NavigableSet<BigInteger>>();
    this.items_timed = new TreeMap<BigInteger, ExtendedKey<K>>();

    this.gets = BigInteger.ZERO;
    this.used = BigInteger.ZERO;
  }

  @Override public Receipt bluCacheGet(
    final K key)
    throws E,
      JCacheException
  {
    NullCheck.notNull(key, "Key");

    final Receipt r = this.cacheGetActual(key);
    this.eventObjectRetrieved(key, r);
    return r;
  }

  private void cacheCheckBorrowingLimit(
    final K key)
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
      final CachedValue<TCACHE> v = this.items.get(k);
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

        final CachedValue<TCACHE> existing = this.items.get(key);
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
    final BigInteger added_size,
    final BigInteger maximum)
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

  private Receipt cacheGetActual(
    final K key)
    throws E,
      JCacheException
  {
    if (this.cacheIsAvailable(key)) {
      return this.cachePutExistingAvailable(key);
    }

    return this.cacheGetNew(key);
  }

  @Override public BLUCacheConfig cacheGetConfiguration()
  {
    return this.config;
  }

  private Receipt cacheGetNew(
    final K key)
    throws E,
      JCacheException
  {
    assert this.items_available.containsKey(key) == false;

    boolean failed = true;
    TCACHE new_value = null;

    this.cacheCheckBorrowingLimit(key);
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
    final K key)
  {
    NullCheck.notNull(key, "Key");
    return this.items_available.containsKey(key);
  }

  @Override public boolean cacheIsBorrowed(
    final K key)
  {
    NullCheck.notNull(key, "Key");
    return this.items_borrowed.containsKey(key);
  }

  @Override public boolean cacheIsCached(
    final K key)
  {
    NullCheck.notNull(key, "Key");
    return this.items_available.containsKey(key)
      || this.items_borrowed.containsKey(key);
  }

  @Override public BigInteger cacheItemCount()
  {
    return BigInteger.valueOf(this.items.size());
  }

  private void cacheKeyTimeTouch(
    final BigInteger new_time,
    final ExtendedKey<K> key)
  {
    assert this.items_timed.containsKey(new_time) == false;
    this.items_timed.remove(key.getSerial());
    this.items_timed.put(new_time, key);
  }

  private void cacheMarkAvailable(
    final K k,
    final BigInteger s)
  {
    assert this.items_borrowed.containsKey(k);
    final NavigableSet<BigInteger> serials = this.items_borrowed.get(k);
    assert serials.contains(s);

    MapSet.mapSetRemove(this.items_borrowed, k, s);
    MapSet.mapSetAdd(
      this.items_available,
      new FunctionType<Unit, NavigableSet<BigInteger>>() {
        @Override public NavigableSet<BigInteger> call(
          final Unit x)
        {
          return new TreeSet<BigInteger>();
        }
      },
      k,
      s);
  }

  private void cacheMarkBorrowed(
    final ExtendedKey<K> key)
  {
    if (this.items_borrowed.containsKey(key.getKey())) {
      final NavigableSet<BigInteger> serials =
        this.items_borrowed.get(key.getKey());
      assert serials.contains(key.getSerial()) == false;
    }

    MapSet.mapSetRemove(this.items_available, key.getKey(), key.getSerial());
    MapSet.mapSetAdd(
      this.items_borrowed,
      new FunctionType<Unit, NavigableSet<BigInteger>>() {
        @Override public NavigableSet<BigInteger> call(
          final Unit x)
        {
          return new TreeSet<BigInteger>();
        }
      },
      key.getKey(),
      key.getSerial());
  }

  private Receipt cachePut(
    final ExtendedKey<K> ext_key,
    final TCACHE new_value,
    final BigInteger size)
  {
    final CachedValue<TCACHE> cv = new CachedValue<TCACHE>(new_value, size);
    this.items.put(ext_key, cv);
    this.cacheMarkBorrowed(ext_key);
    this.cacheKeyTimeTouch(this.gets, ext_key);
    return new Receipt(ext_key, cv.getValue(), cv.getSize());
  }

  private Receipt cachePutAddNew(
    final K key,
    final TCACHE new_value,
    final BigInteger size)
  {
    this.cacheSizeIncrease(size);
    this.cacheIncrementGets();
    final ExtendedKey<K> ext_key = new ExtendedKey<K>(key, this.gets);
    return this.cachePut(ext_key, new_value, size);
  }

  private Receipt cachePutExistingAvailable(
    final K key)
  {
    assert this.items_available.containsKey(key);

    final BigInteger first_available = this.items_available.get(key).first();
    final ExtendedKey<K> ext_key = new ExtendedKey<K>(key, first_available);

    assert this.items.containsKey(ext_key);
    final CachedValue<TCACHE> old_value = this.items.get(ext_key);

    final TCACHE v = old_value.getValue();
    final BigInteger s = old_value.getSize();
    this.cacheIncrementGets();
    final Receipt receipt = this.cachePut(ext_key, v, s);

    this.eventObjectRetrieved(key, receipt);
    return receipt;
  }

  void cacheReturnReceipt(
    final Receipt r)
  {
    NullCheck.notNull(r, "Receipt");

    if (r.isValid() == false) {
      throw new IllegalStateException(String.format(
        "Receipt %s is not valid",
        r));
    }

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

  @Override public void cacheSetConfiguration(
    final BLUCacheConfig c)
  {
    this.config = NullCheck.notNull(c, "Configuration");
  }

  @Override public BigInteger cacheSize()
  {
    return this.used;
  }

  private void cacheSizeDecrease(
    final BigInteger size)
  {
    this.used = this.used.subtract(size);
  }

  private void cacheSizeIncrease(
    final BigInteger size)
  {
    this.used = this.used.add(size);
  }

  private void cacheValueDelete(
    final ExtendedKey<K> key,
    final CachedValue<TCACHE> existing)
  {
    this.eventObjectEvicted(key.getKey(), existing);
    try {
      this.loader.cacheValueClose(existing.getValue());
    } catch (final Throwable x) {
      this.eventObjectCloseError(key.getKey(), existing, x);
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
    final Receipt receipt)
  {
    if (this.events != null) {
      try {
        this.events.cacheEventValueRetrieved(
          key,
          receipt.value,
          receipt.getSize());
      } catch (final Throwable _) {
        // Ignore
      }
    }
  }
}
