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

We can then construct and evaluate lambda terms in the console. The `const` function contains two nested scopes. Note that the variable names are erased. What remains are two nested scopes.
The `-\/` indicates a variable bound in the outer scope. The `-\/` indicates a free variable.

    scala> val const = lam("x", lam("y", V("x")))
    const: Exp[String] = Lam(Scope(Lam(Scope(V(\/-(V(-\/(()))))))))
    
The part that is the outer scope is `Lam(Scope(...V(-\/(()))...))`. The inner scope is `...Lam(Scope(V(\/-(...))))...`. So what used to be `x` is bound in the outer scope and free in the inner scope.
    
Applying this term to the named variable `V("a")` we are left with a lambda term whose free variable has been instantiated to `V("a")`.

    scala> val constA = whnf(const(V("a")))
    p: Exp[String] = Lam(Scope(V(\/-(V(a)))))

Applying that to a second term gives us the the term we bound to the variable:

    scala> val a = whnf(constA(V("b")))
    a: Exp[String] = V(a)
