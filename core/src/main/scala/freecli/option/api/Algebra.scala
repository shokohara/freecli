package freecli
package option
package api

import cats.free.FreeApplicative
import shapeless.HNil

import core.api.{Description, StringDecoder}

sealed trait Algebra[A]

final case class RequiredOpt[T, A](
  field: OptionField,
  f: T => A,
  g: StringDecoder[T],
  default: Option[T])
  extends Algebra[A]

final case class Opt[T, A](
  field: OptionField,
  f: Option[T] => A,
  g: StringDecoder[T])
  extends Algebra[A]

final case class Flag[A](
  field: OptionField,
  f: Boolean => A)
  extends Algebra[A]

final case class Help[A](
  field: OptionField,
  f: HNil => A)
  extends Algebra[A]

final case class Version[A](
  field: OptionField,
  value: StringValue,
  f: HNil => A)
  extends Algebra[A]

final case class Sub[A](
  description: Description,
  f: FreeApplicative[Algebra, A])
  extends Algebra[A]