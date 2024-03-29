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

package com.io7m.jcache.tests;

import java.math.BigInteger;

import com.io7m.jcache.JCacheEventsType;

class EventCount<K, V> implements JCacheEventsType<K, V>
{
  private int close_errors;

  private int evictions;
  private int loads;
  private int retrievals;
  public EventCount()
  {
    this.close_errors = 0;
    this.evictions = 0;
    this.loads = 0;
    this.retrievals = 0;
  }

  @Override public void cacheEventValueCloseError(
    final K key,
    final V value,
    final BigInteger size,
    final Throwable x)
  {
    ++this.close_errors;
  }

  @Override public void cacheEventValueEvicted(
    final K key,
    final V value,
    final BigInteger size)
  {
    ++this.evictions;
  }

  @Override public void cacheEventValueLoaded(
    final K key,
    final V value,
    final BigInteger size)
  {
    ++this.loads;
  }

  @Override public void cacheEventValueRetrieved(
    final K key,
    final V value,
    final BigInteger size)
  {
    ++this.retrievals;
  }

  public int getCloseErrors()
  {
    return this.close_errors;
  }

  public int getEvictions()
  {
    return this.evictions;
  }

  public int getLoads()
  {
    return this.loads;
  }

  public int getRetrievals()
  {
    return this.retrievals;
  }
}
