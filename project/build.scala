import sbt._
import Project.Setting
import Keys._

object build extends Build {

  type Sett = Project.Setting[_]

  lazy val standardSettings = Defaults.defaultSettings ++ Seq[Sett](
    resolvers += "Typesafe Sonatype Snapshots" at "http://repo.typesafe.com/typesafe/sonatype-snapshots/",
    scalaVersion := "2.9.2",
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
      name := "scala-bound-core",
      libraryDependencies += "org.scalaz" %% "scalaz-core" % "7.0-SNAPSHOT"
    )
  )

  lazy val scalacheckBinding = Project(
    id           = "scalacheck-binding",
    base         = file("scalacheck-binding"),
    dependencies = Seq(core),
    settings     = standardSettings ++ Seq[Sett](
      name := "scala-bound-scalacheck-binding",
      libraryDependencies += "org.scalaz" %% "scalaz-core" % "7.0-SNAPSHOT",
      libraryDependencies += "org.scalacheck" %% "scalacheck" % "1.10.0"
    )
  )

  lazy val f0 = RootProject(uri("https://github.com/joshcough/f0.git"))

  lazy val f0Binding = Project(
    id           = "f0-binding",
    base         = file("f0-binding"),
    dependencies = Seq(core, f0),
    settings     = standardSettings ++ Seq[Sett](
      name := "scala-bound-f0-binding",
      libraryDependencies += "org.scalaz" %% "scalaz-core" % "7.0-SNAPSHOT"
    )
  ).dependsOn(f0)

  lazy val tests = Project(
    id = "tests",
    base = file("tests"),
    dependencies = Seq(core, f0Binding, scalacheckBinding % "test"),
    settings = standardSettings ++ Seq[Sett](
      name := "scala-bound-tests",
      publishArtifact := false,
      libraryDependencies += "org.scalacheck" %% "scalacheck" % "1.10.0"
    )
  )
}
