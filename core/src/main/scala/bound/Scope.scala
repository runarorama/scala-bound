package bound

import scalaz._
import Scalaz._

/**
 * A value of type `Scope[B,F,A]` is an `F` expression with bound variables in `B`
 * and free variables in `A`.
 */
abstract class Scope[+B,F[+_],+A] {
  def unscope: F[Var[B,F[A]]]

  def map[C](f: A => C)(implicit M: Functor[F]): Scope[B,F,C] =
    Scope(unscope map (_ map (_ map f)))

  def flatMap[C,D >: B](f: A => Scope[D,F,C])(implicit M: Monad[F]): Scope[D,F,C] =
    Scope(unscope flatMap {
      case B(b) => M.pure(B(b))
      case F(a) => a flatMap (v => f(v).unscope)
    })

  def traverse[M[+_],C](f: A => M[C])(implicit M: Applicative[M], T: Traverse[F]): M[Scope[B,F,C]] =
    unscope traverse (_ traverse (_ traverse f)) map (Scope(_))

  def foldMap[M](f: A => M)(implicit F: Foldable[F], M: Monoid[M]): M =
    unscope foldMap (_ foldMap (_ foldMap f))

  def foldRight[M](m: M)(f: (A, => M) => M)(implicit F: Foldable[F]) =
    unscope.foldRight(m)((v, b) => v.foldRight(b)((fa, bz) => fa.foldRight(bz)(f)))

  /** Bind a variable in a scope. */
  def bind[C](f: A => F[C])(implicit M: Monad[F]): Scope[B,F,C] =
    Scope[B,F,C](unscope map (_.map(_ flatMap f)))

  /**
   * Enter a scope, instantiating all bound variables.
   *
   * {{{
   * scala> abstract1('a', "abracadabra".toList).instantiate(_ => "foo".toList).mkString
   * res0: String = foobrfoocfoodfoobrfoo
   * }}}
   */
  def instantiate[C >: B, D >: A](k: C => F[D])(implicit M: Monad[F]): F[D] =
    unscope flatMap {
      case B(b) => k(b)
      case F(a) => a
    }

  /** Enter a scope that binds one variable, instantiating it. */
  def instantiate1[D >: A](e: F[D])(implicit M: Monad[F]): F[D] =
    instantiate[B,D](_ => e)

  /**
   * Quotients out the possible placements of `F` in this `Scope` by distributing them all
   * to the leaves. This yields a more traditional de Bruijn indexing scheme for bound
   * variables.
   */
  def toDeBruijn(implicit M: Monad[F]): F[Var[B, A]] =
    unscope flatMap {
      case F(e) => e.map(F(_))
      case B(b) => M.pure(B(b))
    }

  import Show._
  override def toString = Scope.scopeShow[Any,Any,Any](showA, showA, showA, showA).shows(this.asInstanceOf[Scope[Any,Any,Any]])
}

object Scope {
  def apply[B,F[+_],A](f: F[Var[B,F[A]]]): Scope[B,F,A] = new Scope[B,F,A] {
    def unscope = f
  }

  implicit def scopeShow[B,F[+_],A](implicit B: Show[B],
                                             F: Show[F[Var[B,F[A]]]],
                                             A: Show[A],
                                             FA: Show[F[A]]): Show[Scope[B,F,A]] =
    new Show[Scope[B,F,A]] {
      override def shows(s: Scope[B,F,A]) = "Scope(%s)".format(s.unscope.shows)
    }

  implicit def scopeEqual[B,F[+_],A](implicit EB: Equal[B], M: Monad[F], EF: Equal1[F], EA: Equal[A]): Equal[Scope[B,F,A]] =
    new Equal[Scope[B,F,A]] {
      def equal(a: Scope[B,F,A], b: Scope[B,F,A]): Boolean = scopeEqual1[B,F].equal(a, b)
    }

  implicit def scopeEqual1[B,F[+_]](implicit EB: Equal[B], M: Monad[F], E1F: Equal1[F]): Equal1[({type λ[α] = Scope[B,F,α]})#λ] =
    new Equal1[({type λ[α] = Scope[B,F,α]})#λ] {
      def equal[A](a: Scope[B,F,A], b: Scope[B,F,A])(implicit EA: Equal[A]): Boolean =
        E1F.equal(fromScope(a), fromScope(b))
    }

  implicit def scopeMonad[F[+_]:Monad,D]: Monad[({type λ[α] = Scope[D,F,α]})#λ] =
    new Monad[({type λ[α] = Scope[D,F,α]})#λ] {
      val M = Monad[F]
      override def map[A,B](a: Scope[D,F,A])(f: A => B) = a map f
      def point[A](a: => A) = Scope(M.pure(F(M.pure(a))))
      def bind[A,B](e: Scope[D,F,A])(f: A => Scope[D,F,B]) = e flatMap f
    }

  implicit def scopeTraverse[F[+_]:Traverse,D]: Traverse[({type λ[α] = Scope[D,F,α]})#λ] =
    new Traverse[({type λ[α] = Scope[D,F,α]})#λ] {
      def traverseImpl[M[+_]:Applicative,A,B](a: Scope[D,F,A])(f: A => M[B]) = a traverse f
    }

  implicit def scopeFoldable[F[+_]:Foldable,D]: Foldable[({type λ[α] = Scope[D,F,α]})#λ] =
    new Foldable[({type λ[α] = Scope[D,F,α]})#λ] {
      val T = Foldable[F]
      override def foldMap[A,M:Monoid](a: Scope[D,F,A])(f: A => M) = a foldMap f
      def foldRight[A,B](a: Scope[D,F,A], z: => B)(f: (A, => B) => B) = a.foldRight(z)(f)
    }

  implicit def scopeFunctor[F[+_]:Functor,D]: Functor[({type λ[α] = Scope[D,F,α]})#λ] =
    new Functor[({type λ[α] = Scope[D,F,α]})#λ] {
      val M = Functor[F]
      override def map[A,B](a: Scope[D,F,A])(f: A => B) = a map f
    }

  implicit def scopeMonadTrans[B]: MonadTrans[({type λ[φ[+_],α] = Scope[B,φ,α]})#λ] =
    new MonadTrans[({type λ[φ[+_],α] = Scope[B,φ,α]})#λ] {
      def liftM[M[+_]:Monad,A](m: M[A]) = Scope[B,M,A](Monad[M].point(F(m)))
      def apply[M[+_]:Monad]: Monad[({type λ[α] = Scope[B,M,α]})#λ] = scopeMonad[M,B]
    }

  implicit def scopeBound[B]: Bound[({type λ[φ[+_],α] = Scope[B,φ,α]})#λ] =
    new Bound[({type λ[φ[+_],α] = Scope[B,φ,α]})#λ] {
      def bind[F[+_]:Monad,A,C](m: Scope[B,F,A])(f: A => F[C]): Scope[B,F,C] = m bind f
    }
}

