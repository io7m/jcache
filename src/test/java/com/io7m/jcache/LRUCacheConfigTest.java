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

import net.java.quickcheck.QuickCheck;
import net.java.quickcheck.characteristic.AbstractCharacteristic;

import org.junit.Assert;
import org.junit.Test;

import com.io7m.jcache.LRUCacheConfig;

public class LRUCacheConfigTest
{
  @SuppressWarnings("static-method") @Test public void testEquals()
  {
    QuickCheck.forAllVerbose(
      new LRUCacheConfigGenerator(),
      new AbstractCharacteristic<LRUCacheConfig>() {
        @Override protected void doSpecify(
          final LRUCacheConfig config)
          throws Throwable
        {
          Assert.assertEquals(config, config);
          Assert.assertFalse(config.equals(null));
          Assert.assertFalse(config.equals(Integer.valueOf(23)));
          Assert.assertEquals(config, new LRUCacheConfig(config));

          final LRUCacheConfig diff =
            config.withMaximumCapacity(config.getMaximumCapacity() + 1);
          Assert.assertFalse(config.equals(diff));

          Assert.assertTrue(config.hashCode() == config.hashCode());
        }
      });
  }
}
