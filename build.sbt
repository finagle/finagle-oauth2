import ReleaseTransformations._

lazy val finagleVersion = "19.1.0"

lazy val buildSettings = Seq(
  organization := "com.github.finagle",
  scalaVersion := "2.12.7",
  crossScalaVersions := Seq("2.11.12", "2.12.7")
)

val baseSettings = Seq(
  libraryDependencies ++= Seq(
    "com.twitter" %% "finagle-http" % finagleVersion,
    "org.scalacheck" %% "scalacheck" % "1.14.0" % Test,
    "org.scalatest" %% "scalatest" % "3.0.5" % Test
  )
)

lazy val publishSettings = Seq(
  publishMavenStyle := true,
  publishArtifact := true,
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases" at nexus + "service/local/staging/deploy/maven2")
  },
  publishArtifact in Test := false,
  pgpSecretRing := file("local.secring.gpg"),
  pgpPublicRing := file("local.pubring.gpg"),
  releasePublishArtifactsAction := PgpKeys.publishSigned.value,
  releaseIgnoreUntrackedFiles := true,
  licenses := Seq("Apache 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
  homepage := Some(url("https://github.com/finagle/finagle-oauth2")),
  autoAPIMappings := true,
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/finagle/finagle-oauth2"),
      "scm:git:git@github.com:finagle/finagle-oauth2.git"
    )
  ),
  releaseVersionBump := sbtrelease.Version.Bump.Minor,
  releaseProcess := {
    Seq[ReleaseStep](
      checkSnapshotDependencies,
      inquireVersions,
      releaseStepCommandAndRemaining("+clean"),
      releaseStepCommandAndRemaining("+test"),
      setReleaseVersion,
      commitReleaseVersion,
      tagRelease,
      releaseStepCommandAndRemaining("+publishSigned"),
      setNextVersion,
      commitNextVersion,
      releaseStepCommand("sonatypeReleaseAll"),
      pushChanges
    )
  },
  pomExtra :=
    <developers>
      <developer>
        <id>vkostyukov</id>
        <name>Vladimir Kostyukov</name>
        <url>https://kostyukov.net</url>
      </developer>
    </developers>
)

lazy val allSettings = baseSettings ++ buildSettings ++ publishSettings

lazy val oauth2 = project.in(file("."))
  .settings(moduleName := "finagle-oauth2")
  .settings(allSettings)
