# @cached

Macro annotation for caching polymorphic values.

## Usage

```scala
scala> import io.estatico.cached.cached
import io.estatico.cached.cached

scala> :paste
// Entering paste mode (ctrl-D to finish)

@cached implicit def eitherMonad[A]: Monad[Either[A, ?]] = new Monad[Either[A, ?]] {

  def pure[B](b: B): Either[A, B] = Right(b)

  def flatMap[B, C](fa: Either[A, B])(f: B => Either[A, C]): Either[A, C] =
    fa.right.flatMap(f)

  override def map[B, C](fa: Either[A, B])(f: B => C): Either[A, C] =
    fa.right.map(f)

  @tailrec
  def tailRecM[B, C](b: B)(f: B => Either[A, Either[B, C]]): Either[A, C] =
    f(b) match {
      case left @ Left(_) =>
        left.asInstanceOf[Either[A, C]]
      case Right(e) =>
        e match {
          case Left(b1) => tailRecM(b1)(f)
          case right @ Right(_) => right.asInstanceOf[Either[A, C]]
        }
    }
}

// Exiting paste mode, now interpreting.

scala> eitherMonad[String]
res1: cats.Monad[[β$0$]scala.util.Either[String,β$0$]] = $anon$1@3a9f678d

scala> eitherMonad[Int]
res2: cats.Monad[[β$0$]scala.util.Either[Int,β$0$]] = $anon$1@3a9f678d

scala> Monad[Either[String, ?]]
res4: cats.Monad[[β$0$]scala.util.Either[String,β$0$]] = $anon$1@3a9f678d

scala> Monad[Either[Int, ?]]
res5: cats.Monad[[β$0$]scala.util.Either[Int,β$0$]] = $anon$1@3a9f678d
```

As you can see above, the `Monad` instance returned each time is a reference
to the same object, so no new allocations occur.

Compare this with a traditional, non-cached instance which will create a new
instance each time it is summoned -

```scala
scala> import cats._
import cats._

scala> import cats.implicits._
import cats.implicits._

scala> Monad[Either[String, ?]]
res1: cats.Monad[[β$0$]scala.util.Either[String,β$0$]] =
  cats.instances.EitherInstances$$anon$1@299f43d1

scala> Monad[Either[String, ?]]
res2: cats.Monad[[β$0$]scala.util.Either[String,β$0$]] =
  cats.instances.EitherInstances$$anon$1@7f9fd44a
```

## How it works

The previously defined `Monad[Either[A, ?]]` instance -

```scala
@cached implicit def eitherMonad[A]: Monad[Either[A, ?]] = ...
```

will be expanded to something like the following at compile time -

```scala
implicit def eitherMonad[A]: Monad[Either[A, ?]] = __cached__eitherMonad.asInstanceOf[Monad[Either[A, ?]]]
private val __cached__eitherMonad: Monad[Either[Nothing, ?]] = {
  type A = Nothing
  new Monad[Either[A, ?]] {

    def pure[B](b: B): Either[A, B] = Right(b)

    def flatMap[B, C](fa: Either[A, B])(f: B => Either[A, C]): Either[A, C] =
      fa.right.flatMap(f)

    override def map[B, C](fa: Either[A, B])(f: B => C): Either[A, C] =
      fa.right.map(f)

    @tailrec
    def tailRecM[B, C](b: B)(f: B => Either[A, Either[B, C]]): Either[A, C] =
      f(b) match {
        case left @ Left(_) =>
          left.asInstanceOf[Either[A, C]]
        case Right(e) =>
          e match {
            case Left(b1) => tailRecM(b1)(f)
            case right @ Right(_) => right.asInstanceOf[Either[A, C]]
          }
      }
  }
}
```
