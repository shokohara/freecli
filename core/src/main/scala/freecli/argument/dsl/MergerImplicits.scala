package freecli
package argument
package dsl

import cats.syntax.all._
import shapeless._
import shapeless.ops.hlist.LeftFolder

import core.api.{CanProduce, Merger, CanMerge}
import core.poly.toHList

trait MergerImplicits {
  implicit def arguments2Merger[B, O](
    b: B)(
    implicit ev: CanProduce.Aux[B, ArgumentDsl[O]]):
    Merger[ArgumentDsl[O]] = {

    Merger[ArgumentDsl[O]](ev(b))
  }

  implicit def canMergeArguments[A1, A2, A <: HList](
    implicit folder: LeftFolder.Aux[A1 :: A2 :: HNil, HNil, toHList.type, A]):
    CanMerge.Aux[ArgumentDsl[A1], ArgumentDsl[A2], ArgumentDsl[A]] = {

    new CanMerge[ArgumentDsl[A1], ArgumentDsl[A2]] {
      type Out = ArgumentDsl[A]
      def apply(f1: ArgumentDsl[A1], f2: ArgumentDsl[A2]): Out = {
        (f1, f2).mapN((ff1, ff2) => folder(ff1 :: ff2 :: HNil, HNil))
      }
    }
  }
}
