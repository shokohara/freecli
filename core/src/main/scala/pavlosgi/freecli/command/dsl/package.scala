package pavlosgi.freecli.command

import pavlosgi.freecli.free.FreeAlternative
import pavlosgi.freecli.command.api._

package object dsl {
  type CommandDsl[A] = FreeAlternative[Algebra, A]
}
