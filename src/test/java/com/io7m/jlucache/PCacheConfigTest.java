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

package com.io7m.jlucache;

import javax.annotation.Nonnull;

import net.java.quickcheck.QuickCheck;
import net.java.quickcheck.characteristic.AbstractCharacteristic;
import net.java.quickcheck.generator.support.LongGenerator;

import org.junit.Assert;
import org.junit.Test;

import com.io7m.jaux.Constraints.ConstraintError;
import com.io7m.jlucache.PCacheConfig.Builder;

public final class PCacheConfigTest
{
  @SuppressWarnings("static-method") @Test public void testAll()
  {
    QuickCheck.forAllVerbose(
      new LongGenerator(1, Integer.MAX_VALUE - 3),
      new AbstractCharacteristic<Long>() {
        @Override protected void doSpecify(
          final @Nonnull Long x)
          throws Throwable
        {
          final Long y = Long.valueOf(x.longValue() + 1);
          final Builder b = PCacheConfig.newBuilder();
          b.setMaximumAge(x.longValue());
          b.setMaximumSize(x.longValue() + 1);
          Assert.assertEquals(x, Long.valueOf(b.getMaximumAge()));
          Assert.assertEquals(y, Long.valueOf(b.getMaximumSize()));
          final PCacheConfig c = b.create();
          final PCacheConfig d = b.create();
          Assert.assertEquals(x, Long.valueOf(c.getMaximumAge()));
          Assert.assertEquals(y, Long.valueOf(c.getMaximumSize()));
          Assert.assertEquals(c, c);
          Assert.assertEquals(c, d);
          Assert.assertEquals(d, c);
          Assert.assertTrue(c.hashCode() == d.hashCode());
          Assert.assertEquals(c.toString(), d.toString());
          Assert.assertFalse(c.equals(null));
          Assert.assertFalse(c.equals(Integer.valueOf(23)));

          final Long z = Long.valueOf(y.longValue() + 1);
          final Long w = Long.valueOf(z.longValue() + 1);
          b.setMaximumAge(z.longValue());
          final PCacheConfig e = b.create();
          b.setMaximumSize(w.longValue());
          final PCacheConfig f = b.create();

          Assert.assertFalse(c.equals(e));
          Assert.assertFalse(c.equals(f));
          Assert.assertFalse(d.equals(f));
          Assert.assertFalse(d.equals(e));
          Assert.assertFalse(e.equals(f));
        }
      });
  }

  @SuppressWarnings("static-method") @Test(expected = ConstraintError.class) public
    void
    testNoLimit()
      throws ConstraintError
  {
    final Builder b = PCacheConfig.newBuilder();
    b.setNoMaximumAge();
    b.setNoMaximumSize();
    b.create();
  }
}
