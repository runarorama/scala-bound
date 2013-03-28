name := "bound"

resolvers += "Typesafe Sonatype Snapshots" at "http://repo.typesafe.com/typesafe/sonatype-snapshots/"

scalaVersion := "2.10.1"

libraryDependencies += "org.scalaz" %% "scalaz-core" % "7.0-SNAPSHOT"

crossScalaVersions := Seq("2.9.2", "2.9.3", "2.10.0", "2.10.1")

scalacOptions <++= (scalaVersion) map { sv =>
  val versionDepOpts =
    if (sv startsWith "2.9") Seq()
    else Seq("-feature", "-language:higherKinds", "-language:implicitConversions")
  Seq("-deprecation", "-unchecked") ++ versionDepOpts
}