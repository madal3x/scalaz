package scalaz

sealed trait LazyOptionT[F[+_], +A] {
  def run: F[LazyOption[A]]

  import LazyOption._
  import LazyOptionT._
  import LazyEitherT._
  import EitherT._

  def ?[X](some: => X, none: => X)(implicit F: Functor[F]): F[X] =
    F.map(run)(_.?(some, none))

  def isDefined(implicit F: Functor[F]): F[Boolean] =
    F.map(run)(_.isDefined)

  def isEmpty(implicit F: Functor[F]): F[Boolean] =
    F.map(run)(_.isEmpty)

  def getOrElse[AA >: A](default: => AA)(implicit F: Functor[F]): F[AA] =
    F.map(run)(_.getOrElse(default))

  def exists(f: (=> A) => Boolean)(implicit F: Functor[F]): F[Boolean] =
    F.map(run)(_.exists(f))

  def forall(f: (=> A) => Boolean)(implicit F: Functor[F]): F[Boolean] =
    F.map(run)(_.forall(f))

  def toOption(implicit F: Functor[F]): OptionT[F, A] =
    OptionT.optionT(F.map(run)(_.toOption))

  def toLazyRight[X](left: => X)(implicit F: Functor[F]): LazyEitherT[F, X, A] =
    lazyEitherT(F.map(run)(_.toLazyRight(left)))

  def toLazyLeft[X](right: => X)(implicit F: Functor[F]): LazyEitherT[F, A, X] =
    lazyEitherT(F.map(run)(_.toLazyLeft(right)))

  def toRight[X](left: => X)(implicit F: Functor[F]): EitherT[F, X, A] =
    eitherT(F.map(run)(_.fold(z => \/-(z), -\/(left))))

  def toLeft[X](right: => X)(implicit F: Functor[F]): EitherT[F, A, X] =
    eitherT(F.map(run)(_.fold(z => -\/(z), \/-(right))))

  def orElse[AA >: A](a: => LazyOption[AA])(implicit F: Functor[F]): LazyOptionT[F, AA] =
    lazyOptionT(F.map(LazyOptionT.this.run)(_.orElse(a)))

  def map[B](f: (=> A) => B)(implicit F: Functor[F]): LazyOptionT[F, B] =
    lazyOptionT(F.map(run)(_ map f))

  def foreach(f: (=> A) => Unit)(implicit e: Each[F]): Unit =
    e.each(run)(_ foreach f)

  def filter(f: (=> A) => Boolean)(implicit F: Functor[F]): LazyOptionT[F, A] =
    lazyOptionT(F.map(run)(_.filter(f)))

  def flatMap[B](f: (=> A) => LazyOptionT[F, B])(implicit M: Monad[F]): LazyOptionT[F, B] =
    lazyOptionT(M.bind(run)(_.fold(a => f(a).run, M.point(lazyNone[B]))))

  def mapLazyOption[B](f: LazyOption[A] => LazyOption[B])(implicit F: Functor[F]): LazyOptionT[F, B] =
    lazyOptionT(F.map(run)(f))

}

object LazyOptionT extends LazyOptionTFunctions with LazyOptionTInstances {
  def apply[F[+_], A](r: F[LazyOption[A]]): LazyOptionT[F, A] =
    lazyOptionT(r)
}

//
// Prioritized Implicits for type class instances
//

trait LazyOptionTInstances2 {
  implicit def lazyOptionTFunctor[F[+_]](implicit F0: Functor[F]): Functor[({type λ[α] = LazyOptionT[F, α]})#λ] = new LazyOptionTFunctor[F] {
    implicit def F: Functor[F] = F0
  }
}

trait LazyOptionTInstances1 extends LazyOptionTInstances2 {
  @deprecated("Monad[F] will be required, and this instance removed, in 7.1",
              "7.0.5")
  def lazyOptionTApply[F[+_]](implicit F0: Apply[F]): Apply[({type λ[α] = LazyOptionT[F, α]})#λ] = new LazyOptionTApply[F] {
    implicit def F: Apply[F] = F0
  }
}

trait LazyOptionTInstances0 extends LazyOptionTInstances1 {
  @deprecated("Monad[F] will be required, and this instance removed, in 7.1",
              "7.0.5")
  def lazyOptionTApplicative[F[+_]](implicit F0: Applicative[F]): Applicative[({type λ[α] = LazyOptionT[F, α]})#λ] = new LazyOptionTApplicative[F] {
    implicit def F: Applicative[F] = F0
  }
  implicit def lazyOptionEqual[F[+_], A](implicit FA: Equal[F[LazyOption[A]]]): Equal[LazyOptionT[F, A]] = Equal.equalBy((_: LazyOptionT[F, A]).run)
}

trait LazyOptionTInstances extends LazyOptionTInstances0 {
  implicit def lazyOptionTMonadTrans: Hoist[LazyOptionT] = new LazyOptionTHoist {}

  implicit def lazyOptionTMonad[F[+_]](implicit F0: Monad[F]): Monad[({type λ[α] = LazyOptionT[F, α]})#λ] = new LazyOptionTMonad[F] {
    implicit def F: Monad[F] = F0
  }
  implicit def lazyOptionOrder[F[+_], A](implicit FA: Order[F[LazyOption[A]]]): Order[LazyOptionT[F, A]] = Order.orderBy((_: LazyOptionT[F, A]).run)
}

trait LazyOptionTFunctions {
  def lazyOptionT[F[+_], A](r: F[LazyOption[A]]): LazyOptionT[F, A] = new LazyOptionT[F, A] {
    val run = r
  }

  import LazyOption._

  def lazySomeT[F[+_], A](a: => A)(implicit F: Applicative[F]): LazyOptionT[F, A] =
    lazyOptionT(F.point(lazySome(a)))

  def lazyNoneT[F[+_], A](implicit F: Applicative[F]): LazyOptionT[F, A] =
    lazyOptionT(F.point(lazyNone[A]))
}


//
// Implementation traits for type class instances
//

private[scalaz] trait LazyOptionTFunctor[F[+_]] extends Functor[({type λ[α] = LazyOptionT[F, α]})#λ] {
  implicit def F: Functor[F]

  override def map[A, B](fa: LazyOptionT[F, A])(f: A => B): LazyOptionT[F, B] = fa map (a => f(a))
}

private[scalaz] trait LazyOptionTApply[F[+_]] extends Apply[({type λ[α] = LazyOptionT[F, α]})#λ] with LazyOptionTFunctor[F] {
  implicit def F: Apply[F]

  override def ap[A, B](fa: => LazyOptionT[F, A])(f: => LazyOptionT[F, A => B]): LazyOptionT[F, B] =
    LazyOptionT(F.apply2(f.run, fa.run)({ case (ff, aa) => LazyOption.lazyOptionInstance.ap(aa)(ff) }))
}

private[scalaz] trait LazyOptionTApplicative[F[+_]] extends Applicative[({type λ[α] = LazyOptionT[F, α]})#λ] with LazyOptionTApply[F] {
  implicit def F: Applicative[F]
  def point[A](a: => A): LazyOptionT[F, A] = LazyOptionT[F, A](F.point(LazyOption.lazySome(a)))
}

private[scalaz] trait LazyOptionTMonad[F[+_]] extends Monad[({type λ[α] = LazyOptionT[F, α]})#λ] with LazyOptionTApplicative[F] {
  implicit def F: Monad[F]

  override def ap[A, B](fa: => LazyOptionT[F, A])(f: => LazyOptionT[F, A => B]): LazyOptionT[F, B] =
    LazyOptionT(F.bind(f.run)(_ fold (ff => F.map(fa.run)(_ map ((ff:A=>B)(_))),
                                      F.point(LazyOption.lazyNone))))

  def bind[A, B](fa: LazyOptionT[F, A])(f: A => LazyOptionT[F, B]): LazyOptionT[F, B] = fa flatMap (a => f(a))
}

private[scalaz] trait LazyOptionTHoist extends Hoist[LazyOptionT] {
  def liftM[G[+_], A](a: G[A])(implicit G: Monad[G]): LazyOptionT[G, A] =
    LazyOptionT[G, A](G.map[A, LazyOption[A]](a)((a: A) => LazyOption.lazySome(a)))

  def hoist[M[+_]: Monad, N[+_]](f: M ~> N) = new (({type f[x] = LazyOptionT[M, x]})#f ~> ({type f[x] = LazyOptionT[N, x]})#f) {
    def apply[A](fa: LazyOptionT[M, A]): LazyOptionT[N, A] = LazyOptionT(f.apply(fa.run))
  }

  implicit def apply[G[+_] : Monad]: Monad[({type λ[α] = LazyOptionT[G, α]})#λ] = LazyOptionT.lazyOptionTMonad[G]
}
