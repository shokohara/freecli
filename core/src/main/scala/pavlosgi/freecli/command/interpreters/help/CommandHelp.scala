package pavlosgi.freecli.command.interpreters.help

import cats.Monoid
import cats.instances.all._
import cats.syntax.all._

import pavlosgi.freecli.command.api.CommandField
import pavlosgi.freecli.config.interpreters.{help => C}
import pavlosgi.freecli.core.Description
import pavlosgi.freecli.core.formatting._
import pavlosgi.freecli.printer.{Printer, PrinterParts}

sealed trait CommandHelp
case class SimpleHelpCommand(field: CommandField) extends CommandHelp
case class ConfigHelpCommand(field: CommandField, config: C.ConfigHelp)
  extends CommandHelp

case class SubHelpCommand(field: CommandField, subs: CommandsHelp)
  extends CommandHelp

case class ConfigSubHelpCommand(
  field: CommandField,
  config: C.ConfigHelp,
  subs: CommandsHelp)
  extends CommandHelp

case class CommandsHelp(list: List[CommandHelp]) {
  def description(d: Option[Description]) = {
    d match {
      case None => Printer.empty
      case Some(d) =>
        for {
          _ <- Printer.ensureSingleLineSpace
          _ <- Printer.indent(2)
          _ <- Printer.line("Description")
          _ <- Printer.line(d.value)
          _ <- Printer.indent(-2)
        } yield ()
    }
  }

  def result: PrinterParts = {
    list.traverseU {
      case SimpleHelpCommand(field) =>
        for {
          _ <- Printer.line(field.name.name.bold)
          _ <- description(field.description)
          _ <- Printer.ensureSingleLineSpace
        } yield ()

      case ConfigHelpCommand(field, config) =>
        val options = config.options.fold(" ")(_ => " [options] ")
        for {
          _ <- Printer.line(s"${field.name.name.bold}$options${config.oneline.display()}")
          _ <- description(field.description)
          _ <- Printer.newLine
          _ <- Printer.indent(2)
          _ <- Printer.add(config.result)
          _ <- Printer.ensureSingleLineSpace
        } yield ()

      case SubHelpCommand(field, subs) =>
        for {
          _ <- Printer.line(field.name.name.bold)
          _ <- description(field.description)
          _ <- Printer.newLine
          _ <- Printer.indent(2)
          _ <- Printer.line("Commands")
          _ <- Printer.add(subs.result)
          _ <- Printer.ensureSingleLineSpace
        } yield ()

      case ConfigSubHelpCommand(field, config, subs) =>
        val options = config.options.fold(" ")(_ => " [options] ")
        for {
          _ <- Printer.line(s"${field.name.name.bold}$options${config.oneline.display()}")
          _ <- description(field.description)
          _ <- Printer.newLine
          _ <- Printer.indent(2)
          _ <- Printer.add(config.result)
          _ <- Printer.newLine
          _ <- Printer.line("Commands")
          _ <- Printer.add(subs.result)
          _ <- Printer.ensureSingleLineSpace
        } yield ()

    }.run
  }
}

object CommandsHelp {
  def single(c: CommandHelp) = CommandsHelp(List(c))

  implicit object monoidInstance extends Monoid[CommandsHelp] {
    def empty = CommandsHelp(List.empty)
    def combine(c1: CommandsHelp, c2: CommandsHelp) =
      CommandsHelp(c1.list ++ c2.list)
  }
}