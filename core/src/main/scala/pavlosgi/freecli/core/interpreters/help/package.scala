package pavlosgi.freecli.core.interpreters

import cats.data._

package object help {
  type Result[A] = State[HelpState, Unit]

  object Result {
    def indentation(f: Int => Int): Result[Unit] = {
      for {
        hs <- State.get[HelpState]
        _ <- State.set(hs.copy(indentation = f(hs.indentation)))
      } yield ()
    }

    def newline: Result[Unit] = {
      for {
        hs <- State.get[HelpState]
        _ <- State.set(hs.copy(text = s"${hs.text}\n"))
      } yield ()
    }

    def appendAtIndentation(text: String): Result[Unit] = {
      for {
        hs <- State.get[HelpState]
        indentation =
          (0 until hs.indentation)
           .foldLeft[String]("")((a, _) => a + " ")

        _ <- append(s"$indentation$text")
      } yield ()
    }

    def appendAtIndentationLn(text: String): Result[Unit] = {
      for {
        _ <- appendAtIndentation(text)
        _ <- newline
      } yield ()
    }

    def section(text: String): Result[Unit] = {
      for {
        _  <- newline
        _  <- appendAtIndentation(s"$text:".bold)
      } yield ()
    }

    def append(text: String): Result[Unit] = {
      for {
        hs <- State.get[HelpState]
        _  <- State.set(hs.copy(text = s"${hs.text}$text"))
      } yield ()
    }
  }

  implicit class StringOps(s: String) {
    def underline: String = s"${Console.UNDERLINED}$s${Console.RESET}"
    def yellow: String = s"${Console.YELLOW}$s${Console.RESET}"
    def magenta: String = s"${Console.MAGENTA}$s${Console.RESET}"
    def cyan: String = s"${Console.CYAN}$s${Console.RESET}"
    def bold: String = s"${Console.BOLD}$s${Console.RESET}"
  }
}
