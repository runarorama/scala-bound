import sbt._
import Project.Setting
import Keys._

object build extends Build {

  type Sett = Project.Setting[_]

  lazy val standardSettings = Defaults.defaultSettings ++ Seq[Sett](
    organization := "bound",
    version := "1.2",
    resolvers += "Typesafe Sonatype Snapshots" at "http://repo.typesafe.com/typesafe/sonatype-snapshots/",
    resolvers += "joshcough bintray maven" at "http://dl.bintray.com/joshcough/maven/",
    scalaVersion := "2.10.2",
    description := "A Scala library for variable bindings in embedded languages.",
    licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
    publishMavenStyle := true,
    crossScalaVersions := Seq("2.9.2", "2.9.3", "2.10.0", "2.10.1", "2.10.2"),
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
      description := "A Scala library for variable bindings in embedded languages.",
      libraryDependencies += "org.scalaz" %% "scalaz-core" % "7.0.2"
    )
  )

  lazy val scalacheckBinding = Project(
    id           = "bound-scalacheck-binding",
    base         = file("scalacheck-binding"),
    dependencies = Seq(core),
    settings     = standardSettings ++ Seq[Sett](
      name := "bound-scalacheck-binding",
      libraryDependencies += "org.scalaz" %% "scalaz-core" % "7.0.2",
      libraryDependencies += "org.scalacheck" %% "scalacheck" % "1.10.0"
    )
  )

  lazy val f0Binding = Project(
    id           = "bound-f0-binding",
    base         = file("f0-binding"),
    dependencies = Seq(core),
    settings     = standardSettings ++ Seq[Sett](
      name := "bound-f0-binding",
      libraryDependencies += "org.scalaz" %% "scalaz-core" % "7.0.2",
      libraryDependencies += "clarifi" %% "f0" % "1.0"
    )
  )

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

  lazy val examples = Project(
    id           = "bound-examples",
    base         = file("examples"),
    dependencies = Seq(core, scalacheckBinding),
    settings     = standardSettings ++ Seq[Sett](
      name := "bound-examples",
      libraryDependencies += "org.scalaz" %% "scalaz-core" % "7.0.2",
      libraryDependencies += "clarifi" %% "f0" % "1.0"
    )
  )
}
