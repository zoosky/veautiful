// Turn this project into a Scala.js project by importing these settings

val versionStr = "0.1-SNAPSHOT"

val scalaVersionStr = "2.12.1"

lazy val veautiful = project.in(file("veautiful"))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    name := "Example",

    version := versionStr,

    scalaVersion := scalaVersionStr,

    scalaJSUseMainModuleInitializer := true,

    testFrameworks += new TestFramework("utest.runner.Framework"),

    libraryDependencies ++= Seq(
        "org.scala-js" %%% "scalajs-dom" % "0.9.2",
        "com.lihaoyi" %%% "utest" % "0.4.5" % "test"
    )
)

lazy val docs = project.in(file("docs"))
  .enablePlugins(ScalaJSPlugin)
  .dependsOn(veautiful)
  .settings(
      name := "veautiful-docs",

      version := versionStr,

      scalaVersion := scalaVersionStr,

      scalaJSUseMainModuleInitializer := true,

      testFrameworks += new TestFramework("utest.runner.Framework"),

      libraryDependencies ++= Seq(
          "org.scala-js" %%% "scalajs-dom" % "0.9.2",
          "com.lihaoyi" %%% "utest" % "0.4.5" % "test"
      )
  )


