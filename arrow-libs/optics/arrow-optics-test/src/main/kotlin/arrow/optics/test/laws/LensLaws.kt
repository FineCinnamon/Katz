package arrow.optics.test.laws

import arrow.core.compose
import arrow.core.identity
import arrow.optics.Lens
import arrow.core.test.laws.Law
import arrow.core.test.laws.equalUnderTheLaw
import io.kotest.property.Arb
import io.kotest.property.checkAll

object LensLaws {

  fun <A, B> laws(
    lensGen: Arb<Lens<A, B>>,
    aGen: Arb<A>,
    bGen: Arb<B>,
    funcGen: Arb<(B) -> B>,
    eqa: (A, A) -> Boolean = { a, b -> a == b },
    eqb: (B, B) -> Boolean = { a, b -> a == b }
  ): List<Law> =
    listOf(
      Law("Lens law: get set") { lensGetSet(lensGen, aGen, eqa) },
      Law("Lens law: set get") { lensSetGet(lensGen, aGen, bGen, eqb) },
      Law("Lens law: is set idempotent") { lensSetIdempotent(lensGen, aGen, bGen, eqa) },
      Law("Lens law: modify identity") { lensModifyIdentity(lensGen, aGen, eqa) },
      Law("Lens law: compose modify") { lensComposeModify(lensGen, aGen, funcGen, eqa) },
      Law("Lens law: consistent set modify") { lensConsistentSetModify(lensGen, aGen, bGen, eqa) }
    )

  /**
   * Warning: Use only when a `Gen.constant()` applies
   */
  fun <A, B> laws(
    lens: Lens<A, B>,
    aGen: Arb<A>,
    bGen: Arb<B>,
    funcGen: Arb<(B) -> B>,
    eqa: (A, A) -> Boolean = { a, b -> a == b },
    eqb: (B, B) -> Boolean = { a, b -> a == b }
  ): List<Law> = laws(Gen.constant(lens), aGen, bGen, funcGen, eqa, eqb)

  fun <A, B> lensGetSet(lensGen: Arb<Lens<A, B>>, aGen: Arb<A>, eq: (A, A) -> Boolean) =
    checkAll(lensGen, aGen) { lens, a ->
      lens.run {
        set(a, get(a)).equalUnderTheLaw(a, eq)
      }
    }

  fun <A, B> lensSetGet(lensGen: Arb<Lens<A, B>>, aGen: Arb<A>, bGen: Arb<B>, eq: (B, B) -> Boolean) =
    checkAll(lensGen, aGen, bGen) { lens, a, b ->
      lens.run {
        get(set(a, b)).equalUnderTheLaw(b, eq)
      }
    }

  fun <A, B> lensSetIdempotent(lensGen: Arb<Lens<A, B>>, aGen: Arb<A>, bGen: Arb<B>, eq: (A, A) -> Boolean) =
    checkAll(lensGen, aGen, bGen) { lens, a, b ->
      lens.run {
        set(set(a, b), b).equalUnderTheLaw(set(a, b), eq)
      }
    }

  fun <A, B> lensModifyIdentity(lensGen: Arb<Lens<A, B>>, aGen: Arb<A>, eq: (A, A) -> Boolean) =
    checkAll(lensGen, aGen) { lens, a ->
      lens.run {
        modify(a, ::identity).equalUnderTheLaw(a, eq)
      }
    }

  fun <A, B> lensComposeModify(lensGen: Arb<Lens<A, B>>, aGen: Arb<A>, funcGen: Arb<(B) -> B>, eq: (A, A) -> Boolean) =
    checkAll(lensGen, aGen, funcGen, funcGen) { lens, a, f, g ->
      lens.run {
        modify(modify(a, f), g).equalUnderTheLaw(modify(a, g compose f), eq)
      }
    }

  fun <A, B> lensConsistentSetModify(lensGen: Arb<Lens<A, B>>, aGen: Arb<A>, bGen: Arb<B>, eq: (A, A) -> Boolean) =
    checkAll(lensGen, aGen, bGen) { lens, a, b ->
      lens.run {
        set(a, b).equalUnderTheLaw(modify(a) { b }, eq)
      }
    }
}
