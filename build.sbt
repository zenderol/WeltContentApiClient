import sbt.Keys._
import sbt._
import scoverage.ScoverageSbtPlugin.autoImport._

import scala.util.Properties

val buildNumber = Properties.envOrNone("BUILD_NUMBER")
val isSnapshot = buildNumber.isEmpty
val PlayVersion = "2.7.3"
val AWSVersion = "1.11.548"
val actualVersion: String = s"4.5.${buildNumber.getOrElse("0-local")}"

def withTests(project: Project) = project % "test->test;compile->compile"

val frontendCompilationSettings = Seq(
  organization := "de.welt",
  scalaVersion := "2.12.9",
  crossScalaVersions := Seq("2.12.9", "2.13.0"),
  version in ThisBuild := s"${actualVersion}_$PlayVersion${if (isSnapshot) "-SNAPSHOT" else ""}",

  licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
  javacOptions ++= Seq("-deprecation", "-source", "-opt:l:default"),
  publishArtifact in Test := false,

  fork in Test := true,
  javaOptions in Test += "-Xmx2048M",
  javaOptions in Test += "-Dconfig.resource=application.test.conf",
  javaOptions in Test += "-XX:ReservedCodeCacheSize=128m",
  javaOptions in Test += "-XX:MaxMetaspaceSize=512m",
  javaOptions in Test += "-Duser.timezone=Europe/Berlin",
  javaOptions in Test += "-Dconfig.resource=application.test.conf",
  baseDirectory in Test := file("."),
  // disable scaladoc
  sources in(Compile, doc) := Seq()
)

val frontendDependencyManagementSettings = Seq(
  resolvers := Seq(
    Resolver.mavenLocal,
    Resolver.jcenterRepo,
    Resolver.bintrayRepo("welt", "metrics-play")
  ),
  // https://www.typesafe.com/blog/improved-dependency-management-with-sbt-0137
  updateOptions := updateOptions.value.withCachedResolution(true)
)

val coreDependencySettings = Seq(
  libraryDependencies ++= Seq(
    "org.mockito" % "mockito-core" % "2.27.0" % Test,
    "org.scalatestplus.play" %% "scalatestplus-play" % "4.0.3" % Test
  )
)
val clientDependencySettings = Seq(
  libraryDependencies ++= Seq(
    "com.typesafe.play" %% "play-ws" % PlayVersion % Provided,
    "com.typesafe.play" %% "play-guice" % PlayVersion % Provided,
    "com.typesafe.play" %% "play-ws" % PlayVersion % Provided,
    "com.typesafe.play" %% "play-cache" % PlayVersion % Provided,
    "com.typesafe" % "config" % "1.3.4" % Provided,

    "ch.qos.logback" % "logback-classic" % "1.2.3" % Provided,

    "com.amazonaws" % "aws-java-sdk-core" % AWSVersion,
    "com.amazonaws" % "aws-java-sdk-s3" % AWSVersion,
    "com.amazonaws" % "aws-java-sdk-ssm" % AWSVersion,
    "com.amazonaws" % "aws-java-sdk-sts" % AWSVersion,
    "de.welt" %% "metrics-play" % "2.7.3_7",

    "org.scalatestplus.play" %% "scalatestplus-play" % "4.0.3" % Test,
    "org.mockito" % "mockito-core" % "2.27.0" % Test
  )
)

val bintraySettings = Seq(
  pomExtra :=
    <scm>
      <url>git@github.com:spring-media/rbbt-WeltContentApiClient.git</url>
      <connection>scm:git:git@github.com:spring-media/rbbt-WeltContentApiClient.git</connection>
    </scm>
      <developers>
        <developer>
          <id>thisismana</id>
          <name>Matthias Naber</name>
          <url>https://github.com/thisismana</url>
        </developer>
        <developer>
          <id>harryurban</id>
          <name>Harry Urban</name>
          <url>https://github.com/harryurban</url>
        </developer>
        <developer>
          <id>bobaaaaa</id>
          <name>Patrick Dahms</name>
          <url>https://github.com/bobaaaaa</url>
        </developer>
      </developers>,
  bintrayRepository := "welt-content-api-client",
  bintrayOrganization := Some("welt"),
  bintrayVcsUrl := Some("git@github.com:you/your-repo.git")
)

def codeStyleSettings = Seq(
  // scoverage
  coverageExcludedPackages := "<empty>;",
  //    coverageMinimum := 25, // we are not that good yet ;)
  coverageFailOnMinimum := true,

  // scalastyle
  scalastyleConfig := file("scalastyle-config.xml"),
  scalastyleFailOnError := true
)

def project(id: String) = Project(id, base = file(id))
  .settings(frontendCompilationSettings: _*)
  .settings(frontendDependencyManagementSettings: _*)
  .settings(bintraySettings: _*)
  .settings(codeStyleSettings: _*)


val core = project("core")
  .settings(
    name := "welt-content-api-core"
  )
  .settings(coreDependencySettings: _*)
  .settings(clientDependencySettings: _*)

val coreTest = project("core_test")
  .settings(
    name := "welt-content-api-core-test"
  )
  .dependsOn(withTests(core)).aggregate(core)

val raw = project("raw")
  .settings(clientDependencySettings: _*)
  .settings(
    name := "welt-content-api-raw"
  )
  .dependsOn(withTests(core)).aggregate(core)

val pressed = project("pressed")
  .settings(
    name := "welt-content-api-pressed"
  )
  .settings(coreDependencySettings: _*)
  .settings(clientDependencySettings: _*)
  .dependsOn(withTests(core)).aggregate(core)
  .dependsOn(withTests(raw)).aggregate(raw)

val main = Project("root", base = file("."))
  .settings(
    name := "welt-content-api-root"
  )
  .settings(frontendCompilationSettings: _*)
  .settings(
    publish := {},
    bintrayUnpublish := {}
  )
  .aggregate(core,
    coreTest,
    raw,
    pressed)
