name := "kafka-web-console"

version := "2.1.0-SNAPSHOT"

resolvers ++= {
  Seq(
    "Local Maven Repository" at "file:///Users/cfchou/.m2/repository"
  )
}

libraryDependencies ++= Seq(
  jdbc,
  cache,
  "org.squeryl" % "squeryl_2.10" % "0.9.5-6",
  "com.twitter" % "util-zk_2.10" % "6.23.0",
  "com.101tec" % "zkclient" % "0.3",
  // "com.twitter" % "finagle-core_2.10" % "6.15.0",
  "org.quartz-scheduler" % "quartz" % "2.2.1",
  "org.apache.kafka" % "kafka_2.10" % "0.8.2.1"
    exclude("javax.jms", "jms")
    exclude("com.sun.jdmk", "jmxtools")
    exclude("com.sun.jmx", "jmxri"),
  "com.github.okapies" % "finagle-kafka_2.10" % "0.1.5"
)

lazy val root = (project in file(".")).enablePlugins(PlayScala)

includeFilter in (Assets, LessKeys.less) := "*.less"

excludeFilter in (Assets, LessKeys.less) := "_*.less"
