package pavlosgi
package freecli
package config
package parser

import algebra._
import dsl.ConfigDsl

import cats.data._
import cats.std.list._
import cats.std.option._
import cats.syntax.all._
import cats.~>

trait Operations extends {
  private type StateResult[A] = State[Seq[String], A]
  private type Result[A] = EitherT[StateResult, NonEmptyList[ParsingError], A]

  def parse[G[_]: Plugin, A]
    (args: Seq[String])
    (d: ConfigDsl[G, A])
    (implicit nat: G ~> Parser): ValidatedNel[ParsingError, A] = {

    parseAndReturnExtraArgs(args)(d) match {
      case (Nil, r)     => r.toValidated
      case (remArgs, r) =>
        r.toValidated.leftMap(e => NonEmptyList(InvalidArgs(remArgs), e.toList))
    }
  }

  def parseAndReturnExtraArgs[G[_]: Plugin, A]
    (args: Seq[String])
    (d: ConfigDsl[G, A])
    (implicit nat: G ~> Parser):
    (Seq[String], NonEmptyList[ParsingError] Either A) = {

    d.apply(parserConfigAlgebra).value.run(args).value match {
      case (remArgs, r) => remArgs -> r
    }
  }

  private def parserConfigAlgebra[G[_]: Plugin]
    (implicit nat: G ~> Parser): ConfigAlgebra[Result, G] =

    new ConfigAlgebra[Result, G] {
      def arg[A, B](field: Field,
                    f: B => A,
                    default: Option[B])
                   (implicit ev: G[B]): Result[A] = {

        for {
          optV <- findOptFieldValue(field)
          parser = nat.apply(ev)
          res <- parseOrDefault(field, optV, parser, default)
        } yield f(res)
      }

      def opt[A, B](field: Field,
                    f: Option[B] => A)
                   (implicit ev: G[B]): Result[A] = {

        for {
          optV <- findOptFieldValue(field)
          parser = nat.apply(ev)
          res <- EitherT.fromEither[StateResult](optV.traverseU(v =>
                  parser(v).toLeftMap(e =>
                    e.map(GenParsingError.toParsingError(field, _)))))

        } yield f(res)
      }

      def sub[A](field: SubField,
                 f: ApplyConfigAlgebra[G, A],
                 default: Option[A]): Result[A] = {

        for {
          subArgs <- findSubFieldArgs(field)
          (remArgs, resX) = parseAndReturnExtraArgs(subArgs)(f)
          res <- EitherT.fromEither[StateResult](resX)
          _   <- EitherT.right(State.modify[Seq[String]](s => s ++ remArgs))
        } yield res
      }

      override def pure[A](x: A): Result[A] = EitherT.pure(x)
      override def ap[A, B](ff: Result[A => B])(fa: Result[A]): Result[B] = {
        EitherT.apply(for {
          faEither <- fa.value
          ffEither <- ff.value
        } yield {
          faEither.toValidated.ap(ffEither.toValidated).toEither
        })
      }
    }

  private def findOptFieldValue(field: Field): Result[Option[String]] = {
    def findOpt(args: Seq[String]):
      (Seq[String], NonEmptyList[ParsingError] Either Option[String]) = {

      val Pattern = s"^${field.name.show}$$".r
      val PatternAbreviation = s"^${field.abbreviation.show}$$".r

      val argsWithIndex = args.zipWithIndex
      val fieldIdx = argsWithIndex.collectFirst {
        case (arg, idx) if Pattern.pattern.matcher(arg).matches => idx
        case (arg, idx) if PatternAbreviation.pattern.matcher(arg).matches => idx
      }

      fieldIdx match {
        case Some(idx) if argsWithIndex.size > idx + 1 =>
          val remArgs = argsWithIndex.filter(a => a._2 != idx && a._2 != idx + 1)
                          .map(_._1)

          remArgs -> Right(Some(argsWithIndex.apply(idx + 1)._1))

        case Some(idx) =>
          args -> Left(NonEmptyList[ParsingError](FieldValueMissing(field)))

        case None => args -> Right(None)
      }
    }

    EitherT.apply(for {
      args <- State.get[Seq[String]]
      (remArgs, optX) = findOpt(args)
      res <- State.pure(optX)
      _   <- State.set(remArgs)
    } yield res)
  }

  private def findSubFieldArgs(field: SubField): Result[Seq[String]] = {
    def findArgs(args: Seq[String]):
      (Seq[String], NonEmptyList[ParsingError] Either Seq[String]) = {

        val Pattern = s"^${field.name.show}$$".r
        val index = args.indexWhere(Pattern.pattern.matcher(_).matches)
        if (index != -1) {
          args.take(index) -> Right(args.drop(index + 1))
        } else {
          args -> Left(NonEmptyList[ParsingError](SubFieldMissing(field)))
        }
    }

    EitherT.apply(for {
      args <- State.get[Seq[String]]
      (remArgs, subArgs) = findArgs(args)
      res <- State.pure(subArgs)
      _   <- State.set(remArgs)
    } yield res)
  }

  private def parseOrDefault[field, B]
    (field: Field,
     value: Option[String],
     parser: Parser[B],
     default: Option[B]): Result[B] = {

    (value, default) match {
      case (Some(v), _) =>
        EitherT.fromEither[StateResult](parser(v).leftMap(e =>
          e.map(GenParsingError.toParsingError(field, _))).toEither)

      case (None, Some(v)) =>
        v.pure[Result]

      case (None, None) =>
        EitherT.fromEither(
          Left(NonEmptyList[ParsingError](FieldMissingAndNoDefault(field))))
    }
  }

}


