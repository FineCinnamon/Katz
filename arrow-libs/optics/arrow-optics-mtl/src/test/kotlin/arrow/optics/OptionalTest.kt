package arrow.optics

import arrow.core.Option
import arrow.core.test.UnitSpec
import arrow.core.test.generators.functionAToB
import arrow.core.test.generators.option
import arrow.core.toT
import arrow.mtl.State
import arrow.mtl.map
import arrow.mtl.run
import arrow.optics.mtl.assign
import arrow.optics.mtl.assignOld
import arrow.optics.mtl.assign_
import arrow.optics.mtl.extract
import arrow.optics.mtl.extractMap
import arrow.optics.mtl.toState
import arrow.optics.mtl.update
import arrow.optics.mtl.updateOld
import arrow.optics.mtl.update_
import io.kotest.property.Arb
import io.kotest.property.checkAll

class OptionalTest : UnitSpec() {

  init {

    val successInt = Option.some<Int>().asOptional()

    "Extract should extract the focus from the state" {
      checkAll(Gen.option(Arb.int())) { tryInt ->
        successInt.extract().run(tryInt) ==
          State { x: Option<Int> ->
            x toT successInt.getOption(x)
          }.run(tryInt)
      }
    }

    "toState should be an alias to extract" {
      checkAll(Gen.option(Arb.int())) { x ->
        successInt.toState().run(x) == successInt.extract().run(x)
      }
    }

    "extractMap with f should be same as extract and map" {
      checkAll(Gen.option(Arb.int()), Arb.functionAToB<Int, Int>(Arb.int())) { x, f ->
        successInt.extractMap(f).run(x) == successInt.extract().map { it.map(f) }.run(x)
      }
    }

    "update f should be same modify f within State and returning new state" {
      checkAll(Gen.option(Arb.int()), Arb.functionAToB<Int, Int>(Arb.int())) { x, f ->
        successInt.update(f).run(x) ==
          State { xx: Option<Int> ->
            successInt.modify(xx, f)
              .let { it toT successInt.getOption(it) }
          }.run(x)
      }
    }

    "updateOld f should be same as modify f within State and returning old state" {
      checkAll(Gen.option(Arb.int()), Arb.functionAToB<Int, Int>(Arb.int())) { x, f ->
        successInt.updateOld(f).run(x) ==
          State { xx: Option<Int> ->
            successInt.modify(xx, f) toT successInt.getOption(xx)
          }.run(x)
      }
    }

    "update_ f should be as modify f within State and returning Unit" {
      checkAll(Gen.option(Arb.int()), Arb.functionAToB<Int, Int>(Arb.int())) { x, f ->
        successInt.update_(f).run(x) ==
          State { xx: Option<Int> ->
            successInt.modify(xx, f) toT Unit
          }.run(x)
      }
    }

    "assign a should be same set a within State and returning new value" {
      checkAll(Gen.option(Arb.int()), Arb.int()) { x, i ->
        successInt.assign(i).run(x) ==
          State { xx: Option<Int> ->
            successInt.set(xx, i)
              .let { it toT successInt.getOption(it) }
          }.run(x)
      }
    }

    "assignOld f should be same as modify f within State and returning old state" {
      checkAll(Gen.option(Arb.int()), Arb.int()) { x, i ->
        successInt.assignOld(i).run(x) ==
          State { xx: Option<Int> ->
            successInt.set(xx, i) toT successInt.getOption(xx)
          }.run(x)
      }
    }

    "assign_ f should be as modify f within State and returning Unit" {
      checkAll(Gen.option(Arb.int()), Arb.int()) { x, i ->
        successInt.assign_(i).run(x) ==
          State { xx: Option<Int> ->
            successInt.set(xx, i) toT Unit
          }.run(x)
      }
    }
  }
}
