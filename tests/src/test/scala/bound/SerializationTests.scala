package bound

import org.scalacheck.{Arbitrary, Prop}
import org.scalacheck.Prop._
import bound.scalacheck.BoundArbitraryInstances._
import BoundSerialization._
import f0.{Reader, Writer}
import f0.Readers._
import f0.Writers._

object SerializationTests extends BoundProperties("Scope Tests"){

  test("put/get var == var")(clone(varW(intW, stringW), varR(intR, stringR)))

  def clone[A:Arbitrary,F](w: Writer[A,F], r: Reader[A,F]): Prop =
    forAll((a: A) => {
      (r(w.toByteArray(a)) ?= a)
    })
}
