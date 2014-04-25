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
import com.io7m.jcache.PCacheConfig;
import com.io7m.jcache.PCacheConfig.BuilderType;
import com.io7m.jcache.PCacheTrivial;
import com.io7m.jcache.PCacheType;
import com.io7m.jcache.tests.LUCacheLoaderFaultInjectable.Failure;
import com.io7m.jfunctional.Pair;
import com.io7m.jnull.NullCheckException;
import com.io7m.junreachable.UnreachableCodeException;

@SuppressWarnings("static-method") public final class PCacheTrivialTest
{
  private PCacheType<Integer, Integer, Failure> newCache()
  {
    PCacheType<Integer, Integer, Failure> pc;
    PCacheConfig c;
    final BuilderType b = PCacheConfig.newBuilder();
    c = b.create();
    final LUCacheLoaderFaultInjectable<Integer, Integer> loader =
      new LUCacheLoaderFaultInjectable<Integer, Integer>();
    pc = PCacheTrivial.newCache(loader, c);
    return pc;
  }

  private
    Pair<PCacheType<String, Integer, Failure>, LUCacheLoaderFaultInjectable<String, Integer>>
    newCacheWithMaximumAge(
      final long age)
  {
    PCacheType<String, Integer, Failure> pc;
    PCacheConfig c;
    final BuilderType b = PCacheConfig.newBuilder();
    b.setMaximumAge(BigInteger.valueOf(age));
    b.setNoMaximumSize();
    c = b.create();
    final LUCacheLoaderFaultInjectable<String, Integer> loader =
      new LUCacheLoaderFaultInjectable<String, Integer>();
    pc = PCacheTrivial.newCache(loader, c);
    return Pair.pair(pc, loader);
  }

  private
    Pair<PCacheType<String, Integer, Failure>, LUCacheLoaderFaultInjectable<String, Integer>>
    newCacheWithMaximumSize(
      final long size)
  {
    PCacheType<String, Integer, Failure> pc;
    PCacheConfig c;
    final BuilderType b = PCacheConfig.newBuilder();
    b.setNoMaximumAge();
    b.setMaximumSize(BigInteger.valueOf(size));
    c = b.create();
    final LUCacheLoaderFaultInjectable<String, Integer> loader =
      new LUCacheLoaderFaultInjectable<String, Integer>();
    pc = PCacheTrivial.newCache(loader, c);
    return Pair.pair(pc, loader);
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
    final Pair<PCacheType<String, Integer, Failure>, LUCacheLoaderFaultInjectable<String, Integer>> pair =
      this.newCacheWithMaximumAge(2);

    final EventLog<String, Integer> ev = new EventLog<String, Integer>();
    pair.getLeft().cacheEventsSubscribe(ev);

    pair.getRight().setFailure(false);
    pair.getRight().setLoadedValue(0);
    pair.getRight().setLoadedValueSize(BigInteger.ONE);

    ev.reset();
    pair.getLeft().cachePeriodStart();
    pair.getLeft().cacheGetPeriodic("key0");
    Assert.assertTrue(ev.loaded);
    Assert.assertEquals("key0", ev.loaded_key);
    Assert.assertEquals(Integer.valueOf(0), ev.loaded_value);
    Assert.assertEquals(BigInteger.ONE, ev.loaded_size);
    Assert.assertTrue(ev.retrieved);
    Assert.assertEquals("key0", ev.retrieved_key);
    Assert.assertEquals(Integer.valueOf(0), ev.retrieved_value);
    Assert.assertEquals(BigInteger.ONE, ev.retrieved_size);

    pair.getRight().setFailure(false);
    pair.getRight().setLoadedValue(1);
    pair.getRight().setLoadedValueSize(BigInteger.ONE);

    ev.reset();
    pair.getLeft().cacheGetPeriodic("key1");
    Assert.assertTrue(ev.loaded);
    Assert.assertEquals("key1", ev.loaded_key);
    Assert.assertEquals(Integer.valueOf(1), ev.loaded_value);
    Assert.assertEquals(BigInteger.ONE, ev.loaded_size);
    Assert.assertTrue(ev.retrieved);
    Assert.assertEquals("key1", ev.retrieved_key);
    Assert.assertEquals(Integer.valueOf(1), ev.retrieved_value);
    Assert.assertEquals(BigInteger.ONE, ev.retrieved_size);

    for (int i = 2; i < 10; ++i) {
      pair.getRight().setFailure(false);
      pair.getRight().setLoadedValue(i);
      pair.getRight().setLoadedValueSize(BigInteger.ONE);

      ev.reset();
      pair.getLeft().cacheGetPeriodic("key" + i);

      Assert.assertTrue(ev.loaded);
      Assert.assertEquals("key" + i, ev.loaded_key);
      Assert.assertEquals(Integer.valueOf(i), ev.loaded_value);
      Assert.assertEquals(BigInteger.ONE, ev.loaded_size);

      Assert.assertTrue(ev.retrieved);
      Assert.assertEquals("key" + i, ev.retrieved_key);
      Assert.assertEquals(Integer.valueOf(i), ev.retrieved_value);
      Assert.assertEquals(BigInteger.ONE, ev.retrieved_size);

      Assert.assertFalse(ev.evicted);
    }

    pair.getLeft().cacheEventsUnsubscribe();

    for (int i = 2; i < 10; ++i) {
      pair.getRight().setFailure(false);
      pair.getRight().setLoadedValue(i);
      pair.getRight().setLoadedValueSize(BigInteger.ONE);

      ev.reset();
      pair.getLeft().cacheGetPeriodic("key" + i);

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
    final Pair<PCacheType<String, Integer, Failure>, LUCacheLoaderFaultInjectable<String, Integer>> pair =
      this.newCacheWithMaximumAge(1);

    final EventLog<String, Integer> ev = new EventLog<String, Integer>();
    pair.getLeft().cacheEventsSubscribe(ev);

    ev.reset();
    pair.getLeft().cachePeriodStart();
    pair.getRight().setFailure(false);
    pair.getRight().setLoadedValue(0);
    pair.getRight().setLoadedValueSize(BigInteger.ONE);
    pair.getLeft().cacheGetPeriodic("key0");
    Assert.assertTrue(ev.loaded);
    Assert.assertEquals("key0", ev.loaded_key);
    Assert.assertEquals(Integer.valueOf(0), ev.loaded_value);
    Assert.assertEquals(BigInteger.ONE, ev.loaded_size);

    ev.reset();
    pair.getRight().setFailure(false);
    pair.getRight().setLoadedValue(1);
    pair.getRight().setLoadedValueSize(BigInteger.ONE);
    pair.getRight().setCloseFailure(true);
    pair.getLeft().cachePeriodEnd();
    pair.getLeft().cacheDelete();

    Assert.assertTrue(ev.close_error);
    Assert.assertEquals("key0", ev.close_error_key);
    Assert.assertEquals(Integer.valueOf(0), ev.close_error_value);
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
    final Pair<PCacheType<String, Integer, Failure>, LUCacheLoaderFaultInjectable<String, Integer>> pair =
      this.newCacheWithMaximumAge(2);

    pair.getLeft().cachePeriodStart();
    pair.getLeft().cacheEventsSubscribe(new EventThrown<String, Integer>());
    pair.getRight().setFailure(false);
    pair.getRight().setLoadedValue(0);
    pair.getRight().setLoadedValueSize(BigInteger.ONE);

    pair.getLeft().cacheGetPeriodic("key0");
    pair.getRight().setFailure(false);
    pair.getRight().setLoadedValue(1);
    pair.getRight().setLoadedValueSize(BigInteger.ONE);

    pair.getLeft().cacheGetPeriodic("key1");

    for (int i = 2; i < 10; ++i) {
      pair.getRight().setFailure(false);
      pair.getRight().setLoadedValue(i);
      pair.getRight().setLoadedValueSize(BigInteger.ONE);

      pair.getLeft().cacheGetPeriodic("key" + i);
    }

    for (int i = 2; i < 10; ++i) {
      pair.getRight().setFailure(false);
      pair.getRight().setCloseFailure(true);
      pair.getRight().setLoadedValue(i);
      pair.getRight().setLoadedValueSize(BigInteger.ONE);

      pair.getLeft().cacheGetPeriodic("key" + i);
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
    final Pair<PCacheType<String, Integer, Failure>, LUCacheLoaderFaultInjectable<String, Integer>> pair =
      this.newCacheWithMaximumAge(2L);
    pair.getLeft().cacheEventsSubscribe(
      (JCacheEventsType<String, Integer>) TestUtilities.actuallyNull());
  }

  @Test public void testEvictionAge()
    throws Failure,
      JCacheException
  {
    final Pair<PCacheType<String, Integer, Failure>, LUCacheLoaderFaultInjectable<String, Integer>> p =
      this.newCacheWithMaximumAge(2);

    p.getLeft().cacheEventsSubscribe(new EventLog<String, Integer>());

    p.getLeft().cachePeriodStart();
    for (int index = 0; index < 10; ++index) {
      p.getRight().setLoadedValue(Integer.valueOf(index));
      p.getRight().setLoadedValueSize(BigInteger.ONE);
      p.getLeft().cacheGetPeriodic("key" + index);
    }
    p.getLeft().cachePeriodEnd();
    Assert.assertEquals(BigInteger.valueOf(10), p.getLeft().cacheItemCount());
    Assert.assertEquals(BigInteger.valueOf(10), p.getLeft().cacheSize());

    p.getLeft().cachePeriodStart();
    for (int index = 10; index < 20; ++index) {
      p.getRight().setLoadedValue(Integer.valueOf(index));
      p.getRight().setLoadedValueSize(BigInteger.ONE);
      p.getLeft().cacheGetPeriodic("key" + index);
    }
    p.getLeft().cachePeriodEnd();
    Assert.assertEquals(BigInteger.valueOf(20), p.getLeft().cacheItemCount());
    Assert.assertEquals(BigInteger.valueOf(20), p.getLeft().cacheSize());

    p.getLeft().cachePeriodStart();
    p.getLeft().cachePeriodEnd();
    Assert.assertEquals(BigInteger.valueOf(10), p.getLeft().cacheItemCount());
    Assert.assertEquals(BigInteger.valueOf(10), p.getLeft().cacheSize());

    p.getLeft().cachePeriodStart();
    p.getLeft().cachePeriodEnd();
    Assert.assertEquals(BigInteger.ZERO, p.getLeft().cacheItemCount());
    Assert.assertEquals(BigInteger.ZERO, p.getLeft().cacheSize());
  }

  @Test public void testEvictionSize()
    throws Failure,
      JCacheException
  {
    final Pair<PCacheType<String, Integer, Failure>, LUCacheLoaderFaultInjectable<String, Integer>> p =
      this.newCacheWithMaximumSize(10);

    p.getLeft().cacheEventsSubscribe(new EventLog<String, Integer>());

    p.getLeft().cachePeriodStart();
    for (int index = 0; index < 10; ++index) {
      p.getRight().setLoadedValue(Integer.valueOf(index));
      p.getRight().setLoadedValueSize(BigInteger.ONE);
      p.getLeft().cacheGetPeriodic("key" + index);
    }
    p.getLeft().cachePeriodEnd();
    Assert.assertEquals(BigInteger.valueOf(10), p.getLeft().cacheItemCount());
    Assert.assertEquals(BigInteger.valueOf(10), p.getLeft().cacheSize());

    p.getLeft().cachePeriodStart();
    for (int index = 10; index < 20; ++index) {
      p.getRight().setLoadedValue(Integer.valueOf(index));
      p.getRight().setLoadedValueSize(BigInteger.ONE);
      p.getLeft().cacheGetPeriodic("key" + index);
    }
    p.getLeft().cachePeriodEnd();
    for (int index = 0; index < 10; ++index) {
      Assert.assertFalse(p.getLeft().cacheIsCached("key" + index));
    }
    for (int index = 10; index < 20; ++index) {
      Assert.assertTrue(p.getLeft().cacheIsCached("key" + index));
    }
    Assert.assertEquals(BigInteger.valueOf(10), p.getLeft().cacheItemCount());
    Assert.assertEquals(BigInteger.valueOf(10), p.getLeft().cacheSize());

    p.getLeft().cachePeriodStart();
    for (int index = 20; index < 30; ++index) {
      p.getRight().setLoadedValue(Integer.valueOf(index));
      p.getRight().setLoadedValueSize(BigInteger.ONE);
      p.getLeft().cacheGetPeriodic("key" + index);
    }
    p.getLeft().cachePeriodEnd();
    for (int index = 10; index < 20; ++index) {
      Assert.assertFalse(p.getLeft().cacheIsCached("key" + index));
    }
    for (int index = 20; index < 30; ++index) {
      Assert.assertTrue(p.getLeft().cacheIsCached("key" + index));
    }
    Assert.assertEquals(BigInteger.valueOf(10), p.getLeft().cacheItemCount());
    Assert.assertEquals(BigInteger.valueOf(10), p.getLeft().cacheSize());
  }

  @Test(expected = IllegalStateException.class) public void testGetNoPeriod()
    throws Failure,
      JCacheException
  {
    PCacheType<Integer, Integer, Failure> pc = TestUtilities.actuallyNull();

    pc = this.newCache();
    assert pc != TestUtilities.actuallyNull();
    pc.cacheGetPeriodic(Integer.valueOf(23));
  }

  @Test(expected = NullCheckException.class) public void testGetNull()
    throws Failure,
      JCacheException
  {
    PCacheType<Integer, Integer, Failure> pc = TestUtilities.actuallyNull();

    pc = this.newCache();
    assert pc != TestUtilities.actuallyNull();
    pc.cacheGetPeriodic((Integer) TestUtilities.actuallyNull());
  }

  /**
   * Checking if a TestUtilities.actuallyNull() key is cached fails.
   */

  @Test(expected = NullCheckException.class) public void testIsCachedNull()
  {
    LRUCacheTrivial<Long, Long, Failure> cache = TestUtilities.actuallyNull();

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
    final Pair<PCacheType<String, Integer, Failure>, LUCacheLoaderFaultInjectable<String, Integer>> pair =
      this.newCacheWithMaximumAge(32L);

    pair.getRight().setFailure(true);
    pair.getRight().setLoadedValue(Integer.valueOf(23));
    pair.getRight().setLoadedValueSize(BigInteger.valueOf(4));
    pair.getLeft().cachePeriodStart();
    pair.getLeft().cacheGetPeriodic("23");
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
    final Pair<PCacheType<String, Integer, Failure>, LUCacheLoaderFaultInjectable<String, Integer>> pair =
      this.newCacheWithMaximumSize(32L);

    pair.getRight().setFailure(false);
    pair.getRight().setLoadedValue(Integer.valueOf(1));
    pair.getRight().setLoadedValueSize(BigInteger.valueOf(33L));
    pair.getLeft().cachePeriodStart();
    pair.getLeft().cacheGetPeriodic("23");
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
    final Pair<PCacheType<String, Integer, Failure>, LUCacheLoaderFaultInjectable<String, Integer>> pair =
      this.newCacheWithMaximumAge(32L);

    pair.getRight().setFailure(false);
    pair.getRight().setLoadedValue(Integer.valueOf(1));
    pair.getRight().setLoadedValueSize(BigInteger.valueOf(-1));
    pair.getLeft().cachePeriodStart();
    pair.getLeft().cacheGetPeriodic("23");
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
    final Pair<PCacheType<String, Integer, Failure>, LUCacheLoaderFaultInjectable<String, Integer>> pair =
      this.newCacheWithMaximumAge(32L);

    pair.getRight().setFailure(false);
    pair.getRight().setLoadedValue((Integer) TestUtilities.actuallyNull());
    pair.getRight().setLoadedValueSize(BigInteger.valueOf(4));
    pair.getLeft().cachePeriodStart();
    pair.getLeft().cacheGetPeriodic("23");
  }

  @Test(expected = NullCheckException.class) public void testNullConfig()
  {
    final LUCacheLoaderFaultInjectable<Integer, Integer> loader =
      new LUCacheLoaderFaultInjectable<Integer, Integer>();
    PCacheTrivial.newCache(
      loader,
      (PCacheConfig) TestUtilities.actuallyNull());
  }

  @Test(expected = NullCheckException.class) public void testNullLoader()
  {
    PCacheConfig c = TestUtilities.actuallyNull();
    try {
      final BuilderType b = PCacheConfig.newBuilder();
      c = b.create();
    } catch (final Throwable x) {
      throw new UnreachableCodeException(x);
    }

    assert c != TestUtilities.actuallyNull();
    PCacheTrivial.newCache(
      (JCacheLoaderType<Object, Object, JCacheException>) TestUtilities
        .actuallyNull(),
      c);
  }

  @Test(expected = IllegalStateException.class) public
    void
    testPeriodAlreadyStarted()
  {
    PCacheType<Integer, Integer, Failure> pc = TestUtilities.actuallyNull();

    try {
      pc = this.newCache();
      pc.cachePeriodStart();
    } catch (final Throwable x) {
      throw new UnreachableCodeException(x);
    }

    assert pc != TestUtilities.actuallyNull();
    pc.cachePeriodStart();
  }

  @Test(expected = IllegalStateException.class) public
    void
    testPeriodNotStarted()
  {
    PCacheType<Integer, Integer, Failure> pc = TestUtilities.actuallyNull();

    pc = this.newCache();
    assert pc != TestUtilities.actuallyNull();
    pc.cachePeriodEnd();
  }

  /**
   * Repeatedly requesting a key keeps the key cached.
   * 
   * @throws JCacheException
   */

  @Test public void testUpdateTime()
    throws Failure,
      JCacheException
  {
    final Pair<PCacheType<String, Integer, Failure>, LUCacheLoaderFaultInjectable<String, Integer>> pair =
      this.newCacheWithMaximumAge(2);

    final EventLog<String, Integer> ev = new EventLog<String, Integer>();
    pair.getLeft().cacheEventsSubscribe(ev);

    for (int index = 0; index < 10; ++index) {
      ev.reset();
      pair.getRight().setLoadedValueSize(BigInteger.ONE);
      pair.getRight().setLoadedValue(Integer.valueOf(index));
      pair.getRight().setFailure(false);

      pair.getLeft().cachePeriodStart();
      pair.getLeft().cacheGetPeriodic("key0");
      pair.getLeft().cachePeriodEnd();
      Assert.assertFalse(ev.evicted);
    }
  }
}
