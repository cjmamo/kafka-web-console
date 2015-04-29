Kafka Web Console
=========
Kafka Web Console is a Java web application for monitoring [Apache Kafka](http://kafka.apache.org/). With a **modern** web browser, you can view from the console:

   - Registered brokers
   
![brokers](/img/brokers.png)

***

   - Topics, partitions, log sizes, and partition leaders 
   
![topics](/img/topics.png)

***

   - Consumer groups, individual consumers, consumer owners, partition offsets and lag 
    
![topic](/img/topic.png)

***

   - Graphs showing consumer offset and lag history as well as consumer/producer message throughput history. 
    
![topic](/img/offset-history.png)

***

   - Latest published topic messages (requires web browser support for WebSocket)

![topic feed](/img/topic-feed.png)

***

Furthermore, the console provides a JSON API described in [RAML](/public/api-console/kafka-web-console.raml). The API can be tested using the embedded API Console accessible through the URL http://*[hostname]*:*[port]*/api/console. 

Requirements
---
- Play Framework 2.2.x
- Apache Kafka 0.8.x
- Zookeeper 3.3.3 or 3.3.4

Deployment
----
Consult Play!'s documentation for [deployment options and instructions](http://www.playframework.com/documentation/2.2.x/Production).

Local Dev Setup
   - Clone the repo
   - Make sure sbt is installed
   - In the root dir of the repo run sbt
   - When sbt has finished doing its thing you should be in the shell kafka-web-console
   - Run run - This should do more downloading then finally show you something like:

```
--- (Running the application from SBT, auto-reloading is enabled) ---

[info] play - Listening for HTTP on /0:0:0:0:0:0:0:0:9000

(Server started, use Ctrl+D to stop and go back to the console...)
```

Getting Started
---


2. Kafka Web Console requires a relational database. By default, the server connects to an embedded H2 database and no database installation or configuration is needed. Consult Play!'s documentation to [specify a database for the console](http://www.playframework.com/documentation/2.2.x/ScalaDatabase). The following databases are supported:
   - H2 (default)
   - PostgreSql
   - Oracle
   - DB2
   - MySQL
   - Apache Derby
   - Microsoft SQL Server

   Changing the database might necessitate making minor modifications to the [DDL](conf/evolutions/default) to accommodate the new database.
   
3. Before you can monitor a broker, you need to register the Zookeeper server associated with it:

![register zookeeper](/img/register-zookeeper.png)

Filling in the form and clicking on *Connect* will register the Zookeeper server. Once the console has successfully established a connection with the registered Zookeeper server, it can retrieve all necessary information about brokers, topics, and consumers:

![zookeepers](/img/zookeepers.png)

Support
---
Please [report](http://github.com/claudemamo/kafka-web-console/issues) any bugs or desired features.
