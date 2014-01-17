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

import com.io7m.jaux.Constraints.ConstraintError;

/**
 * The type of mutable least-used caches, containing values of type
 * <code>V</code> with each value associated with values of <code>K</code>,
 * throwing <code>E</code> on load/cache failures.
 * 
 * @param <K>
 *          The type of keys
 * @param <V>
 *          The type of cached values
 * @param <E>
 *          The type of exceptions raised during loading
 */

public interface LUCache<K, V, E extends Throwable> extends
  JCacheReadable<K>,
  JCacheDeletable,
  JCacheEventsSubscription<K, V>
{
  /**
   * Retrieve a value named <code>key</code>, loading it if necessary.
   * 
   * @return The cached or loaded value associated with <code>key</code>.
   * @param key
   *          The key identifying the value to be retrieved.
   * @throws ConstraintError
   *           Iff <code>key == null</code>, or an internal constraint error
   *           occurs.
   * @throws E
   *           Iff the value named <code>key</code> raises an exception of
   *           type <code>E</code> upon loading.
   * @throws JCacheException
   *           Iff the value cannot be cached (possibly due to being too
   *           large, or violating other constraints of the particular cache
   *           implementation).
   */

  public @Nonnull V cacheGetLU(
    final @Nonnull K key)
    throws ConstraintError,
      E,
      JCacheException;
}
