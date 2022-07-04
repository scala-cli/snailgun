package snailgun

import caseapp._

// format: off
@ArgsName("The command and arguments for the Nailgun server")
final case class SnailgunOptions(
    @HelpMessage("Specify the host name of the target Nailgun server")
      nailgunServer: Option[String] = None,
    @HelpMessage("Specify the port of the target Nailgun server")
      nailgunPort: Option[Int] = None,
    @HelpMessage("Enable verbosity of the Nailgun client")
      verbose: Boolean = false,
    @HelpMessage("Print version of Nailgun client before running command")
      nailgunShowVersion: Boolean = false,
    @HelpMessage("Whether to redirect stdin to the server")
      hasInput: Boolean = true
)
// format: on

object SnailgunOptions {
  implicit lazy val parser: Parser[SnailgunOptions] = Parser.derive
  implicit lazy val help: Help[SnailgunOptions] = Help.derive
}
