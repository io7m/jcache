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

import javax.annotation.Nonnull;

import com.io7m.jaux.Constraints;
import com.io7m.jaux.Constraints.ConstraintError;

/**
 * The main exception type raised by cache operations.
 */

public final class LUCacheException extends Throwable
{
  /**
   * The possible error codes raised by cache operations.
   */

  public static enum Code
  {
    /**
     * The loader for the cache returned <code>null</code> for a given key.
     */

    LUCACHE_LOADER_RETURNED_NULL,

    /**
     * An object cannot be stored in the cache, because its size is greater
     * than the cache's maximum capacity.
     */

    LUCACHE_OBJECT_TOO_LARGE,

    /**
     * An object cannot be stored in the cache, because its size is less than
     * one unit.
     */

    LUCACHE_OBJECT_TOO_SMALL
  }

  private static final long serialVersionUID;

  static {
    serialVersionUID = -4142305723422812182L;
  }

  /**
   * Construct an exception indicating that a loader returned <tt>null</tt>.
   * 
   * @param key
   *          The key
   * @return An exception
   * @throws ConstraintError
   *           Iff an internal constraint error occurs.
   */

  static @Nonnull <K> LUCacheException errorLoaderReturnedNull(
    final @Nonnull K key)
    throws ConstraintError
  {
    final StringBuilder m = new StringBuilder();
    m.append("Loader returned null for '");
    m.append(key);
    m.append("'");
    return new LUCacheException(
      Code.LUCACHE_LOADER_RETURNED_NULL,
      m.toString());
  }

  /**
   * Construct an exception indicating that an object is too large for the
   * cache.
   * 
   * @param key
   *          The key
   * @param size
   *          The size of the object
   * @param maximum
   *          The maximum object size
   * @return An exception
   * @throws ConstraintError
   *           Iff an internal constraint error occurs.
   */

  static @Nonnull <K> LUCacheException errorObjectTooLarge(
    final @Nonnull K key,
    final long size,
    final long maximum)
    throws ConstraintError
  {
    final StringBuilder m = new StringBuilder();
    m.append("Object for '");
    m.append(key);
    m.append("' is of size ");
    m.append(size);
    m.append(", which is too large for a cache with maximum capacity of ");
    m.append(maximum);
    return new LUCacheException(Code.LUCACHE_OBJECT_TOO_LARGE, m.toString());
  }

  /**
   * Construct an exception indicating that an object returned a negative
   * size.
   * 
   * @param key
   *          The key
   * @param size
   *          The size of the object
   * 
   * @return An exception
   * @throws ConstraintError
   *           Iff an internal constraint error occurs.
   */

  static @Nonnull <K> LUCacheException errorObjectTooSmall(
    final @Nonnull K key,
    final long size)
    throws ConstraintError
  {
    final StringBuilder m = new StringBuilder();
    m.append("Object for '");
    m.append(key);
    m.append("' is of size ");
    m.append(size);
    m.append(", which is too small: must be at least 1");
    return new LUCacheException(Code.LUCACHE_OBJECT_TOO_SMALL, m.toString());
  }

  private final @Nonnull Code code;

  LUCacheException(
    final @Nonnull Code in_code,
    final @Nonnull String in_message)
    throws ConstraintError
  {
    super(in_message);
    this.code = Constraints.constrainNotNull(in_code, "Code");
  }

  /**
   * @return The error code for the exception
   */

  public @Nonnull Code getCode()
  {
    return this.code;
  }
}
