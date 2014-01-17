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

/**
 * The interface supported by types that can load objects of type
 * <code>V</code>, named <code>K</code>, throwing <code>E</code> on failure.
 * 
 * @param <K>
 *          The type of keys
 * @param <V>
 *          The type of cached values
 * @param <E>
 *          The type of exceptions raised during loading
 */

public interface JCacheLoader<K, V, E extends Throwable>
{
  /**
   * Destroy <code>v</code>, freeing any associated resources.
   * 
   * @param v
   *          The object to destroy
   * @throws E
   *           Iff an exception is raised during deletion
   */

  public void cacheValueClose(
    final @Nonnull V v)
    throws E;

  /**
   * Load an object named <code>key</code>, throwing an exception of type
   * <code>E</code> on failure.
   * 
   * @return The loaded object
   * @param key
   *          The key that identifies the object
   * @throws E
   *           Iff loading fails
   */

  public @Nonnull V cacheValueLoad(
    final @Nonnull K key)
    throws E;

  /**
   * @return The size in units of <code>v</code>.
   * @param v
   *          The loaded object
   */

  public @Nonnull BigInteger cacheValueSizeOf(
    final @Nonnull V v);
}
