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

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import com.io7m.jaux.Constraints;
import com.io7m.jaux.Constraints.ConstraintError;

/**
 * An immutable configuration type for BLU caches.
 */

@Immutable public final class BLUCacheConfig
{
  /**
   * @return a new empty configuration.
   * 
   * @throws ConstraintError
   *           Iff an internal constraint error occurs.
   */

  public static @Nonnull BLUCacheConfig empty()
    throws ConstraintError
  {
    return new BLUCacheConfig(BigInteger.ZERO, BigInteger.ONE);
  }

  /**
   * Construct a new cache configuration based on <tt>other</tt>.
   * 
   * @param other
   *          An existing configuration
   * @return A new configuration derived from <tt>other</tt>
   * @throws ConstraintError
   *           Iff <code>other == null</code>.
   */

  public static @Nonnull BLUCacheConfig withConfig(
    final @Nonnull BLUCacheConfig other)
    throws ConstraintError
  {
    Constraints.constrainNotNull(other, "Configuration");
    return new BLUCacheConfig(
      other.getMaximumCapacity(),
      other.getMaximumBorrowsPerKey());
  }

  private final @Nonnull BigInteger max_borrows;
  private final @Nonnull BigInteger max_capacity;

  private BLUCacheConfig(
    final @Nonnull BigInteger in_max_capacity,
    final @Nonnull BigInteger in_max_borrows)
    throws ConstraintError
  {
    Constraints.constrainNotNull(in_max_capacity, "Maximum capacity");
    Constraints.constrainNotNull(in_max_borrows, "Maximum borrows");
    Constraints.constrainArbitrary(
      in_max_capacity.compareTo(BigInteger.ZERO) >= 0,
      "Maximum capacity is at least 0");
    Constraints.constrainArbitrary(
      in_max_borrows.compareTo(BigInteger.ONE) >= 0,
      "Maximum borrow is at least 1");

    this.max_capacity = in_max_capacity;
    this.max_borrows = in_max_borrows;
  }

  @Override public boolean equals(
    final Object obj)
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

  public @Nonnull BigInteger getMaximumBorrowsPerKey()
  {
    return this.max_borrows;
  }

  /**
   * @return The maximum capacity of the cache that will be created.
   */

  public @Nonnull BigInteger getMaximumCapacity()
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
    return builder.toString();
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
   * 
   * @param max
   *          The maximum number of borrowed items per key
   * @return A new cache configuration
   * @throws ConstraintError
   *           Iff any internal constraint error occurs
   */

  public @Nonnull BLUCacheConfig withMaximumBorrowsPerKey(
    final @Nonnull BigInteger max)
    throws ConstraintError
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
   * @throws ConstraintError
   *           Iff any internal constraint error occurs
   */

  public @Nonnull BLUCacheConfig withMaximumCapacity(
    final @Nonnull BigInteger max)
    throws ConstraintError
  {
    return new BLUCacheConfig(max, this.max_borrows);
  }
}
