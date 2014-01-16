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

import javax.annotation.Nonnull;

import org.junit.Assert;
import org.junit.Test;

import com.io7m.jaux.Constraints.ConstraintError;
import com.io7m.jaux.UnreachableCodeException;
import com.io7m.jaux.functional.Pair;
import com.io7m.jcache.LRUCacheConfig;
import com.io7m.jcache.LRUCacheTrivial;
import com.io7m.jcache.LUCacheException;
import com.io7m.jcache.LUCacheException.Code;
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
        LRUCacheConfig.empty().withMaximumCapacity(capacity);
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
   * @throws LUCacheException
   */

  @SuppressWarnings("boxing") @Test public void testDelete()
    throws Failure,
      ConstraintError,
      LUCacheException
  {
    final Pair<LUCacheLoaderFaultInjectable<String, Long>, LRUCacheTrivial<String, Long, Failure>> pair =
      this.newCache(8L);

    final EventCount<String, Long> ec = new EventCount<String, Long>();
    pair.second.luCacheEventsSubscribe(ec);

    for (long i = 0; i < 8; ++i) {
      pair.first.setFailure(false);
      pair.first.setLoadedValue(i);
      pair.first.setLoadedValueSize(1);

      pair.second.luCacheGet("k" + i);
      Assert.assertTrue(pair.second.luCacheIsCached("k" + i));
      Assert.assertEquals(i + 1, pair.second.luCacheItems());
      Assert.assertEquals(i + 1, pair.second.luCacheSize());
    }

    pair.second.luCacheDelete();
    Assert.assertEquals(0, pair.second.luCacheItems());
    Assert.assertEquals(0, pair.second.luCacheSize());

    Assert.assertEquals(8, ec.getEvictions());
    Assert.assertEquals(8, ec.getLoads());
    Assert.assertEquals(0, ec.getCloseErrors());
    Assert.assertEquals(8, ec.getRetrievals());
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
    final Pair<LUCacheLoaderFaultInjectable<String, Long>, LRUCacheTrivial<String, Long, Failure>> pair =
      this.newCache(2L);

    final EventLog<String, Long> ev = new EventLog<String, Long>();
    pair.second.luCacheEventsSubscribe(ev);

    pair.first.setFailure(false);
    pair.first.setLoadedValue(0L);
    pair.first.setLoadedValueSize(1);

    ev.reset();
    pair.second.luCacheGet("key0");
    Assert.assertTrue(ev.loaded);
    Assert.assertEquals("key0", ev.loaded_key);
    Assert.assertEquals(Long.valueOf(0L), ev.loaded_value);
    Assert.assertEquals(Long.valueOf(1L), Long.valueOf(ev.loaded_size));
    Assert.assertTrue(ev.retrieved);
    Assert.assertEquals("key0", ev.retrieved_key);
    Assert.assertEquals(Long.valueOf(0L), ev.retrieved_value);
    Assert.assertEquals(Long.valueOf(1L), Long.valueOf(ev.retrieved_size));

    pair.first.setFailure(false);
    pair.first.setLoadedValue(1L);
    pair.first.setLoadedValueSize(1);

    ev.reset();
    pair.second.luCacheGet("key1");
    Assert.assertTrue(ev.loaded);
    Assert.assertEquals("key1", ev.loaded_key);
    Assert.assertEquals(Long.valueOf(1L), ev.loaded_value);
    Assert.assertEquals(Long.valueOf(1L), Long.valueOf(ev.loaded_size));
    Assert.assertTrue(ev.retrieved);
    Assert.assertEquals("key1", ev.retrieved_key);
    Assert.assertEquals(Long.valueOf(1L), ev.retrieved_value);
    Assert.assertEquals(Long.valueOf(1L), Long.valueOf(ev.retrieved_size));

    for (long i = 2; i < 10; ++i) {
      pair.first.setFailure(false);
      pair.first.setLoadedValue(i);
      pair.first.setLoadedValueSize(1);

      ev.reset();
      pair.second.luCacheGet("key" + i);

      Assert.assertTrue(ev.loaded);
      Assert.assertEquals("key" + i, ev.loaded_key);
      Assert.assertEquals(Long.valueOf(i), ev.loaded_value);
      Assert.assertEquals(Long.valueOf(1L), Long.valueOf(ev.loaded_size));

      Assert.assertTrue(ev.retrieved);
      Assert.assertEquals("key" + i, ev.retrieved_key);
      Assert.assertEquals(Long.valueOf(i), ev.retrieved_value);
      Assert.assertEquals(Long.valueOf(1L), Long.valueOf(ev.retrieved_size));

      Assert.assertTrue(ev.evicted);
      Assert.assertEquals("key" + (i - 2), ev.evicted_key);
      Assert.assertEquals(Long.valueOf(i - 2), ev.evicted_value);
      Assert.assertEquals(Long.valueOf(1L), Long.valueOf(ev.evicted_size));
    }

    pair.second.luCacheEventsUnsubscribe();

    for (long i = 2; i < 10; ++i) {
      pair.first.setFailure(false);
      pair.first.setLoadedValue(i);
      pair.first.setLoadedValueSize(1);

      ev.reset();
      pair.second.luCacheGet("key" + i);

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
    final Pair<LUCacheLoaderFaultInjectable<String, Long>, LRUCacheTrivial<String, Long, Failure>> pair =
      this.newCache(1L);

    final EventLog<String, Long> ev = new EventLog<String, Long>();
    pair.second.luCacheEventsSubscribe(ev);

    ev.reset();
    pair.first.setFailure(false);
    pair.first.setLoadedValue(0L);
    pair.first.setLoadedValueSize(1);
    pair.second.luCacheGet("key0");
    Assert.assertTrue(ev.loaded);
    Assert.assertEquals("key0", ev.loaded_key);
    Assert.assertEquals(Long.valueOf(0L), ev.loaded_value);
    Assert.assertEquals(1, ev.loaded_size);

    ev.reset();
    pair.first.setFailure(false);
    pair.first.setLoadedValue(1L);
    pair.first.setLoadedValueSize(1);
    pair.first.setCloseFailure(true);
    pair.second.luCacheGet("key1");
    Assert.assertTrue(ev.close_error);
    Assert.assertEquals("key0", ev.close_error_key);
    Assert.assertEquals(Long.valueOf(0L), ev.close_error_value);
    Assert.assertTrue(ev.loaded);
    Assert.assertEquals("key1", ev.loaded_key);
    Assert.assertEquals(Long.valueOf(1L), ev.loaded_value);
    Assert.assertEquals(1, ev.loaded_size);
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
    final Pair<LUCacheLoaderFaultInjectable<String, Long>, LRUCacheTrivial<String, Long, Failure>> pair =
      this.newCache(2L);

    pair.second.luCacheEventsSubscribe(new EventThrown<String, Long>());
    pair.first.setFailure(false);
    pair.first.setLoadedValue(0L);
    pair.first.setLoadedValueSize(1);

    pair.second.luCacheGet("key0");
    pair.first.setFailure(false);
    pair.first.setLoadedValue(1L);
    pair.first.setLoadedValueSize(1);

    pair.second.luCacheGet("key1");

    for (long i = 2; i < 10; ++i) {
      pair.first.setFailure(false);
      pair.first.setLoadedValue(i);
      pair.first.setLoadedValueSize(1);

      pair.second.luCacheGet("key" + i);
    }

    for (long i = 2; i < 10; ++i) {
      pair.first.setFailure(false);
      pair.first.setCloseFailure(true);
      pair.first.setLoadedValue(i);
      pair.first.setLoadedValueSize(1);

      pair.second.luCacheGet("key" + i);
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
    final Pair<LUCacheLoaderFaultInjectable<String, Long>, LRUCacheTrivial<String, Long, Failure>> pair =
      this.newCache(2L);
    pair.second.luCacheEventsSubscribe(null);
  }

  /**
   * Caching items evicts the oldest items first.
   * 
   * @throws LUCacheException
   */

  @SuppressWarnings("boxing") @Test public void testEviction()
    throws Failure,
      ConstraintError,
      LUCacheException
  {
    final Pair<LUCacheLoaderFaultInjectable<String, Long>, LRUCacheTrivial<String, Long, Failure>> pair =
      this.newCache(2L);

    pair.first.setFailure(false);
    pair.first.setLoadedValue(0L);
    pair.first.setLoadedValueSize(1);

    pair.second.luCacheGet("key0");
    pair.second.luCacheGet("key1");
    Assert.assertTrue(pair.second.luCacheIsCached("key0"));
    Assert.assertTrue(pair.second.luCacheIsCached("key1"));
    Assert.assertEquals(2, pair.second.luCacheItems());
    Assert.assertEquals(2, pair.second.luCacheSize());

    for (long i = 2; i < 10; ++i) {
      pair.first.setFailure(false);
      pair.first.setLoadedValue(i);
      pair.first.setLoadedValueSize(1);

      Assert.assertEquals(2, pair.second.luCacheItems());
      Assert.assertEquals(2, pair.second.luCacheSize());

      final Long r = pair.second.luCacheGet("key" + i);
      for (long k = 0; k < (i - 2); ++k) {
        Assert.assertFalse(pair.second.luCacheIsCached("key" + k));
      }
      final Long q = pair.second.luCacheGet("key" + i);
      Assert.assertEquals(r, q);

      Assert.assertTrue(pair.second.luCacheIsCached("key" + (i - 1)));
      Assert.assertTrue(pair.second.luCacheIsCached("key" + i));
      Assert.assertEquals(2, pair.second.luCacheItems());
      Assert.assertEquals(2, pair.second.luCacheSize());
    }
  }

  /**
   * A cache of size 1 can hold one object of size 1.
   * 
   * @throws LUCacheException
   */

  @SuppressWarnings("boxing") @Test public void testEvictSize()
    throws Failure,
      ConstraintError,
      LUCacheException
  {
    final Pair<LUCacheLoaderFaultInjectable<String, Long>, LRUCacheTrivial<String, Long, Failure>> pair =
      this.newCache(1L);

    final EventLog<String, Long> ev = new EventLog<String, Long>();
    pair.second.luCacheEventsSubscribe(ev);

    ev.reset();
    pair.first.setFailure(false);
    pair.first.setLoadedValue(0L);
    pair.first.setLoadedValueSize(1);
    pair.second.luCacheGet("key0");
    Assert.assertTrue(ev.loaded);
    Assert.assertEquals("key0", ev.loaded_key);
    Assert.assertEquals(Long.valueOf(0L), ev.loaded_value);
    Assert.assertEquals(Long.valueOf(1L), Long.valueOf(ev.loaded_size));
    Assert.assertTrue(ev.retrieved);
    Assert.assertEquals("key0", ev.retrieved_key);
    Assert.assertEquals(Long.valueOf(0L), ev.retrieved_value);
    Assert.assertEquals(Long.valueOf(1L), Long.valueOf(ev.retrieved_size));

    ev.reset();
    pair.first.setFailure(false);
    pair.first.setLoadedValue(1L);
    pair.first.setLoadedValueSize(1);
    pair.second.luCacheGet("key1");
    Assert.assertTrue(ev.loaded);
    Assert.assertEquals("key1", ev.loaded_key);
    Assert.assertEquals(Long.valueOf(1L), ev.loaded_value);
    Assert.assertEquals(Long.valueOf(1L), Long.valueOf(ev.loaded_size));
    Assert.assertTrue(ev.retrieved);
    Assert.assertEquals("key1", ev.retrieved_key);
    Assert.assertEquals(Long.valueOf(1L), ev.retrieved_value);
    Assert.assertEquals(Long.valueOf(1L), Long.valueOf(ev.retrieved_size));
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
    final Pair<LUCacheLoaderFaultInjectable<Long, Long>, LRUCacheTrivial<Long, Long, Failure>> pair =
      this.newCache(32L);

    pair.first.setFailure(true);
    pair.first.setLoadedValue(Long.valueOf(23));
    pair.first.setLoadedValueSize(4);
    pair.second.luCacheGet(Long.valueOf(23));
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
    final Pair<LUCacheLoaderFaultInjectable<Long, Long>, LRUCacheTrivial<Long, Long, Failure>> pair =
      this.newCache(32L);

    try {
      pair.first.setFailure(false);
      pair.first.setLoadedValue(Long.valueOf(1));
      pair.first.setLoadedValueSize(33L);
      pair.second.luCacheGet(Long.valueOf(23));
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
    final Pair<LUCacheLoaderFaultInjectable<Long, Long>, LRUCacheTrivial<Long, Long, Failure>> pair =
      this.newCache(32L);

    try {
      pair.first.setFailure(false);
      pair.first.setLoadedValue(Long.valueOf(1));
      pair.first.setLoadedValueSize(-1);
      pair.second.luCacheGet(Long.valueOf(23));
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
    final Pair<LUCacheLoaderFaultInjectable<Long, Long>, LRUCacheTrivial<Long, Long, Failure>> pair =
      this.newCache(32L);

    try {
      pair.first.setFailure(false);
      pair.first.setLoadedValue(null);
      pair.first.setLoadedValueSize(4);
      pair.second.luCacheGet(Long.valueOf(23));
    } catch (final LUCacheException x) {
      Assert.assertEquals(Code.LUCACHE_LOADER_RETURNED_NULL, x.getCode());
      throw x;
    }
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
    Assert.assertEquals(0, cache.luCacheItems());
    Assert.assertEquals(0, cache.luCacheSize());
    Assert.assertEquals(config, cache.lruCacheConfiguration());

    for (long e = 2; e <= 32; e *= 2) {
      Assert.assertFalse(cache.luCacheIsCached(Long.valueOf((long) Math.pow(
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
      LUCacheException
  {
    final Pair<LUCacheLoaderFaultInjectable<String, Long>, LRUCacheTrivial<String, Long, Failure>> pair =
      this.newCache(32L);

    final String s0 = pair.second.toString();

    pair.first.setFailure(false);
    pair.first.setLoadedValue(0L);
    pair.first.setLoadedValueSize(1);
    pair.second.luCacheGet("key0");

    final String s1 = pair.second.toString();

    Assert.assertFalse(s0.equals(s1));
  }
}
