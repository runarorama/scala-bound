import language.higherKinds
import language.implicitConversions

import scalaz._
import Scalaz._

package object bound {
  type Var[+A,+B] = A \/ B

  implicit def varShow[A:Show,B:Show]: Show[Var[A,B]] = new Show[Var[A,B]] {
    override def shows(v: Var[A,B]) = v match {
      case B(a) => "B(%s)".format(a.shows)
      case F(a) => "F(%s)".format(a.shows)
    }
  }

  import Scope._

  /**
   * Replace the free variable `a` with `p` in `w`.
   *
   * {{{
   * scala> substitute("foo", List("qux", "corge"), List("bar", "foo", "baz"))
   * res0: List[String] = List(bar, qux, corge, baz)
   * }}}
   */
  def substitute[F[_]:Monad,A:Equal](a: A, p: F[A], w: F[A]): F[A] =
    w flatMap (b => if (a === b) p else Monad[F].pure(b))

  /**
   * Replace a free variable `a` with another free variable `b` in `w`.
   *
   * {{{
   * scala> substituteVar("Alice", "Donna", List("Alice", "Bob", "Charlie")
   * List[String] = List(Donna, Bob, Charlie)
   * res0: List[String] = List(bar, qux, corge, baz)
   * }}}
   */
  def substituteVar[F[_]:Functor,A:Equal](a: A, p: A, w: F[A]): F[A] =
    w map (b => if (a === b) p else b)

  /**
   * If a term has no free variables, you can freely change the type of free variables it's
   * parameterized on.
   */
  def closed[F[_]:Traverse,A,B](w: F[A]): Option[F[B]] =
    w traverse (_ => None)

  /** A closed term has no free variables. */
  def isClosed[F[_]:Foldable,A](w: F[A]): Boolean =
    w.all(_ => false)

  /**
   * Capture some free variables in an expression to yield a scope with bound variables in `b`.
   *
   * {{{
   * scala> abstrakt("aeiou".toList)(x => { val i = "bario".indexOf(x); option(i >= 0, i)).shows }
   * res0: String = Scope([B(1),F([e]),B(3),B(4),F([u])])
   * }}}
   */
  def abstrakt[F[+_]:Monad,A,B](w: F[A])(f: A => Option[B]): Scope[B,F,A] =
    Scope(w map (a => f(a) match {
      case Some(z) => B(z)
      case None    => F(Monad[F].pure(a))
    }))

  /**
   * Abstract over a single variable.
   *
   * {{{
   * scala> abstract1('a', "abracadabra".toList).shows
   * res0: String = Scope([B(()),F([b]),F([r]),B(()),F([c]),B(()),F([d]),B(()),F([b]),F([r]),B(())])
   * }}}
   */
  def abstract1[F[+_]:Monad,A:Equal,B](a: A, w: F[A]): Scope[Unit,F,A] =
    abstrakt(w)(b => if (a === b) Some(()) else None)

  /** Abstraction capturing named bound variables. */
  def abstractName[F[+_]:Monad,A,B](w: F[A])(f: A => Option[B]): Scope[Name[A,B],F,A] =
    Scope[Name[A,B],F,A](w map (a => f(a) match {
      case Some(b) => B(Name(a, b))
      case None    => F(Monad[F].pure(a))
    }))

  /** Abstract over a single variable */
  def abstract1Name[F[+_]:Monad,A:Equal,B](a: A, t: F[A])(f: A => Option[B]): Scope[Name[A,Unit],F,A] =
    abstractName(t)(b => if (a === b) Some(()) else None)

  /**
   * Enter a scope, instantiating all bound variables, but discarding (comonadic)
   * metadata, like its name.
   **/
  def instantiateName[F[+_]:Monad,N[_]:Comonad,A,B](e: Scope[N[B],F,A])(k: B => F[A]): F[A] =
    e.unscope flatMap {
      case B(b) => k(Comonad[N].copoint(b))
      case F(a) => a
    }

  /**
   * Enter a scope, instantiating all bound variables.
   *
   * {{{
   * scala> instantiate(abstract1('a', "abracadabra".toList))(_ => "foo".toList).mkString
   * res0: String = foobrfoocfoodfoobrfoo
   * }}}
   */
  def instantiate[F[+_]:Monad,A,B](e: Scope[B,F,A])(k: B => F[A]): F[A] =
    e.unscope flatMap {
      case B(b) => k(b)
      case F(a) => a
    }

  /** Enter a scope that binds one variable, instantiating it. */
  def instantiate1[F[+_]:Monad,N,A](e: F[A], s: Scope[N, F, A]): F[A] =
    instantiate(s)(_ => e)

  /**
   * Quotients out the possible placements of `F` in `Scope` by distributing them all
   * to the leaves. This yields a more traditional de Bruijn indexing scheme for bound
   * variables.
   */
  def fromScope[F[+_]:Monad,A,B](s: Scope[B, F, A]): F[Var[B, A]] =
    s.unscope flatMap {
      case F(e) => e.map(F(_))
      case B(b) => Monad[F].pure(B(b))
    }

  /** Convert from traditional de Bruijn to generalized de Bruijn indices. */
  def toScope[F[+_]:Monad,A,B](e: F[Var[B, A]]): Scope[B, F, A] =
    Scope[B, F, A](e map (_ map (Monad[F].pure(_))))

  implicit def toBoundOps[B,F[+_],A](s: Scope[B,F,A]) =
    new BoundOps[({type λ[φ[+_],α] = Scope[B,φ,α]})#λ,F,A] {
      def self = s
      implicit def T = scopeBound
    }
}
