package bound

import scalaz._

/**
 * A `Var[B,F]` is a variable that may either be "bound" (`B`) or "free" (`F`).
 * It is really a type alias for `scalaz.\/`.
 */
object F {
  def apply[B,F](v: F): Var[B,F] = \/-(v).asInstanceOf[Var[B,F]]
  def unapply[B,F](v: Var[B,F]): Option[F] = v.toOption
}

object B {
  def apply[B,F](v: B): Var[B,F] = -\/(v).asInstanceOf[Var[B,F]]
  def unapply[B,F](v: Var[B,F]): Option[B] = v.fold(Some(_), _ => None)
}
