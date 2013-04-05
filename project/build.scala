import sbt._
import Project.Setting
import Keys._

object build extends Build {

  type Sett = Project.Setting[_]

  lazy val standardSettings = Defaults.defaultSettings ++ Seq[Sett](
    organization := "bound",
    version := "0.2-SNAPSHOT",
    resolvers += "Typesafe Sonatype Snapshots" at "http://repo.typesafe.com/typesafe/sonatype-snapshots/",
    scalaVersion := "2.10.1",
    crossScalaVersions := Seq("2.9.2", "2.9.3", "2.10.0", "2.10.1"),
    scalacOptions <++= (scalaVersion) map { sv =>
      val versionDepOpts =
        if (sv startsWith "2.9") Seq()
        else Seq("-feature", "-language:higherKinds", "-language:implicitConversions")
      Seq("-deprecation", "-unchecked") ++ versionDepOpts
    }
  )

  lazy val bound = Project(
    id = "bound",
    base = file("."),
    aggregate = Seq(core, scalacheckBinding, f0Binding, tests)
  )

  lazy val core = Project(
    id = "core",
    base = file("core"),
    settings = standardSettings ++ Seq[Sett](
      name := "bound-core",
      libraryDependencies += "org.scalaz" %% "scalaz-core" % "7.0-SNAPSHOT"
    )
  )

  lazy val scalacheckBinding = Project(
    id           = "bound-scalacheck-binding",
    base         = file("scalacheck-binding"),
    dependencies = Seq(core),
    settings     = standardSettings ++ Seq[Sett](
      name := "bound-scalacheck-binding",
      libraryDependencies += "org.scalaz" %% "scalaz-core" % "7.0-SNAPSHOT",
      libraryDependencies += "org.scalacheck" %% "scalacheck" % "1.10.0"
    )
  )

  lazy val f0 = {
    // force sbt to get the latest version of f0
    // 'sbt update' doesn't seem to get the latest even though this says that it should
    // http://stackoverflow.com/questions/8864317/how-do-i-refresh-updated-git-dependency-artifacts-in-sbt
    // so instead we have to go to github and get the latest version.
    val sha = scala.io.Source.fromURL("https://api.github.com/repos/joshcough/f0/commits?sha=master").
      takeWhile(_ != ',').mkString.dropWhile(_!=':').drop(2).dropRight(1)
    RootProject(uri("https://github.com/joshcough/f0.git#" + sha))
  }

  lazy val f0Binding = Project(
    id           = "bound-f0-binding",
    base         = file("f0-binding"),
    dependencies = Seq(core, f0),
    settings     = standardSettings ++ Seq[Sett](
      name := "bound-f0-binding",
      libraryDependencies += "org.scalaz" %% "scalaz-core" % "7.0-SNAPSHOT"
    )
  ).dependsOn(f0)

  lazy val tests = Project(
    id = "bound-tests",
    base = file("tests"),
    dependencies = Seq(core, f0Binding, scalacheckBinding % "test"),
    settings = standardSettings ++ Seq[Sett](
      name := "bound-tests",
      publishArtifact := false,
      libraryDependencies += "org.scalacheck" %% "scalacheck" % "1.10.0"
    )
  )
}
