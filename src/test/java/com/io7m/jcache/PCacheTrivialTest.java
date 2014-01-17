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

package com.io7m.jcache;

import java.math.BigInteger;

import org.junit.Assert;
import org.junit.Test;

import com.io7m.jaux.Constraints.ConstraintError;
import com.io7m.jaux.UnreachableCodeException;
import com.io7m.jaux.functional.Pair;
import com.io7m.jcache.JCacheException.Code;
import com.io7m.jcache.LUCacheLoaderFaultInjectable.Failure;
import com.io7m.jcache.PCacheConfig.Builder;

public final class PCacheTrivialTest
{
  @SuppressWarnings("static-method") private
    PCache<Integer, Integer, Failure>
    newCache()
  {
    try {
      PCache<Integer, Integer, Failure> pc;
      PCacheConfig c;
      final Builder b = PCacheConfig.newBuilder();
      c = b.create();
      final LUCacheLoaderFaultInjectable<Integer, Integer> loader =
        new LUCacheLoaderFaultInjectable<Integer, Integer>();
      pc = PCacheTrivial.newCache(loader, c);
      return pc;
    } catch (final ConstraintError x) {
      throw new UnreachableCodeException(x);
    }
  }

  @SuppressWarnings("static-method") private
    Pair<PCache<String, Integer, Failure>, LUCacheLoaderFaultInjectable<String, Integer>>
    newCacheWithMaximumAge(
      final long age)
  {
    try {
      PCache<String, Integer, Failure> pc;
      PCacheConfig c;
      final Builder b = PCacheConfig.newBuilder();
      b.setMaximumAge(BigInteger.valueOf(age));
      b.setNoMaximumSize();
      c = b.create();
      final LUCacheLoaderFaultInjectable<String, Integer> loader =
        new LUCacheLoaderFaultInjectable<String, Integer>();
      pc = PCacheTrivial.newCache(loader, c);
      return new Pair<PCache<String, Integer, Failure>, LUCacheLoaderFaultInjectable<String, Integer>>(
        pc,
        loader);
    } catch (final ConstraintError x) {
      throw new UnreachableCodeException(x);
    }
  }

  @SuppressWarnings("static-method") private
    Pair<PCache<String, Integer, Failure>, LUCacheLoaderFaultInjectable<String, Integer>>
    newCacheWithMaximumSize(
      final long size)
  {
    try {
      PCache<String, Integer, Failure> pc;
      PCacheConfig c;
      final Builder b = PCacheConfig.newBuilder();
      b.setNoMaximumAge();
      b.setMaximumSize(BigInteger.valueOf(size));
      c = b.create();
      final LUCacheLoaderFaultInjectable<String, Integer> loader =
        new LUCacheLoaderFaultInjectable<String, Integer>();
      pc = PCacheTrivial.newCache(loader, c);
      return new Pair<PCache<String, Integer, Failure>, LUCacheLoaderFaultInjectable<String, Integer>>(
        pc,
        loader);
    } catch (final ConstraintError x) {
      throw new UnreachableCodeException(x);
    }
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
    final Pair<PCache<String, Integer, Failure>, LUCacheLoaderFaultInjectable<String, Integer>> pair =
      this.newCacheWithMaximumAge(2);

    final EventLog<String, Integer> ev = new EventLog<String, Integer>();
    pair.first.cacheEventsSubscribe(ev);

    pair.second.setFailure(false);
    pair.second.setLoadedValue(0);
    pair.second.setLoadedValueSize(BigInteger.ONE);

    ev.reset();
    pair.first.cachePeriodStart();
    pair.first.cacheGetPeriodic("key0");
    Assert.assertTrue(ev.loaded);
    Assert.assertEquals("key0", ev.loaded_key);
    Assert.assertEquals(Integer.valueOf(0), ev.loaded_value);
    Assert.assertEquals(BigInteger.ONE, ev.loaded_size);
    Assert.assertTrue(ev.retrieved);
    Assert.assertEquals("key0", ev.retrieved_key);
    Assert.assertEquals(Integer.valueOf(0), ev.retrieved_value);
    Assert.assertEquals(BigInteger.ONE, ev.retrieved_size);

    pair.second.setFailure(false);
    pair.second.setLoadedValue(1);
    pair.second.setLoadedValueSize(BigInteger.ONE);

    ev.reset();
    pair.first.cacheGetPeriodic("key1");
    Assert.assertTrue(ev.loaded);
    Assert.assertEquals("key1", ev.loaded_key);
    Assert.assertEquals(Integer.valueOf(1), ev.loaded_value);
    Assert.assertEquals(BigInteger.ONE, ev.loaded_size);
    Assert.assertTrue(ev.retrieved);
    Assert.assertEquals("key1", ev.retrieved_key);
    Assert.assertEquals(Integer.valueOf(1), ev.retrieved_value);
    Assert.assertEquals(BigInteger.ONE, ev.retrieved_size);

    for (int i = 2; i < 10; ++i) {
      pair.second.setFailure(false);
      pair.second.setLoadedValue(i);
      pair.second.setLoadedValueSize(BigInteger.ONE);

      ev.reset();
      pair.first.cacheGetPeriodic("key" + i);

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

    pair.first.cacheEventsUnsubscribe();

    for (int i = 2; i < 10; ++i) {
      pair.second.setFailure(false);
      pair.second.setLoadedValue(i);
      pair.second.setLoadedValueSize(BigInteger.ONE);

      ev.reset();
      pair.first.cacheGetPeriodic("key" + i);

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
    final Pair<PCache<String, Integer, Failure>, LUCacheLoaderFaultInjectable<String, Integer>> pair =
      this.newCacheWithMaximumAge(1);

    final EventLog<String, Integer> ev = new EventLog<String, Integer>();
    pair.first.cacheEventsSubscribe(ev);

    ev.reset();
    pair.first.cachePeriodStart();
    pair.second.setFailure(false);
    pair.second.setLoadedValue(0);
    pair.second.setLoadedValueSize(BigInteger.ONE);
    pair.first.cacheGetPeriodic("key0");
    Assert.assertTrue(ev.loaded);
    Assert.assertEquals("key0", ev.loaded_key);
    Assert.assertEquals(Integer.valueOf(0), ev.loaded_value);
    Assert.assertEquals(BigInteger.ONE, ev.loaded_size);

    ev.reset();
    pair.second.setFailure(false);
    pair.second.setLoadedValue(1);
    pair.second.setLoadedValueSize(BigInteger.ONE);
    pair.second.setCloseFailure(true);
    pair.first.cachePeriodEnd();
    pair.first.cacheDelete();

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
      ConstraintError,
      JCacheException
  {
    final Pair<PCache<String, Integer, Failure>, LUCacheLoaderFaultInjectable<String, Integer>> pair =
      this.newCacheWithMaximumAge(2);

    pair.first.cachePeriodStart();
    pair.first.cacheEventsSubscribe(new EventThrown<String, Integer>());
    pair.second.setFailure(false);
    pair.second.setLoadedValue(0);
    pair.second.setLoadedValueSize(BigInteger.ONE);

    pair.first.cacheGetPeriodic("key0");
    pair.second.setFailure(false);
    pair.second.setLoadedValue(1);
    pair.second.setLoadedValueSize(BigInteger.ONE);

    pair.first.cacheGetPeriodic("key1");

    for (int i = 2; i < 10; ++i) {
      pair.second.setFailure(false);
      pair.second.setLoadedValue(i);
      pair.second.setLoadedValueSize(BigInteger.ONE);

      pair.first.cacheGetPeriodic("key" + i);
    }

    for (int i = 2; i < 10; ++i) {
      pair.second.setFailure(false);
      pair.second.setCloseFailure(true);
      pair.second.setLoadedValue(i);
      pair.second.setLoadedValueSize(BigInteger.ONE);

      pair.first.cacheGetPeriodic("key" + i);
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
    final Pair<PCache<String, Integer, Failure>, LUCacheLoaderFaultInjectable<String, Integer>> pair =
      this.newCacheWithMaximumAge(2L);
    pair.first.cacheEventsSubscribe(null);
  }

  @Test public void testEvictionAge()
    throws ConstraintError,
      Failure,
      JCacheException
  {
    final Pair<PCache<String, Integer, Failure>, LUCacheLoaderFaultInjectable<String, Integer>> p =
      this.newCacheWithMaximumAge(2);

    p.first.cacheEventsSubscribe(new EventLog<String, Integer>());

    p.first.cachePeriodStart();
    for (int index = 0; index < 10; ++index) {
      p.second.setLoadedValue(Integer.valueOf(index));
      p.second.setLoadedValueSize(BigInteger.ONE);
      p.first.cacheGetPeriodic("key" + index);
    }
    p.first.cachePeriodEnd();
    Assert.assertEquals(BigInteger.valueOf(10), p.first.cacheItemCount());
    Assert.assertEquals(BigInteger.valueOf(10), p.first.cacheSize());

    p.first.cachePeriodStart();
    for (int index = 10; index < 20; ++index) {
      p.second.setLoadedValue(Integer.valueOf(index));
      p.second.setLoadedValueSize(BigInteger.ONE);
      p.first.cacheGetPeriodic("key" + index);
    }
    p.first.cachePeriodEnd();
    Assert.assertEquals(BigInteger.valueOf(20), p.first.cacheItemCount());
    Assert.assertEquals(BigInteger.valueOf(20), p.first.cacheSize());

    p.first.cachePeriodStart();
    p.first.cachePeriodEnd();
    Assert.assertEquals(BigInteger.valueOf(10), p.first.cacheItemCount());
    Assert.assertEquals(BigInteger.valueOf(10), p.first.cacheSize());

    p.first.cachePeriodStart();
    p.first.cachePeriodEnd();
    Assert.assertEquals(BigInteger.ZERO, p.first.cacheItemCount());
    Assert.assertEquals(BigInteger.ZERO, p.first.cacheSize());
  }

  @Test public void testEvictionSize()
    throws ConstraintError,
      Failure,
      JCacheException
  {
    final Pair<PCache<String, Integer, Failure>, LUCacheLoaderFaultInjectable<String, Integer>> p =
      this.newCacheWithMaximumSize(10);

    p.first.cacheEventsSubscribe(new EventLog<String, Integer>());

    p.first.cachePeriodStart();
    for (int index = 0; index < 10; ++index) {
      p.second.setLoadedValue(Integer.valueOf(index));
      p.second.setLoadedValueSize(BigInteger.ONE);
      p.first.cacheGetPeriodic("key" + index);
    }
    p.first.cachePeriodEnd();
    Assert.assertEquals(BigInteger.valueOf(10), p.first.cacheItemCount());
    Assert.assertEquals(BigInteger.valueOf(10), p.first.cacheSize());

    p.first.cachePeriodStart();
    for (int index = 10; index < 20; ++index) {
      p.second.setLoadedValue(Integer.valueOf(index));
      p.second.setLoadedValueSize(BigInteger.ONE);
      p.first.cacheGetPeriodic("key" + index);
    }
    p.first.cachePeriodEnd();
    for (int index = 0; index < 10; ++index) {
      Assert.assertFalse(p.first.cacheIsCached("key" + index));
    }
    for (int index = 10; index < 20; ++index) {
      Assert.assertTrue(p.first.cacheIsCached("key" + index));
    }
    Assert.assertEquals(BigInteger.valueOf(10), p.first.cacheItemCount());
    Assert.assertEquals(BigInteger.valueOf(10), p.first.cacheSize());

    p.first.cachePeriodStart();
    for (int index = 20; index < 30; ++index) {
      p.second.setLoadedValue(Integer.valueOf(index));
      p.second.setLoadedValueSize(BigInteger.ONE);
      p.first.cacheGetPeriodic("key" + index);
    }
    p.first.cachePeriodEnd();
    for (int index = 10; index < 20; ++index) {
      Assert.assertFalse(p.first.cacheIsCached("key" + index));
    }
    for (int index = 20; index < 30; ++index) {
      Assert.assertTrue(p.first.cacheIsCached("key" + index));
    }
    Assert.assertEquals(BigInteger.valueOf(10), p.first.cacheItemCount());
    Assert.assertEquals(BigInteger.valueOf(10), p.first.cacheSize());
  }

  @Test(expected = ConstraintError.class) public void testGetNoPeriod()
    throws ConstraintError,
      Failure,
      JCacheException
  {
    PCache<Integer, Integer, Failure> pc = null;

    pc = this.newCache();
    assert pc != null;
    pc.cacheGetPeriodic(Integer.valueOf(23));
  }

  @Test(expected = ConstraintError.class) public void testGetNull()
    throws ConstraintError,
      Failure,
      JCacheException
  {
    PCache<Integer, Integer, Failure> pc = null;

    pc = this.newCache();
    assert pc != null;
    pc.cacheGetPeriodic(null);
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
    final Pair<PCache<String, Integer, Failure>, LUCacheLoaderFaultInjectable<String, Integer>> pair =
      this.newCacheWithMaximumAge(32L);

    pair.second.setFailure(true);
    pair.second.setLoadedValue(Integer.valueOf(23));
    pair.second.setLoadedValueSize(BigInteger.valueOf(4));
    pair.first.cachePeriodStart();
    pair.first.cacheGetPeriodic("23");
  }

  /**
   * A loader returning an object that cannot fit in the cache is an error.
   * 
   * @throws JCacheException
   */

  @Test(expected = JCacheException.class) public void testLoadHugeSize()
    throws Failure,
      ConstraintError,
      JCacheException
  {
    final Pair<PCache<String, Integer, Failure>, LUCacheLoaderFaultInjectable<String, Integer>> pair =
      this.newCacheWithMaximumSize(32L);

    try {
      pair.second.setFailure(false);
      pair.second.setLoadedValue(Integer.valueOf(1));
      pair.second.setLoadedValueSize(BigInteger.valueOf(33L));
      pair.first.cachePeriodStart();
      pair.first.cacheGetPeriodic("23");
    } catch (final JCacheException x) {
      Assert.assertEquals(Code.LUCACHE_OBJECT_TOO_LARGE, x.getCode());
      throw x;
    }
  }

  /**
   * A loader returning a negative size is a cache error.
   * 
   * @throws JCacheException
   */

  @Test(expected = JCacheException.class) public void testLoadNegativeSize()
    throws Failure,
      ConstraintError,
      JCacheException
  {
    final Pair<PCache<String, Integer, Failure>, LUCacheLoaderFaultInjectable<String, Integer>> pair =
      this.newCacheWithMaximumAge(32L);

    try {
      pair.second.setFailure(false);
      pair.second.setLoadedValue(Integer.valueOf(1));
      pair.second.setLoadedValueSize(BigInteger.valueOf(-1));
      pair.first.cachePeriodStart();
      pair.first.cacheGetPeriodic("23");
    } catch (final JCacheException x) {
      Assert.assertEquals(Code.LUCACHE_OBJECT_TOO_SMALL, x.getCode());
      throw x;
    }
  }

  /**
   * A loader returning <code>null</code> is a cache error.
   * 
   * @throws JCacheException
   */

  @Test(expected = JCacheException.class) public void testLoadNull()
    throws Failure,
      ConstraintError,
      JCacheException
  {
    final Pair<PCache<String, Integer, Failure>, LUCacheLoaderFaultInjectable<String, Integer>> pair =
      this.newCacheWithMaximumAge(32L);

    try {
      pair.second.setFailure(false);
      pair.second.setLoadedValue(null);
      pair.second.setLoadedValueSize(BigInteger.valueOf(4));
      pair.first.cachePeriodStart();
      pair.first.cacheGetPeriodic("23");
    } catch (final JCacheException x) {
      Assert.assertEquals(Code.LUCACHE_LOADER_RETURNED_NULL, x.getCode());
      throw x;
    }
  }

  @SuppressWarnings("static-method") @Test(expected = ConstraintError.class) public
    void
    testNullConfig()
      throws ConstraintError
  {
    final LUCacheLoaderFaultInjectable<Integer, Integer> loader =
      new LUCacheLoaderFaultInjectable<Integer, Integer>();
    PCacheTrivial.newCache(loader, null);
  }

  @SuppressWarnings("static-method") @Test(expected = ConstraintError.class) public
    void
    testNullLoader()
      throws ConstraintError
  {
    PCacheConfig c = null;
    try {
      final Builder b = PCacheConfig.newBuilder();
      c = b.create();
    } catch (final ConstraintError x) {
      throw new UnreachableCodeException(x);
    }

    assert c != null;
    PCacheTrivial.newCache(null, c);
  }

  @Test(expected = ConstraintError.class) public
    void
    testPeriodAlreadyStarted()
      throws ConstraintError
  {
    PCache<Integer, Integer, Failure> pc = null;

    try {
      pc = this.newCache();
      pc.cachePeriodStart();
    } catch (final ConstraintError x) {
      throw new UnreachableCodeException(x);
    }

    assert pc != null;
    pc.cachePeriodStart();
  }

  @Test(expected = ConstraintError.class) public void testPeriodNotStarted()
    throws ConstraintError
  {
    PCache<Integer, Integer, Failure> pc = null;

    pc = this.newCache();
    assert pc != null;
    pc.cachePeriodEnd();
  }

  /**
   * Repeatedly requesting a key keeps the key cached.
   * 
   * @throws JCacheException
   */

  @Test public void testUpdateTime()
    throws Failure,
      ConstraintError,
      JCacheException
  {
    final Pair<PCache<String, Integer, Failure>, LUCacheLoaderFaultInjectable<String, Integer>> pair =
      this.newCacheWithMaximumAge(2);

    final EventLog<String, Integer> ev = new EventLog<String, Integer>();
    pair.first.cacheEventsSubscribe(ev);

    for (int index = 0; index < 10; ++index) {
      ev.reset();
      pair.second.setLoadedValueSize(BigInteger.ONE);
      pair.second.setLoadedValue(Integer.valueOf(index));
      pair.second.setFailure(false);

      pair.first.cachePeriodStart();
      pair.first.cacheGetPeriodic("key0");
      pair.first.cachePeriodEnd();
      Assert.assertFalse(ev.evicted);
    }
  }
}
