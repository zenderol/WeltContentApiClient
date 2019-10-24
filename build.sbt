import sbt.Keys._
import sbt._
import scoverage.ScoverageSbtPlugin.autoImport._

import scala.util.Properties

val buildNumber = Properties.envOrNone("BUILD_NUMBER")
val isSnapshot = buildNumber.isEmpty
val PlayVersion = "2.7.3"
val AWSVersion = "1.11.631"
val actualVersion: String = s"5.1.${buildNumber.getOrElse("0-local")}"

val javaVersion: Int = sys.props("java.specification.version") match {
  case "1.8" => 8
  case v if v.forall(_.isDigit) => v.toInt
  case v if !v.forall(_.isDigit) => v.take(2).toInt
  case v@_ => throw new IllegalStateException(s"Cannot detect java version for java.specification.version=$v")
}
val javaVersionError: String = {
  val error = s"* Java 11 is required for this project. Found $javaVersion. *"
  s"""
     |${"*" * error.length}
     |${error}
     |${"*" * error.length}""".stripMargin
}
initialize := {
  val _ = initialize.value
  assert(Set(11).contains(javaVersion), javaVersionError)
}

def withTests(project: Project) = project % "test->test;compile->compile"

val frontendCompilationSettings = Seq(
  organization := "de.welt",
  scalaVersion := "2.12.10",
  crossScalaVersions := Seq(scalaVersion.value, "2.13.1"),
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
    Resolver.jcenterRepo
  ),
  // https://www.typesafe.com/blog/improved-dependency-management-with-sbt-0137
  updateOptions := updateOptions.value.withCachedResolution(true)
)
val vMockito = "3.0.0"
val coreDependencySettings = Seq(
  libraryDependencies ++= Seq(
    "org.mockito" % "mockito-core" % vMockito % Test,
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
    "com.kenshoo" %% "metrics-play" % "2.7.3_0.8.1",

    "org.scalatestplus.play" %% "scalatestplus-play" % "4.0.3" % Test,
    "org.mockito" % "mockito-core" % vMockito % Test
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
