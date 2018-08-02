import sbt.Keys._
import sbt._
import scoverage.ScoverageSbtPlugin.autoImport._

import scala.util.Properties

val buildNumber = Properties.envOrNone("BUILD_NUMBER")
val isSnapshot = buildNumber.isEmpty
val PlayVersion = "2.6.15"
val PlayJsonVersion = "2.6.9"
val actualVersion: String = s"1.8.${buildNumber.getOrElse("0-local")}"

def withTests(project: Project) = project % "test->test;compile->compile"

val frontendCompilationSettings = Seq(
  organization := "de.welt",
  scalaVersion := "2.12.6",
  version in ThisBuild := s"${actualVersion}_$PlayVersion${if (isSnapshot) "-SNAPSHOT" else ""}",

  licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
  javacOptions ++= Seq("-deprecation", "-source", "-opt:l:default"),
  publishArtifact in Test := false,

  fork in Test := true,
  javaOptions in Test += "-Xmx2048M",
  javaOptions in Test += "-Dconfig.resource=application.test.conf",
  javaOptions in Test += "-XX:+UseConcMarkSweepGC",
  javaOptions in Test += "-XX:ReservedCodeCacheSize=128m",
  javaOptions in Test += "-XX:MaxMetaspaceSize=512m",
  javaOptions in Test += "-Duser.timezone=Europe/Berlin",
  baseDirectory in Test := file("."),
  // disable scaladoc
  sources in (Compile, doc) := Seq()
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
    "com.typesafe.play" %% "play-json" % PlayJsonVersion % Provided,

    "org.mockito" % "mockito-core" % "1.10.19" % Test,
    "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.1" % Test
  )
)
val clientDependencySettings = Seq(
  libraryDependencies ++= Seq(
    "com.typesafe.play" %% "play-ws" % PlayVersion % Provided,
    "com.typesafe.play" %% "play-guice" % PlayVersion % Provided,
    "com.typesafe.play" %% "play-ws" % PlayVersion % Provided,
    "com.typesafe.play" %% "play-cache" % PlayVersion % Provided,
    "com.typesafe" % "config" % "1.3.1" % Provided,

    "ch.qos.logback" % "logback-classic" % "1.2.3" % Provided,

    "com.amazonaws" % "aws-java-sdk-core" % "1.11.235",
    "com.amazonaws" % "aws-java-sdk-s3" % "1.11.235",
    "com.kenshoo" %% "metrics-play" % "2.6.6_0.6.2",

    "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % Test,
    "org.mockito" % "mockito-core" % "1.10.19" % Test
  )
)

val bintraySettings = Seq(
  pomExtra :=
    <scm>
      <url>git@github.com:WeltN24/{name.value}.git</url>
      <connection>scm:git:git@github.com:WeltN24/{name.value}.git</connection>
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


// only in "welt-content-api-client"
val utils = project("utils")
  .settings(
    name := "welt-content-api-utils",
    libraryDependencies += "com.google.guava" % "guava" % "22.0",
    libraryDependencies += "com.typesafe.play" %% "play" % PlayVersion % Provided
  )
  .settings(coreDependencySettings: _*)

val core = project("core")
  .settings(
    name := "welt-content-api-core"
  )
  .settings(coreDependencySettings: _*)

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
  .dependsOn(withTests(utils)).aggregate(utils)
  .dependsOn(withTests(core)).aggregate(core)

val pressed = project("pressed")
  .settings(
    name := "welt-content-api-pressed"
  )
  .settings(coreDependencySettings: _*)
  .dependsOn(withTests(core)).aggregate(core)

val coreClient = project("core-client")
  .settings(
    name := "welt-content-api-core-client"
  )
  .settings(clientDependencySettings: _*)
  .dependsOn(withTests(utils)).aggregate(utils)
  .dependsOn(withTests(core)).aggregate(core)

val rawClient = project("raw-client")
  .settings(
    name := "welt-content-api-raw-client"
  )
  .settings(clientDependencySettings: _*)
  .dependsOn(withTests(coreClient)).aggregate(coreClient)
  .dependsOn(withTests(raw)).aggregate(raw)

val rawAdminClient = project("raw-admin-client")
  .settings(
    name := "welt-content-api-raw-admin-client"
  )
  .settings(clientDependencySettings: _*)
  .dependsOn(withTests(rawClient)).aggregate(rawClient)

val pressedClient = project("pressed-client")
  .settings(
    name := "welt-content-api-pressed-client"
  )
  .settings(clientDependencySettings: _*)
  .dependsOn(withTests(coreClient)).aggregate(coreClient)
  .dependsOn(withTests(pressed)).aggregate(pressed)
  .dependsOn(withTests(rawClient)).aggregate(rawClient)

val main = Project("root", base = file("."))
  .settings(
    name := "welt-content-api-root"
  )
  .settings(frontendCompilationSettings: _*)
  .settings(
    publish := {},
    bintrayUnpublish := {}
  )
  .aggregate(core, coreClient, coreTest, raw, rawClient, rawAdminClient, pressed, pressedClient)
