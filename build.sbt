import com.typesafe.sbt.SbtScalariform.ScalariformKeys

import scalariform.formatter.preferences._

name := """play-rfc6902"""

organization := "de.lenabrueder"

version := "0.6-SNAPSHOT"

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

scalaVersion := "2.11.8"

crossScalaVersions := Seq("2.11.8")

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-json" % "2.5.3",
  "org.scalatest" %% "scalatest" % "2.2.6" % "test"
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

scapegoatVersion := "1.2.0"
compile in Compile <<= (compile in Compile) dependsOn scapegoat
