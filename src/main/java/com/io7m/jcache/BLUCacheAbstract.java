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

import javax.annotation.Nonnull;

import com.io7m.jaux.Constraints;
import com.io7m.jaux.Constraints.ConstraintError;

/**
 * <p>
 * An abstract implementation of {@link BLUCacheType} that forwards all method
 * calls to a given implementation of {@link BLUCacheType}.
 * </p>
 * <p>
 * This exists in order to provide a class that third-party code can extend in
 * order to declare convenient subtypes of {@link BLUCacheType} (specifically
 * so that they can use interfaces to provide type synonyms without having to
 * override all of the methods themselves). For example:
 * </p>
 * <p>
 * <code>
 * interface ShortNameType extends BLUCacheType<Integer, Boolean, IllegalArgumentException> 
 * { 
 * 
 * }
 * 
 * final class ShortNameCache
 *   extends BLUCacheAbstract<Integer, Boolean, IllegalArgumentException>
 *   implements ShortNameType
 * {
 *   public static ShortNameType wrap(BLUCacheType<Integer, Boolean, IllegalArgumentException> c)
 *   {
 *     return new ShortNameCache(c);
 *   }
 *   
 *   private ShortNameCache(BLUCacheType<Integer, Boolean, IllegalArgumentException> c)
 *   {
 *     super(c);
 *   }
 * }
 * </code>
 * </p>
 * <p>
 * Now,
 * <code>ShortNameCache <: BLUCacheType<Integer, Boolean, IllegalArgumentException></code>
 * and <code>ShortNameCache <: ShortNameType</code>, but the writer of
 * <code>ShortNameCache</code> did not have to fill in all of the methods.
 * </p>
 * <p>
 * For security reasons, the {@link BLUCacheAbstract} class fills in a no-op
 * finalizer in order to prevent hostile subclasses from attacking the
 * implementation.
 * </p>
 * 
 * @param <K>
 *          The type of keys
 * @param <V>
 *          The type of values
 * @param <E>
 *          The type of exceptions
 */

public abstract class BLUCacheAbstract<K, V, E extends Throwable> implements
  BLUCacheType<K, V, E>
{
  private final @Nonnull BLUCacheType<K, V, E> cache;

  protected BLUCacheAbstract(
    final @Nonnull BLUCacheType<K, V, E> in_cache)
    throws ConstraintError
  {
    this.cache = Constraints.constrainNotNull(in_cache, "Cache");
  }

  @Override public final BLUCacheReceiptType<K, V> bluCacheGet(
    final @Nonnull K key)
    throws ConstraintError,
      E,
      JCacheException
  {
    return this.cache.bluCacheGet(key);
  }

  @Override public final void cacheDelete()
  {
    this.cache.cacheDelete();
  }

  @Override public final void cacheEventsSubscribe(
    final @Nonnull JCacheEventsType<K, V> events)
    throws ConstraintError
  {
    this.cache.cacheEventsSubscribe(events);
  }

  @Override public final void cacheEventsUnsubscribe()
  {
    this.cache.cacheEventsUnsubscribe();
  }

  @Override public final boolean cacheIsAvailable(
    final @Nonnull K key)
    throws ConstraintError
  {
    return this.cache.cacheIsAvailable(key);
  }

  @Override public final boolean cacheIsBorrowed(
    final @Nonnull K key)
    throws ConstraintError
  {
    return this.cache.cacheIsBorrowed(key);
  }

  @Override public final boolean cacheIsCached(
    final @Nonnull K key)
    throws ConstraintError
  {
    return this.cache.cacheIsCached(key);
  }

  @Override public final BigInteger cacheItemCount()
  {
    return this.cache.cacheItemCount();
  }

  @Override public final BigInteger cacheSize()
  {
    return this.cache.cacheSize();
  }

  @Override protected final void finalize()
    throws Throwable
  {
    // Nothing
  }
}
