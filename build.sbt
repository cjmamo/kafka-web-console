name := "kafka-web-console"

version := "2.1.0-SNAPSHOT"

scalaVersion := "2.10.4"

libraryDependencies ++= Seq(
  jdbc,
  cache,
  "org.squeryl" % "squeryl_2.10" % "0.9.5-6",
  "com.twitter" % "util-zk_2.10" % "6.11.0",
  "com.twitter" % "finagle-core_2.10" % "6.15.0",
  "org.quartz-scheduler" % "quartz" % "2.2.1",
  "org.apache.kafka" % "kafka_2.10" % "0.8.1.1"
    exclude("javax.jms", "jms")
    exclude("com.sun.jdmk", "jmxtools")
    exclude("com.sun.jmx", "jmxri")
)

enablePlugins(PlayScala, SbtWeb)

includeFilter in (Assets, LessKeys.less) := "custom.less"

/*
 * Basic metadata for building native packages
 *
 * https://www.playframework.com/documentation/2.3.x/ProductionDist#The-Native-Packager
 *
 * - Run `sbt stage`
 * - Inspect target/universal/stage
 * - Run e.g. `sbt debian:packageBin`
 */
import com.typesafe.sbt.SbtNativePackager._
import NativePackagerKeys._

maintainer := "Claude Mamo <claude.mamo@gmail.com>"

packageSummary := "A web application for monitoring Apache Kafka"

packageDescription := packageSummary.value

daemonUser in Linux := normalizedName.value

daemonGroup in Linux := (daemonUser in Linux).value

