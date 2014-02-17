name := "kafka-web-console"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  jdbc,
  cache,
  //  "com.101tec" % "zkclient" % "0.3",
  //  "com.yammer.metrics" % "metrics-core" % "2.2.0",
  //  "log4j" % "log4j" % "1.2.15"
  //    exclude("javax.jms", "jms")
  //    exclude("com.sun.jdmk", "jmxtools")
  //    exclude("com.sun.jmx", "jmxri")
  "org.squeryl" % "squeryl_2.10" % "0.9.5-6",
  "com.twitter" % "util-zk_2.10" % "6.11.0",
  "org.apache.kafka" % "kafka_2.10" % "0.8.0"
    exclude("javax.jms", "jms")
    exclude("com.sun.jdmk", "jmxtools")
    exclude("com.sun.jmx", "jmxri")
)

play.Project.playScalaSettings
