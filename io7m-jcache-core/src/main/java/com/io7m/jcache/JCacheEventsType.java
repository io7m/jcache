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
 * Interface supported by types that can receive cache events.
 * 
 * @param <K>
 *          The type of keys
 * @param <V>
 *          The type of cached values
 */

public interface JCacheEventsType<K, V>
{
  /**
   * <p>
   * Called when a value <code>value</code>, associated with <code>key</code>,
   * failed to close due to exception <code>x</code>.
   * </p>
   * 
   * @param key
   *          The key
   * @param value
   *          The value
   * @param size
   *          The size of <tt>value</tt>
   * @param x
   *          The exception raised
   */

  void cacheEventValueCloseError(
    final  K key,
    final  V value,
    final  BigInteger size,
    final  Throwable x);

  /**
   * <p>
   * Called when a value <code>value</code> is associated with
   * <code>key</code> is about to be evicted.
   * </p>
   * 
   * @param key
   *          The key
   * @param value
   *          The value
   * @param size
   *          The size of <tt>value</tt>
   */

  void cacheEventValueEvicted(
    final  K key,
    final  V value,
    final  BigInteger size);

  /**
   * <p>
   * Called when a value <code>value</code> is first loaded, prior to any
   * cache size checks.
   * </p>
   * 
   * @param key
   *          The key
   * @param value
   *          The value
   * @param size
   *          The size of <tt>value</tt>
   */

  void cacheEventValueLoaded(
    final  K key,
    final  V value,
    final  BigInteger size);

  /**
   * <p>
   * Called when a value <code>value</code> is retrieved via <code>key</code>.
   * </p>
   * 
   * @param key
   *          The key
   * @param value
   *          The value
   * @param size
   *          The size of <tt>value</tt>
   */

  void cacheEventValueRetrieved(
    final  K key,
    final  V value,
    final  BigInteger size);
}
