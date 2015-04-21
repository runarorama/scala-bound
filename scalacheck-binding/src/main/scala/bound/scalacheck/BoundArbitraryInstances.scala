package bound
package scalacheck

import org.scalacheck.{Gen, Arbitrary}
import Arbitrary._
import Gen._
import scalaz._
import Scalaz._
import bound._

/**
 * Instances of {@link scalacheck.Arbitrary} for types in scala-bound.
 */
object BoundArbitraryInstances {

  private def arb[A: Arbitrary]: Arbitrary[A] = implicitly[Arbitrary[A]]

  implicit def ArbitraryVar[T, U](implicit b: Arbitrary[T], f: Arbitrary[U]): Arbitrary[Var[T, U]] =
    Arbitrary(oneOf(arbitrary[T] map ((t: T) => B(t)), arbitrary[U] map ((u: U) => F(u))))

  trait Arbitrary1[F[_]] {
    def arbitrary1[A](implicit a: Arbitrary[A]): Arbitrary[F[A]]
  }

  implicit def ArbitraryScope[B,F[_],A](implicit AB: Arbitrary[B], AF: Arbitrary1[F], AA: Arbitrary[A]): Arbitrary[Scope[B,F,A]] =
    Arbitrary(AF.arbitrary1(ArbitraryVar(AB,AF.arbitrary1(AA))).arbitrary.map(Scope(_)))

  implicit val Arbitrary1List: Arbitrary1[List] = new Arbitrary1[List] {
    def arbitrary1[A](implicit a: Arbitrary[A]): Arbitrary[List[A]] = implicitly[Arbitrary[List[A]]]
  }

  implicit val Arbitrary1Maybe: Arbitrary1[Option] = new Arbitrary1[Option] {
    def arbitrary1[A](implicit a: Arbitrary[A]): Arbitrary[Option[A]] = implicitly[Arbitrary[Option[A]]]
  }
}
