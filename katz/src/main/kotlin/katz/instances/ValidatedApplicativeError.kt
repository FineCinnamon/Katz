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

class ValidatedApplicativeError<E>(val SE: Semigroup<E>) : ApplicativeError<ValidatedF<E>, E> {

    override fun <A> pure(a: A): Validated<E, A> = Validated.Valid(a)

    override fun <A> raiseError(e: E): Validated<E, A> = Validated.Invalid(e)

    override fun <A> handleErrorWith(fa: ValidatedKind<E, A>, f: (E) -> ValidatedKind<E, A>): Validated<E, A> =
            fa.ev().fold({ f(it).ev() }, { Validated.Valid(it) })

    override fun <A, B> ap(fa: ValidatedKind<E, A>, ff: HK<ValidatedF<E>, (A) -> B>): Validated<E, B> =
            fa.ev().ap(ff.ev(), SE)
}

fun <E, A> ValidatedKind<E, A>.ev(): Validated<E, A> = this as Validated<E, A>
