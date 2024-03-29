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
 * Interface for subscribing to cache events.
 * 
 * @param <K>
 *          The type of keys
 * @param <V>
 *          The type of values
 */

public interface JCacheEventsSubscriptionType<K, V>
{
  /**
   * Subscribe to events for the current cache, replacing any existing
   * subscriptions (if any). The cache will call functions in the given
   * interface when events occur.
   * 
   * @param events
   *          The event receiver.
   */

  void cacheEventsSubscribe(
    final JCacheEventsType<K, V> events);

  /**
   * Stop receiving events for the current cache.
   */

  void cacheEventsUnsubscribe();

}
