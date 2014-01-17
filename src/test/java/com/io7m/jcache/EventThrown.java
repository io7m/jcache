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

import java.math.BigInteger;

class EventThrown<K, V> implements JCacheEvents<K, V>
{
  @Override public void cacheEventValueCloseError(
    final K key,
    final V value,
    final BigInteger size,
    final Throwable x)
  {
    throw new AssertionError("Close error");
  }

  @Override public void cacheEventValueEvicted(
    final K key,
    final V value,
    final BigInteger size)
  {
    throw new AssertionError("Evicted");
  }

  @Override public void cacheEventValueLoaded(
    final K key,
    final V value,
    final BigInteger size)
  {
    throw new AssertionError("Loaded");
  }

  @Override public void cacheEventValueRetrieved(
    final K key,
    final V value,
    final BigInteger size)
  {
    throw new AssertionError("Retrieved");
  }
}
