/*
 * Copyright (C) 2017 The Katz Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package katz

class IorMonad<L>(val SL: Semigroup<L>) : Monad<HK<Ior.F, L>> {
    override fun <A, B> flatMap(fa: IorKind<L, A>, f: (A) -> IorKind<L, B>): Ior<L, B> =
            fa.ev().flatMap(SL, { f(it).ev() })

    override fun <A> pure(a: A): Ior<L, A> = Ior.Right(a)

}

fun <A, B> IorKind<A, B>.ev(): Ior<A, B> = this as Ior<A, B>