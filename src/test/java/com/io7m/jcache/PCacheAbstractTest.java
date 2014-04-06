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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigInteger;
import java.util.BitSet;

import org.junit.Assert;
import org.junit.Test;

import com.io7m.jaux.Constraints.ConstraintError;

public class PCacheAbstractTest
{
  private final class Completeness extends
    PCacheAbstract<Integer, Integer, IllegalArgumentException>
  {
    protected Completeness(
      final PCacheType<Integer, Integer, IllegalArgumentException> in_cache)
      throws ConstraintError
    {
      super(in_cache);
    }
  }

  @Test public void testComplete()
    throws ConstraintError,
      IllegalArgumentException,
      JCacheException
  {
    final BitSet calls = new BitSet();
    final Completeness c =
      new Completeness(
        new PCacheType<Integer, Integer, IllegalArgumentException>() {

          @Override public void cacheDelete()
          {
            Assert.assertFalse(calls.get(7));
            calls.set(7);
          }

          @Override public void cacheEventsSubscribe(
            final JCacheEventsType<Integer, Integer> events)
            throws ConstraintError
          {
            Assert.assertFalse(calls.get(6));
            calls.set(6);
          }

          @Override public void cacheEventsUnsubscribe()
          {
            Assert.assertFalse(calls.get(5));
            calls.set(5);
          }

          @Override public Integer cacheGetPeriodic(
            final Integer key)
            throws ConstraintError,
              IllegalArgumentException,
              JCacheException
          {
            Assert.assertFalse(calls.get(3));
            calls.set(3);
            return Integer.valueOf(0);
          }

          @Override public boolean cacheIsCached(
            final Integer key)
            throws ConstraintError
          {
            Assert.assertFalse(calls.get(2));
            calls.set(2);
            return true;
          }

          @Override public BigInteger cacheItemCount()
          {
            Assert.assertFalse(calls.get(1));
            calls.set(1);
            return BigInteger.ONE;
          }

          @Override public void cachePeriodEnd()
            throws ConstraintError
          {
            Assert.assertFalse(calls.get(4));
            calls.set(4);
          }

          @Override public void cachePeriodStart()
            throws ConstraintError
          {
            Assert.assertFalse(calls.get(8));
            calls.set(8);
          }

          @Override public BigInteger cacheSize()
          {
            Assert.assertFalse(calls.get(0));
            calls.set(0);
            return BigInteger.ONE;
          }
        });

    c.cacheGetPeriodic(null);
    c.cachePeriodStart();
    c.cachePeriodEnd();
    c.cacheSize();
    c.cacheItemCount();
    c.cacheIsCached(null);
    c.cacheEventsSubscribe(null);
    c.cacheEventsUnsubscribe();
    c.cacheDelete();

    for (int index = 0; index <= 8; ++index) {
      Assert.assertTrue(calls.get(index));
    }
  }

  @SuppressWarnings("static-method") @Test public void testFinal()
  {
    final Method[] ms = PCacheAbstract.class.getMethods();
    for (final Method m : ms) {
      final String name = m.getName();
      if (name.equals("equals")) {
        continue;
      }
      if (name.equals("toString")) {
        continue;
      }
      if (name.equals("hashCode")) {
        continue;
      }

      System.out.println("Check final: " + name);
      Assert.assertTrue(Modifier.isFinal(m.getModifiers()));
    }
  }
}
