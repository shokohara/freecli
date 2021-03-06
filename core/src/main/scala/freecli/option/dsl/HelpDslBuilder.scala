package freecli
package option
package dsl

import cats.free.FreeApplicative
import shapeless._
import shapeless.ops.hlist.Prepend

import core.api.{CanProduce, Description}
import option.api._

case class HelpDslBuilder[H <: HList](list: H) {

  def --(
    name: String)
   (implicit ev: Prepend[H, OptionFieldName :: HNil],
    ev2: NotContainsConstraint[H, OptionFieldName]) =

    new HelpDslBuilder(list :+ OptionFieldName(name))

  def -(
    abbr: Char)
   (implicit ev: Prepend[H, OptionFieldAbbreviation :: HNil],
    ev2: NotContainsConstraint[H, OptionFieldAbbreviation]) =

    new HelpDslBuilder(list :+ OptionFieldAbbreviation(abbr))

  def -~(
    description: Description)
   (implicit ev: Prepend[H, Description :: HNil],
    ev2: NotContainsConstraint[H, Description]) =

    new HelpDslBuilder(list :+ description)

}

object HelpDslBuilder {
  def help: HelpDslBuilder[HNil] =
    new HelpDslBuilder[HNil](HNil)

  implicit def canProduceOptionDsl[H <: HList](
    implicit canProduceField: CanProduce.Aux[H, (OptionField, HNil)]):
    CanProduce.Aux[HelpDslBuilder[H], OptionDsl[HNil]] = {
    new CanProduce[HelpDslBuilder[H]] {
      type Out = OptionDsl[HNil]
      def apply(t: HelpDslBuilder[H]): Out = {
        val (field, _) = canProduceField.apply(t.list)
        FreeApplicative.lift(Help[HNil](field, identity))
      }
    }
  }
}