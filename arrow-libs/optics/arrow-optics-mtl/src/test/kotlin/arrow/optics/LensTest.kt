package arrow.optics

import arrow.core.test.UnitSpec
import arrow.core.test.generators.functionAToB
import arrow.core.toT
import arrow.mtl.State
import arrow.mtl.map
import arrow.mtl.run
import arrow.mtl.runId
import arrow.optics.mtl.ask
import arrow.optics.mtl.asks
import arrow.optics.mtl.assign
import arrow.optics.mtl.assignOld
import arrow.optics.mtl.assign_
import arrow.optics.mtl.extract
import arrow.optics.mtl.extractMap
import arrow.optics.mtl.toReader
import arrow.optics.mtl.toState
import arrow.optics.mtl.update
import arrow.optics.mtl.updateOld
import arrow.optics.mtl.update_
import io.kotest.property.Arb
import io.kotest.property.checkAll

class LensTest : UnitSpec() {

  init {

    "Asking for the focus in a Reader" {
      checkAll(genToken) { token: Token ->
        tokenLens.ask().runId(token) == token.value
      }
    }

    "toReader is an alias for ask" {
      checkAll(genToken) { token: Token ->
        tokenLens.ask().runId(token) == tokenLens.toReader().runId(token)
      }
    }

    "Asks with f is the same as applying f to the focus of the lens" {
      checkAll(genToken, Arb.functionAToB<String, String>(Arb.string())) { token, f ->
        tokenLens.asks(f).runId(token) == f(token.value)
      }
    }

    "Extract should extract the focus from the state" {
      checkAll(genToken) { generatedToken ->
        tokenLens.extract().run(generatedToken) ==
          State { token: Token ->
            token toT tokenLens.get(token)
          }.run(generatedToken)
      }
    }

    "toState should be an alias to extract" {
      checkAll(genToken) { token ->
        tokenLens.toState().run(token) == tokenLens.extract().run(token)
      }
    }

    "Extracts with f should be same as extract and map" {
      checkAll(genToken, Arb.functionAToB<String, String>(Arb.string())) { generatedToken, f ->
        tokenLens.extractMap(f).run(generatedToken) == tokenLens.extract().map(f).run(generatedToken)
      }
    }

    "update f should be same modify f within State and returning new state" {
      checkAll(genToken, Arb.functionAToB<String, String>(Arb.string())) { generatedToken, f ->
        tokenLens.update(f).run(generatedToken) ==
          State { token: Token ->
            tokenLens.modify(token, f)
              .let { it toT it.value }
          }.run(generatedToken)
      }
    }

    "updateOld f should be same as modify f within State and returning old state" {
      checkAll(genToken, Arb.functionAToB<String, String>(Arb.string())) { generatedToken, f ->
        tokenLens.updateOld(f).run(generatedToken) ==
          State { token: Token ->
            tokenLens.modify(token, f) toT tokenLens.get(token)
          }.run(generatedToken)
      }
    }

    "update_ f should be as modify f within State and returning Unit" {
      checkAll(genToken, Arb.functionAToB<String, String>(Arb.string())) { generatedToken, f ->
        tokenLens.update_(f).run(generatedToken) ==
          State { token: Token ->
            tokenLens.modify(token, f) toT Unit
          }.run(generatedToken)
      }
    }

    "assign a should be same set a within State and returning new value" {
      checkAll(genToken, Arb.string()) { generatedToken, string ->
        tokenLens.assign(string).run(generatedToken) ==
          State { token: Token ->
            tokenLens.set(token, string)
              .let { it toT it.value }
          }.run(generatedToken)
      }
    }

    "assignOld f should be same as modify f within State and returning old state" {
      checkAll(genToken, Arb.string()) { generatedToken, string ->
        tokenLens.assignOld(string).run(generatedToken) ==
          State { token: Token ->
            tokenLens.set(token, string) toT tokenLens.get(token)
          }.run(generatedToken)
      }
    }

    "assign_ f should be as modify f within State and returning Unit" {
      checkAll(genToken, Arb.string()) { generatedToken, string ->
        tokenLens.assign_(string).run(generatedToken) ==
          State { token: Token ->
            tokenLens.set(token, string) toT Unit
          }.run(generatedToken)
      }
    }
  }
}
