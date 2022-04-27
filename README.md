# Scala CLI temporary fork of snailgun

This temporary fork adds a few features to the original [snailgun library](https://github.com/jvican/snailgun). These changes are motivated by how [Scala CLI](https://github.com/VirtusLab/scala-cli) uses snailgun.

This fork allows
- not to supply a stdin `InputStream` (and not spawn a dedicated thread for it down-the-line)
- to pass a thread pool, that snailgun can use to watch for output coming from the nailgun server (rather than spawning a new thread for each command sent to the server)

## Building

This fork replaces sbt by Mill. Build it with
```text
$ ./mill __.compile
```

Run tests with
```text
$ ./mill __.test
```

## Downloading

This fork JARs are pushed to Maven Central. Depend on it from Mill with
```scala
def ivyDeps = super.ivyDeps() ++ Seq(
  ivy"io.github.alexarchambault.scala-cli.snailgun::snailgun-core:0.4.1-sc1"
)
```
