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

import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

import com.io7m.jaux.UnreachableCodeException;
import com.io7m.jaux.functional.Function;
import com.io7m.jaux.functional.Unit;

/**
 * Convenience functions for dealing with maps with set values.
 */

final class MapSet
{
  /**
   * Add a value to the set associated with the given key. Create a set if
   * necessary.
   * 
   * @param m
   *          The map
   * @param new_set
   *          A function to create a new empty set
   * @param key
   *          The key
   * @param value
   *          The value
   */

  static <KT, VT, ST extends Set<VT>> void mapSetAdd(
    final @Nonnull Map<KT, ST> m,
    final @Nonnull Function<Unit, ST> new_set,
    final @Nonnull KT key,
    final @Nonnull VT value)
  {
    final ST s;
    if (m.containsKey(key)) {
      s = m.get(key);
    } else {
      s = new_set.call(Unit.unit());
    }

    s.add(value);
    m.put(key, s);
  }

  /**
   * Remove a value from the set associated with the given key. Remove the
   * associated key/set if the resulting set is empty.
   * 
   * @param m
   *          The map
   * @param key
   *          The key
   * @param value
   *          The value
   */

  static <KT, VT, ST extends Set<VT>> void mapSetRemove(
    final @Nonnull Map<KT, ST> m,
    final @Nonnull KT key,
    final @Nonnull VT value)
  {
    if (m.containsKey(key)) {
      final ST s = m.get(key);
      s.remove(value);
      if (s.isEmpty()) {
        m.remove(key);
      }
    }
  }

  private MapSet()
  {
    throw new UnreachableCodeException();
  }
}
