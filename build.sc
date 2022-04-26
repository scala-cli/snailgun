import mill._
import mill.scalalib._

def scalaVersions = Seq("2.12.11", "2.13.4")

object core extends Cross[Core](scalaVersions: _*)

class Core(val crossScalaVersion: String) extends CrossSbtModule {
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
