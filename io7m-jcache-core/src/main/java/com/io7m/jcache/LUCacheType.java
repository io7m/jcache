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

/**
 * The type of mutable least-used caches, containing values of type
 * <code>T</code> with each value associated with values of <code>K</code>,
 * throwing <code>E</code> on load/cache failures.
 *
 * @param <K>
 *          The type of keys
 * @param <TVIEW>
 *          The type of cached values, as visible to users of the cache
 * @param <TCACHE>
 *          The type of cached values, as visible to cache implementations
 * @param <E>
 *          The type of exceptions raised during loading
 */

public interface LUCacheType<K, TVIEW, TCACHE extends TVIEW, E extends Throwable> extends
  JCacheReadableType<K>,
  JCacheDeletableType,
  JCacheEventsSubscriptionType<K, TCACHE>
{
  /**
   * Retrieve a value named <code>key</code>, loading it if necessary.
   *
   * @return The cached or loaded value associated with <code>key</code>.
   * @param key
   *          The key identifying the value to be retrieved.
   * @throws E
   *           Iff the value named <code>key</code> raises an exception of
   *           type <code>E</code> upon loading.
   * @throws JCacheException
   *           Iff the value cannot be cached (possibly due to being too
   *           large, or violating other constraints of the particular cache
   *           implementation).
   */

  TVIEW cacheGetLU(
    final K key)
    throws E,
      JCacheException;
}
