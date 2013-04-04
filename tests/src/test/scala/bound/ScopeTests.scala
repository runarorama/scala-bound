package bound

import org.scalacheck.Prop._
import bound.scalacheck.BoundArbitraryInstances._

object ScopeTests extends BoundProperties("Scope Tests"){

  test("== reflexivity for Scope (with List)")(forAll{ (scope:Scope[Int, List, Int]) =>
    scope ?= scope
  })

  test("== reflexivity for Scope (with Option)")(forAll{ (scope:Scope[Int, Option, Int]) =>
    scope ?= scope
  })
}
