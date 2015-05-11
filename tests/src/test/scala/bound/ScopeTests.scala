package bound

import org.scalacheck.Prop._
import bound.scalacheck.BoundArbitraryInstances._
import scalaz._
import Scalaz._
import Scope._

object ScopeTests extends BoundProperties("Scope Tests"){

  test("== reflexivity for Scope (with List)")(forAll{ (scope:Scope[Int, List, String]) => eqls(scope, scope) })
  test("== reflexivity for Scope (with Option)")(forAll{ (scope:Scope[Int, Option, String]) => eqls(scope, scope) })

  def eqls[F[+_]](a:Scope[Int, F, String], b: Scope[Int, F, String])(implicit eql: Equal[Scope[Int, F, String]]) = {
    eql.equal(a, b)
  }

  object instances {
    def functor[F[_]: Functor, A] = Functor[({type λ[α] = Scope[A, F, α]})#λ]
    def foldable[F[_]: Foldable, A] = Foldable[({type λ[α] = Scope[A, F, α]})#λ]
    def traverse[F[_]: Traverse, A] = Traverse[({type λ[α] = Scope[A, F, α]})#λ]
    def monad[F[_]: Monad, A] = Monad[({type λ[α] = Scope[A, F, α]})#λ]

    // checking absence of ambiguity
    def foldable[F[_]: Traverse, A] = Foldable[({type λ[α] = Scope[A, F, α]})#λ]
    def functor[F[_]: Traverse, A] = Functor[({type λ[α] = Scope[A, F, α]})#λ]
    def functor[F[_]: Monad, A] = Functor[({type λ[α] = Scope[A, F, α]})#λ]
    def functor[F[_]: Traverse: Monad, A] = Functor[({type λ[α] = Scope[A, F, α]})#λ]
  }
}
