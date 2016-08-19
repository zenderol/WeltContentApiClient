import bintray.BintrayPlugin.autoImport._
import com.scalapenos.sbt.prompt.SbtPrompt.autoImport._
import org.scalastyle.sbt.ScalastylePlugin._
import sbt.Keys._
import sbt._
import scoverage.ScoverageSbtPlugin.autoImport._

object MyBuild extends Build {

  val forScala2_4 = if (System.getenv("PLAY_VERSION") == null) {
    false
  } else {
    System.getenv("PLAY_VERSION").toBoolean
  }
  val isSnapshot = false

  scalaVersion := "2.11.8"
  val playVersion = if (forScala2_4) "2.4.8" else "2.5.3"

  def withTests(project: Project) = project % "test->test;compile->compile"

  private val actualVersion: String = "0.3.5"

  val frontendCompilationSettings = Seq(
    organization := "de.welt",
    scalaVersion := "2.11.8",
    version := s"$actualVersion${if (forScala2_4) "_2.4.0" else ""}${if (isSnapshot) "-SNAPSHOT" else ""}",

    licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
    javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
    publishArtifact in Test := false,
    promptTheme := com.scalapenos.sbt.prompt.PromptThemes.ScalapenosTheme
  )

  val frontendDependencyManagementSettings = Seq(
    resolvers := Seq(
      Resolver.typesafeRepo("releases"),
      Resolver.jcenterRepo
    ),
    // https://www.typesafe.com/blog/improved-dependency-management-with-sbt-0137
    updateOptions := updateOptions.value.withCachedResolution(true)
  )

  val coreDependencySettings = Seq(
    libraryDependencies ++= Seq(
      "com.typesafe.play" %% "play" % playVersion % Provided,
      "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test,
      "org.mockito" % "mockito-core" % "1.10.19" % Test,
      "com.typesafe.play" %% "play-cache" % playVersion % Provided
    )
  )
  val clientDependencySettings = Seq(
    libraryDependencies ++= Seq(
      "org.asynchttpclient" % "async-http-client" % "2.0.10",
      "ch.qos.logback" % "logback-classic" % "1.1.7",

      "com.amazonaws" % "aws-java-sdk-core" % "1.11.13",
      "com.amazonaws" % "aws-java-sdk-s3" % "1.11.13",

      "com.typesafe" % "config" % "1.3.0" % Provided,

      "com.typesafe.play" %% "play-ws" % playVersion % Provided,
      "com.typesafe.play" %% "play-cache" % playVersion % Provided,

      "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test,
      "org.mockito" % "mockito-core" % "1.10.19" % Test
    ) ++ (
      if (forScala2_4) {
        Seq("com.kenshoo" %% "metrics-play" % "2.4.0_0.4.1")
      }
      else {
        Seq("de.threedimensions" %% "metrics-play" % "2.5.13")
      }
      )
  )


  val bintraySettings = Seq(
    pomExtra := (
      <scm>
        <url>git@github.com:WeltN24/
          {name.value}
          .git</url>
        <connection>scm:git:git@github.com:WeltN24/
          {name.value}
          .git</connection>
      </scm>
        <developers>
          <developer>
            <id>thisismana</id>
            <name>Matthias Naber</name>
            <url>https://github.com/thisismana</url>
          </developer>
        </developers>
      ),
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

  val client = project("client")
    .settings(
      name := "welt-content-api-client"
    )
    .settings(clientDependencySettings: _*)
    .dependsOn(withTests(core)).aggregate(core)

  val admin = project("admin")
    .settings(
      name := "welt-content-api-admin-client"
    )
    .settings(clientDependencySettings: _*)
    .dependsOn(withTests(client)).aggregate(client)

  val main = Project("Root", base = file("."))
    .settings(
      name := "welt-content-api-root"
    )
    .settings(frontendCompilationSettings: _*)
    .settings(
      publish := {},
      bintrayUnpublish := {}
    )
    .aggregate(core, client, admin)

}
