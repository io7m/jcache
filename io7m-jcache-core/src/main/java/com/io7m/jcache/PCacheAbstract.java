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

import com.io7m.jnull.NullCheck;

/**
 * <p>
 * An abstract implementation of {@link PCacheType} that forwards all method
 * calls to a given implementation of {@link PCacheType}.
 * </p>
 * <p>
 * This exists in order to provide a class that third-party code can extend in
 * order to declare convenient subtypes of {@link PCacheType} (specifically so
 * that they can use interfaces to provide type synonyms without having to
 * override all of the methods themselves). For example:
 * </p>
 * <p>
 * <code>
 * interface ShortNameType extends PCacheType<Integer, Boolean, IllegalArgumentException> 
 * { 
 * 
 * }
 * 
 * final class ShortNameCache
 *   extends PCacheAbstract<Integer, Boolean, IllegalArgumentException>
 *   implements ShortNameType
 * {
 *   public static ShortNameType wrap(PCacheType<Integer, Boolean, IllegalArgumentException> c)
 *   {
 *     return new ShortNameCache(c);
 *   }
 *   
 *   private ShortNameCache(PCacheType<Integer, Boolean, IllegalArgumentException> c)
 *   {
 *     super(c);
 *   }
 * }
 * </code>
 * </p>
 * <p>
 * Now,
 * <code>ShortNameCache <: PCacheType<Integer, Boolean, IllegalArgumentException></code>
 * and <code>ShortNameCache <: ShortNameType</code>, but the writer of
 * <code>ShortNameCache</code> did not have to fill in all of the methods.
 * </p>
 * <p>
 * For security reasons, the {@link PCacheAbstract} class fills in a no-op
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

public abstract class PCacheAbstract<K, V, E extends Throwable> implements
  PCacheType<K, V, E>
{
  private final PCacheType<K, V, E> cache;

  protected PCacheAbstract(
    final PCacheType<K, V, E> in_cache)
  {
    this.cache = NullCheck.notNull(in_cache, "Cache");
  }

  @Override public final void cacheDelete()
  {
    this.cache.cacheDelete();
  }

  @Override public final void cacheEventsSubscribe(
    final JCacheEventsType<K, V> events)
  {
    this.cache.cacheEventsSubscribe(events);
  }

  @Override public final void cacheEventsUnsubscribe()
  {
    this.cache.cacheEventsUnsubscribe();
  }

  @Override public final V cacheGetPeriodic(
    final K key)
    throws E,
      JCacheException
  {
    return this.cache.cacheGetPeriodic(key);
  }

  @Override public final boolean cacheIsCached(
    final K key)
  {
    return this.cache.cacheIsCached(key);
  }

  @Override public final BigInteger cacheItemCount()
  {
    return this.cache.cacheItemCount();
  }

  @Override public final void cachePeriodEnd()
  {
    this.cache.cachePeriodEnd();
  }

  @Override public final void cachePeriodStart()
  {
    this.cache.cachePeriodStart();
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