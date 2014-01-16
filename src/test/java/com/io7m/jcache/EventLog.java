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

class EventLog<K, V> implements LUCacheEvents<K, V>
{
  public EventLog()
  {
    // Nothing
  }

  public boolean   close_error           = false;
  public Throwable close_error_exception = null;
  public K         close_error_key       = null;
  public long      close_error_size      = 0L;

  public V         close_error_value     = null;
  public boolean   evicted               = false;
  public K         evicted_key           = null;
  public long      evicted_size          = 0L;

  public V         evicted_value         = null;
  public boolean   loaded                = false;
  public K         loaded_key            = null;
  public long      loaded_size           = 0L;

  public V         loaded_value          = null;
  public boolean   retrieved             = false;
  public K         retrieved_key         = null;
  public long      retrieved_size        = 0L;
  public V         retrieved_value       = null;

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
