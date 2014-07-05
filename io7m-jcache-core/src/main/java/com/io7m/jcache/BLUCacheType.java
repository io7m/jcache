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
 * <p>
 * The type of mutable borrowing least-used caches.
 * </p>
 * <p>
 * A borrowing cache operates as a normal {@link LUCacheType} except that
 * requested keys are marked as <i>borrowed</i> until they are explicitly
 * returned. Attempting to request a key multiple times results in multiple
 * values being loaded by the cache so that each requester receives a fresh
 * copy of the associated value.
 * </p>
 * <p>
 * Because keys and values must be explicitly returned to the cache, a
 * {@link BLUCacheType} is not interchangeable with a {@link LUCacheType} and
 * therefore is not type-compatible.
 * </p>
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

public interface BLUCacheType<K, TVIEW, TCACHE extends TVIEW, E extends Throwable> extends
  JCacheDeletableType,
  JCacheEventsSubscriptionType<K, TCACHE>,
  BLUCacheReadableType<K>
{
  /**
   * <p>
   * Retrieve a value named <code>key</code>, loading it if necessary. If
   * <code>key</code> has no non-<i>borrowed</i> values, a new value is loaded
   * and marked as <i>borrowed</i>. Otherwise, one of the non-<i>borrowed</i>
   * values associated with <code>key</code> is marked as <i>borrowed</i> and
   * returned.
   * </p>
   *
   * @return A receipt containing a cached or loaded value associated with
   *         <code>key</code>
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

  BLUCacheReceiptType<K, TVIEW> bluCacheGet(
    final K key)
    throws E,
      JCacheException;
}
