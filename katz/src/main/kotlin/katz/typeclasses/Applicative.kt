/*
 * Copyright (C) 2017 The Katz Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:Suppress("UNUSED_PARAMETER")

package katz

interface Applicative<F> : Functor<F> {

    fun <A> pure(a: A): HK<F, A>

    fun <A, B> ap(fa: HK<F, A>, ff: HK<F, (A) -> B>): HK<F, B>

    fun <A, B> product(fa: HK<F, A>, fb: HK<F, B>): HK<F, Tuple2<A, B>> =
            ap(fb, map(fa) { a: A -> { b: B -> Tuple2(a, b) } })

    override fun <A, B> map(fa: HK<F, A>, f: (A) -> B): HK<F, B> = ap(fa, pure(f))

    fun <A, B, Z> map2(fa: HK<F, A>, fb: HK<F, B>, f: (Tuple2<A, B>) -> Z): HK<F, Z> =
            map(product(fa, fb), f)
}

data class Tuple2<out A, out B>(val a: A, val b: B)
data class Tuple3<out A, out B, out C>(val a: A, val b: B, val c: C)
data class Tuple4<out A, out B, out C, out D>(val a: A, val b: B, val c: C, val d: D)
data class Tuple5<out A, out B, out C, out D, out E>(val a: A, val b: B, val c: C, val d: D, val e: E)
data class Tuple6<out A, out B, out C, out D, out E, out F>(val a: A, val b: B, val c: C, val d: D, val e: E, val f: F)
data class Tuple7<out A, out B, out C, out D, out E, out F, out G>(val a: A, val b: B, val c: C, val d: D, val e: E, val f: F, val g: G)
data class Tuple8<out A, out B, out C, out D, out E, out F, out G, out H>(val a: A, val b: B, val c: C, val d: D, val e: E, val f: F, val g: G, val h: H)
data class Tuple9<out A, out B, out C, out D, out E, out F, out G, out H, out I>(val a: A, val b: B, val c: C, val d: D, val e: E, val f: F, val g: G, val h: H, val i: I)
data class Tuple10<out A, out B, out C, out D, out E, out F, out G, out H, out I, out J>(val a: A, val b: B, val c: C, val d: D, val e: E, val f: F, val g: G, val h: H, val i: I, val j: J)

fun <HKF, A, Z> HK<HKF, A>.product(AP: Applicative<HKF>, other: HK<HKF, Z>): HK<HKF, Tuple2<A, Z>> =
        AP.product(this, other)

fun <HKF, A, B, Z> HK<HKF, Tuple2<A, B>>.product(
        AP: Applicative<HKF>,
        other: HK<HKF, Z>,
        dummyImplicit: Any? = null): HK<HKF, Tuple3<A, B, Z>> =
        AP.map(AP.product(this, other), { Tuple3(it.a.a, it.a.b, it.b) })

fun <HKF, A, B, C, Z> HK<HKF, Tuple3<A, B, C>>.product(
        AP: Applicative<HKF>,
        other: HK<HKF, Z>,
        dummyImplicit: Any? = null,
        dummyImplicit2: Any? = null): HK<HKF, Tuple4<A, B, C, Z>> =
        AP.map(AP.product(this, other), { Tuple4(it.a.a, it.a.b, it.a.c, it.b) })

fun <HKF, A, B, C, D, Z> HK<HKF, Tuple4<A, B, C, D>>.product(
        AP: Applicative<HKF>,
        other: HK<HKF, Z>,
        dummyImplicit: Any? = null,
        dummyImplicit2: Any? = null,
        dummyImplicit3: Any? = null): HK<HKF, Tuple5<A, B, C, D, Z>> =
        AP.map(AP.product(this, other), { Tuple5(it.a.a, it.a.b, it.a.c, it.a.d, it.b) })

fun <HKF, A, B, C, D, E, Z> HK<HKF, Tuple5<A, B, C, D, E>>.product(
        AP: Applicative<HKF>,
        other: HK<HKF, Z>,
        dummyImplicit: Any? = null,
        dummyImplicit2: Any? = null,
        dummyImplicit3: Any? = null,
        dummyImplicit4: Any? = null): HK<HKF, Tuple6<A, B, C, D, E, Z>> =
        AP.map(AP.product(this, other), { Tuple6(it.a.a, it.a.b, it.a.c, it.a.d, it.a.e, it.b) })

fun <HKF, A, B, C, D, E, F, Z> HK<HKF, Tuple6<A, B, C, D, E, F>>.product(
        AP: Applicative<HKF>,
        other: HK<HKF, Z>,
        dummyImplicit: Any? = null,
        dummyImplicit2: Any? = null,
        dummyImplicit3: Any? = null,
        dummyImplicit4: Any? = null,
        dummyImplicit5: Any? = null): HK<HKF, Tuple7<A, B, C, D, E, F, Z>> =
        AP.map(AP.product(this, other), { Tuple7(it.a.a, it.a.b, it.a.c, it.a.d, it.a.e, it.a.f, it.b) })

fun <HKF, A, B, C, D, E, F, G, Z> HK<HKF, Tuple7<A, B, C, D, E, F, G>>.product(
        AP: Applicative<HKF>,
        other: HK<HKF, Z>,
        dummyImplicit: Any? = null,
        dummyImplicit2: Any? = null,
        dummyImplicit3: Any? = null,
        dummyImplicit4: Any? = null,
        dummyImplicit5: Any? = null,
        dummyImplicit6: Any? = null): HK<HKF, Tuple8<A, B, C, D, E, F, G, Z>> =
        AP.map(AP.product(this, other), { Tuple8(it.a.a, it.a.b, it.a.c, it.a.d, it.a.e, it.a.f, it.a.g, it.b) })

fun <HKF, A, B, C, D, E, F, G, H, Z> HK<HKF, Tuple8<A, B, C, D, E, F, G, H>>.product(
        AP: Applicative<HKF>,
        other: HK<HKF, Z>,
        dummyImplicit: Any? = null,
        dummyImplicit2: Any? = null,
        dummyImplicit3: Any? = null,
        dummyImplicit4: Any? = null,
        dummyImplicit5: Any? = null,
        dummyImplicit6: Any? = null,
        dummyImplicit7: Any? = null): HK<HKF, Tuple9<A, B, C, D, E, F, G, H, Z>> =
        AP.map(AP.product(this, other), { Tuple9(it.a.a, it.a.b, it.a.c, it.a.d, it.a.e, it.a.f, it.a.g, it.a.h, it.b) })

fun <HKF, A, B, C, D, E, F, G, H, I, Z> HK<HKF, Tuple9<A, B, C, D, E, F, G, H, I>>.product(
        AP: Applicative<HKF>,
        other: HK<HKF, Z>,
        dummyImplicit: Any? = null,
        dummyImplicit2: Any? = null,
        dummyImplicit3: Any? = null,
        dummyImplicit4: Any? = null,
        dummyImplicit5: Any? = null,
        dummyImplicit6: Any? = null,
        dummyImplicit7: Any? = null,
        dummyImplicit9: Any? = null): HK<HKF, Tuple10<A, B, C, D, E, F, G, H, I, Z>> =
        AP.map(AP.product(this, other), { Tuple10(it.a.a, it.a.b, it.a.c, it.a.d, it.a.e, it.a.f, it.a.g, it.a.h, it.a.i, it.b) })

fun <HKF, A, B> Applicative<HKF>.tupled(
        a: HK<HKF, A>,
        b: HK<HKF, B>): HK<HKF, Tuple2<A, B>> =
        a.product(this, b)

fun <HKF, A, B, C> Applicative<HKF>.tupled(
        a: HK<HKF, A>,
        b: HK<HKF, B>,
        c: HK<HKF, C>): HK<HKF, Tuple3<A, B, C>> =
        a.product(this, b).product(this, c)

fun <HKF, A, B, C, D> Applicative<HKF>.tupled(
        a: HK<HKF, A>,
        b: HK<HKF, B>,
        c: HK<HKF, C>,
        d: HK<HKF, D>): HK<HKF, Tuple4<A, B, C, D>> =
        a.product(this, b).product(this, c).product(this, d)

fun <HKF, A, B, C, D, E> Applicative<HKF>.tupled(
        a: HK<HKF, A>,
        b: HK<HKF, B>,
        c: HK<HKF, C>,
        d: HK<HKF, D>,
        e: HK<HKF, E>): HK<HKF, Tuple5<A, B, C, D, E>> =
        a.product(this, b).product(this, c).product(this, d).product(this, e)

fun <HKF, A, B, C, D, E, F> Applicative<HKF>.tupled(
        a: HK<HKF, A>,
        b: HK<HKF, B>,
        c: HK<HKF, C>,
        d: HK<HKF, D>,
        e: HK<HKF, E>,
        f: HK<HKF, F>): HK<HKF, Tuple6<A, B, C, D, E, F>> =
        a.product(this, b).product(this, c).product(this, d).product(this, e).product(this, f)

fun <HKF, A, B, C, D, E, F, G> Applicative<HKF>.tupled(
        a: HK<HKF, A>,
        b: HK<HKF, B>,
        c: HK<HKF, C>,
        d: HK<HKF, D>,
        e: HK<HKF, E>,
        f: HK<HKF, F>,
        g: HK<HKF, G>): HK<HKF, Tuple7<A, B, C, D, E, F, G>> =
        a.product(this, b).product(this, c).product(this, d).product(this, e).product(this, f).product(this, g)

fun <HKF, A, B, C, D, E, F, G, H> Applicative<HKF>.tupled(
        a: HK<HKF, A>,
        b: HK<HKF, B>,
        c: HK<HKF, C>,
        d: HK<HKF, D>,
        e: HK<HKF, E>,
        f: HK<HKF, F>,
        g: HK<HKF, G>,
        h: HK<HKF, H>): HK<HKF, Tuple8<A, B, C, D, E, F, G, H>> =
        a.product(this, b).product(this, c).product(this, d).product(this, e).product(this, f).product(this, g).product(this, h)

fun <HKF, A, B, C, D, E, F, G, H, I> Applicative<HKF>.tupled(
        a: HK<HKF, A>,
        b: HK<HKF, B>,
        c: HK<HKF, C>,
        d: HK<HKF, D>,
        e: HK<HKF, E>,
        f: HK<HKF, F>,
        g: HK<HKF, G>,
        h: HK<HKF, H>,
        i: HK<HKF, I>): HK<HKF, Tuple9<A, B, C, D, E, F, G, H, I>> =
        a.product(this, b).product(this, c).product(this, d).product(this, e).product(this, f).product(this, g).product(this, h).product(this, i)

fun <HKF, A, B, C, D, E, F, G, H, I, J> Applicative<HKF>.tupled(
        a: HK<HKF, A>,
        b: HK<HKF, B>,
        c: HK<HKF, C>,
        d: HK<HKF, D>,
        e: HK<HKF, E>,
        f: HK<HKF, F>,
        g: HK<HKF, G>,
        h: HK<HKF, H>,
        i: HK<HKF, I>,
        j: HK<HKF, J>): HK<HKF, Tuple10<A, B, C, D, E, F, G, H, I, J>> =
        a.product(this, b).product(this, c).product(this, d).product(this, e).product(this, f).product(this, g).product(this, h).product(this, i).product(this, j)

fun <HKF, A, B, Z> Applicative<HKF>.map(
        a: HK<HKF, A>,
        b: HK<HKF, B>,
        lbd: (Tuple2<A, B>) -> Z): HK<HKF, Z> =
        this.map(a.product(this, b), lbd)

fun <HKF, A, B, C, Z> Applicative<HKF>.map(
        a: HK<HKF, A>,
        b: HK<HKF, B>,
        c: HK<HKF, C>,
        lbd: (Tuple3<A, B, C>) -> Z): HK<HKF, Z> =
        this.map(a.product(this, b).product(this, c), lbd)

fun <HKF, A, B, C, D, Z> Applicative<HKF>.map(
        a: HK<HKF, A>,
        b: HK<HKF, B>,
        c: HK<HKF, C>,
        d: HK<HKF, D>,
        lbd: (Tuple4<A, B, C, D>) -> Z): HK<HKF, Z> =
        this.map(a.product(this, b).product(this, c).product(this, d), lbd)

fun <HKF, A, B, C, D, E, Z> Applicative<HKF>.map(
        a: HK<HKF, A>,
        b: HK<HKF, B>,
        c: HK<HKF, C>,
        d: HK<HKF, D>,
        e: HK<HKF, E>,
        lbd: (Tuple5<A, B, C, D, E>) -> Z): HK<HKF, Z> =
        this.map(a.product(this, b).product(this, c).product(this, d).product(this, e), lbd)

fun <HKF, A, B, C, D, E, F, Z> Applicative<HKF>.map(
        a: HK<HKF, A>,
        b: HK<HKF, B>,
        c: HK<HKF, C>,
        d: HK<HKF, D>,
        e: HK<HKF, E>,
        f: HK<HKF, F>,
        lbd: (Tuple6<A, B, C, D, E, F>) -> Z): HK<HKF, Z> =
        this.map(a.product(this, b).product(this, c).product(this, d).product(this, e).product(this, f), lbd)

fun <HKF, A, B, C, D, E, F, G, Z> Applicative<HKF>.map(
        a: HK<HKF, A>,
        b: HK<HKF, B>,
        c: HK<HKF, C>,
        d: HK<HKF, D>,
        e: HK<HKF, E>,
        f: HK<HKF, F>,
        g: HK<HKF, G>,
        lbd: (Tuple7<A, B, C, D, E, F, G>) -> Z): HK<HKF, Z> =
        this.map(a.product(this, b).product(this, c).product(this, d).product(this, e).product(this, f).product(this, g), lbd)

fun <HKF, A, B, C, D, E, F, G, H, Z> Applicative<HKF>.map(
        a: HK<HKF, A>,
        b: HK<HKF, B>,
        c: HK<HKF, C>,
        d: HK<HKF, D>,
        e: HK<HKF, E>,
        f: HK<HKF, F>,
        g: HK<HKF, G>,
        h: HK<HKF, H>,
        lbd: (Tuple8<A, B, C, D, E, F, G, H>) -> Z): HK<HKF, Z> =
        this.map(a.product(this, b).product(this, c).product(this, d).product(this, e).product(this, f).product(this, g).product(this, h), lbd)

fun <HKF, A, B, C, D, E, F, G, H, I, Z> Applicative<HKF>.map(
        a: HK<HKF, A>,
        b: HK<HKF, B>,
        c: HK<HKF, C>,
        d: HK<HKF, D>,
        e: HK<HKF, E>,
        f: HK<HKF, F>,
        g: HK<HKF, G>,
        h: HK<HKF, H>,
        i: HK<HKF, I>,
        lbd: (Tuple9<A, B, C, D, E, F, G, H, I>) -> Z): HK<HKF, Z> =
        this.map(a.product(this, b).product(this, c).product(this, d).product(this, e).product(this, f).product(this, g).product(this, h).product(this, i), lbd)

fun <HKF, A, B, C, D, E, F, G, H, I, J, Z> Applicative<HKF>.map(
        a: HK<HKF, A>,
        b: HK<HKF, B>,
        c: HK<HKF, C>,
        d: HK<HKF, D>,
        e: HK<HKF, E>,
        f: HK<HKF, F>,
        g: HK<HKF, G>,
        h: HK<HKF, H>,
        i: HK<HKF, I>,
        j: HK<HKF, J>,
        lbd: (Tuple10<A, B, C, D, E, F, G, H, I, J>) -> Z): HK<HKF, Z> =
        this.map(a.product(this, b).product(this, c).product(this, d).product(this, e).product(this, f).product(this, g).product(this, h).product(this, i).product(this, j), lbd)