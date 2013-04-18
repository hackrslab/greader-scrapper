import sbt._

object Dependencies {
  val resolutionRepos = Seq(
    "spray repo" at "http://repo.spray.io/"
    , "codehale repo" at "http://repo.codahale.com"
  )

  def listUnmanaged(base: RichFile): Keys.Classpath = {
    val baseDirectories = (base / "custom-libs")
    (baseDirectories ** "*.jar").classpath
  }

  def compile   (deps: ModuleID*): Seq[ModuleID] = deps map (_ % "compile")
  def provided  (deps: ModuleID*): Seq[ModuleID] = deps map (_ % "provided")
  def test      (deps: ModuleID*): Seq[ModuleID] = deps map (_ % "test")
  def runtime   (deps: ModuleID*): Seq[ModuleID] = deps map (_ % "runtime")
  def container (deps: ModuleID*): Seq[ModuleID] = deps map (_ % "container")

  val sprayCan      = "io.spray"                               % "spray-can"                     % "1.1-M7"
  val sprayRouting  = "io.spray"                               % "spray-routing"                 % "1.1-M7"
  val rome          = "rome"                                   % "rome"                          % "1.0"
  val redis         = "com.top10"                              %% "scala-redis-client"           % "1.10.0"
  val metricsServer = "com.geekple"                            %% "gmetrics-server"              % "0.1"
  val metricsClient = "com.geekple"                            %% "gmetrics-client"              % "0.1"
  val metricsCore   = "com.yammer.metrics"                     % "metrics-core"                  % "3.0.0-BETA1"
  val metricsJson   = "com.yammer.metrics"                     % "metrics-json"                  % "3.0.0-BETA1"
  val jackson       = "com.fasterxml.jackson.module"           %% "jackson-module-scala"         % "2.1.3"
  val akkaActor     = "com.typesafe.akka"                      %%  "akka-actor"                  % "2.1.2"
  val akkaSlf4j     = "com.typesafe.akka"                      %%  "akka-slf4j"                  % "2.1.2"
  val akkaTestKit   = "com.typesafe.akka"                      %%  "akka-testkit"                % "2.1.2"
  val scalatest     = "org.scalatest"                          %%  "scalatest"                   % "1.9.1"
  val specs2        = "org.specs2"                             %%  "specs2"                      % "1.14"
  val sprayJson     = "io.spray"                               %%  "spray-json"                  % "1.2.3"
}
