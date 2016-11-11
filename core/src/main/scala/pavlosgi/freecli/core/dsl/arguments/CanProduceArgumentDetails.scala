package pavlosgi.freecli.core.dsl.arguments

import shapeless._
import shapeless.ops.hlist.{Diff, Intersection, LeftFolder}

import pavlosgi.freecli.core.api.arguments._
import pavlosgi.freecli.core.dsl.arguments.CanProduceArgumentDetails._

trait CanProduceArgumentDetails[T, H <: HList] {
  type IOut <: HList
  type DOut <: HList

  def intersection: Intersection.Aux[H, ArgumentDetailsTypes, IOut]
  def folder: LeftFolder.Aux[IOut, ArgumentDetails, aggregate.type, ArgumentDetails]
  def diff: Diff.Aux[H, IOut, DOut]
  def typeable: Typeable[T]

  def apply(list: H): (ArgumentDetails, DOut) = {
    val inters = intersection.apply(list)
    val field = getArgumentDetails[T, IOut](inters)(folder, typeable)
    val remaining = diff.apply(list)

    field -> remaining
  }
}

object CanProduceArgumentDetails {
  type ArgumentDetailsTypes =
    Placeholder :: Description :: HNil

  type Aux[T, H <: HList, Out <: HList] = CanProduceArgumentDetails[T, H] {
    type DOut = Out
  }

  object aggregate extends Poly2 {
    implicit def caseArgumentDetailsPlaceholder:
      Case.Aux[ArgumentDetails, Placeholder, ArgumentDetails] =

      at[ArgumentDetails, Placeholder] {
        case (ad, p: Placeholder) =>
          ArgumentDetails(p, ad.description)
      }

    implicit def caseArgumentDetailsDescription:
      Case.Aux[ArgumentDetails, Description, ArgumentDetails] =

      at[ArgumentDetails, Description] {
        case (ad, d) =>
          ad.copy(description = Some(d))
      }
  }

  implicit def canProduceArgumentDetails[T, H <: HList, Out0 <: HList, Out1 <: HList](
    implicit ev: Intersection.Aux[H, ArgumentDetailsTypes, Out0],
    ev2: LeftFolder.Aux[Out0, ArgumentDetails, aggregate.type, ArgumentDetails],
    ev3: Diff.Aux[H, Out0, Out1],
    ev4: Typeable[T]) = {

    new CanProduceArgumentDetails[T, H] {
      type IOut = Out0
      type DOut = Out1
      def intersection = ev
      def folder = ev2
      def diff = ev3
      def typeable = ev4
    }
  }

  def getArgumentDetails[T, E <: HList](
    list: E)
   (implicit ev: LeftFolder.Aux[E, ArgumentDetails, aggregate.type, ArgumentDetails],
    ev2: Typeable[T]):
    ev.Out = {

    list.foldLeft(ArgumentDetails(Placeholder(
      ev2.describe), None))(aggregate)
  }
}