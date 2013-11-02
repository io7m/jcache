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

import com.io7m.jaux.Constraints;
import com.io7m.jaux.Constraints.ConstraintError;

/**
 * The main exception type raised by cache operations.
 */

public final class LUCacheException extends Throwable
{
  /**
   * The possible error codes raised by cache operations.
   */

  public static enum Code
  {
    /**
     * An object cannot be stored in the cache, because its size is greater
     * than the cache's maximum capacity.
     */

    LUCACHE_OBJECT_TOO_LARGE,

    /**
     * An object cannot be stored in the cache, because its size is less than
     * one unit.
     */

    LUCACHE_OBJECT_TOO_SMALL,

    /**
     * The loader for the cache returned <code>null</code> for a given key.
     */

    LUCACHE_LOADER_RETURNED_NULL
  }

  private static final long   serialVersionUID;

  static {
    serialVersionUID = -4142305723422812182L;
  }

  private final @Nonnull Code code;

  LUCacheException(
    final @Nonnull Code code,
    final @Nonnull String message)
    throws ConstraintError
  {
    super(message);
    this.code = Constraints.constrainNotNull(code, "Code");
  }

  public @Nonnull Code getCode()
  {
    return this.code;
  }
}
