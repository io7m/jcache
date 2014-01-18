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

import javax.annotation.Nonnull;

import org.junit.Assert;
import org.junit.Test;

import com.io7m.jaux.Constraints.ConstraintError;
import com.io7m.jaux.UnreachableCodeException;
import com.io7m.jaux.functional.Pair;
import com.io7m.jcache.JCacheException.JCacheExceptionLoaderReturnedNull;
import com.io7m.jcache.JCacheException.JCacheExceptionObjectTooLarge;
import com.io7m.jcache.JCacheException.JCacheExceptionObjectTooSmall;
import com.io7m.jcache.LUCacheLoaderFaultInjectable.Failure;

public final class LRUCacheTrivialTest
{
  @SuppressWarnings("static-method") private @Nonnull
    <K, V>
    Pair<LUCacheLoaderFaultInjectable<K, V>, LRUCacheTrivial<K, V, Failure>>
    newCache(
      final long capacity)
  {
    try {
      final LRUCacheConfig config =
        LRUCacheConfig.empty().withMaximumCapacity(
          BigInteger.valueOf(capacity));
      final LUCacheLoaderFaultInjectable<K, V> loader =
        new LUCacheLoaderFaultInjectable<K, V>();
      final LRUCacheTrivial<K, V, Failure> cache =
        LRUCacheTrivial.newCache(loader, config);
      return new Pair<LUCacheLoaderFaultInjectable<K, V>, LRUCacheTrivial<K, V, Failure>>(
        loader,
        cache);
    } catch (final ConstraintError x) {
      throw new UnreachableCodeException(x);
    }
  }

  /**
   * Clearing a cache deletes all of the items.
   * 
   * @throws JCacheException
   */

  @SuppressWarnings("boxing") @Test public void testDelete()
    throws Failure,
      ConstraintError,
      JCacheException
  {
    final Pair<LUCacheLoaderFaultInjectable<String, Long>, LRUCacheTrivial<String, Long, Failure>> pair =
      this.newCache(8L);

    final EventCount<String, Long> ec = new EventCount<String, Long>();
    pair.second.cacheEventsSubscribe(ec);

    for (long i = 0; i < 8; ++i) {
      pair.first.setFailure(false);
      pair.first.setLoadedValue(i);
      pair.first.setLoadedValueSize(BigInteger.valueOf(1));

      pair.second.cacheGetLU("k" + i);
      Assert.assertTrue(pair.second.cacheIsCached("k" + i));
      Assert.assertEquals(
        BigInteger.valueOf(i + 1),
        pair.second.cacheItemCount());
      Assert.assertEquals(BigInteger.valueOf(i + 1), pair.second.cacheSize());
    }

    pair.second.cacheDelete();
    Assert.assertEquals(BigInteger.ZERO, pair.second.cacheItemCount());
    Assert.assertEquals(BigInteger.ZERO, pair.second.cacheSize());

    Assert.assertEquals(8, ec.getEvictions());
    Assert.assertEquals(8, ec.getLoads());
    Assert.assertEquals(0, ec.getCloseErrors());
    Assert.assertEquals(8, ec.getRetrievals());
  }

  /**
   * Events are delivered.
   * 
   * @throws JCacheException
   */

  @SuppressWarnings("boxing") @Test public void testEvents()
    throws Failure,
      ConstraintError,
      JCacheException
  {
    final Pair<LUCacheLoaderFaultInjectable<String, Long>, LRUCacheTrivial<String, Long, Failure>> pair =
      this.newCache(2L);

    final EventLog<String, Long> ev = new EventLog<String, Long>();
    pair.second.cacheEventsSubscribe(ev);

    pair.first.setFailure(false);
    pair.first.setLoadedValue(0L);
    pair.first.setLoadedValueSize(BigInteger.valueOf(1));

    ev.reset();
    pair.second.cacheGetLU("key0");
    Assert.assertTrue(ev.loaded);
    Assert.assertEquals("key0", ev.loaded_key);
    Assert.assertEquals(Long.valueOf(0L), ev.loaded_value);
    Assert.assertEquals(BigInteger.ONE, ev.loaded_size);
    Assert.assertTrue(ev.retrieved);
    Assert.assertEquals("key0", ev.retrieved_key);
    Assert.assertEquals(Long.valueOf(0L), ev.retrieved_value);
    Assert.assertEquals(BigInteger.ONE, ev.retrieved_size);

    pair.first.setFailure(false);
    pair.first.setLoadedValue(1L);
    pair.first.setLoadedValueSize(BigInteger.valueOf(1));

    ev.reset();
    pair.second.cacheGetLU("key1");
    Assert.assertTrue(ev.loaded);
    Assert.assertEquals("key1", ev.loaded_key);
    Assert.assertEquals(Long.valueOf(1L), ev.loaded_value);
    Assert.assertEquals(BigInteger.ONE, ev.loaded_size);
    Assert.assertTrue(ev.retrieved);
    Assert.assertEquals("key1", ev.retrieved_key);
    Assert.assertEquals(Long.valueOf(1L), ev.retrieved_value);
    Assert.assertEquals(BigInteger.ONE, ev.retrieved_size);

    for (long i = 2; i < 10; ++i) {
      pair.first.setFailure(false);
      pair.first.setLoadedValue(i);
      pair.first.setLoadedValueSize(BigInteger.valueOf(1));

      ev.reset();
      pair.second.cacheGetLU("key" + i);

      Assert.assertTrue(ev.loaded);
      Assert.assertEquals("key" + i, ev.loaded_key);
      Assert.assertEquals(Long.valueOf(i), ev.loaded_value);
      Assert.assertEquals(BigInteger.ONE, ev.loaded_size);

      Assert.assertTrue(ev.retrieved);
      Assert.assertEquals("key" + i, ev.retrieved_key);
      Assert.assertEquals(Long.valueOf(i), ev.retrieved_value);
      Assert.assertEquals(BigInteger.ONE, ev.retrieved_size);

      Assert.assertTrue(ev.evicted);
      Assert.assertEquals("key" + (i - 2), ev.evicted_key);
      Assert.assertEquals(Long.valueOf(i - 2), ev.evicted_value);
      Assert.assertEquals(BigInteger.ONE, ev.evicted_size);
    }

    pair.second.cacheEventsUnsubscribe();

    for (long i = 2; i < 10; ++i) {
      pair.first.setFailure(false);
      pair.first.setLoadedValue(i);
      pair.first.setLoadedValueSize(BigInteger.valueOf(1));

      ev.reset();
      pair.second.cacheGetLU("key" + i);

      Assert.assertFalse(ev.loaded);
      Assert.assertFalse(ev.retrieved);
      Assert.assertFalse(ev.evicted);
    }
  }

  /**
   * Exceptions raised during closing are delivered.
   * 
   * @throws JCacheException
   */

  @SuppressWarnings("boxing") @Test public void testEventsCloseError()
    throws Failure,
      ConstraintError,
      JCacheException
  {
    final Pair<LUCacheLoaderFaultInjectable<String, Long>, LRUCacheTrivial<String, Long, Failure>> pair =
      this.newCache(1L);

    final EventLog<String, Long> ev = new EventLog<String, Long>();
    pair.second.cacheEventsSubscribe(ev);

    ev.reset();
    pair.first.setFailure(false);
    pair.first.setLoadedValue(0L);
    pair.first.setLoadedValueSize(BigInteger.valueOf(1));
    pair.second.cacheGetLU("key0");
    Assert.assertTrue(ev.loaded);
    Assert.assertEquals("key0", ev.loaded_key);
    Assert.assertEquals(Long.valueOf(0L), ev.loaded_value);
    Assert.assertEquals(BigInteger.ONE, ev.loaded_size);

    ev.reset();
    pair.first.setFailure(false);
    pair.first.setLoadedValue(1L);
    pair.first.setLoadedValueSize(BigInteger.valueOf(1));
    pair.first.setCloseFailure(true);
    pair.second.cacheGetLU("key1");
    Assert.assertTrue(ev.close_error);
    Assert.assertEquals("key0", ev.close_error_key);
    Assert.assertEquals(Long.valueOf(0L), ev.close_error_value);
    Assert.assertTrue(ev.loaded);
    Assert.assertEquals("key1", ev.loaded_key);
    Assert.assertEquals(Long.valueOf(1L), ev.loaded_value);
    Assert.assertEquals(BigInteger.ONE, ev.loaded_size);
  }

  /**
   * Exceptions are not propagated.
   * 
   * @throws JCacheException
   */

  @SuppressWarnings("boxing") @Test public void testEventsExceptions()
    throws Failure,
      ConstraintError,
      JCacheException
  {
    final Pair<LUCacheLoaderFaultInjectable<String, Long>, LRUCacheTrivial<String, Long, Failure>> pair =
      this.newCache(2L);

    pair.second.cacheEventsSubscribe(new EventThrown<String, Long>());
    pair.first.setFailure(false);
    pair.first.setLoadedValue(0L);
    pair.first.setLoadedValueSize(BigInteger.valueOf(1));

    pair.second.cacheGetLU("key0");
    pair.first.setFailure(false);
    pair.first.setLoadedValue(1L);
    pair.first.setLoadedValueSize(BigInteger.valueOf(1));

    pair.second.cacheGetLU("key1");

    for (long i = 2; i < 10; ++i) {
      pair.first.setFailure(false);
      pair.first.setLoadedValue(i);
      pair.first.setLoadedValueSize(BigInteger.valueOf(1));

      pair.second.cacheGetLU("key" + i);
    }

    for (long i = 2; i < 10; ++i) {
      pair.first.setFailure(false);
      pair.first.setCloseFailure(true);
      pair.first.setLoadedValue(i);
      pair.first.setLoadedValueSize(BigInteger.valueOf(1));

      pair.second.cacheGetLU("key" + i);
    }
  }

  /**
   * Trying to subscribe with null fails.
   * 
   * @throws JCacheException
   */

  @Test(expected = ConstraintError.class) public void testEventsNull()
    throws ConstraintError,
      JCacheException
  {
    final Pair<LUCacheLoaderFaultInjectable<String, Long>, LRUCacheTrivial<String, Long, Failure>> pair =
      this.newCache(2L);
    pair.second.cacheEventsSubscribe(null);
  }

  /**
   * Caching items evicts the oldest items first.
   * 
   * @throws JCacheException
   */

  @SuppressWarnings("boxing") @Test public void testEviction()
    throws Failure,
      ConstraintError,
      JCacheException
  {
    final Pair<LUCacheLoaderFaultInjectable<String, Long>, LRUCacheTrivial<String, Long, Failure>> pair =
      this.newCache(2L);

    pair.first.setFailure(false);
    pair.first.setLoadedValue(0L);
    pair.first.setLoadedValueSize(BigInteger.valueOf(1));

    pair.second.cacheGetLU("key0");
    pair.second.cacheGetLU("key1");
    Assert.assertTrue(pair.second.cacheIsCached("key0"));
    Assert.assertTrue(pair.second.cacheIsCached("key1"));
    Assert.assertEquals(BigInteger.valueOf(2), pair.second.cacheItemCount());
    Assert.assertEquals(BigInteger.valueOf(2), pair.second.cacheSize());

    for (long i = 2; i < 10; ++i) {
      pair.first.setFailure(false);
      pair.first.setLoadedValue(i);
      pair.first.setLoadedValueSize(BigInteger.valueOf(1));

      Assert
        .assertEquals(BigInteger.valueOf(2), pair.second.cacheItemCount());
      Assert.assertEquals(BigInteger.valueOf(2), pair.second.cacheSize());

      final Long r = pair.second.cacheGetLU("key" + i);
      for (long k = 0; k < (i - 2); ++k) {
        Assert.assertFalse(pair.second.cacheIsCached("key" + k));
      }
      final Long q = pair.second.cacheGetLU("key" + i);
      Assert.assertEquals(r, q);

      Assert.assertTrue(pair.second.cacheIsCached("key" + (i - 1)));
      Assert.assertTrue(pair.second.cacheIsCached("key" + i));
      Assert
        .assertEquals(BigInteger.valueOf(2), pair.second.cacheItemCount());
      Assert.assertEquals(BigInteger.valueOf(2), pair.second.cacheSize());
    }
  }

  /**
   * A cache of size 1 can hold one object of size 1.
   * 
   * @throws JCacheException
   */

  @SuppressWarnings("boxing") @Test public void testEvictSize()
    throws Failure,
      ConstraintError,
      JCacheException
  {
    final Pair<LUCacheLoaderFaultInjectable<String, Long>, LRUCacheTrivial<String, Long, Failure>> pair =
      this.newCache(1L);

    final EventLog<String, Long> ev = new EventLog<String, Long>();
    pair.second.cacheEventsSubscribe(ev);

    ev.reset();
    pair.first.setFailure(false);
    pair.first.setLoadedValue(0L);
    pair.first.setLoadedValueSize(BigInteger.valueOf(1));
    pair.second.cacheGetLU("key0");
    Assert.assertTrue(ev.loaded);
    Assert.assertEquals("key0", ev.loaded_key);
    Assert.assertEquals(Long.valueOf(0L), ev.loaded_value);
    Assert.assertEquals(BigInteger.ONE, ev.loaded_size);
    Assert.assertTrue(ev.retrieved);
    Assert.assertEquals("key0", ev.retrieved_key);
    Assert.assertEquals(Long.valueOf(0L), ev.retrieved_value);
    Assert.assertEquals(BigInteger.ONE, ev.retrieved_size);

    ev.reset();
    pair.first.setFailure(false);
    pair.first.setLoadedValue(1L);
    pair.first.setLoadedValueSize(BigInteger.valueOf(1));
    pair.second.cacheGetLU("key1");
    Assert.assertTrue(ev.loaded);
    Assert.assertEquals("key1", ev.loaded_key);
    Assert.assertEquals(Long.valueOf(1L), ev.loaded_value);
    Assert.assertEquals(BigInteger.ONE, ev.loaded_size);
    Assert.assertTrue(ev.retrieved);
    Assert.assertEquals("key1", ev.retrieved_key);
    Assert.assertEquals(Long.valueOf(1L), ev.retrieved_value);
    Assert.assertEquals(BigInteger.ONE, ev.retrieved_size);
  }

  /**
   * Checking if a null key is cached fails.
   */

  @SuppressWarnings("static-method") @Test(expected = ConstraintError.class) public
    void
    testIsCachedNull()
      throws ConstraintError
  {
    LRUCacheTrivial<Long, Long, Failure> cache = null;

    try {
      final LRUCacheConfig config = LRUCacheConfig.empty();
      cache =
        LRUCacheTrivial.newCache(
          new LUCacheLoaderFaultInjectable<Long, Long>(),
          config);
    } catch (final Throwable x) {
      Assert.fail(x.getMessage());
    }

    assert cache != null;
    cache.cacheIsCached(null);
  }

  /**
   * Failing to load an item is signalled.
   */

  @Test(expected = Failure.class) public void testLoadFailure()
    throws Failure,
      ConstraintError,
      JCacheException
  {
    final Pair<LUCacheLoaderFaultInjectable<Long, Long>, LRUCacheTrivial<Long, Long, Failure>> pair =
      this.newCache(32L);

    pair.first.setFailure(true);
    pair.first.setLoadedValue(Long.valueOf(23));
    pair.first.setLoadedValueSize(BigInteger.valueOf(4));
    pair.second.cacheGetLU(Long.valueOf(23));
  }

  /**
   * A loader returning an object that cannot fit in the cache is an error.
   * 
   * @throws JCacheException
   */

  @Test(expected = JCacheExceptionObjectTooLarge.class) public
    void
    testLoadHugeSize()
      throws Failure,
        ConstraintError,
        JCacheException
  {
    final Pair<LUCacheLoaderFaultInjectable<Long, Long>, LRUCacheTrivial<Long, Long, Failure>> pair =
      this.newCache(32L);

    pair.first.setFailure(false);
    pair.first.setLoadedValue(Long.valueOf(1));
    pair.first.setLoadedValueSize(BigInteger.valueOf(33L));
    pair.second.cacheGetLU(Long.valueOf(23));
  }

  /**
   * A loader returning a negative size is a cache error.
   * 
   * @throws JCacheException
   */

  @Test(expected = JCacheExceptionObjectTooSmall.class) public
    void
    testLoadNegativeSize()
      throws Failure,
        ConstraintError,
        JCacheException
  {
    final Pair<LUCacheLoaderFaultInjectable<Long, Long>, LRUCacheTrivial<Long, Long, Failure>> pair =
      this.newCache(32L);

    pair.first.setFailure(false);
    pair.first.setLoadedValue(Long.valueOf(1));
    pair.first.setLoadedValueSize(BigInteger.valueOf(-1));
    pair.second.cacheGetLU(Long.valueOf(23));
  }

  /**
   * A loader returning <code>null</code> is a cache error.
   * 
   * @throws JCacheException
   */

  @Test(expected = JCacheExceptionLoaderReturnedNull.class) public
    void
    testLoadNull()
      throws Failure,
        ConstraintError,
        JCacheException
  {
    final Pair<LUCacheLoaderFaultInjectable<Long, Long>, LRUCacheTrivial<Long, Long, Failure>> pair =
      this.newCache(32L);

    pair.first.setFailure(false);
    pair.first.setLoadedValue(null);
    pair.first.setLoadedValueSize(BigInteger.valueOf(4));
    pair.second.cacheGetLU(Long.valueOf(23));
  }

  /**
   * Creating a cache succeeds.
   */

  @SuppressWarnings("static-method") @Test public void testNew()
    throws ConstraintError
  {
    final LRUCacheConfig config = LRUCacheConfig.empty();
    final LRUCacheTrivial<Long, Long, Failure> cache =
      LRUCacheTrivial.newCache(
        new LUCacheLoaderFaultInjectable<Long, Long>(),
        config);
    Assert.assertEquals(BigInteger.ZERO, cache.cacheItemCount());
    Assert.assertEquals(BigInteger.ZERO, cache.cacheSize());
    Assert.assertEquals(config, cache.lruCacheConfiguration());

    for (long e = 2; e <= 32; e *= 2) {
      Assert.assertFalse(cache.cacheIsCached(Long.valueOf((long) Math.pow(
        2,
        32))));
    }
  }

  /**
   * Creating a cache with a null loader fails.
   */

  @SuppressWarnings("static-method") @Test(expected = ConstraintError.class) public
    void
    testNullConfig()
      throws ConstraintError
  {
    LRUCacheTrivial.newCache(
      new LUCacheLoaderFaultInjectable<Long, Long>(),
      null);
  }

  /**
   * Creating a cache with a null loader fails.
   */

  @SuppressWarnings("static-method") @Test(expected = ConstraintError.class) public
    void
    testNullLoader()
      throws ConstraintError
  {
    final LRUCacheConfig config = LRUCacheConfig.empty();
    LRUCacheTrivial.newCache(null, config);
  }

  /**
   * Basic toString tests.
   */

  @SuppressWarnings("boxing") @Test public void testToString()
    throws Failure,
      ConstraintError,
      JCacheException
  {
    final Pair<LUCacheLoaderFaultInjectable<String, Long>, LRUCacheTrivial<String, Long, Failure>> pair =
      this.newCache(32L);

    final String s0 = pair.second.toString();

    pair.first.setFailure(false);
    pair.first.setLoadedValue(0L);
    pair.first.setLoadedValueSize(BigInteger.valueOf(1));
    pair.second.cacheGetLU("key0");

    final String s1 = pair.second.toString();

    Assert.assertFalse(s0.equals(s1));
  }
}
