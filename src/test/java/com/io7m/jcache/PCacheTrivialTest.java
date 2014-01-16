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

import org.junit.Assert;
import org.junit.Test;

import com.io7m.jaux.Constraints.ConstraintError;
import com.io7m.jaux.UnreachableCodeException;
import com.io7m.jaux.functional.Pair;
import com.io7m.jcache.LRUCacheConfig;
import com.io7m.jcache.LRUCacheTrivial;
import com.io7m.jcache.LUCacheException;
import com.io7m.jcache.PCache;
import com.io7m.jcache.PCacheConfig;
import com.io7m.jcache.PCacheTrivial;
import com.io7m.jcache.LUCacheException.Code;
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
      b.setMaximumAge(age);
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
      b.setMaximumSize(size);
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
   * @throws LUCacheException
   */

  @SuppressWarnings("boxing") @Test public void testEvents()
    throws Failure,
      ConstraintError,
      LUCacheException
  {
    final Pair<PCache<String, Integer, Failure>, LUCacheLoaderFaultInjectable<String, Integer>> pair =
      this.newCacheWithMaximumAge(2);

    final EventLog<String, Integer> ev = new EventLog<String, Integer>();
    pair.first.luCacheEventsSubscribe(ev);

    pair.second.setFailure(false);
    pair.second.setLoadedValue(0);
    pair.second.setLoadedValueSize(1);

    ev.reset();
    pair.first.pcPeriodStart();
    pair.first.pcCacheGet("key0");
    Assert.assertTrue(ev.loaded);
    Assert.assertEquals("key0", ev.loaded_key);
    Assert.assertEquals(Integer.valueOf(0), ev.loaded_value);
    Assert.assertEquals(1, ev.loaded_size);
    Assert.assertTrue(ev.retrieved);
    Assert.assertEquals("key0", ev.retrieved_key);
    Assert.assertEquals(Integer.valueOf(0), ev.retrieved_value);
    Assert.assertEquals(1, ev.retrieved_size);

    pair.second.setFailure(false);
    pair.second.setLoadedValue(1);
    pair.second.setLoadedValueSize(1);

    ev.reset();
    pair.first.pcCacheGet("key1");
    Assert.assertTrue(ev.loaded);
    Assert.assertEquals("key1", ev.loaded_key);
    Assert.assertEquals(Integer.valueOf(1), ev.loaded_value);
    Assert.assertEquals(1, ev.loaded_size);
    Assert.assertTrue(ev.retrieved);
    Assert.assertEquals("key1", ev.retrieved_key);
    Assert.assertEquals(Integer.valueOf(1), ev.retrieved_value);
    Assert.assertEquals(1, ev.retrieved_size);

    for (int i = 2; i < 10; ++i) {
      pair.second.setFailure(false);
      pair.second.setLoadedValue(i);
      pair.second.setLoadedValueSize(1);

      ev.reset();
      pair.first.pcCacheGet("key" + i);

      Assert.assertTrue(ev.loaded);
      Assert.assertEquals("key" + i, ev.loaded_key);
      Assert.assertEquals(Integer.valueOf(i), ev.loaded_value);
      Assert.assertEquals(1, ev.loaded_size);

      Assert.assertTrue(ev.retrieved);
      Assert.assertEquals("key" + i, ev.retrieved_key);
      Assert.assertEquals(Integer.valueOf(i), ev.retrieved_value);
      Assert.assertEquals(1, ev.retrieved_size);

      Assert.assertFalse(ev.evicted);
    }

    pair.first.luCacheEventsUnsubscribe();

    for (int i = 2; i < 10; ++i) {
      pair.second.setFailure(false);
      pair.second.setLoadedValue(i);
      pair.second.setLoadedValueSize(1);

      ev.reset();
      pair.first.pcCacheGet("key" + i);

      Assert.assertFalse(ev.loaded);
      Assert.assertFalse(ev.retrieved);
      Assert.assertFalse(ev.evicted);
    }
  }

  /**
   * Exceptions raised during closing are delivered.
   * 
   * @throws LUCacheException
   */

  @SuppressWarnings("boxing") @Test public void testEventsCloseError()
    throws Failure,
      ConstraintError,
      LUCacheException
  {
    final Pair<PCache<String, Integer, Failure>, LUCacheLoaderFaultInjectable<String, Integer>> pair =
      this.newCacheWithMaximumAge(1);

    final EventLog<String, Integer> ev = new EventLog<String, Integer>();
    pair.first.luCacheEventsSubscribe(ev);

    ev.reset();
    pair.first.pcPeriodStart();
    pair.second.setFailure(false);
    pair.second.setLoadedValue(0);
    pair.second.setLoadedValueSize(1);
    pair.first.pcCacheGet("key0");
    Assert.assertTrue(ev.loaded);
    Assert.assertEquals("key0", ev.loaded_key);
    Assert.assertEquals(Integer.valueOf(0), ev.loaded_value);
    Assert.assertEquals(1, ev.loaded_size);

    ev.reset();
    pair.second.setFailure(false);
    pair.second.setLoadedValue(1);
    pair.second.setLoadedValueSize(1);
    pair.second.setCloseFailure(true);
    pair.first.pcPeriodEnd();
    pair.first.luCacheDelete();

    Assert.assertTrue(ev.close_error);
    Assert.assertEquals("key0", ev.close_error_key);
    Assert.assertEquals(Integer.valueOf(0), ev.close_error_value);
  }

  /**
   * Exceptions are not propagated.
   * 
   * @throws LUCacheException
   */

  @SuppressWarnings("boxing") @Test public void testEventsExceptions()
    throws Failure,
      ConstraintError,
      LUCacheException
  {
    final Pair<PCache<String, Integer, Failure>, LUCacheLoaderFaultInjectable<String, Integer>> pair =
      this.newCacheWithMaximumAge(2);

    pair.first.pcPeriodStart();
    pair.first.luCacheEventsSubscribe(new EventThrown<String, Integer>());
    pair.second.setFailure(false);
    pair.second.setLoadedValue(0);
    pair.second.setLoadedValueSize(1);

    pair.first.pcCacheGet("key0");
    pair.second.setFailure(false);
    pair.second.setLoadedValue(1);
    pair.second.setLoadedValueSize(1);

    pair.first.pcCacheGet("key1");

    for (int i = 2; i < 10; ++i) {
      pair.second.setFailure(false);
      pair.second.setLoadedValue(i);
      pair.second.setLoadedValueSize(1);

      pair.first.pcCacheGet("key" + i);
    }

    for (int i = 2; i < 10; ++i) {
      pair.second.setFailure(false);
      pair.second.setCloseFailure(true);
      pair.second.setLoadedValue(i);
      pair.second.setLoadedValueSize(1);

      pair.first.pcCacheGet("key" + i);
    }
  }

  /**
   * Trying to subscribe with null fails.
   * 
   * @throws LUCacheException
   */

  @Test(expected = ConstraintError.class) public void testEventsNull()
    throws ConstraintError,
      LUCacheException
  {
    final Pair<PCache<String, Integer, Failure>, LUCacheLoaderFaultInjectable<String, Integer>> pair =
      this.newCacheWithMaximumAge(2L);
    pair.first.luCacheEventsSubscribe(null);
  }

  @Test public void testEvictionAge()
    throws ConstraintError,
      Failure,
      LUCacheException
  {
    final Pair<PCache<String, Integer, Failure>, LUCacheLoaderFaultInjectable<String, Integer>> p =
      this.newCacheWithMaximumAge(2);

    p.first.luCacheEventsSubscribe(new EventLog<String, Integer>());

    p.first.pcPeriodStart();
    for (int index = 0; index < 10; ++index) {
      p.second.setLoadedValue(Integer.valueOf(index));
      p.second.setLoadedValueSize(1);
      p.first.pcCacheGet("key" + index);
    }
    p.first.pcPeriodEnd();
    Assert.assertEquals(10, p.first.luCacheItems());
    Assert.assertEquals(10, p.first.luCacheSize());

    p.first.pcPeriodStart();
    for (int index = 10; index < 20; ++index) {
      p.second.setLoadedValue(Integer.valueOf(index));
      p.second.setLoadedValueSize(1);
      p.first.pcCacheGet("key" + index);
    }
    p.first.pcPeriodEnd();
    Assert.assertEquals(20, p.first.luCacheItems());
    Assert.assertEquals(20, p.first.luCacheSize());

    p.first.pcPeriodStart();
    p.first.pcPeriodEnd();
    Assert.assertEquals(10, p.first.luCacheItems());
    Assert.assertEquals(10, p.first.luCacheSize());

    p.first.pcPeriodStart();
    p.first.pcPeriodEnd();
    Assert.assertEquals(0, p.first.luCacheItems());
    Assert.assertEquals(0, p.first.luCacheSize());
  }

  @Test public void testEvictionSize()
    throws ConstraintError,
      Failure,
      LUCacheException
  {
    final Pair<PCache<String, Integer, Failure>, LUCacheLoaderFaultInjectable<String, Integer>> p =
      this.newCacheWithMaximumSize(10);

    p.first.luCacheEventsSubscribe(new EventLog<String, Integer>());

    p.first.pcPeriodStart();
    for (int index = 0; index < 10; ++index) {
      p.second.setLoadedValue(Integer.valueOf(index));
      p.second.setLoadedValueSize(1);
      p.first.pcCacheGet("key" + index);
    }
    p.first.pcPeriodEnd();
    Assert.assertEquals(10, p.first.luCacheItems());
    Assert.assertEquals(10, p.first.luCacheSize());

    p.first.pcPeriodStart();
    for (int index = 10; index < 20; ++index) {
      p.second.setLoadedValue(Integer.valueOf(index));
      p.second.setLoadedValueSize(1);
      p.first.pcCacheGet("key" + index);
    }
    p.first.pcPeriodEnd();
    for (int index = 0; index < 10; ++index) {
      Assert.assertFalse(p.first.luCacheIsCached("key" + index));
    }
    for (int index = 10; index < 20; ++index) {
      Assert.assertTrue(p.first.luCacheIsCached("key" + index));
    }
    Assert.assertEquals(10, p.first.luCacheItems());
    Assert.assertEquals(10, p.first.luCacheSize());

    p.first.pcPeriodStart();
    for (int index = 20; index < 30; ++index) {
      p.second.setLoadedValue(Integer.valueOf(index));
      p.second.setLoadedValueSize(1);
      p.first.pcCacheGet("key" + index);
    }
    p.first.pcPeriodEnd();
    for (int index = 10; index < 20; ++index) {
      Assert.assertFalse(p.first.luCacheIsCached("key" + index));
    }
    for (int index = 20; index < 30; ++index) {
      Assert.assertTrue(p.first.luCacheIsCached("key" + index));
    }
    Assert.assertEquals(10, p.first.luCacheItems());
    Assert.assertEquals(10, p.first.luCacheSize());
  }

  @Test(expected = ConstraintError.class) public void testGetNoPeriod()
    throws ConstraintError,
      Failure,
      LUCacheException
  {
    PCache<Integer, Integer, Failure> pc = null;

    pc = this.newCache();
    assert pc != null;
    pc.pcCacheGet(Integer.valueOf(23));
  }

  @Test(expected = ConstraintError.class) public void testGetNull()
    throws ConstraintError,
      Failure,
      LUCacheException
  {
    PCache<Integer, Integer, Failure> pc = null;

    pc = this.newCache();
    assert pc != null;
    pc.pcCacheGet(null);
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
    cache.luCacheIsCached(null);
  }

  /**
   * Failing to load an item is signalled.
   */

  @Test(expected = Failure.class) public void testLoadFailure()
    throws Failure,
      ConstraintError,
      LUCacheException
  {
    final Pair<PCache<String, Integer, Failure>, LUCacheLoaderFaultInjectable<String, Integer>> pair =
      this.newCacheWithMaximumAge(32L);

    pair.second.setFailure(true);
    pair.second.setLoadedValue(Integer.valueOf(23));
    pair.second.setLoadedValueSize(4);
    pair.first.pcPeriodStart();
    pair.first.pcCacheGet("23");
  }

  /**
   * A loader returning an object that cannot fit in the cache is an error.
   * 
   * @throws LUCacheException
   */

  @Test(expected = LUCacheException.class) public void testLoadHugeSize()
    throws Failure,
      ConstraintError,
      LUCacheException
  {
    final Pair<PCache<String, Integer, Failure>, LUCacheLoaderFaultInjectable<String, Integer>> pair =
      this.newCacheWithMaximumSize(32L);

    try {
      pair.second.setFailure(false);
      pair.second.setLoadedValue(Integer.valueOf(1));
      pair.second.setLoadedValueSize(33L);
      pair.first.pcPeriodStart();
      pair.first.pcCacheGet("23");
    } catch (final LUCacheException x) {
      Assert.assertEquals(Code.LUCACHE_OBJECT_TOO_LARGE, x.getCode());
      throw x;
    }
  }

  /**
   * A loader returning a negative size is a cache error.
   * 
   * @throws LUCacheException
   */

  @Test(expected = LUCacheException.class) public void testLoadNegativeSize()
    throws Failure,
      ConstraintError,
      LUCacheException
  {
    final Pair<PCache<String, Integer, Failure>, LUCacheLoaderFaultInjectable<String, Integer>> pair =
      this.newCacheWithMaximumAge(32L);

    try {
      pair.second.setFailure(false);
      pair.second.setLoadedValue(Integer.valueOf(1));
      pair.second.setLoadedValueSize(-1);
      pair.first.pcPeriodStart();
      pair.first.pcCacheGet("23");
    } catch (final LUCacheException x) {
      Assert.assertEquals(Code.LUCACHE_OBJECT_TOO_SMALL, x.getCode());
      throw x;
    }
  }

  /**
   * A loader returning <code>null</code> is a cache error.
   * 
   * @throws LUCacheException
   */

  @Test(expected = LUCacheException.class) public void testLoadNull()
    throws Failure,
      ConstraintError,
      LUCacheException
  {
    final Pair<PCache<String, Integer, Failure>, LUCacheLoaderFaultInjectable<String, Integer>> pair =
      this.newCacheWithMaximumAge(32L);

    try {
      pair.second.setFailure(false);
      pair.second.setLoadedValue(null);
      pair.second.setLoadedValueSize(4);
      pair.first.pcPeriodStart();
      pair.first.pcCacheGet("23");
    } catch (final LUCacheException x) {
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
      pc.pcPeriodStart();
    } catch (final ConstraintError x) {
      throw new UnreachableCodeException(x);
    }

    assert pc != null;
    pc.pcPeriodStart();
  }

  @Test(expected = ConstraintError.class) public void testPeriodNotStarted()
    throws ConstraintError
  {
    PCache<Integer, Integer, Failure> pc = null;

    pc = this.newCache();
    assert pc != null;
    pc.pcPeriodEnd();
  }

  /**
   * Repeatedly requesting a key keeps the key cached.
   * 
   * @throws LUCacheException
   */

  @Test public void testUpdateTime()
    throws Failure,
      ConstraintError,
      LUCacheException
  {
    final Pair<PCache<String, Integer, Failure>, LUCacheLoaderFaultInjectable<String, Integer>> pair =
      this.newCacheWithMaximumAge(2);

    final EventLog<String, Integer> ev = new EventLog<String, Integer>();
    pair.first.luCacheEventsSubscribe(ev);

    for (int index = 0; index < 10; ++index) {
      ev.reset();
      pair.second.setLoadedValueSize(1);
      pair.second.setLoadedValue(Integer.valueOf(index));
      pair.second.setFailure(false);

      pair.first.pcPeriodStart();
      pair.first.pcCacheGet("key0");
      pair.first.pcPeriodEnd();
      Assert.assertFalse(ev.evicted);
    }
  }
}
