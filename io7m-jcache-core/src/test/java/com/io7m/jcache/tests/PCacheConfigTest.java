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

import javax.annotation.Nonnull;

import net.java.quickcheck.QuickCheck;
import net.java.quickcheck.characteristic.AbstractCharacteristic;
import net.java.quickcheck.generator.support.LongGenerator;

import org.junit.Assert;
import org.junit.Test;

import com.io7m.jcache.PCacheConfig;
import com.io7m.jcache.PCacheConfig.BuilderType;

@SuppressWarnings("static-method") public final class PCacheConfigTest
{
  @Test public void testAll()
  {
    QuickCheck.forAllVerbose(
      new LongGenerator(1, Integer.MAX_VALUE - 3),
      new AbstractCharacteristic<Long>() {
        @Override protected void doSpecify(
          final @Nonnull Long x)
          throws Throwable
        {
          final BigInteger xi = BigInteger.valueOf(x.longValue());
          final BigInteger xii = xi.add(BigInteger.ONE);

          final Long y = Long.valueOf(xii.longValue());
          final BuilderType b = PCacheConfig.newBuilder();
          b.setMaximumAge(xi);
          b.setMaximumSize(xii);
          Assert.assertEquals(xi, b.getMaximumAge());
          Assert.assertEquals(xii, b.getMaximumSize());
          final PCacheConfig c = b.create();
          final PCacheConfig d = b.create();
          Assert.assertEquals(xi, c.getMaximumAge());
          Assert.assertEquals(xii, c.getMaximumSize());
          Assert.assertEquals(c, c);
          Assert.assertEquals(c, d);
          Assert.assertEquals(d, c);
          Assert.assertTrue(c.hashCode() == d.hashCode());
          Assert.assertEquals(c.toString(), d.toString());
          Assert.assertFalse(c.equals(null));
          Assert.assertFalse(c.equals(Integer.valueOf(23)));

          final BigInteger z = BigInteger.valueOf(y.longValue() + 1);
          final BigInteger w = BigInteger.valueOf(z.longValue() + 1);
          b.setMaximumAge(z);
          final PCacheConfig e = b.create();
          b.setMaximumSize(w);
          final PCacheConfig f = b.create();

          Assert.assertFalse(c.equals(e));
          Assert.assertFalse(c.equals(f));
          Assert.assertFalse(d.equals(f));
          Assert.assertFalse(d.equals(e));
          Assert.assertFalse(e.equals(f));
        }
      });
  }

  @Test(expected = IllegalArgumentException.class) public void testNoLimit()
  {
    final BuilderType b = PCacheConfig.newBuilder();
    b.setNoMaximumAge();
    b.setNoMaximumSize();
    b.create();
  }
}
