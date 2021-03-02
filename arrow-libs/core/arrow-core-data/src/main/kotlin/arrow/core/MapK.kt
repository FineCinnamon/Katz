package arrow.core

object MapK {

  inline fun <Key, B, C, D> mapN(
    b: Map<Key, B>,
    c: Map<Key, C>,
    map: (Key, B, C) -> D
  ): Map<Key, D> =
    mapN(
      b,
      c,
      emptyMap<Key, Unit>(),
      emptyMap<Key, Unit>(),
      emptyMap<Key, Unit>(),
      emptyMap<Key, Unit>(),
      emptyMap<Key, Unit>(),
      emptyMap<Key, Unit>(),
      emptyMap<Key, Unit>(),
      emptyMap<Key, Unit>()
    ) { key, bb, cc, _, _, _, _, _, _, _, _ ->
      map(key, bb, cc)
    }

  inline fun <Key, B, C, D, E> mapN(
    b: Map<Key, B>,
    c: Map<Key, C>,
    d: Map<Key, D>,
    map: (Key, B, C, D) -> E
  ): Map<Key, E> =
    mapN(
      b,
      c,
      d,
      emptyMap<Key, Unit>(),
      emptyMap<Key, Unit>(),
      emptyMap<Key, Unit>(),
      emptyMap<Key, Unit>(),
      emptyMap<Key, Unit>(),
      emptyMap<Key, Unit>(),
      emptyMap<Key, Unit>()
    ) { key, bb, cc, dd, _, _, _, _, _, _, _ ->
      map(key, bb, cc, dd)
    }

  inline fun <Key, B, C, D, E, F> mapN(
    b: Map<Key, B>,
    c: Map<Key, C>,
    d: Map<Key, D>,
    e: Map<Key, E>,
    map: (Key, B, C, D, E) -> F
  ): Map<Key, F> =
    mapN(
      b,
      c,
      d,
      e,
      emptyMap<Key, Unit>(),
      emptyMap<Key, Unit>(),
      emptyMap<Key, Unit>(),
      emptyMap<Key, Unit>(),
      emptyMap<Key, Unit>(),
      emptyMap<Key, Unit>()
    ) { key, bb, cc, dd, ee, _, _, _, _, _, _ ->
      map(key, bb, cc, dd, ee)
    }

  inline fun <Key, B, C, D, E, F, G> mapN(
    b: Map<Key, B>,
    c: Map<Key, C>,
    d: Map<Key, D>,
    e: Map<Key, E>,
    f: Map<Key, F>,
    map: (Key, B, C, D, E, F) -> G
  ): Map<Key, G> =
    mapN(
      b,
      c,
      d,
      e,
      f,
      emptyMap<Key, Unit>(),
      emptyMap<Key, Unit>(),
      emptyMap<Key, Unit>(),
      emptyMap<Key, Unit>(),
      emptyMap<Key, Unit>()
    ) { key, bb, cc, dd, ee, ff, _, _, _, _, _ ->
      map(key, bb, cc, dd, ee, ff)
    }

  inline fun <Key, B, C, D, E, F, G, H> mapN(
    b: Map<Key, B>,
    c: Map<Key, C>,
    d: Map<Key, D>,
    e: Map<Key, E>,
    f: Map<Key, F>,
    g: Map<Key, G>,
    map: (Key, B, C, D, E, F, G) -> H
  ): Map<Key, H> =
    mapN(
      b,
      c,
      d,
      e,
      f,
      g,
      emptyMap<Key, Unit>(),
      emptyMap<Key, Unit>(),
      emptyMap<Key, Unit>(),
      emptyMap<Key, Unit>()
    ) { key, bb, cc, dd, ee, ff, gg, _, _, _, _ ->
      map(key, bb, cc, dd, ee, ff, gg)
    }

  inline fun <Key, B, C, D, E, F, G, H, I> mapN(
    b: Map<Key, B>,
    c: Map<Key, C>,
    d: Map<Key, D>,
    e: Map<Key, E>,
    f: Map<Key, F>,
    g: Map<Key, G>,
    h: Map<Key, H>,
    map: (Key, B, C, D, E, F, G, H) -> I
  ): Map<Key, I> =
    mapN(
      b,
      c,
      d,
      e,
      f,
      g,
      h,
      emptyMap<Key, Unit>(),
      emptyMap<Key, Unit>(),
      emptyMap<Key, Unit>()
    ) { key, bb, cc, dd, ee, ff, gg, hh, _, _, _ ->
      map(key, bb, cc, dd, ee, ff, gg, hh)
    }

  inline fun <Key, B, C, D, E, F, G, H, I, J> mapN(
    b: Map<Key, B>,
    c: Map<Key, C>,
    d: Map<Key, D>,
    e: Map<Key, E>,
    f: Map<Key, F>,
    g: Map<Key, G>,
    h: Map<Key, H>,
    i: Map<Key, I>,
    map: (Key, B, C, D, E, F, G, H, I) -> J
  ): Map<Key, J> =
    mapN(
      b,
      c,
      d,
      e,
      f,
      g,
      h,
      i,
      emptyMap<Key, Unit>(),
      emptyMap<Key, Unit>()
    ) { key, bb, cc, dd, ee, ff, gg, hh, ii, _, _ ->
      map(key, bb, cc, dd, ee, ff, gg, hh, ii)
    }

  inline fun <Key, B, C, D, E, F, G, H, I, J, K> mapN(
    b: Map<Key, B>,
    c: Map<Key, C>,
    d: Map<Key, D>,
    e: Map<Key, E>,
    f: Map<Key, F>,
    g: Map<Key, G>,
    h: Map<Key, H>,
    i: Map<Key, I>,
    j: Map<Key, J>,
    map: (Key, B, C, D, E, F, G, H, I, J) -> K
  ): Map<Key, K> =
    mapN(b, c, d, e, f, g, h, i, j, emptyMap<Key, Unit>()) { key, bb, cc, dd, ee, ff, gg, hh, ii, jj, _ ->
      map(key, bb, cc, dd, ee, ff, gg, hh, ii, jj)
    }

  inline fun <Key, B, C, D, E, F, G, H, I, J, K, L> mapN(
    b: Map<Key, B>,
    c: Map<Key, C>,
    d: Map<Key, D>,
    e: Map<Key, E>,
    f: Map<Key, F>,
    g: Map<Key, G>,
    h: Map<Key, H>,
    i: Map<Key, I>,
    j: Map<Key, J>,
    k: Map<Key, K>,
    map: (Key, B, C, D, E, F, G, H, I, J, K) -> L
  ): Map<Key, L> {
    val destination = LinkedHashMap<Key, L>(b.size)
    for ((key, bb) in b) {
      Nullable.mapN(
        c[key],
        d[key],
        e[key],
        f[key],
        g[key],
        h[key],
        i[key],
        j[key],
        k[key]
      ) { cc, dd, ee, ff, gg, hh, ii, jj, kk ->
        map(key, bb, cc, dd, ee, ff, gg, hh, ii, jj, kk)
      }?.let { l -> destination.put(key, l) }
    }
    return destination
  }
}

internal fun <K, A> Pair<K, A>?.asIterable(): Iterable<Pair<K, A>> =
  when (this) {
    null -> emptyList()
    else -> listOf(this)
  }
