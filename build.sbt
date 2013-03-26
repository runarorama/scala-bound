name := "bound"

resolvers += "Typesafe Sonatype Snapshots" at "http://repo.typesafe.com/typesafe/sonatype-snapshots/"

scalaVersion := "2.10.1"

libraryDependencies += "org.scalaz" %% "scalaz-core" % "7.0-SNAPSHOT"

scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature")
