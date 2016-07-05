name := """welt-content-api-client"""

organization := "de.welt"

licenses +=("MIT", url("http://opensource.org/licenses/MIT"))

javacOptions ++= Seq("-source", "1.8", "-target", "1.8")

scalaVersion := "2.11.8"

//crossScalaVersions := Seq("2.11.8")

resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

libraryDependencies ++= Seq(

  "org.asynchttpclient" % "async-http-client" % "2.0.10",
  "ch.qos.logback" % "logback-classic" % "1.1.7",

  "com.amazonaws" % "aws-java-sdk-core" % "1.11.13",
  "com.amazonaws" % "aws-java-sdk-s3" % "1.11.13",

  "com.typesafe" % "config" % "1.3.0" % Provided,
  "com.typesafe.play" %% "play" % "2.4.0" % Provided,
  "com.typesafe.play" %% "play-ws" % "2.4.0" % Provided,
  "com.typesafe.play" %% "play-cache" % "2.4.0" % Provided,

  "org.scalatest" %% "scalatest" % "2.2.1" % Test
)

publishArtifact in Test := false
pomExtra := (
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
    </developers>
  )

bintrayRepository := "welt-content-api-client"
bintrayOrganization := Some("welt")
bintrayVcsUrl := Some("git@github.com:you/your-repo.git")

com.typesafe.sbt.SbtGit.versionWithGit
