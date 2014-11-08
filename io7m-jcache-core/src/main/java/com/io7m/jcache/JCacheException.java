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

/**
 * The main exception type raised by cache operations.
 */

public abstract class JCacheException extends RuntimeException
{
  /**
   * The loader for the cache returned <code>null</code> for a given key.
   */

  public static final class JCacheExceptionLoaderReturnedNull extends
    JCacheException
  {
    private static final long serialVersionUID = -6689633824482956839L;

    JCacheExceptionLoaderReturnedNull(
      final String message)
    {
      super(message);
    }
  }

  /**
   * An object cannot be stored in the cache, because its size is greater than
   * the cache's maximum capacity.
   */

  public static final class JCacheExceptionObjectTooLarge extends
    JCacheException
  {
    private static final long serialVersionUID = -8321243236404184147L;

    JCacheExceptionObjectTooLarge(
      final String message)
    {
      super(message);
    }
  }

  /**
   * An object cannot be stored in the cache, because its size is less than
   * one unit.
   */

  public static final class JCacheExceptionObjectTooSmall extends
    JCacheException
  {
    private static final long serialVersionUID = -5641094673536325741L;

    JCacheExceptionObjectTooSmall(
      final String message)
    {
      super(message);
    }
  }

  /**
   * The cache cannot grow larger than {@link Integer#MAX_VALUE} items.
   */

  public static final class JCacheExceptionSizeOverflow extends
    JCacheException
  {
    private static final long serialVersionUID = -4677450626738446783L;

    JCacheExceptionSizeOverflow(
      final String message)
    {
      super(message);
    }
  }

  /**
   * The cache has too many borrowed values.
   */

  public static final class JCacheExceptionTooManyBorrows extends
    JCacheException
  {
    private static final long serialVersionUID = -2563241109501423200L;

    JCacheExceptionTooManyBorrows(
      final String message)
    {
      super(message);
    }
  }

  private static final long serialVersionUID;

  static {
    serialVersionUID = -4142305723422812182L;
  }

  static JCacheExceptionSizeOverflow errorInternalCacheOverflow(
    final int size)
  {
    final StringBuilder m = new StringBuilder();
    m.append("The cache cannot accept more than ");
    m.append(size);
    m.append(" items due to limitations in the Java standard library");
    final String r = m.toString();
    assert r != null;
    return new JCacheExceptionSizeOverflow(r);
  }

  /**
   * Construct an exception indicating that a loader returned <tt>null</tt>.
   *
   * @param key
   *          The key
   * @return An exception
   */

  static <K> JCacheException errorLoaderReturnedNull(
    final K key)
  {
    final StringBuilder m = new StringBuilder();
    m.append("Loader returned null for '");
    m.append(key);
    m.append("'");
    final String r = m.toString();
    assert r != null;
    return new JCacheExceptionLoaderReturnedNull(r);
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

  static <K> JCacheException errorObjectTooLarge(
    final K key,
    final BigInteger size,
    final BigInteger maximum)
  {
    final StringBuilder m = new StringBuilder();
    m.append("Object for '");
    m.append(key);
    m.append("' is of size ");
    m.append(size);
    m.append(", which is too large for a cache with maximum capacity of ");
    m.append(maximum);
    final String r = m.toString();
    assert r != null;
    return new JCacheExceptionObjectTooLarge(r);
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

  static <K> JCacheException errorObjectTooSmall(
    final K key,
    final BigInteger size)
  {
    final StringBuilder m = new StringBuilder();
    m.append("Object for '");
    m.append(key);
    m.append("' is of size ");
    m.append(size);
    m.append(", which is too small: must be at least 1");
    final String r = m.toString();
    assert r != null;
    return new JCacheExceptionObjectTooSmall(r);
  }

  static <K> JCacheException tooManyBorrows(
    final K key)
  {
    final StringBuilder m = new StringBuilder();
    m.append("The cache contains too many borrowed items for key ");
    m.append(key);
    final String r = m.toString();
    assert r != null;
    return new JCacheExceptionTooManyBorrows(r);
  }

  JCacheException(
    final String message)
  {
    super(message);
  }
}
