import ReleaseTransformations._

organization in ThisBuild := "io.estatico"

lazy val root = project.in(file("."))
  .aggregate(cachedJS, cachedJVM, catsTestsJVM, catsTestsJS)
  .settings(noPublishSettings)

lazy val cached = crossProject.in(file("."))
  .settings(defaultSettings)
  .settings(releasePublishSettings)
  .settings(name := "cached")

lazy val cachedJVM = cached.jvm
lazy val cachedJS = cached.js

lazy val catsTests = crossProject.in(file("cats-tests"))
  .dependsOn(cached)
  .settings(defaultSettings)
  .settings(noPublishSettings)
  .settings(
    name := "cached-cats-tests",
    description := "Test suite for cached + cats interop",
    libraryDependencies ++= Seq(
      "org.typelevel" %%% "cats-core" % "1.0.1"
    )
  )

lazy val catsTestsJVM = catsTests.jvm
lazy val catsTestsJS = catsTests.js

lazy val noPublishSettings = Seq(
  publish := (),
  publishLocal := (),
  publishArtifact := false
)

lazy val releasePublishSettings = Seq(
  releaseCrossBuild := true,
  releasePublishArtifactsAction := PgpKeys.publishSigned.value,
  releaseProcess := Seq[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    runClean,
    runTest,
    setReleaseVersion,
    commitReleaseVersion,
    tagRelease,
    publishArtifacts,
    setNextVersion,
    commitNextVersion,
    ReleaseStep(action = Command.process("sonatypeReleaseAll", _)),
    pushChanges
  ),
  homepage := Some(url("https://github.com/estatico/scala-cached")),
  licenses := Seq("Apache 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
  publishMavenStyle := true,
  publishArtifact in Test := false,
  pomIncludeRepository := { _ => false },
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases"  at nexus + "service/local/staging/deploy/maven2")
  },
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/estatico/scala-cached"),
      "scm:git:git@github.com:estatico/scala-cached.git"
    )
  ),
  developers := List(
    Developer("caryrobbins", "Cary Robbins", "carymrobbins@gmail.com", url("http://caryrobbins.com"))
  ),

  credentials ++= (
    for {
      username <- Option(System.getenv().get("SONATYPE_USERNAME"))
      password <- Option(System.getenv().get("SONATYPE_PASSWORD"))
    } yield Credentials(
      "Sonatype Nexus Repository Manager",
      "oss.sonatype.org",
      username,
      password
    )
  ).toSeq
)

lazy val defaultSettings = Seq(
  defaultScalacOptions,
  defaultLibraryDependencies,
  addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full),
  addCompilerPlugin("org.spire-math" % "kind-projector" % "0.9.4" cross CrossVersion.binary)
)

lazy val defaultScalacOptions = scalacOptions ++= Seq(
  "-Xfatal-warnings",
  "-unchecked",
  "-feature",
  "-deprecation",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-language:experimental.macros"
)

lazy val defaultLibraryDependencies = libraryDependencies ++= Seq(
  "org.typelevel" %% "macro-compat" % "1.1.1",
  scalaOrganization.value % "scala-reflect" % scalaVersion.value % Provided,
  scalaOrganization.value % "scala-compiler" % scalaVersion.value % Provided,
  "org.scalacheck" %%% "scalacheck" % "1.13.4" % "test",
  "org.scalatest" %%% "scalatest" % "3.0.0" % "test"
)
