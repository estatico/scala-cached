package io.estatico.cached

import org.scalatest.{FlatSpec, Matchers}

class CachedMacrosTest extends FlatSpec with Matchers {

  import CachedMacrosTest._
  import counters._

  behavior of "@cached"

  it should "cache Constants instances" in {

  }

  it should "cache Either.left and Either.right" in {
    Either.left[Int]("foo") shouldBe Left("foo")
    leftBuilderNewCount shouldBe 1
    Either.left[String](17) shouldBe Left(17)
    leftBuilderNewCount shouldBe 1

    Either.right[Int]("bar") shouldBe Right("bar")
    rightBuilderNewCount shouldBe 1
    Either.right[String](45) shouldBe Right(45)
    rightBuilderNewCount shouldBe 1
  }
}

object CachedMacrosTest {

  import counters._

  trait Constants[F[_]] {
    def UNIT: F[Unit]
    def TRUE: F[Boolean]
    def FALSE: F[Boolean]
    def ZERO: F[Int]
    def ONE: F[Int]
    def EMPTY_STRING: F[String]
  }

  object Constants {

    def apply[F[_] : Constants]: Constants[F] = implicitly

    def instance[F[_]](fromPure: FromPure[F]): Constants[F] = fromPure

    def UNIT[F[_]](implicit ev: Constants[F]): F[Unit] = ev.UNIT
    def TRUE[F[_]](implicit ev: Constants[F]): F[Boolean] = ev.TRUE
    def FALSE[F[_]](implicit ev: Constants[F]): F[Boolean] = ev.FALSE
    def ZERO[F[_]](implicit ev: Constants[F]): F[Int] = ev.ZERO
    def ONE[F[_]](implicit ev: Constants[F]): F[Int] = ev.ONE
    def EMPTY_STRING[F[_]](implicit ev: Constants[F]): F[String] = ev.EMPTY_STRING

    trait FromPure[F[_]] extends Constants[F] {
      protected def apply[A](a: A): F[A]
      override final val UNIT: F[Unit] = apply(())
      override final val TRUE: F[Boolean] = apply(true)
      override final val FALSE: F[Boolean] = apply(false)
      override final val ZERO: F[Int] = apply(0)
      override final val ONE: F[Int] = apply(1)
      override final val EMPTY_STRING: F[String] = apply("")
    }
  }

  sealed trait Xor[+L, +R]
  final case class XorL[+L](l: L) extends Xor[L, Nothing]
  final case class XorR[+R](r: R) extends Xor[Nothing, R]

  def uncachedConstantsXor[B]: Constants[Xor[B, ?]] = new Constants.FromPure[Xor[B, ?]] {
    uncachedConstantsXorNewCount += 1
    override protected def apply[A](a: A): Xor[B, A] = XorR(a)
  }

  @cached implicit def constantsXor[B]: Constants[Xor[B, ?]] = new Constants.FromPure[Xor[B, ?]] {
    constantsXorNewCount += 1
    override protected def apply[A](a: A): Xor[B, A] = XorR(a)
  }

  implicit final class EitherObjOps(val repr: Either.type) extends AnyVal {
    def left[R]: LeftBuilder[R] = LeftBuilder[R]
    def right[L]: RightBuilder[L] = RightBuilder[L]
  }

  final class LeftBuilder[R] {
    def apply[L](x: L): Either[L, R] = Left(x)
  }


  @cached def LeftBuilder[R]: LeftBuilder[R] = {
    leftBuilderNewCount += 1
    new LeftBuilder[R]
  }

  final class RightBuilder[L] {
    def apply[R](x: R): Either[L, R] = Right(x)
  }

  @cached def RightBuilder[L]: RightBuilder[L] = {
    rightBuilderNewCount += 1
    new RightBuilder[L]
  }

  object counters {
    var uncachedConstantsXorNewCount = 0
    var constantsXorNewCount = 0
    var leftBuilderNewCount = 0
    var rightBuilderNewCount = 0
  }
}
