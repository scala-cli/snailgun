package snailgun

import caseapp._
import snailgun.protocol.Defaults
import snailgun.protocol.Streams
import snailgun.logging.SnailgunLogger

import java.io.{InputStream, PrintStream}
import java.net.ConnectException
import java.util.concurrent.atomic.AtomicBoolean

object Snailgun extends CaseApp[SnailgunOptions] {
  override def stopAtFirstUnrecognized = true
  def run(options: SnailgunOptions, args: RemainingArgs): Unit = {
    val inOpt = if (options.hasInput) Some(System.in) else None
    val out = System.out
    val err = System.err

    def errorAndExit(msg: String): Nothing = { err.println(msg); sys.exit(1) }
    if (options.nailgunShowVersion)
      out.println(s"Nailgun v${Defaults.Version}")

    val (cmd, cmdArgs) = args.all.toList match {
      case Nil => errorAndExit("Missing command for Nailgun server!")
      case cmd :: cmdArgs => (cmd, cmdArgs)
    }

    val streams = Streams(inOpt, out, err)
    val hostServer = options.nailgunServer
      .orElse(Defaults.env.get("NAILGUN_SERVER"))
      .getOrElse(Defaults.Host)
    val portServer = options.nailgunPort
      .orElse(Defaults.env.get("NAILGUN_PORT").map(_.toInt))
      .getOrElse(Defaults.Port)
    val client = TcpClient(hostServer, portServer)
    val logger = new SnailgunLogger("log", out, isVerbose = options.verbose)
    val code =
      try
        client.run(
          cmd,
          cmdArgs.toArray,
          Defaults.cwd,
          Defaults.env,
          streams,
          logger,
          new AtomicBoolean(false)
        )
      catch {
        case _: ConnectException =>
          errorAndExit(s"No server running in $hostServer:$portServer!")
      }
    logger.debug(s"Return code is $code")
    if (code != 0)
      sys.exit(code)
  }
}
