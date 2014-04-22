import sbtassembly.Plugin._
import com.typesafe.sbt.SbtStartScript
import AssemblyKeys._
import spray.revolver.RevolverPlugin.Revolver

organization  := "com.flipcast"

name          := "flipcast"

version       := "0.1"

scalaVersion  := "2.10.2"

scalacOptions := Seq(
  "-encoding", "UTF-8",
  "-feature",
  "-unchecked",
  "-deprecation",
  "-target:jvm-1.6",
  "-language:postfixOps",
  "-language:implicitConversions",
  "-Xlog-reflective-calls",
  "-Ywarn-adapted-args",
  "-language:existentials"
)

resolvers ++= Seq(
  "spray repo" at "http://repo.spray.io/",
  "sonatype snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"
)

libraryDependencies ++= Seq(
  "io.spray"                        %   "spray-can"                     % "1.2.0",
  "io.spray"                        %   "spray-routing"                 % "1.2.0",
  "io.spray"                        %   "spray-caching"                 % "1.2.0",
  "io.spray"                        %   "spray-testkit"                 % "1.2.0"                  % "test",
  "io.spray"                        %   "spray-client"                  % "1.2.0",
  "io.spray"                        %   "spray-json_2.10"               % "1.2.5",
  "com.typesafe.akka"               %%  "akka-actor"                    % "2.2.4",
  "com.typesafe.akka"               %%  "akka-slf4j"                    % "2.2.4",
  "com.typesafe.akka"               %%  "akka-testkit"                  % "2.2.4"                  % "test",
  "ch.qos.logback"                  %   "logback-classic"               % "1.0.9",
  "com.fasterxml.uuid"              %   "java-uuid-generator"           % "3.1.3",
  "com.codahale.metrics"            %   "metrics-logback"               % "3.0.1",
  "com.codahale.metrics"            %   "metrics-graphite"              % "3.0.1",
  "com.codahale.metrics"            %   "metrics-jvm"                   % "3.0.1",
  "com.hazelcast"                   %   "hazelcast"                     % "3.1.2",
  "commons-validator"               %   "commons-validator"             % "1.4.0",
  "commons-codec"                   %   "commons-codec"                 % "1.5",
  "com.google.guava"                %   "guava"                         % "15.0",
  "com.fasterxml.jackson.module"    %   "jackson-module-scala_2.10"     % "2.2.3",
  "com.notnoop.apns"                %   "apns"                          % "0.2.3",
  "org.mongodb"                     %   "casbah_2.10"                   % "2.6.5",
  "com.github.sstone"               %   "amqp-client_2.10"              % "1.3-ML4",
  "commons-io"                      %   "commons-io"                    % "2.4",
  "org.specs2"                      %%  "specs2"                        % "1.13"                    % "test"
)

assemblySettings

parallelExecution in Test := false

assembleArtifact in packageScala := true

test in assembly := {}

jarName in assembly := "flipcast-service.jar"

excludedJars in assembly <<= (fullClasspath in assembly) map { cp =>
  cp filter {
    elem => {
      (elem.data.getName == "servlet-api-2.5-20081211.jar") || (elem.data.getName == "slf4j-log4j12-1.7.2.jar") || (elem.data.getName == "netty-3.5.9.Final.jar") || (elem.data.getName == "jcl-over-slf4j-1.6.1.jar")
    }
  }
}

seq(Revolver.settings: _*)

seq(SbtStartScript.startScriptForJarSettings: _*)
