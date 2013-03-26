package bound

import language.higherKinds

import scalaz.{Name => _, _}
import scalaz.syntax.monad._
import scalaz.syntax.equal._

case class Name[N,B](name: N, value: B)

object Name {
  // Type-class Instances //

  implicit def nameShow[N:Show,B:Show]: Show[Name[N,B]] = new Show[Name[N,B]] {
    override def shows(n: Name[N,B]) = s"Name %s %s".format(n.name, n.value)
  }

  implicit def nameOrder[N, B: Order]: Order[Name[N,B]] = new Order[Name[N,B]] {
    def order(m: Name[N,B], n: Name[N,B]) = Order[B].order(m.value, n.value)
  }

  implicit def nameTraverse[N]: Traverse[({type λ[α]=Name[N,α]})#λ] =
    new Traverse[({type λ[α]=Name[N,α]})#λ] {
      def traverseImpl[F[_]:Applicative,A,B](n: Name[N,A])(f: A => F[B]): F[Name[N,B]] =
        Functor[F].map(f(n.value))(x => n.copy(value = x))
    }

  implicit val nameBifunctor: Bifunctor[Name] = new Bifunctor[Name] {
    def bimap[A,B,C,D](n: Name[A,B])(f: A => C, g: B => D) =
      Name(f(n.name), g(n.value))
  }

  implicit def nameComonad[N]: Comonad[({type λ[α]=Name[N,α]})#λ] =
    new Comonad[({type λ[α]=Name[N,α]})#λ] {
      def copoint[A](p: Name[N,A]) = p.value
      def cojoin[A](p: Name[N,A]) = Name(p.name, p)
      def map[A,B](p: Name[N,A])(f: A => B) = Name(p.name, f(p.value))
      def cobind[A,B](p: Name[N,A])(f: Name[N,A] => B) =
        Name(p.name, f(p))
    }

  implicit def bitraverse: Bifoldable[Name] = new Bifoldable[Name] {
    def bifoldMap[A,B,M:Monoid](n: Name[A,B])(f: A => M)(g: B => M) =
      Monoid[M].append(f(n.name), g(n.value))
    def bifoldRight[A,B,R](n: Name[A,B], z: => R)(f: (A, => R) => R)(g: (B, => R) => R) =
      f(n.name, g(n.value, z))
  }
}


