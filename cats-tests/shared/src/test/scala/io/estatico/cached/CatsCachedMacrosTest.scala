package io.estatico.cached

import cats.Monad
import cats.syntax.flatMap._
import cats.syntax.functor._
import org.scalatest.{FlatSpec, Matchers}
import scala.annotation.tailrec

class CatsCachedMacrosTest extends FlatSpec with Matchers {

  import CatsCachedMacrosTest._
  import counters._

  "Monad[Either[A, ?]]" should "be cached" in {

    Monad[Either[String, ?]] shouldBe eitherMonad[Nothing]

    eitherMonad[String].flatMap(Right(1))(x =>
      eitherMonad[String].map(Left("foo"))(y => x + y)
    ) shouldBe Left("foo")
    eitherMonadNewCount shouldBe 1

    Monad[Either[String, ?]].flatMap(Right(1))(x =>
      Monad[Either[String, ?]].map(Left("foo"))(y => x + y)
    ) shouldBe Left("foo")
    eitherMonadNewCount shouldBe 1

    (for {
      x <- Right(1): Either[String, Int]
      y <- Right(2): Either[String, Int]
    } yield x + y) shouldBe Right(3)
    eitherMonadNewCount shouldBe 1
  }
}

object CatsCachedMacrosTest {

  import counters._

  @cached implicit def eitherMonad[A]: Monad[Either[A, ?]] = new Monad[Either[A, ?]] {

    eitherMonadNewCount += 1

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

  object counters {
    var eitherMonadNewCount = 0
  }
}
