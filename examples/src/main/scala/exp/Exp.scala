package exp

import bound._
import bound.Scope._
import scalaz._
import Scalaz._

/**
data Exp a
  = V a
  | Exp a :@ Exp a
  | Lam (Scope () Exp a)
  | Let [Scope Int Exp a] (Scope Int Exp a)
  deriving (Eq,Ord,Show,Read)

  -- | Reduce a term to weak head normal form
  whnf :: Exp a -> Exp a
  whnf e@V{}   = e
  whnf e@Lam{} = e
  whnf (f :@ a) = case whnf f of
    Lam b -> whnf (instantiate1 a b)
    f'    -> f' :@ a
  whnf (Let bs b) = whnf (inst b)
    where es = map inst bs
          inst = instantiate (es !!)
  */

//https://github.com/ekmett/bound/blob/master/examples/Simple.hs
object Exp {

  sealed trait Exp[+A] {
    def *[B >: A](e:Exp[B]) = App(this, e)
  }
    case class V[+A](a: A) extends Exp[A]
    case class Lam[+A](s: Scope[Unit, Exp, A]) extends Exp[A]
    case class App[+A](f: Exp[A], x: Exp[A]) extends Exp[A]
    case class Let[+A](bindings: List[Scope[Int, Exp, A]], body: Scope[Int, Exp, A]) extends Exp[A]

  implicit def expMonad: Monad[Exp] = new Monad[Exp]{
    def point[A](a: => A) = V(a)
    def bind[A,B](e: Exp[A])(f: A => Exp[B]): Exp[B] = e match {
      case V(a)          => f(a)
      case Lam(s)        => Lam(s >>>= f)
      case App(fun, arg) => App(fun >>= f, arg >>= f)
      case Let(bs, b)    => Let(bs map (_ >>>= f), b >>>= f)
    }
  }

  implicit def expTraversable: Traverse[Exp] = new Traverse[Exp]{
    def traverseImpl[F[+_], A, B](exp : Exp[A])(f : A => F[B])(implicit A: Applicative[F]) : F[Exp[B]] = exp match {
      case V(a)       => f(a).map(V(_))
      case App(x, y)  => A.apply2(traverse(x)(f), traverse(y)(f))(App(_, _))
      case Lam(e)     => e.traverse(f).map(Lam(_))
      case Let(bs, b) => A.apply2(bs.traverse(s => s.traverse(f)), b.traverse(f))(Let(_, _))
    }
  }

  def instantiateR[B,F[+_],A](f: B => F[A])(s: Scope[B,F,A])(implicit M: Monad[F]): F[A] =
    instantiate(s)(f)

  def abstractR[B,F[+_],A](f : A => Option[B])(w : F[A])(implicit M: scalaz.Monad[F]) = abstrakt(w)(f)

  def nf[A](e:Exp[A]): Exp[A] = e match {
    case V(_)       => e
    case Lam(b)     => Lam(toScope(nf(fromScope(b))))
    case App(f, a)  => whnf(f) match {
      case Lam(b)   => nf(instantiate1(a, b))
      case f1       => App(nf(f), nf(a))
    }
    case Let(bs, b) =>
      def inst = instantiateR((i: Int) => es(i)) _ // Scope[Int,Exp,A] => Exp[A]
      lazy val es: Stream[Exp[A]] = bs.toStream.map(inst)
      nf(inst(b))
  }

  def whnf[A](e: Exp[A]): Exp[A] = e match {
    case V(_)       => e
    case Lam(_)    => e
    case App(f, a) => whnf(f) match {
      case Lam(b)  => instantiate1(a, b)
      case _        => App(f, a)
    }
    case Let(bs, b)  =>
      def inst = instantiateR((i: Int) => es(i)) _ // Scope[Int,Exp,A] => Exp[A]
      def es: Stream[Exp[A]] = {
        val res = bs.toStream.map(inst)
        println(res)
        res
      }
      whnf(inst(b))
  }

  //  A smart constructor for Lamb
  //  >>> lam "y" (lam "x" (V "x" :@ V "y"))
  //  Lamb (Scope (Lamb (Scope (V (B ()) :@ V (F (V (B ())))))))
  def lam[A,F[+_]](v: A, b: Exp[A])(implicit m: Monad[F], e: Equal[A]) = Lam(abstract1(v,b))

  def let_[A](es: List[(A, Exp[A])], e:Exp[A]): Exp[A] = es match {
    case Nil => e
    case _ =>
      def abstr(e:Exp[A]) = abstractR((a:A) => {
        val i = es.map(_._1).indexOf(a)
        if(i>=0) Some(i) else None
      })(e)
      Let(es.map(t => abstr(t._2)), abstr(e))
  }

  implicit class PimpedExp(e: Exp[String]) {
      def !:(s:String) = lam[String, Exp](s, e)
  }

  def closed[F[+_], A, B](fa:F[A])(implicit T: Traverse[F]): Option[F[B]] =
    fa.traverse(Function.const(None))

  //  true :: Exp String
  //  true = lam "F" $ lam "T" $ V "T"
  val True: Exp[String] = lam[String, Exp]("F", lam("T", V("T")))

  val cooked = closed[Exp, String, String](let_(List(
    ("False",  "f" !: "t" !: V("f"))
  , ("True",   "f" !: "t" !: V("t"))
  , ("if",     "b" !: "t" !: "f" !: V("b") * V("f") * V("t"))
  , ("Zero",   "z" !: "s" !: V("z"))
  , ("Succ",   "n" !: "z" !: "s" !: V("s") * V("n"))
  , ("one",    V("Succ") * V("Zero"))
  , ("two",    V("Succ") * V("one"))
  , ("three",  V("Succ") * V("two"))
  , ("isZero", "n" !: V("n") * V("True") * ("m" !: V("False")))
  , ("const",  "x" !: "y" !: V("x"))
  , ("Pair",   "a" !: "b" !: "p" !: V("p") * V("a") * V("b"))
  , ("fst",    "ab" !: V("ab") * ("a" !: "b" !: V("a")))
  , ("snd",    "ab" !: V("ab") * ("a" !: "b" !: V("b")))
  , ("add",    "x" !: "y" !: V("x") * V("y") * ("n" !: V("Succ") * (V("add") * V("n") * V("y"))))
  , ("mul",    "x" !: "y" !: V("x") * V("Zero") * ("n" !: V("add") * V("y") * (V("mul") * V("n") * V("y"))))
  , ("fac",    "x" !: V("x") * V("one") * ("n" !: V("mul") * V("x") * (V("fac") * V("n"))))
  , ("eqnat",  "x" !: "y" !: V("x") * (V("y") * V("True") * (V("const") * V("False"))) * ("x1" !: V("y") * V("False") * ("y1" !: V("eqnat") * V("x1") * V("y1"))))
  , ("sumto",  "x" !: V("x") * V("Zero") * ("n" !: V("add") * V("x") * (V("sumto") * V("n"))))
  , ("n5",     V("add") * V("two") * V("three"))
  , ("n6",     V("add") * V("three") * V("three"))
  , ("n17",    V("add") * V("n6") * (V("add") * V("n6") * V("n5")))
  , ("n37",    V("Succ") * (V("mul") * V("n6") * V("n6")))
  , ("n703",   V("sumto") * V("n37"))
  , ("n720",   V("fac") * V("n6"))
  ), (V("eqnat") * V("n720") * (V("add") * V("n703") * V("n17"))))).get

}


//  val x: Scope[Unit, Exp, Var[Unit, Exp[Nothing]]] = Scope(App(V (B ()), V (F (V (B ())))))
//  val y: Scope[Unit, Exp, Nothing] = Scope(V (B ()))
//  val z = Lam(Scope(Lam(y)))
