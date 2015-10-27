import com.typesafe.sbt.SbtScalariform.ScalariformKeys
import com.typesafe.sbt.SbtScalariform._

import scalariform.formatter.preferences._

name := """play-rfc6902"""

organization := "de.lenabrueder"

version := "0.4-SNAPSHOT"

licenses += ("LGPL", url("https://opensource.org/licenses/LGPL-3.0"))

publishMavenStyle := true

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

publishArtifact in Test := false

pomExtra := (
  <scm>
    <url>git@github.com:lenalebt/play-rfc6902.git</url>
    <developerConnection>scm:git:git@github.com:lenalebt/play-rfc6902.git</developerConnection>
    <connection>scm:git:https://github.com/lenalebt/play-rfc6902.git</connection>
  </scm>
  <developers>
    <developer>
      <name>Lena Brueder</name>
      <email>oss@lena-brueder.de</email>
      <url>https://github.com/lenalebt</url>
    </developer>
  </developers>)

homepage := Some(url("https://github.com/lenalebt/play-rfc6902"))

scalaVersion := "2.11.7"

crossScalaVersions := Seq("2.11.7")

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-json" % "2.4.3",
  "org.scalatest" %% "scalatest" % "2.2.1" % "test"
)

scalariformSettings

ScalariformKeys.preferences := ScalariformKeys.preferences.value
  .setPreference(AlignParameters, true)
  .setPreference(IndentWithTabs, false)
  .setPreference(AlignSingleLineCaseStatements, true)
  .setPreference(DoubleIndentClassDeclaration, true)
  .setPreference(IndentLocalDefs, true)
  .setPreference(IndentPackageBlocks, true)
  .setPreference(IndentSpaces, 2)
  .setPreference(PreserveDanglingCloseParenthesis, true)
