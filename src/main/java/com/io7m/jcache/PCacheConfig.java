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

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import com.io7m.jaux.Constraints;
import com.io7m.jaux.Constraints.ConstraintError;

@Immutable public final class PCacheConfig
{
  /**
   * A mutable configuration builder.
   */

  public interface Builder
  {
    /**
     * Create a new configuration.
     * 
     * @throws ConstraintError
     *           Iff both the maximum age and maximum size are unlimited.
     */

    public @Nonnull PCacheConfig create()
      throws ConstraintError;

    /**
     * Retrieve the current maximum age.
     */

    public long getMaximumAge();

    /**
     * Retrieve the current maximum size, in units.
     */

    public long getMaximumSize();

    /**
     * Set the current maximum age.
     * 
     * @throws ConstraintError
     *           Iff <code>age &lt; 1</code>.
     */

    public void setMaximumAge(
      final long age)
      throws ConstraintError;

    /**
     * Set the current maximum size, in units.
     * 
     * @throws ConstraintError
     *           Iff <code>size &lt; 1</code>.
     */

    public void setMaximumSize(
      final long size)
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
   * Create a new build from which to create configurations.
   */

  public static @Nonnull Builder newBuilder()
  {
    return new Builder() {
      private long maximum_age  = 1;
      private long maximum_size = 1;

      @SuppressWarnings("synthetic-access") @Override public
        PCacheConfig
        create()
          throws ConstraintError
      {
        return new PCacheConfig(this.maximum_size, this.maximum_age);
      }

      @Override public long getMaximumAge()
      {
        return this.maximum_age;
      }

      @Override public long getMaximumSize()
      {
        return this.maximum_size;
      }

      @Override public void setMaximumAge(
        final long age)
        throws ConstraintError
      {
        this.maximum_age =
          Constraints.constrainRange(age, 1, Long.MAX_VALUE, "Maximum age");
      }

      @Override public void setMaximumSize(
        final long size)
        throws ConstraintError
      {
        this.maximum_size =
          Constraints.constrainRange(size, 1, Long.MAX_VALUE, "Maximum size");
      }

      @Override public void setNoMaximumAge()
      {
        this.maximum_age = 0;
      }

      @Override public void setNoMaximumSize()
      {
        this.maximum_size = 0;
      }
    };
  }

  private final long maximum_age;
  private final long maximum_size;

  private PCacheConfig(
    final long max_size,
    final long max_age)
    throws ConstraintError
  {
    Constraints.constrainArbitrary(
      (max_age > 0) || (max_size > 0),
      "Either maximum age or size is non-zero");
    this.maximum_size =
      Constraints.constrainRange(max_size, 0, Long.MAX_VALUE, "Maximum size");
    this.maximum_age =
      Constraints.constrainRange(max_age, 0, Long.MAX_VALUE, "Maximum age");
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
   * Retrieve the maximum age <code>A</code> of items in the cache, or
   * <code>0</code> if there is no maximum age.
   * </p>
   * <p>
   * Items that have not been referenced for at least <code>A</code> periods
   * will be removed from the cache at the end of the current period.
   * </p>
   */

  public long getMaximumAge()
  {
    return this.maximum_age;
  }

  /**
   * Retrieve the maximum size of the cache, in units, or <code>0</code> if
   * there is no maximum size.
   */

  public long getMaximumSize()
  {
    return this.maximum_size;
  }

  @Override public int hashCode()
  {
    final int prime = 31;
    int result = 1;
    result =
      (prime * result) + (int) (this.maximum_age ^ (this.maximum_age >>> 32));
    result =
      (prime * result)
        + (int) (this.maximum_size ^ (this.maximum_size >>> 32));
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
