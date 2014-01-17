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
 * Periodic cache configuration.
 */

@Immutable public final class PCacheConfig
{
  /**
   * A mutable configuration builder.
   */

  public interface Builder
  {
    /**
     * @return A new configuration with all of the settings configured so far
     * 
     * @throws ConstraintError
     *           Iff both the maximum age and maximum size are unlimited.
     */

    public @Nonnull PCacheConfig create()
      throws ConstraintError;

    /**
     * @return The current maximum age.
     */

    public @Nonnull BigInteger getMaximumAge();

    /**
     * @return The current maximum size, in units.
     */

    public @Nonnull BigInteger getMaximumSize();

    /**
     * Set the current maximum age.
     * 
     * @param age
     *          The new maximum age
     * @throws ConstraintError
     *           Iff <code>age &lt; 1</code>.
     */

    public void setMaximumAge(
      final @Nonnull BigInteger age)
      throws ConstraintError;

    /**
     * Set the current maximum size.
     * 
     * @param size
     *          The maximum size in units
     * @throws ConstraintError
     *           Iff <code>size &lt; 1</code>.
     */

    public void setMaximumSize(
      final @Nonnull BigInteger size)
      throws ConstraintError;

    /**
     * Remove any limit on the maximum age of cached items.
     */

    public void setNoMaximumAge();

    /**
     * Remove any limit on the maximum size of the cache.
     */

    public void setNoMaximumSize();
  }

  /**
   * @return A new builder from which to create configurations.
   */

  public static @Nonnull Builder newBuilder()
  {
    return new Builder() {
      private BigInteger maximum_age  = BigInteger.ONE;
      private BigInteger maximum_size = BigInteger.ONE;

      @SuppressWarnings("synthetic-access") @Override public
        PCacheConfig
        create()
          throws ConstraintError
      {
        return new PCacheConfig(this.maximum_size, this.maximum_age);
      }

      @Override public BigInteger getMaximumAge()
      {
        return this.maximum_age;
      }

      @Override public BigInteger getMaximumSize()
      {
        return this.maximum_size;
      }

      @Override public void setMaximumAge(
        final @Nonnull BigInteger age)
        throws ConstraintError
      {
        Constraints.constrainNotNull(age, "Maximum age");
        Constraints.constrainArbitrary(
          age.compareTo(BigInteger.ZERO) > 0,
          "Maximum age is > 0");
        this.maximum_age = age;
      }

      @Override public void setMaximumSize(
        final @Nonnull BigInteger size)
        throws ConstraintError
      {
        Constraints.constrainNotNull(size, "Maximum size");
        Constraints.constrainArbitrary(
          size.compareTo(BigInteger.ZERO) > 0,
          "Maximum age is > 0");
        this.maximum_size = size;

      }

      @Override public void setNoMaximumAge()
      {
        this.maximum_age = BigInteger.ZERO;
      }

      @Override public void setNoMaximumSize()
      {
        this.maximum_size = BigInteger.ZERO;
      }
    };
  }

  private final BigInteger maximum_age;
  private final BigInteger maximum_size;

  private PCacheConfig(
    final BigInteger max_size,
    final BigInteger max_age)
    throws ConstraintError
  {
    Constraints.constrainArbitrary(
      (max_age.compareTo(BigInteger.ZERO) > 0)
        || (max_size.compareTo(BigInteger.ZERO) > 0),
      "Either maximum age or size is non-zero");

    this.maximum_age = max_age;
    this.maximum_size = max_size;
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
    final PCacheConfig other = (PCacheConfig) obj;
    if (this.maximum_age != other.maximum_age) {
      return false;
    }
    if (this.maximum_size != other.maximum_size) {
      return false;
    }
    return true;
  }

  /**
   * <p>
   * Retrieve the current maximum age <code>A</code> of items in the cache.
   * Items that have not been referenced for at least <code>A</code> periods
   * will be removed from the cache at the end of the current period.
   * </p>
   * 
   * @return The maximum age of items in the cache, or <code>0</code> if there
   *         is no maximum age.
   */

  public BigInteger getMaximumAge()
  {
    return this.maximum_age;
  }

  /**
   * @return The maximum size of the cache, in units, or <code>0</code> if
   *         there is no maximum size.
   */

  public BigInteger getMaximumSize()
  {
    return this.maximum_size;
  }

  @Override public int hashCode()
  {
    final int prime = 31;
    int result = 1;
    result = (prime * result) + this.maximum_age.hashCode();
    result = (prime * result) + this.maximum_size.hashCode();
    return result;
  }

  @Override public String toString()
  {
    final StringBuilder builder2 = new StringBuilder();
    builder2.append("[PCacheConfig maximum_size=");
    builder2.append(this.maximum_size);
    builder2.append(" maximum_age=");
    builder2.append(this.maximum_age);
    builder2.append("]");
    return builder2.toString();
  }
}
