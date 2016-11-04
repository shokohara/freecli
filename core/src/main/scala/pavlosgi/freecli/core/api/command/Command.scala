package pavlosgi.freecli.core.api.command

import cats.syntax.show._

sealed trait Command {
  def field: CommandField
  def run(): Unit

  override def toString = field.show
}

object Command {
  def apply(commandField: CommandField, f: => Unit): Command = {
    new Command {
      def field: CommandField = commandField
      def run(): Unit = f
    }
  }
}

case class PartialCommand[P](f: P => Command)

