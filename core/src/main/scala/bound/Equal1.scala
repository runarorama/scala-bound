package bound

import scalaz.{Equal, Scalaz}
import Scalaz._

trait Equal1[F[_]]{
  def equal[A](a1: F[A], a2: F[A])(implicit eq0: Equal[A]): Boolean
}

object Equal1 {
  implicit val Equal1List: Equal1[List] = new Equal1[List] {
    def equal[A](a1: List[A], a2: List[A])(implicit a: Equal[A]): Boolean = implicitly[Equal[List[A]]].equal(a1, a2)
  }

  implicit val Equal1Option: Equal1[Option] = new Equal1[Option] {
    def equal[A](a1: Option[A], a2: Option[A])(implicit a: Equal[A]): Boolean = implicitly[Equal[Option[A]]].equal(a1, a2)
  }
}