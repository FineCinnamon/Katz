package kategory

import io.kotlintest.KTestJUnitRunner
import io.kotlintest.matchers.shouldBe
import io.kotlintest.properties.forAll
import org.junit.runner.RunWith

@RunWith(KTestJUnitRunner::class)
class CoyonedaTest : UnitSpec() {
    val EQ = Eq<CoyonedaKind<IdHK, Int, Int>> { a, b ->
        a.ev().lower(Id.functor()) == a.ev().lower(Id.functor())
    }

    init {

        testLaws(FunctorLaws.laws(Coyoneda.functor(), { Coyoneda.apply(Id(0), { it }) }, EQ))

        "map should be stack-safe" {
            val loops = 10000

            tailrec fun loop(n: Int, acc: Coyoneda<OptionHK, Int, Int>): Coyoneda<OptionHK, Int, Int> =
                    if (n <= 0) acc
                    else loop(n - 1, acc.map { it + 1 })

            val result = loop(loops, Coyoneda.apply(Option.Some(0), { it })).lower(Option.functor())
            val expected = Option.Some(loops)

            expected shouldBe result
        }

        "toYoneda should convert to an equivalent Yoneda" {
            forAll { x: Int ->
                val op = Coyoneda.apply(Id(x), Int::toString)
                val toYoneda = op.toYoneda(Id.functor()).lower().ev()
                val expected = Yoneda.apply(Id(x.toString())).lower().ev()

                expected == toYoneda
            }
        }
    }
}
