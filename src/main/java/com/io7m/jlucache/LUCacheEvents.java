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

/**
 * Interface supported by types that can receive cache events.
 */

public interface LUCacheEvents<K, V>
{
  /**
   * <p>
   * Called when an object <code>value</code>, associated with
   * <code>key</code>, failed to close due to exception <code>x</code>.
   * </p>
   */

  public void luCacheEventObjectCloseError(
    final @Nonnull K key,
    final @Nonnull V value,
    final long size,
    final @Nonnull Throwable x);

  /**
   * <p>
   * Called when an object <code>value</code> is associated with
   * <code>key</code> is about to be evicted.
   * </p>
   */

  public void luCacheEventObjectEvicted(
    final @Nonnull K key,
    final @Nonnull V value,
    final long size);

  /**
   * <p>
   * Called when an object <code>value</code> is first loaded, prior to any
   * cache size checks.
   * </p>
   */

  public void luCacheEventObjectLoaded(
    final @Nonnull K key,
    final @Nonnull V value,
    final long size);

  /**
   * <p>
   * Called when an object <code>value</code> is retrieved via
   * <code>key</code>.
   * </p>
   */

  public void luCacheEventObjectRetrieved(
    final @Nonnull K key,
    final @Nonnull V value,
    final long size);
}
