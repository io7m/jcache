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

import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;
import com.io7m.jranges.RangeCheck;

/**
 * An immutable configuration type for BLU caches.
 */

public final class BLUCacheConfig
{
  /**
   * @return a new empty configuration.
   */

  public static BLUCacheConfig empty()
  {
    return new BLUCacheConfig(BigInteger.ZERO, BigInteger.ONE);
  }

  /**
   * Construct a new cache configuration based on <tt>other</tt>.
   *
   * @param other
   *          An existing configuration
   * @return A new configuration derived from <tt>other</tt>
   */

  public static BLUCacheConfig withConfig(
    final BLUCacheConfig other)
  {
    NullCheck.notNull(other, "Other configuration");
    return new BLUCacheConfig(
      other.getMaximumCapacity(),
      other.getMaximumBorrowsPerKey());
  }

  private final BigInteger max_borrows;
  private final BigInteger max_capacity;

  private BLUCacheConfig(
    final BigInteger in_max_capacity,
    final BigInteger in_max_borrows)
  {
    this.max_capacity =
      RangeCheck.checkGreaterEqualBig(
        NullCheck.notNull(in_max_capacity, "Maximum capacity"),
        "Maximum capacity",
        NullCheck.notNull(BigInteger.ZERO),
        "Smallest maximum");
    this.max_borrows =
      RangeCheck.checkGreaterEqualBig(
        NullCheck.notNull(in_max_borrows, "Maximum borrows"),
        "Maximum borrows",
        NullCheck.notNull(BigInteger.ZERO),
        "Smallest maximum");
  }

  @Override public boolean equals(
    final @Nullable Object obj)
  {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (this.getClass() != obj.getClass()) {
      return false;
    }
    final BLUCacheConfig other = (BLUCacheConfig) obj;
    if (!this.max_borrows.equals(other.max_borrows)) {
      return false;
    }
    if (!this.max_capacity.equals(other.max_capacity)) {
      return false;
    }
    return true;
  }

  /**
   * @return The maximum number of borrows allowed per key.
   */

  public BigInteger getMaximumBorrowsPerKey()
  {
    return this.max_borrows;
  }

  /**
   * @return The maximum capacity of the cache that will be created.
   */

  public BigInteger getMaximumCapacity()
  {
    return this.max_capacity;
  }

  @Override public int hashCode()
  {
    final int prime = 31;
    int result = 1;
    result = (prime * result) + this.max_borrows.hashCode();
    result = (prime * result) + this.max_capacity.hashCode();
    return result;
  }

  @Override public String toString()
  {
    final StringBuilder builder = new StringBuilder();
    builder.append("[BLUCacheConfig max_capacity=");
    builder.append(this.max_capacity);
    builder.append(" max_borrows=");
    builder.append(this.max_borrows);
    builder.append("]");
    final String r = builder.toString();
    assert r != null;
    return r;
  }

  /**
   * <p>
   * Derive a configuration based on the existing configuration with a maximum
   * number of borrowed values <code>max</code>, for any given key.
   * </p>
   * <p>
   * This is mainly useful to detect misuse of the cache; large numbers of
   * borrowed items per key is usually indicative of code that is failing to
   * return items.
   * </p>
   * <p>
   * A value of zero means "no limit".
   * </p>
   *
   * @param max
   *          The maximum number of borrowed items per key
   * @return A new cache configuration
   */

  public BLUCacheConfig withMaximumBorrowsPerKey(
    final BigInteger max)
  {
    return new BLUCacheConfig(this.max_capacity, max);
  }

  /**
   * Derive a configuration based on the existing configuration with a maximum
   * capacity of <code>max</code>.
   *
   * @param max
   *          The maximum cache capacity
   * @return A new cache configuration
   */

  public BLUCacheConfig withMaximumCapacity(
    final BigInteger max)
  {
    return new BLUCacheConfig(max, this.max_borrows);
  }
}
