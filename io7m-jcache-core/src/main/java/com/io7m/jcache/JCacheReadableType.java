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
 * The type of readable least-used caches, containing objects associated with
 * keys of type <code>K</code>.
 * 
 * @param <K>
 *          The type of keys
 */

public interface JCacheReadableType<K>
{
  /**
   * Return <code>true</code> if an object is cached for <code>key</code>.
   * 
   * @param key
   *          The key to query.
   * @return <code>true</code> if an object is cached for <code>key</code>.
   */

  boolean cacheIsCached(
    final K key);

  /**
   * @return The number of items cached.
   */

  BigInteger cacheItemCount();

  /**
   * @return The size of the current cache, in units.
   */

  BigInteger cacheSize();

}
