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
import java.util.ArrayList;

import javax.annotation.Nonnull;

import org.junit.Assert;
import org.junit.Test;

import com.io7m.jcache.BLUCacheConfig;
import com.io7m.jcache.BLUCacheReceiptType;
import com.io7m.jcache.BLUCacheTrivial;
import com.io7m.jcache.JCacheEventsType;
import com.io7m.jcache.JCacheException;
import com.io7m.jcache.JCacheException.JCacheExceptionLoaderReturnedNull;
import com.io7m.jcache.JCacheException.JCacheExceptionObjectTooLarge;
import com.io7m.jcache.JCacheException.JCacheExceptionObjectTooSmall;
import com.io7m.jcache.tests.LUCacheLoaderFaultInjectable.Failure;
import com.io7m.jfunctional.Pair;

public class BLUCacheTrivialTest
{
  @SuppressWarnings("static-method") private @Nonnull
    <K, TVIEW, TCACHE extends TVIEW>
    Pair<LUCacheLoaderFaultInjectable<K, TCACHE>, BLUCacheTrivial<K, TVIEW, TCACHE, Failure>>
    newCache(
      final long capacity,
      final long max_borrows)
  {
    final BLUCacheConfig config =
      BLUCacheConfig
        .empty()
        .withMaximumCapacity(BigInteger.valueOf(capacity))
        .withMaximumBorrowsPerKey(BigInteger.valueOf(max_borrows));

    final LUCacheLoaderFaultInjectable<K, TCACHE> loader =
      new LUCacheLoaderFaultInjectable<K, TCACHE>();
    final BLUCacheTrivial<K, TVIEW, TCACHE, Failure> cache =
      BLUCacheTrivial.newCache(loader, config);
    return Pair.pair(loader, cache);
  }

  @Test public void testBorrow_0()
    throws Failure,
      JCacheException
  {
    final Pair<LUCacheLoaderFaultInjectable<String, BigInteger>, BLUCacheTrivial<String, BigInteger, BigInteger, Failure>> pair =
      this.newCache(8, 2);
    final BLUCacheTrivial<String, BigInteger, BigInteger, Failure> cache =
      pair.getRight();

    final LUCacheLoaderFaultInjectable<String, BigInteger> loader =
      pair.getLeft();
    loader.setLoadedValue(BigInteger.valueOf(23L));
    loader.setLoadedValueSize(BigInteger.ONE);

    final BLUCacheReceiptType<String, BigInteger> r0 =
      cache.bluCacheGet("key0");
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
        JCacheException
  {
    final Pair<LUCacheLoaderFaultInjectable<String, BigInteger>, BLUCacheTrivial<String, BigInteger, BigInteger, Failure>> pair =
      this.newCache(8, 2);
    final BLUCacheTrivial<String, BigInteger, BigInteger, Failure> cache =
      pair.getRight();

    final LUCacheLoaderFaultInjectable<String, BigInteger> loader =
      pair.getLeft();
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
      JCacheException
  {
    final Pair<LUCacheLoaderFaultInjectable<String, BigInteger>, BLUCacheTrivial<String, BigInteger, BigInteger, Failure>> pair =
      this.newCache(8, 2);
    final BLUCacheTrivial<String, BigInteger, BigInteger, Failure> cache =
      pair.getRight();

    final LUCacheLoaderFaultInjectable<String, BigInteger> loader =
      pair.getLeft();
    loader.setLoadedValue(BigInteger.valueOf(23L));
    loader.setLoadedValueSize(BigInteger.ONE);

    for (int index = 0; index < 10; ++index) {
      final BLUCacheReceiptType<String, BigInteger> r =
        cache.bluCacheGet("key0");
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
      JCacheException
  {
    final Pair<LUCacheLoaderFaultInjectable<String, BigInteger>, BLUCacheTrivial<String, BigInteger, BigInteger, Failure>> pair =
      this.newCache(8, 2);
    final BLUCacheTrivial<String, BigInteger, BigInteger, Failure> cache =
      pair.getRight();

    final LUCacheLoaderFaultInjectable<String, BigInteger> loader =
      pair.getLeft();
    loader.setLoadedValue(BigInteger.valueOf(23L));
    loader.setLoadedValueSize(BigInteger.ONE);

    final EventLog<String, BigInteger> elog =
      new EventLog<String, BigInteger>();
    cache.cacheEventsSubscribe(elog);

    final BLUCacheReceiptType<String, BigInteger> r =
      cache.bluCacheGet("key0");
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

  @Test public void testBorrowNoLimit()
    throws Failure,
      JCacheException
  {
    final Pair<LUCacheLoaderFaultInjectable<String, BigInteger>, BLUCacheTrivial<String, BigInteger, BigInteger, Failure>> pair =
      this.newCache(8, 0);
    final BLUCacheTrivial<String, BigInteger, BigInteger, Failure> cache =
      pair.getRight();

    final LUCacheLoaderFaultInjectable<String, BigInteger> loader =
      pair.getLeft();
    loader.setLoadedValue(BigInteger.valueOf(23L));
    loader.setLoadedValueSize(BigInteger.ONE);

    final EventLog<String, BigInteger> elog =
      new EventLog<String, BigInteger>();
    cache.cacheEventsSubscribe(elog);

    final ArrayList<BLUCacheReceiptType<String, BigInteger>> borrows =
      new ArrayList<BLUCacheReceiptType<String, BigInteger>>();

    for (int index = 0; index < 1000; ++index) {
      final BLUCacheReceiptType<String, BigInteger> r =
        cache.bluCacheGet("key0");
      Assert.assertTrue(elog.loaded);
      Assert.assertEquals("key0", elog.loaded_key);
      Assert.assertEquals(BigInteger.valueOf(23L), elog.loaded_value);
      Assert.assertEquals(BigInteger.ONE, elog.loaded_size);

      Assert.assertEquals(BigInteger.valueOf(index + 1), cache.cacheSize());
      Assert.assertEquals(
        BigInteger.valueOf(index + 1),
        cache.cacheItemCount());
      Assert.assertTrue(cache.cacheIsBorrowed("key0"));
      Assert.assertFalse(cache.cacheIsAvailable("key0"));
      Assert.assertTrue(cache.cacheIsCached("key0"));

      borrows.add(r);
    }

    for (int index = 0; index < 1000; ++index) {
      borrows.get(index).returnToCache();
    }

    Assert.assertEquals(BigInteger.valueOf(8), cache.cacheSize());
  }

  @Test public void testDelete_0()
    throws Failure,
      JCacheException
  {
    final int bound = 4;
    final Pair<LUCacheLoaderFaultInjectable<String, BigInteger>, BLUCacheTrivial<String, BigInteger, BigInteger, Failure>> pair =
      this.newCache(bound, 2);
    final BLUCacheTrivial<String, BigInteger, BigInteger, Failure> cache =
      pair.getRight();

    final JCacheEventsType<String, BigInteger> elog =
      new EventLog<String, BigInteger>();
    pair.getRight().cacheEventsSubscribe(elog);

    for (int index = 0; index < 32; ++index) {
      pair.getLeft().setLoadedValue(BigInteger.ONE);
      pair.getLeft().setLoadedValueSize(BigInteger.ONE);
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
      JCacheException
  {
    final int bound = 4;
    final Pair<LUCacheLoaderFaultInjectable<String, BigInteger>, BLUCacheTrivial<String, BigInteger, BigInteger, Failure>> pair =
      this.newCache(bound, 2);
    final BLUCacheTrivial<String, BigInteger, BigInteger, Failure> cache =
      pair.getRight();

    final EventLog<String, BigInteger> elog =
      new EventLog<String, BigInteger>();
    pair.getRight().cacheEventsSubscribe(elog);

    for (int index = 0; index < 32; ++index) {
      pair.getLeft().setLoadedValue(BigInteger.ONE);
      pair.getLeft().setLoadedValueSize(BigInteger.ONE);
      cache.bluCacheGet("key" + index);
    }

    Assert.assertEquals(BigInteger.valueOf(32), cache.cacheSize());
    Assert.assertEquals(BigInteger.valueOf(32), cache.cacheItemCount());

    pair.getLeft().setCloseFailure(true);
    cache.cacheDelete();

    Assert.assertTrue(elog.close_error);
  }

  @Test public void testEvict_0()
    throws Failure,
      JCacheException
  {
    final int bound = 4;
    final Pair<LUCacheLoaderFaultInjectable<String, BigInteger>, BLUCacheTrivial<String, BigInteger, BigInteger, Failure>> pair =
      this.newCache(bound, 2);
    final BLUCacheTrivial<String, BigInteger, BigInteger, Failure> cache =
      pair.getRight();

    final LUCacheLoaderFaultInjectable<String, BigInteger> loader =
      pair.getLeft();
    final JCacheEventsType<String, BigInteger> elog =
      new EventLog<String, BigInteger>();
    pair.getRight().cacheEventsSubscribe(elog);

    final ArrayList<BLUCacheReceiptType<String, BigInteger>> receipts =
      new ArrayList<BLUCacheReceiptType<String, BigInteger>>();

    for (int index = 0; index < 32; ++index) {
      final int return_index = index - bound;
      if (return_index >= 0) {
        final BLUCacheReceiptType<String, BigInteger> r =
          receipts.get(return_index);
        System.out.println("Returning " + r);
        r.returnToCache();
      }

      loader.setLoadedValue(BigInteger.valueOf(23L));
      loader.setLoadedValueSize(BigInteger.ONE);

      final String key = "key" + index;
      final BigInteger count = BigInteger.valueOf(Math.min(bound, index + 1));

      final BLUCacheReceiptType<String, BigInteger> r =
        cache.bluCacheGet(key);
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
        JCacheException
  {
    final Pair<LUCacheLoaderFaultInjectable<String, BigInteger>, BLUCacheTrivial<String, BigInteger, BigInteger, Failure>> pair =
      this.newCache(8, 2);

    pair.getLeft().setFailure(false);
    pair.getLeft().setLoadedValue(null);
    pair.getLeft().setLoadedValueSize(BigInteger.valueOf(1));
    pair.getRight().bluCacheGet("key0");
  }

  @Test(expected = Failure.class) public void testLoadFailure()
    throws Failure,
      JCacheException
  {
    final Pair<LUCacheLoaderFaultInjectable<String, BigInteger>, BLUCacheTrivial<String, BigInteger, BigInteger, Failure>> pair =
      this.newCache(8, 2);

    pair.getLeft().setFailure(true);
    pair.getLeft().setLoadedValue(BigInteger.valueOf(23));
    pair.getLeft().setLoadedValueSize(BigInteger.valueOf(4));
    pair.getRight().bluCacheGet("key0");
  }

  @Test(expected = JCacheExceptionObjectTooLarge.class) public
    void
    testLoadHugeSize()
      throws Failure,
        JCacheException
  {
    final Pair<LUCacheLoaderFaultInjectable<String, BigInteger>, BLUCacheTrivial<String, BigInteger, BigInteger, Failure>> pair =
      this.newCache(8, 2);

    pair.getLeft().setFailure(false);
    pair.getLeft().setLoadedValue(BigInteger.valueOf(1));
    pair.getLeft().setLoadedValueSize(BigInteger.valueOf(33L));
    pair.getRight().bluCacheGet("key0");
  }

  @Test(expected = JCacheExceptionObjectTooSmall.class) public
    void
    testLoadNegativeSize()
      throws Failure,
        JCacheException
  {
    final Pair<LUCacheLoaderFaultInjectable<String, BigInteger>, BLUCacheTrivial<String, BigInteger, BigInteger, Failure>> pair =
      this.newCache(8, 2);

    pair.getLeft().setFailure(false);
    pair.getLeft().setLoadedValue(BigInteger.valueOf(1));
    pair.getLeft().setLoadedValueSize(BigInteger.valueOf(-1));
    pair.getRight().bluCacheGet("key0");
  }

  @Test public void testNew()
  {
    final Pair<LUCacheLoaderFaultInjectable<String, BigInteger>, BLUCacheTrivial<String, BigInteger, BigInteger, Failure>> pair =
      this.newCache(8, 2);
    final BLUCacheTrivial<String, BigInteger, BigInteger, Failure> cache =
      pair.getRight();

    Assert.assertEquals(BigInteger.ZERO, cache.cacheSize());
    Assert.assertEquals(BigInteger.ZERO, cache.cacheItemCount());
  }

  @Test(expected = IllegalStateException.class) public void testReceipt_0()
    throws Failure,
      JCacheException
  {
    final int bound = 4;
    final Pair<LUCacheLoaderFaultInjectable<String, BigInteger>, BLUCacheTrivial<String, BigInteger, BigInteger, Failure>> pair =
      this.newCache(bound, 2);

    final EventLog<String, BigInteger> elog =
      new EventLog<String, BigInteger>();
    pair.getLeft().setLoadedValue(BigInteger.ONE);
    pair.getLeft().setLoadedValueSize(BigInteger.ONE);
    pair.getRight().cacheEventsSubscribe(elog);

    final BLUCacheReceiptType<String, BigInteger> r =
      pair.getRight().bluCacheGet("key0");

    r.returnToCache();
    r.returnToCache();
  }
}
