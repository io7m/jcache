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
 * Periodic cache configuration.
 */

 public final class PCacheConfig
{
  /**
   * A mutable configuration builder.
   */

  public interface BuilderType
  {
    /**
     * @return A new configuration with all of the settings configured so far
     */

    PCacheConfig create();

    /**
     * @return The current maximum age.
     */

    BigInteger getMaximumAge();

    /**
     * @return The current maximum size, in units.
     */

    BigInteger getMaximumSize();

    /**
     * Set the current maximum age. Must be <code>>= 1</code>.
     * 
     * @param age
     *          The new maximum age
     */

    void setMaximumAge(
      final BigInteger age);

    /**
     * Set the current maximum size. Must be <code>>= 1</code>.
     * 
     * @param size
     *          The maximum size in units
     */

    void setMaximumSize(
      final BigInteger size);

    /**
     * Remove any limit on the maximum age of cached items.
     */

    void setNoMaximumAge();

    /**
     * Remove any limit on the maximum size of the cache.
     */

    void setNoMaximumSize();
  }

  /**
   * @return A new builder from which to create configurations.
   */

  public static BuilderType newBuilder()
  {
    return new BuilderType() {
      private BigInteger maximum_age  = BigInteger.ONE;
      private BigInteger maximum_size = BigInteger.ONE;

      @SuppressWarnings("synthetic-access") @Override public
        PCacheConfig
        create()

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
        final BigInteger age)
      {
        this.maximum_age =
          RangeCheck.checkGreaterBig(
            age,
            "Maximum age",
            NullCheck.notNull(BigInteger.ZERO),
            "Smallest age");
      }

      @Override public void setMaximumSize(
        final BigInteger size)
      {
        this.maximum_size =
          RangeCheck.checkGreaterBig(
            size,
            "Maximum size",
            NullCheck.notNull(BigInteger.ZERO),
            "Smallest size");
      }

      @Override public void setNoMaximumAge()
      {
        this.maximum_age = NullCheck.notNull(BigInteger.ZERO);
      }

      @Override public void setNoMaximumSize()
      {
        this.maximum_size = NullCheck.notNull(BigInteger.ZERO);
      }
    };
  }

  private final BigInteger maximum_age;
  private final BigInteger maximum_size;

  private PCacheConfig(
    final BigInteger max_size,
    final BigInteger max_age)
  {
    NullCheck.notNull(max_age, "Maximum age");
    NullCheck.notNull(max_size, "Maximum size");

    final boolean ok =
      (max_age.compareTo(BigInteger.ZERO) > 0)
        || (max_size.compareTo(BigInteger.ZERO) > 0);
    if (ok == false) {
      throw new IllegalArgumentException(
        "Either maximum age or size must be non-zero");
    }

    this.maximum_age = max_age;
    this.maximum_size = max_size;
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
