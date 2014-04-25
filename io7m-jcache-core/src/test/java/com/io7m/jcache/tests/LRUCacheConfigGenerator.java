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

import net.java.quickcheck.Generator;
import net.java.quickcheck.generator.support.LongGenerator;

import com.io7m.jcache.LRUCacheConfig;

public final class LRUCacheConfigGenerator implements
  Generator<LRUCacheConfig>
{
  private final @Nonnull LongGenerator long_gen;

  public LRUCacheConfigGenerator()
  {
    this.long_gen = new LongGenerator(1, Long.MAX_VALUE);
  }

  @SuppressWarnings("boxing") @Override public @Nonnull LRUCacheConfig next()
  {
    final BigInteger max_capacity = BigInteger.valueOf(this.long_gen.next());
    return LRUCacheConfig.empty().withMaximumCapacity(max_capacity);
  }
}
