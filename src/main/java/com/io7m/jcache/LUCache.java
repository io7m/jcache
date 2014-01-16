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

import com.io7m.jaux.Constraints.ConstraintError;

/**
 * The type of mutable least-used caches, containing objects of type
 * <code>V</code> with each object associated with values of <code>K</code>,
 * throwing <code>E</code> on load/cache failures.
 */

public interface LUCache<K, V, E extends Throwable> extends
  LUCacheReadable<K>,
  LUCacheDeletable,
  LUCacheEventsSubscription<K, V>
{
  /**
   * Retrieve an object named <code>key</code>, loading it if necessary.
   * 
   * @throws ConstraintError
   *           Iff <code>key == null</code>, or an internal constraint error
   *           occurs.
   * @throws E
   *           Iff the object named <code>key</code> raises an exception of
   *           type <code>E</code> upon loading.
   * @throws LUCacheException
   *           Iff the object cannot be cached (possibly due to being too
   *           large, or violating other constraints of the particular cache
   *           implementation).
   */

  public @Nonnull V luCacheGet(
    final @Nonnull K key)
    throws ConstraintError,
      E,
      LUCacheException;
}
