import com.typesafe.sbt.SbtScalariform.ScalariformKeys
import com.typesafe.sbt.SbtScalariform._

import scalariform.formatter.preferences._

name := """play-rfc6902"""

organization := "de.lenabrueder"

version := "0.1-SNAPSHOT"

licenses += ("LGPL", url("https://opensource.org/licenses/LGPL-3.0"))

scalaVersion := "2.11.7"

crossScalaVersions := Seq("2.10.4", "2.11.7")

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "2.2.1" % "test",
  "com.typesafe.play" %% "play-json" % "2.4.1"
)

bintraySettings

com.typesafe.sbt.SbtGit.versionWithGit

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