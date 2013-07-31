package bound

import org.scalacheck.Prop._
import bound.scalacheck.BoundArbitraryInstances._
import scalaz.{Equal, Scalaz}
import Scalaz._
import Scope._

object ScopeTests extends BoundProperties("Scope Tests"){

  test("== reflexivity for Scope (with List)")(forAll{ (scope:Scope[Int, List, String]) => eqls(scope, scope) })
  test("== reflexivity for Scope (with Option)")(forAll{ (scope:Scope[Int, Option, String]) => eqls(scope, scope) })

  def eqls[F[+_]](a:Scope[Int, F, String], b: Scope[Int, F, String])(implicit eql: Equal[Scope[Int, F, String]]) = {
    eql.equal(a, b)
  }
}
