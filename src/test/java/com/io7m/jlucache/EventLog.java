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

class EventLog<K, V> implements LUCacheEvents<K, V>
{
  boolean   close_error;
  Throwable close_error_exception;
  K         close_error_key;
  long      close_error_size;

  V         close_error_value;
  boolean   evicted;
  K         evicted_key;
  long      evicted_size;

  V         evicted_value;
  boolean   loaded;
  K         loaded_key;
  long      loaded_size;

  V         loaded_value;
  boolean   retrieved;
  K         retrieved_key;
  long      retrieved_size;
  V         retrieved_value;

  @Override public void luCacheEventObjectCloseError(
    final K key,
    final V value,
    final long size,
    final Throwable x)
  {
    System.out.println("close error: "
      + key
      + ":"
      + value
      + ":"
      + size
      + ": "
      + x.getMessage());

    this.close_error = true;
    this.close_error_key = key;
    this.close_error_value = value;
    this.close_error_size = size;
    this.close_error_exception = x;
  }

  @Override public void luCacheEventObjectEvicted(
    final K key,
    final V value,
    final long size)
  {
    System.out.println("evicted: " + key + ":" + value + ":" + size);

    this.evicted = true;
    this.evicted_key = key;
    this.evicted_value = value;
    this.evicted_size = size;
  }

  @Override public void luCacheEventObjectLoaded(
    final K key,
    final V value,
    final long size)
  {
    System.out.println("loaded: " + key + ":" + value + ":" + size);

    this.loaded = true;
    this.loaded_key = key;
    this.loaded_value = value;
    this.loaded_size = size;
  }

  @Override public void luCacheEventObjectRetrieved(
    final K key,
    final V value,
    final long size)
  {
    System.out.println("retrieved: " + key + ":" + value + ":" + size);

    this.retrieved = true;
    this.retrieved_key = key;
    this.retrieved_value = value;
    this.retrieved_size = size;
  }

  void reset()
  {
    this.evicted = false;
    this.loaded = false;
    this.retrieved = false;
    this.close_error = false;
  }
}
