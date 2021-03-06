package scalaz

////
/**
 * Monad, an [[scalaz.Applicative]] that also supports [[scalaz.Bind]],
 * circumscribed by the monad laws.
 *
 * @see [[scalaz.Monad.MonadLaw]]
 */
////
trait Monad[F[_]] extends Applicative[F] with Bind[F] { self =>
  ////

  override def map[A,B](fa: F[A])(f: A => B) = bind(fa)(a => point(f(a)))

  /**
   * Execute an action repeatedly as long as the given `Boolean` expression
   * returns `true`. The condition is evalated before the loop body.
   * Collects the results into an arbitrary `MonadPlus` value, such as a `List`.
   */
  def whileM[G[_], A](p: F[Boolean], body: => F[A])(implicit G: MonadPlus[G]): F[G[A]] = {
    lazy val f = body
    ifM(p, bind(f)(x => map(whileM(p, f))(xs => G.plus(G.point(x), xs))), point(G.empty))
  }

  /**
   * Execute an action repeatedly as long as the given `Boolean` expression
   * returns `true`. The condition is evaluated before the loop body.
   * Discards results.
   */
  def whileM_[A](p: F[Boolean], body: => F[A]): F[Unit] = {
    lazy val f = body
    ifM(p, bind(f)(_ => whileM_(p, f)), point(()))
  }

  /**
   * Execute an action repeatedly until the `Boolean` condition returns `true`.
   * The condition is evaluated after the loop body. Collects results into an
   * arbitrary `MonadPlus` value, such as a `List`.
   */
  def untilM[G[_], A](f: F[A], cond: => F[Boolean])(implicit G: MonadPlus[G]): F[G[A]] = {
    lazy val p = cond
    bind(f)(x => map(whileM(map(p)(!_), f))(xs => G.plus(G.point(x), xs)))
  }

  /**
   * Execute an action repeatedly until the `Boolean` condition returns `true`.
   * The condition is evaluated after the loop body. Discards results.
   */
  def untilM_[A](f: F[A], cond: => F[Boolean]): F[Unit] = {
    lazy val p = cond
    bind(f)(_ => whileM_(map(p)(!_), f))
  }

  /**
   * Execute an action repeatedly until its result fails to satisfy the given predicate
   * and return that result, discarding all others.
   */
  def iterateWhile[A](f: F[A])(p: A => Boolean): F[A] =
    bind(f)(y => if (p(y)) iterateWhile(f)(p) else point(y))

  /**
   * Execute an action repeatedly until its result satisfies the given predicate
   * and return that result, discarding all others.
   */
  def iterateUntil[A](f: F[A])(p: A => Boolean): F[A] =
    bind(f)(y => if (p(y)) point(y) else iterateUntil(f)(p))

  trait MonadLaw extends ApplicativeLaw {
    /** Lifted `point` is a no-op. */
    def rightIdentity[A](a: F[A])(implicit FA: Equal[F[A]]): Boolean = FA.equal(bind(a)(point(_: A)), a)
    /** Lifted `f` applied to pure `a` is just `f(a)`. */
    def leftIdentity[A, B](a: A, f: A => F[B])(implicit FB: Equal[F[B]]): Boolean = FB.equal(bind(point(a))(f), f(a))
    /**
     * As with semigroups, monadic effects only change when their
     * order is changed, not when the order in which they're
     * combined changes.
     */
    def associativeBind[A, B, C](fa: F[A], f: A => F[B], g: B => F[C])(implicit FC: Equal[F[C]]): Boolean =
      FC.equal(bind(bind(fa)(f))(g), bind(fa)((a: A) => bind(f(a))(g)))
    /** `ap` is consistent with `bind`. */
    def apLikeDerived[A, B](fa: F[A], f: F[A => B])(implicit FB: Equal[F[B]]): Boolean =
      FB.equal(ap(fa)(f), bind(f)(f => map(fa)(f)))
  }
  def monadLaw = new MonadLaw {}
  ////
  val monadSyntax = new scalaz.syntax.MonadSyntax[F] { def F = Monad.this }
}

object Monad {
  @inline def apply[F[_]](implicit F: Monad[F]): Monad[F] = F

  ////

  ////
}
