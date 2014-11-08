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

package com.io7m.jcache.tests;

import java.math.BigInteger;

import org.junit.Assert;
import org.junit.Test;

import com.io7m.jcache.JCacheEventsType;
import com.io7m.jcache.JCacheException;
import com.io7m.jcache.JCacheException.JCacheExceptionLoaderReturnedNull;
import com.io7m.jcache.JCacheException.JCacheExceptionObjectTooLarge;
import com.io7m.jcache.JCacheException.JCacheExceptionObjectTooSmall;
import com.io7m.jcache.JCacheLoaderType;
import com.io7m.jcache.LRUCacheConfig;
import com.io7m.jcache.LRUCacheTrivial;
import com.io7m.jcache.tests.LUCacheLoaderFaultInjectable.Failure;
import com.io7m.jfunctional.Pair;
import com.io7m.jnull.NullCheckException;

@SuppressWarnings("static-method") public final class LRUCacheTrivialTest
{
  private
    <K, TVIEW, TCACHE extends TVIEW>
    Pair<LUCacheLoaderFaultInjectable<K, TCACHE>, LRUCacheTrivial<K, TVIEW, TCACHE, Failure>>
    newCache(
      final long capacity)
  {
    final LRUCacheConfig config =
      LRUCacheConfig
        .empty()
        .withMaximumCapacity(BigInteger.valueOf(capacity));
    final LUCacheLoaderFaultInjectable<K, TCACHE> loader =
      new LUCacheLoaderFaultInjectable<K, TCACHE>();
    final LRUCacheTrivial<K, TVIEW, TCACHE, Failure> cache =
      LRUCacheTrivial.newCache(loader, config);
    return Pair.pair(loader, cache);
  }

  /**
   * Clearing a cache deletes all of the items.
   *
   * @throws JCacheException
   */

  @SuppressWarnings("boxing") @Test public void testDelete()
    throws Failure,
      JCacheException
  {
    final Pair<LUCacheLoaderFaultInjectable<String, Long>, LRUCacheTrivial<String, Long, Long, Failure>> pair =
      this.newCache(8L);

    final EventCount<String, Long> ec = new EventCount<String, Long>();
    pair.getRight().cacheEventsSubscribe(ec);

    for (long i = 0; i < 8; ++i) {
      pair.getLeft().setFailure(false);
      pair.getLeft().setLoadedValue(i);
      pair.getLeft().setLoadedValueSize(BigInteger.valueOf(1));

      pair.getRight().cacheGetLU("k" + i);
      Assert.assertTrue(pair.getRight().cacheIsCached("k" + i));
      Assert.assertEquals(BigInteger.valueOf(i + 1), pair
        .getRight()
        .cacheItemCount());
      Assert.assertEquals(BigInteger.valueOf(i + 1), pair
        .getRight()
        .cacheSize());
    }

    pair.getRight().cacheDelete();
    Assert.assertEquals(BigInteger.ZERO, pair.getRight().cacheItemCount());
    Assert.assertEquals(BigInteger.ZERO, pair.getRight().cacheSize());

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
      JCacheException
  {
    final Pair<LUCacheLoaderFaultInjectable<String, Long>, LRUCacheTrivial<String, Long, Long, Failure>> pair =
      this.newCache(2L);

    final EventLog<String, Long> ev = new EventLog<String, Long>();
    pair.getRight().cacheEventsSubscribe(ev);

    pair.getLeft().setFailure(false);
    pair.getLeft().setLoadedValue(0L);
    pair.getLeft().setLoadedValueSize(BigInteger.valueOf(1));

    ev.reset();
    pair.getRight().cacheGetLU("key0");
    Assert.assertTrue(ev.loaded);
    Assert.assertEquals("key0", ev.loaded_key);
    Assert.assertEquals(Long.valueOf(0L), ev.loaded_value);
    Assert.assertEquals(BigInteger.ONE, ev.loaded_size);
    Assert.assertTrue(ev.retrieved);
    Assert.assertEquals("key0", ev.retrieved_key);
    Assert.assertEquals(Long.valueOf(0L), ev.retrieved_value);
    Assert.assertEquals(BigInteger.ONE, ev.retrieved_size);

    pair.getLeft().setFailure(false);
    pair.getLeft().setLoadedValue(1L);
    pair.getLeft().setLoadedValueSize(BigInteger.valueOf(1));

    ev.reset();
    pair.getRight().cacheGetLU("key1");
    Assert.assertTrue(ev.loaded);
    Assert.assertEquals("key1", ev.loaded_key);
    Assert.assertEquals(Long.valueOf(1L), ev.loaded_value);
    Assert.assertEquals(BigInteger.ONE, ev.loaded_size);
    Assert.assertTrue(ev.retrieved);
    Assert.assertEquals("key1", ev.retrieved_key);
    Assert.assertEquals(Long.valueOf(1L), ev.retrieved_value);
    Assert.assertEquals(BigInteger.ONE, ev.retrieved_size);

    for (long i = 2; i < 10; ++i) {
      pair.getLeft().setFailure(false);
      pair.getLeft().setLoadedValue(i);
      pair.getLeft().setLoadedValueSize(BigInteger.valueOf(1));

      ev.reset();
      pair.getRight().cacheGetLU("key" + i);

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

    pair.getRight().cacheEventsUnsubscribe();

    for (long i = 2; i < 10; ++i) {
      pair.getLeft().setFailure(false);
      pair.getLeft().setLoadedValue(i);
      pair.getLeft().setLoadedValueSize(BigInteger.valueOf(1));

      ev.reset();
      pair.getRight().cacheGetLU("key" + i);

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
      JCacheException
  {
    final Pair<LUCacheLoaderFaultInjectable<String, Long>, LRUCacheTrivial<String, Long, Long, Failure>> pair =
      this.newCache(1L);

    final EventLog<String, Long> ev = new EventLog<String, Long>();
    pair.getRight().cacheEventsSubscribe(ev);

    ev.reset();
    pair.getLeft().setFailure(false);
    pair.getLeft().setLoadedValue(0L);
    pair.getLeft().setLoadedValueSize(BigInteger.valueOf(1));
    pair.getRight().cacheGetLU("key0");
    Assert.assertTrue(ev.loaded);
    Assert.assertEquals("key0", ev.loaded_key);
    Assert.assertEquals(Long.valueOf(0L), ev.loaded_value);
    Assert.assertEquals(BigInteger.ONE, ev.loaded_size);

    ev.reset();
    pair.getLeft().setFailure(false);
    pair.getLeft().setLoadedValue(1L);
    pair.getLeft().setLoadedValueSize(BigInteger.valueOf(1));
    pair.getLeft().setCloseFailure(true);
    pair.getRight().cacheGetLU("key1");
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
      JCacheException
  {
    final Pair<LUCacheLoaderFaultInjectable<String, Long>, LRUCacheTrivial<String, Long, Long, Failure>> pair =
      this.newCache(2L);

    pair.getRight().cacheEventsSubscribe(new EventThrown<String, Long>());
    pair.getLeft().setFailure(false);
    pair.getLeft().setLoadedValue(0L);
    pair.getLeft().setLoadedValueSize(BigInteger.valueOf(1));

    pair.getRight().cacheGetLU("key0");
    pair.getLeft().setFailure(false);
    pair.getLeft().setLoadedValue(1L);
    pair.getLeft().setLoadedValueSize(BigInteger.valueOf(1));

    pair.getRight().cacheGetLU("key1");

    for (long i = 2; i < 10; ++i) {
      pair.getLeft().setFailure(false);
      pair.getLeft().setLoadedValue(i);
      pair.getLeft().setLoadedValueSize(BigInteger.valueOf(1));

      pair.getRight().cacheGetLU("key" + i);
    }

    for (long i = 2; i < 10; ++i) {
      pair.getLeft().setFailure(false);
      pair.getLeft().setCloseFailure(true);
      pair.getLeft().setLoadedValue(i);
      pair.getLeft().setLoadedValueSize(BigInteger.valueOf(1));

      pair.getRight().cacheGetLU("key" + i);
    }
  }

  /**
   * Trying to subscribe with TestUtilities.actuallyNull() fails.
   *
   * @throws JCacheException
   */

  @Test(expected = NullCheckException.class) public void testEventsNull()
    throws JCacheException
  {
    final Pair<LUCacheLoaderFaultInjectable<String, Long>, LRUCacheTrivial<String, Long, Long, Failure>> pair =
      this.newCache(2L);
    pair.getRight().cacheEventsSubscribe(
      (JCacheEventsType<String, Long>) TestUtilities.actuallyNull());
  }

  /**
   * Caching items evicts the oldest items first.
   *
   * @throws JCacheException
   */

  @SuppressWarnings("boxing") @Test public void testEviction()
    throws Failure,
      JCacheException
  {
    final Pair<LUCacheLoaderFaultInjectable<String, Long>, LRUCacheTrivial<String, Long, Long, Failure>> pair =
      this.newCache(2L);

    pair.getLeft().setFailure(false);
    pair.getLeft().setLoadedValue(0L);
    pair.getLeft().setLoadedValueSize(BigInteger.valueOf(1));

    pair.getRight().cacheGetLU("key0");
    pair.getRight().cacheGetLU("key1");
    Assert.assertTrue(pair.getRight().cacheIsCached("key0"));
    Assert.assertTrue(pair.getRight().cacheIsCached("key1"));
    Assert.assertEquals(BigInteger.valueOf(2), pair
      .getRight()
      .cacheItemCount());
    Assert.assertEquals(BigInteger.valueOf(2), pair.getRight().cacheSize());

    for (long i = 2; i < 10; ++i) {
      pair.getLeft().setFailure(false);
      pair.getLeft().setLoadedValue(i);
      pair.getLeft().setLoadedValueSize(BigInteger.valueOf(1));

      Assert.assertEquals(BigInteger.valueOf(2), pair
        .getRight()
        .cacheItemCount());
      Assert.assertEquals(BigInteger.valueOf(2), pair.getRight().cacheSize());

      final Long r = pair.getRight().cacheGetLU("key" + i);
      for (long k = 0; k < (i - 2); ++k) {
        Assert.assertFalse(pair.getRight().cacheIsCached("key" + k));
      }
      final Long q = pair.getRight().cacheGetLU("key" + i);
      Assert.assertEquals(r, q);

      Assert.assertTrue(pair.getRight().cacheIsCached("key" + (i - 1)));
      Assert.assertTrue(pair.getRight().cacheIsCached("key" + i));
      Assert.assertEquals(BigInteger.valueOf(2), pair
        .getRight()
        .cacheItemCount());
      Assert.assertEquals(BigInteger.valueOf(2), pair.getRight().cacheSize());
    }
  }

  /**
   * Caching items evicts the oldest items first after resizing.
   *
   * @throws JCacheException
   */

  @SuppressWarnings("boxing") @Test public void testEvictionResized_0()
    throws Failure,
      JCacheException
  {
    final Pair<LUCacheLoaderFaultInjectable<String, Long>, LRUCacheTrivial<String, Long, Long, Failure>> pair =
      this.newCache(100L);

    final LUCacheLoaderFaultInjectable<String, Long> faults = pair.getLeft();
    faults.setFailure(false);
    faults.setLoadedValue(0L);
    faults.setLoadedValueSize(BigInteger.valueOf(1));

    final LRUCacheTrivial<String, Long, Long, Failure> cache =
      pair.getRight();
    for (long i = 0; i < 100; ++i) {
      faults.setFailure(false);
      faults.setLoadedValue(i);
      faults.setLoadedValueSize(BigInteger.valueOf(1));
      cache.cacheGetLU("key" + i);
      Assert.assertEquals(i + 1, cache.cacheSize().intValue());
    }

    Assert.assertEquals(100, cache.cacheSize().intValue());

    final LRUCacheConfig config =
      LRUCacheConfig.empty().withMaximumCapacity(BigInteger.valueOf(10));
    cache.cacheSetConfiguration(config);

    faults.setFailure(false);
    faults.setLoadedValue(101L);
    faults.setLoadedValueSize(BigInteger.valueOf(1));
    cache.cacheGetLU("key" + 101);

    Assert.assertEquals(10, cache.cacheSize().intValue());
  }

  /**
   * Check that eviction is correct when the cache configuration is changed
   * and an object exists in the cache with a size greater than the new
   * maximum.
   *
   * @throws JCacheException
   */

  @SuppressWarnings("boxing") @Test public void testEvictionResized_1()
    throws Failure,
      JCacheException
  {
    final LRUCacheConfig config =
      LRUCacheConfig.empty().withMaximumCapacity(BigInteger.valueOf(10L));

    final LUCacheLoaderFaultInjectable<Long, String> loader =
      new LUCacheLoaderFaultInjectable<Long, String>();
    final LRUCacheTrivial<Long, String, String, Failure> cache =
      LRUCacheTrivial.newCache(loader, config);

    loader.setLoadedValueSize(BigInteger.valueOf("hellohello".length()));
    loader.setLoadedValue("hellohello");

    cache.cacheGetLU(Long.valueOf(0));

    Assert.assertEquals(10, cache.cacheSize().intValue());
    Assert.assertTrue(cache.cacheIsCached(Long.valueOf(0)));

    final LRUCacheConfig config_after =
      LRUCacheConfig.empty().withMaximumCapacity(BigInteger.valueOf(5L));

    cache.cacheSetConfiguration(config_after);

    loader.setLoadedValueSize(BigInteger.valueOf("hello".length()));
    loader.setLoadedValue("hello");
    cache.cacheGetLU(Long.valueOf(1));

    Assert.assertEquals(5, cache.cacheSize().intValue());
    Assert.assertTrue(cache.cacheIsCached(Long.valueOf(1)));
    Assert.assertFalse(cache.cacheIsCached(Long.valueOf(0)));
  }

  /**
   * A cache of size 1 can hold one object of size 1.
   *
   * @throws JCacheException
   */

  @SuppressWarnings("boxing") @Test public void testEvictSize()
    throws Failure,
      JCacheException
  {
    final Pair<LUCacheLoaderFaultInjectable<String, Long>, LRUCacheTrivial<String, Long, Long, Failure>> pair =
      this.newCache(1L);

    final EventLog<String, Long> ev = new EventLog<String, Long>();
    pair.getRight().cacheEventsSubscribe(ev);

    ev.reset();
    pair.getLeft().setFailure(false);
    pair.getLeft().setLoadedValue(0L);
    pair.getLeft().setLoadedValueSize(BigInteger.valueOf(1));
    pair.getRight().cacheGetLU("key0");
    Assert.assertTrue(ev.loaded);
    Assert.assertEquals("key0", ev.loaded_key);
    Assert.assertEquals(Long.valueOf(0L), ev.loaded_value);
    Assert.assertEquals(BigInteger.ONE, ev.loaded_size);
    Assert.assertTrue(ev.retrieved);
    Assert.assertEquals("key0", ev.retrieved_key);
    Assert.assertEquals(Long.valueOf(0L), ev.retrieved_value);
    Assert.assertEquals(BigInteger.ONE, ev.retrieved_size);

    ev.reset();
    pair.getLeft().setFailure(false);
    pair.getLeft().setLoadedValue(1L);
    pair.getLeft().setLoadedValueSize(BigInteger.valueOf(1));
    pair.getRight().cacheGetLU("key1");
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
   * Checking if a TestUtilities.actuallyNull() key is cached fails.
   */

  @Test(expected = NullCheckException.class) public void testIsCachedNull()
  {
    LRUCacheTrivial<Long, Long, Long, Failure> cache =
      TestUtilities.actuallyNull();

    try {
      final LRUCacheConfig config = LRUCacheConfig.empty();
      cache =
        LRUCacheTrivial.newCache(
          new LUCacheLoaderFaultInjectable<Long, Long>(),
          config);
    } catch (final Throwable x) {
      Assert.fail(x.getMessage());
    }

    assert cache != TestUtilities.actuallyNull();
    cache.cacheIsCached((Long) TestUtilities.actuallyNull());
  }

  /**
   * Failing to load an item is signalled.
   */

  @Test(expected = Failure.class) public void testLoadFailure()
    throws Failure,
      JCacheException
  {
    final Pair<LUCacheLoaderFaultInjectable<Long, Long>, LRUCacheTrivial<Long, Long, Long, Failure>> pair =
      this.newCache(32L);

    pair.getLeft().setFailure(true);
    pair.getLeft().setLoadedValue(Long.valueOf(23));
    pair.getLeft().setLoadedValueSize(BigInteger.valueOf(4));
    pair.getRight().cacheGetLU(Long.valueOf(23));
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
        JCacheException
  {
    final Pair<LUCacheLoaderFaultInjectable<Long, Long>, LRUCacheTrivial<Long, Long, Long, Failure>> pair =
      this.newCache(32L);

    pair.getLeft().setFailure(false);
    pair.getLeft().setLoadedValue(Long.valueOf(1));
    pair.getLeft().setLoadedValueSize(BigInteger.valueOf(33L));
    pair.getRight().cacheGetLU(Long.valueOf(23));
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
        JCacheException
  {
    final Pair<LUCacheLoaderFaultInjectable<Long, Long>, LRUCacheTrivial<Long, Long, Long, Failure>> pair =
      this.newCache(32L);

    pair.getLeft().setFailure(false);
    pair.getLeft().setLoadedValue(Long.valueOf(1));
    pair.getLeft().setLoadedValueSize(BigInteger.valueOf(-1));
    pair.getRight().cacheGetLU(Long.valueOf(23));
  }

  /**
   * A loader returning <code>TestUtilities.actuallyNull()</code> is a cache
   * error.
   *
   * @throws JCacheException
   */

  @Test(expected = JCacheExceptionLoaderReturnedNull.class) public
    void
    testLoadNull()
      throws Failure,
        JCacheException
  {
    final Pair<LUCacheLoaderFaultInjectable<Long, Long>, LRUCacheTrivial<Long, Long, Long, Failure>> pair =
      this.newCache(32L);

    pair.getLeft().setFailure(false);
    pair.getLeft().setLoadedValue((Long) TestUtilities.actuallyNull());
    pair.getLeft().setLoadedValueSize(BigInteger.valueOf(4));
    pair.getRight().cacheGetLU(Long.valueOf(23));
  }

  /**
   * Passing a null configuration is an error.
   *
   * @throws JCacheException
   */

  @Test(expected = NullCheckException.class) public
    void
    testNullConfiguration()
      throws JCacheException
  {
    final Pair<LUCacheLoaderFaultInjectable<Long, Long>, LRUCacheTrivial<Long, Long, Long, Failure>> pair =
      this.newCache(32L);

    pair.getRight().cacheSetConfiguration(
      (LRUCacheConfig) TestUtilities.actuallyNull());
  }

  /**
   * Creating a cache succeeds.
   */

  @Test public void testNew()
  {
    final LRUCacheConfig config = LRUCacheConfig.empty();
    final LRUCacheTrivial<Long, Long, Long, Failure> cache =
      LRUCacheTrivial.newCache(
        new LUCacheLoaderFaultInjectable<Long, Long>(),
        config);
    Assert.assertEquals(BigInteger.ZERO, cache.cacheItemCount());
    Assert.assertEquals(BigInteger.ZERO, cache.cacheSize());
    Assert.assertEquals(config, cache.cacheGetConfiguration());

    for (long e = 2; e <= 32; e *= 2) {
      Assert.assertFalse(cache.cacheIsCached(Long.valueOf((long) Math.pow(
        2,
        32))));
    }
  }

  /**
   * Creating a cache with a TestUtilities.actuallyNull() loader fails.
   */

  @Test(expected = NullCheckException.class) public void testNullConfig()
  {
    LRUCacheTrivial.newCache(
      new LUCacheLoaderFaultInjectable<Long, Long>(),
      (LRUCacheConfig) TestUtilities.actuallyNull());
  }

  /**
   * Creating a cache with a TestUtilities.actuallyNull() loader fails.
   */

  @SuppressWarnings("unchecked") @Test(expected = NullCheckException.class) public
    void
    testNullLoader()
  {
    final LRUCacheConfig config = LRUCacheConfig.empty();
    LRUCacheTrivial.newCache(
      (JCacheLoaderType<Object, Object, JCacheException>) TestUtilities
        .actuallyNull(),
      config);
  }

  /**
   * Basic toString tests.
   */

  @SuppressWarnings("boxing") @Test public void testToString()
    throws Failure,
      JCacheException
  {
    final Pair<LUCacheLoaderFaultInjectable<String, Long>, LRUCacheTrivial<String, Long, Long, Failure>> pair =
      this.newCache(32L);

    final String s0 = pair.getRight().toString();

    pair.getLeft().setFailure(false);
    pair.getLeft().setLoadedValue(0L);
    pair.getLeft().setLoadedValueSize(BigInteger.valueOf(1));
    pair.getRight().cacheGetLU("key0");

    final String s1 = pair.getRight().toString();

    Assert.assertFalse(s0.equals(s1));
  }
}
