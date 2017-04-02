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

import io.kotlintest.KTestJUnitRunner
import io.kotlintest.matchers.fail
import io.kotlintest.matchers.shouldBe
import io.kotlintest.properties.forAll
import katz.Option.*
import kotlinx.collections.immutable.immutableHashMapOf
import org.junit.runner.RunWith
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.*

@RunWith(KTestJUnitRunner::class)
class OptionTest : UnitSpec() {
    init {
        "map should modify value" {
            Some(12).map { "flower" } shouldBe Some("flower")
            None.map { "flower" } shouldBe None
        }

        "flatMap should modify entity" {
            Some(1).flatMap { None } shouldBe None
            Some(1).flatMap { Some("something") } shouldBe Some("something")
            None.flatMap { Some("something") } shouldBe None
        }

        "getOrElse should return value" {
            Some(12).getOrElse { 17 } shouldBe 12
            None.getOrElse { 17 } shouldBe 17
        }

        "exits should evaluate value" {
            val none: Option<Int> = None

            Some(12).exists { it > 10 } shouldBe true
            Some(7).exists { it > 10 } shouldBe false
            none.exists { it > 10 } shouldBe false
        }

        "fold should return default value on None" {
            val exception = Exception()
            val result: Option<String> = None
            result.fold(
                    { exception },
                    { fail("Some should not be called") }
            ) shouldBe exception
        }

        "fold should call function on Some" {
            val value = "Some value"
            val result: Option<String> = Some(value)
            result.fold(
                    { fail("None should not be called") },
                    { value }
            ) shouldBe value
        }

        "fromNullable should work for both null and non-null values of nullable types" {
            forAll { a: Int? ->
                // This seems to be generating only non-null values, so it is complemented by the next test
                val o: Option<Int> = Option.fromNullable(a)
                if (a == null) o == None else o == Some(a)
            }
        }

        "fromNullable should return none for null values of nullable types" {
            val a: Int? = null
            Companion.fromNullable(a) shouldBe None
        }

        "Option.monad.flatMap should be consistent with Option#flatMap" {
            forAll { a: Int ->
                val x = { b: Int -> Option(b * a) }
                val option = Option(a)
                option.flatMap(x) == OptionMonad.flatMap(option, x)
            }
        }

        "Option.monad.binding should for comprehend over option" {
            val result = OptionMonad.binding {
                val x = !Option(1)
                val y = Option(1).bind()
                val z = bind { Option(1) }
                yields(x + y + z)
            }
            result shouldBe Option(3)
        }

        "Cartesian builder should build products over option" {
            OptionMonad.map(Option(1), Option("a"), Option(true), { (a, b, c) ->
                "$a $b $c"
            }) shouldBe Option("1 a true")
        }

        "Cartesian builder works inside for comprehensions" {
            val result = OptionMonad.binding {
                val (x, y, z) = !OptionMonad.tupled(Option(1), Option(1), Option(1))
                val a = Option(1).bind()
                val b = bind { Option(1) }
                yields(x + y + z + a + b)
            }
            result shouldBe Option(5)
        }

        "kikimangui" {
            val i: Monad<Option.F> = instance()
            
        }
    }
}


//
open class TypeLiteral<T> {
    val type: Type
        get() = (javaClass.genericSuperclass as ParameterizedType).actualTypeArguments[0]
}

inline fun <reified T> typeLiteral(): Type = object : TypeLiteral<T>() {}.type

data class TypeclassInstance<T: Any, F : Any>(val tc: KClass<T>, val fc: KClass<F>)

val GlobalInstances = immutableHashMapOf(
        (typeLiteral<Monad<Option.F>>()).to(OptionMonad)
)

inline fun <reified T : Any> instance(): T = GlobalInstances.getValue(typeLiteral<T>()) as T

//inline fun <reified T : Any> instance(): T {
//    val type = object : TypeReference<T>() {}.type
//    type.typeName
//    if (type is ParameterizedType)
//        type.actualTypeArguments.forEach {
//            println(it.typeName)
//        }
//}