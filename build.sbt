name := "bound"

version := "1.0"

description := "A Scala library for variable bindings in embedded languages."

resolvers += "Typesafe Sonatype Snapshots" at "http://repo.typesafe.com/typesafe/sonatype-snapshots/"

scalaVersion := "2.10.2"

libraryDependencies += "org.scalaz" %% "scalaz-core" % "7.0.2"

scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature")

scalacOptions <++= scalaVersion map {
  case sv if sv.contains("2.10") =>
    Seq("-feature", "-language:implicitConversions", "-language:higherKinds", "-language:existentials", "-language:postfixOps")
  case _ =>
    Seq("-Ydependent-method-types")
}

seq(bintraySettings:_*)

licenses += ("MIT", url("http://opensource.org/licenses/MIT"))

publishMavenStyle := true

