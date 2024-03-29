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
 * The type of readable borrowing least-used caches, containing objects
 * associated with keys of type <code>K</code>.
 *
 * @param <K>
 *          The type of keys
 */

public interface BLUCacheReadableType<K> extends JCacheReadableType<K>
{
  /**
   * @return <code>true</code> iff a value associated with <code>key</code> is
   *         available
   * @param key
   *          The key
   */

  boolean cacheIsAvailable(
    final K key);

  /**
   * @return <code>true</code> iff a value associated with <code>key</code> is
   *         borrowed
   * @param key
   *          The key
   */

  boolean cacheIsBorrowed(
    final K key);

  /**
   * @return The current cache configuration.
   */

  BLUCacheConfig cacheGetConfiguration();
}
