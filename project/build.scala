import sbt._
import Project.Setting
import Keys._

object build extends Build {

  type Sett = Project.Setting[_]

  lazy val standardSettings = Defaults.defaultSettings ++ Seq[Sett](
    organization := "bound",
    version := "1.3.0",
    resolvers += Resolver.jcenterRepo,
    resolvers += "Typesafe Sonatype Snapshots" at "http://repo.typesafe.com/typesafe/sonatype-snapshots/",
    scalaVersion := "2.11.6",
    description := "A Scala library for variable bindings in embedded languages.",
    licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
    publishMavenStyle := true,
    crossScalaVersions := Seq("2.10.5", "2.11.6"),
    scalacOptions <++= (scalaVersion) map { sv =>
      val versionDepOpts =
        if (sv startsWith "2.9") Seq()
        else Seq("-feature", "-language:higherKinds", "-language:implicitConversions")
      Seq("-deprecation", "-unchecked") ++ versionDepOpts
    },
    libraryDependencies += "org.scalaz" %% "scalaz-core" % "7.1.+"
  )

  lazy val bound = Project(
    id = "bound",
    base = file("."),
    settings = standardSettings ++ Seq[Sett](
      name := "bound",
      publishArtifact := false
    ),
    aggregate = Seq(core, scalacheckBinding, f0Binding, tests, examples)
  )

  lazy val core = Project(
    id = "core",
    base = file("core"),
    settings = standardSettings ++ Seq[Sett](
      name := "bound-core",
      description := "A Scala library for variable bindings in embedded languages."
    )
  )

  lazy val scalacheckBinding = Project(
    id           = "bound-scalacheck-binding",
    base         = file("scalacheck-binding"),
    dependencies = Seq(core),
    settings     = standardSettings ++ Seq[Sett](
      name := "bound-scalacheck-binding",
      libraryDependencies += "org.scalacheck" %% "scalacheck" % "1.12.2"
    )
  )

  lazy val f0Binding = Project(
    id           = "bound-f0-binding",
    base         = file("f0-binding"),
    dependencies = Seq(core),
    settings     = standardSettings ++ Seq[Sett](
      name := "bound-f0-binding",
      libraryDependencies += "com.clarifi" %% "f0" % "1.1.3"
    )
  )

  lazy val tests = Project(
    id = "bound-tests",
    base = file("tests"),
    dependencies = Seq(core, f0Binding, scalacheckBinding % "test"),
    settings = standardSettings ++ Seq[Sett](
      name := "bound-tests",
      publishArtifact := false,
      libraryDependencies += "org.scalacheck" %% "scalacheck" % "1.12.2"
    )
  )

  lazy val examples = Project(
    id           = "bound-examples",
    base         = file("examples"),
    dependencies = Seq(core, scalacheckBinding),
    settings     = standardSettings ++ Seq[Sett](
      name := "bound-examples",
      publishArtifact := false,
      libraryDependencies += "com.clarifi" %% "f0" % "1.1.3"
    )
  )
}
