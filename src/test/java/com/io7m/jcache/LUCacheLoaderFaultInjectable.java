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

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

/**
 * Loader that fails to load on demand.
 */

public final class LUCacheLoaderFaultInjectable<K, V> implements
  JCacheLoader<K, V, LUCacheLoaderFaultInjectable.Failure>
{
  static class Failure extends Exception
  {
    private static final long serialVersionUID;

    static {
      serialVersionUID = 6949889900092909518L;
    }
  }

  private boolean         close_fail;
  private boolean         fail;
  private BigInteger      size = BigInteger.ZERO;
  private @CheckForNull V value;

  @Override public void cacheValueClose(
    final V v)
    throws Failure
  {
    if (this.close_fail) {
      throw new Failure();
    }
  }

  @Override public V cacheValueLoad(
    final @Nonnull K key)
    throws Failure
  {
    if (this.fail) {
      throw new Failure();
    }
    return this.value;
  }

  @Override public BigInteger cacheValueSizeOf(
    final @Nonnull V v)
  {
    return this.size;
  }

  public void setCloseFailure(
    final boolean fail)
  {
    this.close_fail = fail;
  }

  public void setFailure(
    final boolean fail)
  {
    this.fail = fail;
  }

  public void setLoadedValue(
    final @Nonnull V value)
  {
    this.value = value;
  }

  public void setLoadedValueSize(
    final BigInteger size)
  {
    this.size = size;
  }
}
