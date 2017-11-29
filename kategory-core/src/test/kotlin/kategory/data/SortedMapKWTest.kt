package kategory

import io.kotlintest.KTestJUnitRunner
import io.kotlintest.matchers.shouldNotBe
import io.kotlintest.properties.forAll
import org.junit.runner.RunWith

@RunWith(KTestJUnitRunner::class)
class SortedMapKWTest : UnitSpec() {

    val EQ: Eq<HK2<SortedMapKWHK, String, Int>> = object : Eq<HK2<SortedMapKWHK, String, Int>> {
        override fun eqv(a: HK2<SortedMapKWHK, String, Int>, b: HK2<SortedMapKWHK, String, Int>): Boolean =
            a.ev().get("key") == b.ev().get("key")
    }

    init {

        "instances can be resolved implicitly" {
            functor<SortedMapKWHK>() shouldNotBe null
            foldable<SortedMapKWHK>() shouldNotBe null
            traverse<SortedMapKWHK>() shouldNotBe null
            semigroup<SortedMapKWKind<String, Int>>() shouldNotBe null
            monoid<SortedMapKWKind<String, Int>>() shouldNotBe null
        }


        testLaws(
                MonoidLaws.laws(SortedMapKW.monoid<String, Int>(),
                        { n -> SortedMapKW(sortedMapOf("key" to n)) },
                        EQ),
                SemigroupLaws.laws(SortedMapKW.monoid<String, Int>(),
                        { n -> SortedMapKW(sortedMapOf("key" to n)) },
                        EQ),
                TraverseLaws.laws(
                        SortedMapKW.traverse<String>(),
                        SortedMapKW.traverse<String>(),
                        { a: Int -> sortedMapOf("key" to a).k() },
                        EQ))

    }


}
