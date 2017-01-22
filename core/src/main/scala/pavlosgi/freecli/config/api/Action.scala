package pavlosgi.freecli.config.api

import pavlosgi.freecli.argument.{api => A}
import pavlosgi.freecli.option.{api => O}

sealed trait Action
case class ArgumentAction(a: A.Action) extends Action {
  def run(): Unit = a match {
    case A.NoOp =>
      ()
      sys.exit(0)
  }
}

case class OptionAction(o: O.Action) extends Action {
  def run(help: String) = {
    o match {
      case v: O.VersionAction =>
        v.run()

      case h: O.HelpAction.type =>
        h.run(help)
    }
    sys.exit(0)
  }
}