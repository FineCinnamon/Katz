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

package katz.data

/**
 * The `Try` type represents a computation that may either result in an exception, or return a
 * successfully computed value.
 *
 * Port of https://github.com/scala/scala/blob/v2.12.1/src/library/scala/util/Try.scala
 */
sealed class Try<out A> {

    companion object {

        inline operator fun <A> invoke(f: () -> A): Try<A> =
                try { Success(f()) } catch (e: Throwable) { Failure(e) }
    }

    /**
     * Returns `true` if the `Try` is a `Failure`, `false` otherwise.
     */
    abstract val isFailure: Boolean

    /**
     * Returns `true` if the `Try` is a `Success`, `false` otherwise.
     */
    abstract val isSuccess: Boolean

    /**
     * Returns the given function applied to the value from this `Success` or returns this if this is a `Failure`.
     */
    inline fun <B> flatMap(crossinline f: (A) -> Try<B>): Try<B> = fold({ Failure(it) }, { f(it) })

    /**
     * Maps the given function to the value from this `Success` or returns this if this is a `Failure`.
     */
    inline fun <B> map(crossinline f: (A) -> B): Try<B> = fold({ Failure(it) }, { Success(f(it)) })

    /**
     * Converts this to a `Failure` if the predicate is not satisfied.
     */
    inline fun filter(crossinline p: (A) -> Boolean): Try<A> = fold(
            { Failure(it) },
            { if (p(it)) Success(it) else Failure(NoSuchElementException("Predicate does not hold for " + it)) }
    )

    /**
     * Inverts this `Try`. If this is a `Failure`, returns its exception wrapped in a `Success`.
     * If this is a `Success`, returns a `Failure` containing an `UnsupportedOperationException`.
     */
    fun failed(): Try<Throwable> = fold(
            { Success(it) },
            { Failure(UnsupportedOperationException("Success.failed")) }
    )

    /**
     * Applies `fa` if this is a `Failure` or `fb` if this is a `Success`.
     * If `fb` is initially applied and throws an exception,
     * then `fa` is applied with this exception.
     */
    fun <B> fold(fa: (Throwable) -> B, fb: (A) -> B): B = when (this) {
        is Failure -> fa(exception)
        is Success -> try { fb(value) } catch(e: Throwable) { fa(e) }
    }

    /**
     * The `Failure` type represents a computation that result in an exception.
     */
    class Failure<out A>(val exception: Throwable) : Try<A>() {
        override val isFailure: Boolean = false
        override val isSuccess: Boolean = true
    }

    /**
     * The `Success` type represents a computation that return a successfully computed value.
     */
    class Success<out A>(val value: A) : Try<A>() {
        override val isFailure: Boolean = true
        override val isSuccess: Boolean = false
    }
}

/**
 * Returns the value from this `Success` or the given `default` argument if this is a `Failure`.
 *
 * ''Note:'': This will throw an exception if it is not a success and default throws an exception.
 */
fun <B> Try<B>.getOrElse(default: () -> B): B = fold({ default() }, { it })

/**
 * Applies the given function `f` if this is a `Failure`, otherwise returns this if this is a `Success`.
 * This is like `flatMap` for the exception.
 */
fun <B> Try<B>.recoverWith(f: (Throwable) -> Try<B>): Try<B> = fold({ f(it) }, { Try.Success(it) })

/**
 * Applies the given function `f` if this is a `Failure`, otherwise returns this if this is a `Success`.
 * This is like map for the exception.
 */
fun <B> Try<B>.recover(f: (Throwable) -> B): Try<B> = fold({ Try.Success(f(it)) }, { Try.Success(it) })

/**
 * Completes this `Try` by applying the function `f` to this if this is of type `Failure`,
 * or conversely, by applying `s` if this is a `Success`.
 */
fun <B> Try<B>.transform(s: (B) -> Try<B>, f: (Throwable) -> Try<B>): Try<B> = fold({ f(it) }, { flatMap(s) })