lazy val akkaHttpVersion = "10.1.12"
lazy val akkaVersion    = "2.6.8"

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      version := "0.1",
      organization    := "ch.master.hnv",
      scalaVersion    := "2.13.3"
    )),
    name := "backend-hnv",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http"                % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-spray-json"     % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-actor-typed"         % akkaVersion,
      "com.typesafe.akka" %% "akka-stream"              % akkaVersion,
      "ch.qos.logback"    % "logback-classic"           % "1.2.3",

      "com.typesafe.akka" %% "akka-http-testkit"        % akkaHttpVersion % Test,
      "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion     % Test,
      "org.scalatest"     %% "scalatest"                % "3.0.8"         % Test,

      "org.neo4j.driver" % "neo4j-java-driver" % "4.1.1",
      "com.dimafeng" %% "neotypes" % "0.15.1"
    )
  ).settings(
  mainClass in assembly := Some("ch.master.hnv.Main")
)