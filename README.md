Bound
=====

A library for developing languages with scoped binders (like forall or lambda).

This is a Scala port of [Edward Kmett's Bound library for Haskell](https://github.com/ekmett/bound).

Getting started
---------------

###Add this your build.sbt file:###

```
resolvers += "Runar's Bintray Repo" at "http://dl.bintray.com/runarorama/maven/"

libraryDependencies += "bound" %% "bound-core" % "1.1"
```

Binary packages available at https://bintray.com/runarorama/maven/bound

This library provides convenient combinators for working with "locally-nameless" terms. These can be useful when writing a type checker, evaluator, parser, or pretty-printer for terms that contain binders like forall or lambda. They ease the task of avoiding variable capture and testing for alpha-equivalence.

Example
-------

```scala
import scalaz._
import scalaz.std.string._
import bound._

// An untyped lambda calculus
sealed trait Exp[+A] {
  def apply[B >: A](arg: Exp[B]): Exp[B] = App(this, arg)
}
case class V[+A](a: A) extends Exp[A]
case class App[+A](fn: Exp[A], arg: Exp[A]) extends Exp[A]
case class Lam[+A](s: Scope[Unit, Exp, A]) extends Exp[A]

// A monad for our calculus
implicit val expMonad: Monad[Exp] = new Monad[Exp] {
  def point[A](a: => A) = V(a)
  def bind[A,B](m: Exp[A])(f: A => Exp[B]): Exp[B] = m match {
    case V(a)   => f(a)
    case App(x,y) => App(bind(x)(f), bind(y)(f))
    case Lam(e)   => Lam(e >>>= f)
  }
}

// Lambda abstraction smart constructor.
def lam[A:Equal](v: A, b: Exp[A]): Exp[A] =
  Lam(abstract1(v, b))

// Weak-head normal form evaluator.
def whnf[A](e: Exp[A]): Exp[A] = e match {
  case App(f,a) => whnf(f) match {
    case Lam(b) => whnf(instantiate1(a,b))
    case g      => App(g,a)
  }
  case e => e
}
```

We can then construct and evaluate lambda terms in the console. The `const` function contains two nested scopes. The term `"x"` is free in the inner scope (indicated by `\/-`) and bound in the outer scope (indicated by `-\/`). Note that the variable names are erased.

    scala> val const = lam("x", lam("y", V("x")))
    const: Exp[String] = Lam(Scope(Lam(Scope(V(\/-(V(-\/(()))))))))
    
Applying this term to a term and evaluating it to weak-head normal form, we are left with a lambda term with a single bound variable.

    scala> val constA = whnf(const(V("a")))
    p: Exp[String] = Lam(Scope(V(\/-(V(a)))))

Applying that to a second term gives us the first term:

    scala> val a = whnf(constA(V("b")))
    a: Exp[String] = V(a)
