import mill._
import mill.scalalib._
import mill.scalalib.publish._

import $ivy.`de.tototec::de.tobiasroeser.mill.vcs.version::0.1.4`
import de.tobiasroeser.mill.vcs.version._

import scala.concurrent.duration._

def scalaVersions = Seq("2.12.11", "2.13.4")

object core extends Cross[Core](scalaVersions: _*)

class Core(val crossScalaVersion: String) extends CrossSbtModule with SnailgunPublishModule {
  def ivyDeps = super.ivyDeps() ++ Seq(
    ivy"net.java.dev.jna:jna-platform:5.6.0"
  )
  object test extends Tests {
    def testFramework = "utest.runner.Framework"
    def ivyDeps = super.ivyDeps() ++ Seq(
      ivy"com.lihaoyi::utest:0.7.2",
      ivy"com.lihaoyi::pprint:0.6.0",
      ivy"com.googlecode.java-diff-utils:diffutils:1.3.0",
      ivy"io.monix::monix:3.3.0",
      ivy"ch.epfl.scala:nailgun-server:ee3c4343"
    )
  }
}

def ghOrg = "scala-cli"
def ghName = "snailgun"
trait SnailgunPublishModule extends PublishModule {
  import mill.scalalib.publish._
  def artifactName = "snailgun-" + super.artifactName()
  def pomSettings = PomSettings(
    description = artifactName(),
    organization = "io.github.alexarchambault.scala-cli.snailgun",
    url = s"https://github.com/$ghOrg/$ghName",
    licenses = Seq(License.`Apache-2.0`),
    versionControl = VersionControl.github(ghOrg, ghName),
    developers = Seq(
      Developer("alexarchambault", "Alex Archambault", "https://github.com/alexarchambault")
    )
  )
  def publishVersion =
    finalPublishVersion()
}

private def computePublishVersion(state: VcsState, simple: Boolean): String =
  if (state.commitsSinceLastTag > 0)
    if (simple) {
      val versionOrEmpty = state.lastTag
        .filter(_ != "latest")
        .filter(_ != "nightly")
        .map(_.stripPrefix("v"))
        .flatMap { tag =>
          if (simple) {
            val idx = tag.lastIndexOf(".")
            if (idx >= 0)
              Some(tag.take(idx + 1) + (tag.drop(idx + 1).toInt + 1).toString + "-SNAPSHOT")
            else
              None
          }
          else {
            val idx = tag.indexOf("-")
            if (idx >= 0) Some(tag.take(idx) + "+" + tag.drop(idx + 1) + "-SNAPSHOT")
            else None
          }
        }
        .getOrElse("0.0.1-SNAPSHOT")
      Some(versionOrEmpty)
        .filter(_.nonEmpty)
        .getOrElse(state.format())
    }
    else {
      val rawVersion = os.proc("git", "describe", "--tags").call().out.text().trim
        .stripPrefix("v")
        .replace("latest", "0.0.0")
        .replace("nightly", "0.0.0")
      val idx = rawVersion.indexOf("-")
      if (idx >= 0) rawVersion.take(idx) + "-" + rawVersion.drop(idx + 1) + "-SNAPSHOT"
      else rawVersion
    }
  else
    state
      .lastTag
      .getOrElse(state.format())
      .stripPrefix("v")

private def finalPublishVersion = {
  val isCI = System.getenv("CI") != null
  if (isCI)
    T.persistent {
      val state = VcsVersion.vcsState()
      computePublishVersion(state, simple = false)
    }
  else
    T {
      val state = VcsVersion.vcsState()
      computePublishVersion(state, simple = true)
    }
}

def publishSonatype(tasks: mill.main.Tasks[PublishModule.PublishData]) = T.command {
  publishSonatype0(
    data = define.Target.sequence(tasks.value)(),
    log = T.ctx().log
  )
}

private def publishSonatype0(
  data: Seq[PublishModule.PublishData],
  log: mill.api.Logger
): Unit = {

  val credentials = sys.env("SONATYPE_USERNAME") + ":" + sys.env("SONATYPE_PASSWORD")
  val pgpPassword = sys.env("PGP_PASSWORD")
  val timeout     = 10.minutes

  val artifacts = data.map {
    case PublishModule.PublishData(a, s) =>
      (s.map { case (p, f) => (p.path, f) }, a)
  }

  val isRelease = {
    val versions = artifacts.map(_._2.version).toSet
    val set      = versions.map(!_.endsWith("-SNAPSHOT"))
    assert(
      set.size == 1,
      s"Found both snapshot and non-snapshot versions: ${versions.toVector.sorted.mkString(", ")}"
    )
    set.head
  }
  val publisher = new scalalib.publish.SonatypePublisher(
    uri = "https://s01.oss.sonatype.org/service/local",
    snapshotUri = "https://s01.oss.sonatype.org/content/repositories/snapshots",
    credentials = credentials,
    signed = true,
    // format: off
    gpgArgs = Seq(
      "--detach-sign",
      "--batch=true",
      "--yes",
      "--pinentry-mode", "loopback",
      "--passphrase", pgpPassword,
      "--armor",
      "--use-agent"
    ),
    // format: on
    readTimeout = timeout.toMillis.toInt,
    connectTimeout = timeout.toMillis.toInt,
    log = log,
    awaitTimeout = timeout.toMillis.toInt,
    stagingRelease = isRelease
  )

  publisher.publishAll(isRelease, artifacts: _*)
}
