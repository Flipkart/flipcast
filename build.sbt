import sbtassembly.Plugin._
import com.typesafe.sbt.SbtStartScript
import AssemblyKeys._
import spray.revolver.RevolverPlugin.Revolver

organization  := "com.flipcast"

name          := "flipcast"

version       := "0.1"

scalaVersion  := "2.11.0"

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
  "io.spray"                        %%   "spray-can"                     % "1.3.2",
  "io.spray"                        %%   "spray-routing"                 % "1.3.2",
  "io.spray"                        %%   "spray-testkit"                 % "1.3.2"                  % "test",
  "io.spray"                        %%   "spray-client"                  % "1.3.2",
  "io.spray"                        %%   "spray-json"                    % "1.2.6",
  "com.typesafe.akka"               %%   "akka-actor"                    % "2.3.6",
  "com.typesafe.akka"               %%   "akka-cluster"                  % "2.3.6",
  "com.typesafe.akka"               %%   "akka-contrib"                  % "2.3.6",
  "com.typesafe.akka"               %%   "akka-slf4j"                    % "2.3.6",
  "com.typesafe.akka"               %%   "akka-testkit"                  % "2.3.6"                  % "test",
  "ch.qos.logback"                  %    "logback-classic"               % "1.0.9",
  "com.fasterxml.uuid"              %    "java-uuid-generator"           % "3.1.3",
  "com.codahale.metrics"            %    "metrics-logback"               % "3.0.1",
  "com.codahale.metrics"            %    "metrics-graphite"              % "3.0.1",
  "com.codahale.metrics"            %    "metrics-jvm"                   % "3.0.1",
  "commons-validator"               %    "commons-validator"             % "1.4.0",
  "commons-codec"                   %    "commons-codec"                 % "1.5",
  "com.google.guava"                %    "guava"                         % "15.0",
  "com.notnoop.apns"                %    "apns"                          % "0.2.3",
  "org.mongodb"                     %%   "casbah"                        % "2.7.1",
  "commons-io"                      %    "commons-io"                    % "2.4",
  "com.google.code.findbugs"        %    "jsr305"                        % "2.0.3",
  "org.specs2"                      %%   "specs2"                        % "2.3.11"                 % "test"
)

assemblySettings

parallelExecution in Test := false

assembleArtifact in packageScala := true

test in assembly := {}

jarName in assembly := "flipcast-service.jar"

logLevel in assembly := Level.Warn

seq(Revolver.settings: _*)

seq(SbtStartScript.startScriptForJarSettings: _*)
