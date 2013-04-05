package bound

import org.scalacheck.{Arbitrary, Prop}
import org.scalacheck.Prop._
import bound.scalacheck.BoundArbitraryInstances._
import BoundSerialization._
import f0._
import f0.Readers._
import f0.Writers._
import f0.DynamicF
import scalaz.{Equal, Scalaz}
import Scalaz._
import Scope._

object SerializationTests extends BoundProperties("Scope Tests"){

  test("var == put/get var")(clone(varW(intW, stringW), varR(intR, stringR)))
  test("scope == put/get scope")(clone(scopeW(intW, listW1, stringW), scopeR(intR, listR1, stringR)))

  lazy val listW1 = new Writer1[List] {
    def apply[A](wa: Writer[A, DynamicF]): Writer[List[A], DynamicF] =
      Writers.repeatW(wa).cmap((as: List[A]) => as.toTraversable).erase
  }

  lazy val listR1 = new Reader1[List] {
    def apply[A](wa: Reader[A, DynamicF]): Reader[List[A], DynamicF] = Readers.listR(wa).erase
  }

  def clone[A,F](w: Writer[A,F], r: Reader[A,F])(implicit eql: Equal[A], arb: Arbitrary[A]): Prop =
    forAll((a: A) => {
      import eql.equalSyntax._
      (r(w.toByteArray(a)) === a)
    })
}
