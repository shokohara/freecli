package freecli
package argument
package dsl

import core.api.CanProduce

trait ArgumentDslImplicits {
  implicit def toArgumentDsl[B, A](
    b: B)
   (implicit ev: CanProduce.Aux[B, ArgumentDsl[A]]):
    ArgumentDsl[A] = {

    ev(b)
  }

  implicit def canProduceArgumentDslId[A]:
    CanProduce.Aux[ArgumentDsl[A], ArgumentDsl[A]] = {

    CanProduce.Id[ArgumentDsl[A]]
  }
}