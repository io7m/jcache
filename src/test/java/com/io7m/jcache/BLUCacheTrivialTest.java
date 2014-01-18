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
import java.util.ArrayList;

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

public class BLUCacheTrivialTest
{
  @SuppressWarnings("static-method") private @Nonnull
    <K, V>
    Pair<LUCacheLoaderFaultInjectable<K, V>, BLUCacheTrivial<K, V, Failure>>
    newCache(
      final long capacity,
      final long max_borrows)
  {
    try {
      final BLUCacheConfig config =
        BLUCacheConfig
          .empty()
          .withMaximumCapacity(BigInteger.valueOf(capacity))
          .withMaximumBorrowsPerKey(BigInteger.valueOf(max_borrows));

      final LUCacheLoaderFaultInjectable<K, V> loader =
        new LUCacheLoaderFaultInjectable<K, V>();
      final BLUCacheTrivial<K, V, Failure> cache =
        BLUCacheTrivial.newCache(loader, config);
      return new Pair<LUCacheLoaderFaultInjectable<K, V>, BLUCacheTrivial<K, V, Failure>>(
        loader,
        cache);
    } catch (final ConstraintError x) {
      throw new UnreachableCodeException(x);
    }
  }

  @Test public void testBorrow_0()
    throws Failure,
      ConstraintError,
      JCacheException
  {
    final Pair<LUCacheLoaderFaultInjectable<String, BigInteger>, BLUCacheTrivial<String, BigInteger, Failure>> pair =
      this.newCache(8, 2);
    final BLUCacheTrivial<String, BigInteger, Failure> cache = pair.second;

    final LUCacheLoaderFaultInjectable<String, BigInteger> loader =
      pair.first;
    loader.setLoadedValue(BigInteger.valueOf(23L));
    loader.setLoadedValueSize(BigInteger.ONE);

    final BLUCacheReceipt<String, BigInteger> r0 = cache.bluCacheGet("key0");
    Assert.assertEquals(BigInteger.ONE, cache.cacheSize());
    Assert.assertEquals(BigInteger.ONE, cache.cacheItemCount());
    Assert.assertTrue(cache.cacheIsBorrowed("key0"));
    Assert.assertFalse(cache.cacheIsAvailable("key0"));
    Assert.assertTrue(cache.cacheIsCached("key0"));

    r0.returnToCache();
    Assert.assertEquals(BigInteger.ONE, cache.cacheSize());
    Assert.assertEquals(BigInteger.ONE, cache.cacheItemCount());
    Assert.assertFalse(cache.cacheIsBorrowed("key0"));
    Assert.assertTrue(cache.cacheIsAvailable("key0"));
    Assert.assertTrue(cache.cacheIsCached("key0"));
  }

  @Test(expected = JCacheException.JCacheExceptionTooManyBorrows.class) public
    void
    testBorrow_1()
      throws Failure,
        ConstraintError,
        JCacheException
  {
    final Pair<LUCacheLoaderFaultInjectable<String, BigInteger>, BLUCacheTrivial<String, BigInteger, Failure>> pair =
      this.newCache(8, 2);
    final BLUCacheTrivial<String, BigInteger, Failure> cache = pair.second;

    final LUCacheLoaderFaultInjectable<String, BigInteger> loader =
      pair.first;
    loader.setLoadedValue(BigInteger.valueOf(23L));
    loader.setLoadedValueSize(BigInteger.ONE);

    try {
      cache.bluCacheGet("key0");
      cache.bluCacheGet("key0");
      Assert.assertEquals(BigInteger.valueOf(2), cache.cacheSize());
      Assert.assertEquals(BigInteger.valueOf(2), cache.cacheItemCount());
      Assert.assertTrue(cache.cacheIsBorrowed("key0"));
      Assert.assertFalse(cache.cacheIsAvailable("key0"));
      Assert.assertTrue(cache.cacheIsCached("key0"));
      Assert.assertTrue(cache.cacheIsBorrowed("key0"));
      Assert.assertFalse(cache.cacheIsAvailable("key0"));
      Assert.assertTrue(cache.cacheIsCached("key0"));
    } catch (final JCacheException _) {
      Assert.fail();
    }

    cache.bluCacheGet("key0");
  }

  @Test public void testBorrow_2()
    throws Failure,
      ConstraintError,
      JCacheException
  {
    final Pair<LUCacheLoaderFaultInjectable<String, BigInteger>, BLUCacheTrivial<String, BigInteger, Failure>> pair =
      this.newCache(8, 2);
    final BLUCacheTrivial<String, BigInteger, Failure> cache = pair.second;

    final LUCacheLoaderFaultInjectable<String, BigInteger> loader =
      pair.first;
    loader.setLoadedValue(BigInteger.valueOf(23L));
    loader.setLoadedValueSize(BigInteger.ONE);

    for (int index = 0; index < 10; ++index) {
      final BLUCacheReceipt<String, BigInteger> r = cache.bluCacheGet("key0");
      Assert.assertEquals(BigInteger.ONE, cache.cacheSize());
      Assert.assertEquals(BigInteger.ONE, cache.cacheItemCount());
      Assert.assertTrue(cache.cacheIsBorrowed("key0"));
      Assert.assertFalse(cache.cacheIsAvailable("key0"));
      Assert.assertTrue(cache.cacheIsCached("key0"));
      r.returnToCache();
      Assert.assertEquals(BigInteger.ONE, cache.cacheSize());
      Assert.assertEquals(BigInteger.ONE, cache.cacheItemCount());
      Assert.assertFalse(cache.cacheIsBorrowed("key0"));
      Assert.assertTrue(cache.cacheIsAvailable("key0"));
      Assert.assertTrue(cache.cacheIsCached("key0"));
    }
  }

  @Test public void testBorrow_3()
    throws Failure,
      ConstraintError,
      JCacheException
  {
    final Pair<LUCacheLoaderFaultInjectable<String, BigInteger>, BLUCacheTrivial<String, BigInteger, Failure>> pair =
      this.newCache(8, 2);
    final BLUCacheTrivial<String, BigInteger, Failure> cache = pair.second;

    final LUCacheLoaderFaultInjectable<String, BigInteger> loader =
      pair.first;
    loader.setLoadedValue(BigInteger.valueOf(23L));
    loader.setLoadedValueSize(BigInteger.ONE);

    final EventLog<String, BigInteger> elog =
      new EventLog<String, BigInteger>();
    cache.cacheEventsSubscribe(elog);

    final BLUCacheReceipt<String, BigInteger> r = cache.bluCacheGet("key0");
    Assert.assertTrue(elog.loaded);
    Assert.assertEquals("key0", elog.loaded_key);
    Assert.assertEquals(BigInteger.valueOf(23L), elog.loaded_value);
    Assert.assertEquals(BigInteger.ONE, elog.loaded_size);

    Assert.assertEquals(BigInteger.ONE, cache.cacheSize());
    Assert.assertEquals(BigInteger.ONE, cache.cacheItemCount());
    Assert.assertTrue(cache.cacheIsBorrowed("key0"));
    Assert.assertFalse(cache.cacheIsAvailable("key0"));
    Assert.assertTrue(cache.cacheIsCached("key0"));
  }

  @Test public void testDelete_0()
    throws Failure,
      ConstraintError,
      JCacheException
  {
    final int bound = 4;
    final Pair<LUCacheLoaderFaultInjectable<String, BigInteger>, BLUCacheTrivial<String, BigInteger, Failure>> pair =
      this.newCache(bound, 2);
    final BLUCacheTrivial<String, BigInteger, Failure> cache = pair.second;

    final JCacheEvents<String, BigInteger> elog =
      new EventLog<String, BigInteger>();
    pair.second.cacheEventsSubscribe(elog);

    for (int index = 0; index < 32; ++index) {
      pair.first.setLoadedValue(BigInteger.ONE);
      pair.first.setLoadedValueSize(BigInteger.ONE);
      cache.bluCacheGet("key" + index);
    }

    Assert.assertEquals(BigInteger.valueOf(32), cache.cacheSize());
    Assert.assertEquals(BigInteger.valueOf(32), cache.cacheItemCount());

    cache.cacheDelete();

    Assert.assertEquals(BigInteger.ZERO, cache.cacheSize());
    Assert.assertEquals(BigInteger.ZERO, cache.cacheItemCount());

    for (int index = 0; index < 32; ++index) {
      final String key = "key" + index;
      Assert.assertFalse(cache.cacheIsAvailable(key));
      Assert.assertFalse(cache.cacheIsCached(key));
      Assert.assertFalse(cache.cacheIsBorrowed(key));
    }
  }

  @Test public void testDelete_1()
    throws Failure,
      ConstraintError,
      JCacheException
  {
    final int bound = 4;
    final Pair<LUCacheLoaderFaultInjectable<String, BigInteger>, BLUCacheTrivial<String, BigInteger, Failure>> pair =
      this.newCache(bound, 2);
    final BLUCacheTrivial<String, BigInteger, Failure> cache = pair.second;

    final EventLog<String, BigInteger> elog =
      new EventLog<String, BigInteger>();
    pair.second.cacheEventsSubscribe(elog);

    for (int index = 0; index < 32; ++index) {
      pair.first.setLoadedValue(BigInteger.ONE);
      pair.first.setLoadedValueSize(BigInteger.ONE);
      cache.bluCacheGet("key" + index);
    }

    Assert.assertEquals(BigInteger.valueOf(32), cache.cacheSize());
    Assert.assertEquals(BigInteger.valueOf(32), cache.cacheItemCount());

    pair.first.setCloseFailure(true);
    cache.cacheDelete();

    Assert.assertTrue(elog.close_error);
  }

  @Test public void testEvict_0()
    throws Failure,
      ConstraintError,
      JCacheException
  {
    final int bound = 4;
    final Pair<LUCacheLoaderFaultInjectable<String, BigInteger>, BLUCacheTrivial<String, BigInteger, Failure>> pair =
      this.newCache(bound, 2);
    final BLUCacheTrivial<String, BigInteger, Failure> cache = pair.second;

    final LUCacheLoaderFaultInjectable<String, BigInteger> loader =
      pair.first;
    final JCacheEvents<String, BigInteger> elog =
      new EventLog<String, BigInteger>();
    pair.second.cacheEventsSubscribe(elog);

    final ArrayList<BLUCacheReceipt<String, BigInteger>> receipts =
      new ArrayList<BLUCacheReceipt<String, BigInteger>>();

    for (int index = 0; index < 32; ++index) {
      final int return_index = index - bound;
      if (return_index >= 0) {
        final BLUCacheReceipt<String, BigInteger> r =
          receipts.get(return_index);
        System.out.println("Returning " + r);
        r.returnToCache();
      }

      loader.setLoadedValue(BigInteger.valueOf(23L));
      loader.setLoadedValueSize(BigInteger.ONE);

      final String key = "key" + index;
      final BigInteger count = BigInteger.valueOf(Math.min(bound, index + 1));

      final BLUCacheReceipt<String, BigInteger> r = cache.bluCacheGet(key);
      Assert.assertEquals(count, cache.cacheSize());
      Assert.assertEquals(count, cache.cacheItemCount());
      Assert.assertTrue(cache.cacheIsBorrowed(key));
      Assert.assertFalse(cache.cacheIsAvailable(key));
      Assert.assertTrue(cache.cacheIsCached(key));

      receipts.add(r);
    }

    Assert.assertEquals(BigInteger.valueOf(4), cache.cacheSize());
    Assert.assertEquals(BigInteger.valueOf(4), cache.cacheItemCount());
  }

  @Test(expected = JCacheExceptionLoaderReturnedNull.class) public
    void
    testLoaderReturnedNull()
      throws Failure,
        ConstraintError,
        JCacheException
  {
    final Pair<LUCacheLoaderFaultInjectable<String, BigInteger>, BLUCacheTrivial<String, BigInteger, Failure>> pair =
      this.newCache(8, 2);

    pair.first.setFailure(false);
    pair.first.setLoadedValue(null);
    pair.first.setLoadedValueSize(BigInteger.valueOf(1));
    pair.second.bluCacheGet("key0");
  }

  @Test(expected = Failure.class) public void testLoadFailure()
    throws Failure,
      ConstraintError,
      JCacheException
  {
    final Pair<LUCacheLoaderFaultInjectable<String, BigInteger>, BLUCacheTrivial<String, BigInteger, Failure>> pair =
      this.newCache(8, 2);

    pair.first.setFailure(true);
    pair.first.setLoadedValue(BigInteger.valueOf(23));
    pair.first.setLoadedValueSize(BigInteger.valueOf(4));
    pair.second.bluCacheGet("key0");
  }

  @Test(expected = JCacheExceptionObjectTooLarge.class) public
    void
    testLoadHugeSize()
      throws Failure,
        ConstraintError,
        JCacheException
  {
    final Pair<LUCacheLoaderFaultInjectable<String, BigInteger>, BLUCacheTrivial<String, BigInteger, Failure>> pair =
      this.newCache(8, 2);

    pair.first.setFailure(false);
    pair.first.setLoadedValue(BigInteger.valueOf(1));
    pair.first.setLoadedValueSize(BigInteger.valueOf(33L));
    pair.second.bluCacheGet("key0");
  }

  @Test(expected = JCacheExceptionObjectTooSmall.class) public
    void
    testLoadNegativeSize()
      throws Failure,
        ConstraintError,
        JCacheException
  {
    final Pair<LUCacheLoaderFaultInjectable<String, BigInteger>, BLUCacheTrivial<String, BigInteger, Failure>> pair =
      this.newCache(8, 2);

    pair.first.setFailure(false);
    pair.first.setLoadedValue(BigInteger.valueOf(1));
    pair.first.setLoadedValueSize(BigInteger.valueOf(-1));
    pair.second.bluCacheGet("key0");
  }

  @Test public void testNew()
  {
    final Pair<LUCacheLoaderFaultInjectable<String, BigInteger>, BLUCacheTrivial<String, BigInteger, Failure>> pair =
      this.newCache(8, 2);
    final BLUCacheTrivial<String, BigInteger, Failure> cache = pair.second;

    Assert.assertEquals(BigInteger.ZERO, cache.cacheSize());
    Assert.assertEquals(BigInteger.ZERO, cache.cacheItemCount());
  }

  @Test(expected = ConstraintError.class) public void testReceipt_0()
    throws Failure,
      ConstraintError,
      JCacheException
  {
    final int bound = 4;
    final Pair<LUCacheLoaderFaultInjectable<String, BigInteger>, BLUCacheTrivial<String, BigInteger, Failure>> pair =
      this.newCache(bound, 2);

    final EventLog<String, BigInteger> elog =
      new EventLog<String, BigInteger>();
    pair.first.setLoadedValue(BigInteger.ONE);
    pair.first.setLoadedValueSize(BigInteger.ONE);
    pair.second.cacheEventsSubscribe(elog);

    final BLUCacheReceipt<String, BigInteger> r =
      pair.second.bluCacheGet("key0");

    r.returnToCache();
    r.returnToCache();
  }
}
