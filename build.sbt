name := "kafka-web-console"

version := "2.1.0-SNAPSHOT"

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

play.Project.playScalaSettings
