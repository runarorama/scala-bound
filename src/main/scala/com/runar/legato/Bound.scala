package bound

import language.higherKinds

import scalaz._

/**
 * A class for performing substitution into things that are not necessarily
 * monad transformers.
 */
trait Bound[T[_[_],_]] {
  /** Perform substitution. */
  def bind[F[_]:Monad,A,C](m: T[F, A])(f: A => F[C]): T[F, C]
}

object Bound {
  /**
   * If `T` is a monad transformer then this is the default implementation:
   * `bind(m)(f) = m.flatMap(x => T.liftM(f(x)))`
   */
  implicit def defaultBound[T[_[_],_]:MonadTrans]: Bound[T] = new Bound[T] {
    def bind[F[_]:Monad,A,C](m: T[F, A])(f: A => F[C]) = {
      val T = MonadTrans[T]
      T.apply[F].bind(m)(a => T.liftM(f(a)))
    }
  }
}

trait BoundOps[T[_[_],_], F[_], A] {
  def self: T[F, A]
  implicit def T: Bound[T]

  final def >>>=[C](f: A => F[C])(implicit F: Monad[F]) =
    T.bind(self)(f)

  final def =<<<:[C](f: A => F[C])(implicit F: Monad[F]) =
    >>>=[C](f)
}

