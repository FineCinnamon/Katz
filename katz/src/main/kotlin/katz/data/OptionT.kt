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
package katz

typealias OptionTKind<F, A> = HK2<OptionT.F, F, A>
typealias OptionTF<F> = HK<OptionT.F, F>

/**
 * [OptionT]`<F, A>` is a light wrapper on an `F<`[Option]`<A>>` with some
 * convenient methods for working with this nested structure.
 *
 * It may also be said that [OptionT] is a monad transformer for [Option].
 */
data class OptionT<F, A>(val value: HK<F, Option<A>>) : OptionTKind<F, A> {

    class F private constructor()

    companion object {
        fun <M, A> pure(a: A): OptionT<M, A> = OptionT(instance<Applicative<M>>().pure(Option.Some(a)))

        fun <M> none(): OptionT<M, Nothing> = OptionT(instance<Applicative<M>>().pure(Option.None))

        fun <M, A> fromOption(value: Option<A>): OptionT<M, A> = OptionT(instance<Applicative<M>>().pure(value))
    }

    inline fun <B> fold(crossinline default: () -> B, crossinline f: (A) -> B): HK<F, B> =
            instance<Functor<F>>().map(value, { option -> option.fold({ default() }, { f(it) }) })

    inline fun <B> cata(F: Functor<F>, crossinline default: () -> B, crossinline f: (A) -> B): HK<F, B> =
            fold({ default() }, { f(it) })

    inline fun <B> flatMap(crossinline f: (A) -> OptionT<F, B>): OptionT<F, B> = flatMapF({ it -> f(it).value })

    inline fun <B> flatMapF(crossinline f: (A) -> HK<F, Option<B>>): OptionT<F, B> =
            OptionT(instance<Monad<F>>().flatMap(value, { option -> option.fold({ instance<Applicative<F>>().pure(Option.None) }, { f(it) }) }))

    fun <B> liftF(fa: HK<F, B>): OptionT<F, B> = OptionT(instance<Functor<F>>().map(fa, { Option.Some(it) }))

    inline fun <B> semiflatMap(crossinline f: (A) -> HK<F, B>): OptionT<F, B> =
            flatMap({ option -> liftF(f(option)) })

    inline fun <B> map(crossinline f: (A) -> B): OptionT<F, B> =
            OptionT(instance<Functor<F>>().map(value, { it.map(f) }))

    fun getOrElse(default: () -> A): HK<F, A> = instance<Functor<F>>().map(value, { it.getOrElse(default) })

    inline fun getOrElseF(crossinline default: () -> HK<F, A>): HK<F, A> = instance<Monad<F>>().flatMap(value, { it.fold(default, { instance<Applicative<F>>().pure(it) }) })

    inline fun filter(crossinline p: (A) -> Boolean): OptionT<F, A> = OptionT(instance<Functor<F>>().map(value, { it.filter(p) }))

    inline fun forall(crossinline p: (A) -> Boolean): HK<F, Boolean> = instance<Functor<F>>().map(value, { it.forall(p) })

    fun isDefined(): HK<F, Boolean> = instance<Functor<F>>().map(value, { it.isDefined })

    fun isEmpty(): HK<F, Boolean> = instance<Functor<F>>().map(value, { it.isEmpty })

    inline fun orElse(crossinline default: () -> OptionT<F, A>): OptionT<F, A> =
            orElseF({ default().value })

    inline fun orElseF(crossinline default: () -> HK<F, Option<A>>): OptionT<F, A> =
            OptionT(instance<Monad<F>>().flatMap(value) {
                when (it) {
                    is Option.Some<A> -> instance<Applicative<F>>().pure(it)
                    is Option.None -> default()
                }
            })

    inline fun <B> transform(crossinline f: (Option<A>) -> Option<B>): OptionT<F, B> =
            OptionT(instance<Functor<F>>().map(value, { f(it) }))

    inline fun <B> subflatMap(crossinline f: (A) -> Option<B>): OptionT<F, B> =
            transform({ it.flatMap(f) })

    //TODO: add toRight() and toLeft() once EitherT it's available
}