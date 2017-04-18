package katz

import com.sun.net.httpserver.Authenticator
import io.kotlintest.KTestJUnitRunner
import io.kotlintest.matchers.fail
import io.kotlintest.matchers.shouldBe
import org.junit.runner.RunWith
import katz.Validated.*

@RunWith(KTestJUnitRunner::class)
class ValidatedTest : UnitSpec() {

    init {

        "fold should call function on Invalid" {
            val exception = Exception("My Exception")
            val result: Validated<Throwable, String> = Invalid(exception)
            result.fold(
                    { e -> e.message + " Checked" },
                    { fail("Some should not be called") }
            ) shouldBe "My Exception Checked"
        }

        "fold should call function on Valid" {
            val value = "Some value"
            val result: Validated<Throwable, String> = Valid(value)
            result.fold(
                    { fail("None should not be called") },
                    { a -> a + " processed"}
            ) shouldBe value + " processed"
        }

        "bimap should modify the value if this is Valid or the error in otherwise" {
            Valid(10).bimap({ fail("None should not be called") }, { v -> v.toString() + " is David Tennant" }) shouldBe Valid("10 is David Tennant")
            Invalid(13).bimap({ i -> i.toString() + " is Coming soon!" }, { fail("None should not be called") }) shouldBe Invalid("13 is Coming soon!")
        }

        "map should modify value" {
            Valid(10).map { v -> v.toString() + " is David Tennant" } shouldBe Valid("10 is David Tennant")
            Invalid(13).map { fail("None should not be called") } shouldBe Invalid(13)
        }

        "leftMap should modify error" {
            Valid(10).leftMap { fail("None should not be called") } shouldBe Valid(10)
            Invalid(13).leftMap { i -> i.toString() + " is Coming soon!" } shouldBe Invalid("13 is Coming soon!")
        }

        "exist should return false if is Invalid" {
            Invalid(13).exist { fail("None should not be called") } shouldBe false
        }

        "exist should return the value of predicate if is Valid" {
            Valid(13).exist { v -> v > 10 } shouldBe true
            Valid(13).exist { v -> v < 10 } shouldBe false
        }

        "swap should return Valid(e) if is Invalid and Invalid(v) in otherwise" {
            Valid(13).swap() shouldBe Invalid(13)
            Invalid(13).swap() shouldBe Valid(13)
        }

        "getOrElse should return value if is Valid or default in otherwise" {
            Valid(13).getOrElse { fail("None should not be called") } shouldBe 13
            Invalid(13).getOrElse { "defaultValue" } shouldBe "defaultValue"
        }

        "valueOr should return value if is Valid or the the result of f in otherwise" {
            Valid(13).valueOr { fail("None should not be called") } shouldBe 13
            Invalid(13).valueOr { e ->  e.toString() + " is the defaultValue" } shouldBe "13 is the defaultValue"
        }

        "orElse should return Valid(value) if is Valid or the result of default in otherwise" {
            Valid(13).orElse { fail("None should not be called") } shouldBe Valid(13)
            Invalid(13).orElse { Valid("defaultValue") } shouldBe Valid("defaultValue")
            Invalid(13).orElse { Invalid("defaultValue") } shouldBe Invalid("defaultValue")
        }

        "foldLeft should return b when is Invalid" {
            Invalid(13).foldLeft("Coming soon!") { b, a -> fail("None should not be called") } shouldBe "Coming soon!"
        }

        "foldLeft should return f processed when is Valid" {
            Valid(10).foldLeft("Tennant") { b, a -> a.toString() + " is " + b } shouldBe "10 is Tennant"
        }

        "toEither should return Either.Right(value) if is Valid or Either.Left(error) in otherwise" {
            Valid(10).toEither() shouldBe Either.Right(10)
            Invalid(13).toEither() shouldBe Either.Left(13)
        }

        "toOption should return Option.Some(value) if is Valid or Option.None in otherwise" {
            Valid(10).toOption() shouldBe Option.Some(10)
            Invalid(13).toOption() shouldBe Option.None
        }

        "toList should return listOf(value) if is Valid or empty list in otherwise" {
            Valid(10).toList() shouldBe listOf(10)
            Invalid(13).toList() shouldBe listOf<Int>()
        }

        "toValidatedNel should return Valid(value) if is Valid or Invalid<NonEmptyList<E>, A>(error) in otherwise" {
            Valid(10).toValidatedNel() shouldBe Valid(10)
            Invalid(13).toValidatedNel() shouldBe Invalid(NonEmptyList(13, listOf()))
        }

        val plusIntSemigroup: Semigroup<Int> = object : Semigroup<Int> {
            override fun combine(a: Int, b: Int): Int = a + b
        }

        "findValid should return the first Valid value or combine or Invalid values in otherwise" {
            Valid(10).findValid(plusIntSemigroup, { fail("None should not be called") }) shouldBe Valid(10)
            Invalid(10).findValid(plusIntSemigroup, { Valid(5) }) shouldBe Valid(5)
            Invalid(10).findValid(plusIntSemigroup, { Invalid(5) }) shouldBe Invalid(15)
        }

        "ap should return Valid(f(a)) if both are Valid" {
            Valid(10).ap<Int, Int, Int>(Valid({ a -> a + 5 }), plusIntSemigroup) shouldBe Valid(15)
        }

        "ap should return first Invalid found if is unique or combine both in otherwise" {
            Invalid(10).ap<Int, Int, Int>(Valid({ a -> a + 5 }), plusIntSemigroup) shouldBe Invalid(10)
            Valid(10).ap<Int, Int, Int>(Invalid(5), plusIntSemigroup) shouldBe Invalid(5)
            Invalid(10).ap<Int, Int, Int>(Invalid(5), plusIntSemigroup) shouldBe Invalid(15)
        }

        data class MyException(val msg: String) : Exception()

        "fromTry should return Valid if is Success or Failure in otherwise" {
            Validated.fromTry(Try.Success(10)) shouldBe Valid(10)
            Validated.fromTry<Int>(Try.Failure(MyException(""))) shouldBe Invalid(MyException(""))
        }

        "fromEither should return Valid if is Right or Failure in otherwise" {
            Validated.fromEither(Either.Right(10)) shouldBe Valid(10)
            Validated.fromEither(Either.Left(10)) shouldBe Invalid(10)
        }

        "fromOption should return Valid if is Some or Invalid in otherwise" {
            Validated.fromOption<Int, Int>(Option.Some(10)) { fail("None should not be called") } shouldBe Valid(10)
            Validated.fromOption<Int, Int>(Option.None) { 5 } shouldBe Invalid(5)
        }

        "invalidNel<E> should return a Invalid<NonEmptyList<E>>" {
            Validated.invalidNel<Int, Int>(10) shouldBe Invalid(NonEmptyList(10, listOf()))
        }

        "withEither should return Valid(result) if f return Right" {
            Valid(10).withEither { it.map { it + 5 } } shouldBe Valid(15)
            Invalid(10).withEither { Either.Right(5) } shouldBe Valid(5)
        }

        "withEither should return Invalid(result) if f return Left" {
            Valid(10).withEither { Either.Left(5) } shouldBe Invalid(5)
            Invalid(10).withEither { it } shouldBe Invalid(10)
        }

        val concatStringSG: Semigroup<String> = object : Semigroup<String> {
            override fun combine(a: String, b: String): String = "$a $b"
        }

        "Cartesian builder should build products over homogeneous Validated" {
            ValidatedApplicativeError(concatStringSG).map(
                    Valid("11th"),
                    Valid("Doctor"),
                    Valid("Who"),
                    { (a, b, c) -> "$a $b $c" }) shouldBe Valid("11th Doctor Who")
        }

        "Cartesian builder should build products over heterogeneous Validated" {
            ValidatedApplicativeError(concatStringSG).map(
                    Valid(13),
                    Valid("Doctor"),
                    Valid(false),
                    { (a, b, c) -> "${a}th $b is $c" }) shouldBe Valid("13th Doctor is false")
        }

        "Cartesian builder should build products over Invalid Validated" {
            ValidatedApplicativeError(concatStringSG).map(
                    Invalid("fail1"),
                    Invalid("fail2"),
                    Valid("Who"),
                    { (a, b, c) -> "${a}th $b $c" }) shouldBe Invalid("fail1 fail2")
        }
    }
}
