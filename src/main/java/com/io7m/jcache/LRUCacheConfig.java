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
 * An immutable configuration type for LRU caches.
 */

@Immutable public final class LRUCacheConfig
{
  /**
   * @return a new empty configuration.
   * 
   * @throws ConstraintError
   *           Iff an internal constraint error occurs.
   */

  public static @Nonnull LRUCacheConfig empty()
    throws ConstraintError
  {
    return new LRUCacheConfig(BigInteger.ZERO);
  }

  private final BigInteger max_capacity;

  private LRUCacheConfig(
    final BigInteger in_max_capacity)
    throws ConstraintError
  {
    Constraints.constrainArbitrary(
      in_max_capacity.compareTo(BigInteger.ZERO) >= 0,
      "Maximum capacity is at least 0");

    this.max_capacity = in_max_capacity;
  }

  /**
   * Construct a new cache configuration based on <tt>other</tt>.
   * 
   * @param other
   *          An existing configuration
   * @throws ConstraintError
   *           Iff <code>other == null</code>.
   */

  public LRUCacheConfig(
    final @Nonnull LRUCacheConfig other)
    throws ConstraintError
  {
    this(Constraints
      .constrainNotNull(other, "Configuration")
      .getMaximumCapacity());
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
    final LRUCacheConfig other = (LRUCacheConfig) obj;
    if (this.max_capacity != other.max_capacity) {
      return false;
    }
    return true;
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
    result = (prime * result) + this.max_capacity.hashCode();
    return result;
  }

  @Override public String toString()
  {
    final StringBuilder builder = new StringBuilder();
    builder.append("[LRUCacheConfig [max_capacity=");
    builder.append(this.max_capacity);
    builder.append("]]");
    return builder.toString();
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

  @SuppressWarnings("static-method") public @Nonnull
    LRUCacheConfig
    withMaximumCapacity(
      final BigInteger max)
      throws ConstraintError
  {
    return new LRUCacheConfig(max);
  }
}
