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

package com.io7m.jlucache;

import javax.annotation.Nonnull;

import com.io7m.jaux.Constraints.ConstraintError;

/**
 * <p>
 * The type of mutable periodic caches, containing objects of type
 * <code>V</code> with each object associated with values of <code>K</code>,
 * throwing <code>E</code> on load/cache failures.
 * </p>
 * <p>
 * A periodic cache will evict objects (according to implementation-specific
 * thresholds) at the end of a <i>period</i> delimited with
 * {@link #pcPeriodStart()} and {@link #pcPeriodEnd()}. The intention is to
 * guarantee that all objects requested during a given period will stay in the
 * cache for the entirety of that period, regardless of how large the cache
 * would grow during that time. Typically, objects that have not been accessed
 * for a given number of periods will be evicted at the end of the current
 * period.
 * </p>
 */

public interface PCache<K, V, E extends Throwable> extends
  LUCacheReadable<K>,
  LUCacheDeletable,
  LUCacheEventsSubscription<K, V>
{
  /**
   * Retrieve an object named <code>key</code>, loading it if necessary.
   * 
   * @throws ConstraintError
   *           Iff <code>key == null</code>, {@link #pcPeriodStart()} has not
   *           been called since the last call to {@link #pcPeriodEnd()} (or
   *           ever), or an internal constraint error occurs.
   * @throws E
   *           Iff the object named <code>key</code> raises an exception of
   *           type <code>E</code> upon loading.
   * @throws LUCacheException
   *           Iff the object cannot be cached (possibly due to being too
   *           large, or violating other constraints of the particular cache
   *           implementation).
   */

  public @Nonnull V pcCacheGet(
    final @Nonnull K key)
    throws ConstraintError,
      E,
      LUCacheException;

  /**
   * End the current cache period.
   * 
   * @throws ConstraintError
   *           If a period is not in progress.
   * @see #pcPeriodStart()
   */

  public void pcPeriodEnd()
    throws ConstraintError;

  /**
   * Begin a cache period.
   * 
   * @throws ConstraintError
   *           If a period is already in progress.
   * @see #pcPeriodEnd()
   */

  public void pcPeriodStart()
    throws ConstraintError;
}
