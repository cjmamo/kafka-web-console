Kafka Web Console
=========
Kafka Web Console is a Java web application for monitoring [Apache Kafka](http://kafka.apache.org/). With a **modern** web browser, you can view from the console:

   - Registered brokers
   
![brokers](/img/brokers.png)

***

   - Topics and their partitions
   
![topics](/img/topics.png)

***

   - Consumer groups, individual consumers, and partition offsets for each consumer group
    
![topic](/img/topic.png)

***

   - Latest published topic messages (requires web browser support for WebSocket)

![topic feed](/img/topic-feed.png)

***

Furthermore, the console provides a JSON API.

Requirements
---
- Play Framework 2.2.x
- Apache Kafka 0.8.0
- Zookeeper 3.3.3

Deployment
----
Consult Play!'s documentation for [deployment options and instructions](http://www.playframework.com/documentation/2.2.x/Production).

Getting Started
---
1. Kafka Web Console requires a relational database. Consult Play!'s documentation to [specify the database to be used by the console](http://www.playframework.com/documentation/2.2.x/ScalaDatabase). The following databases are supported:

   - H2
   - PostgreSql
   - Oracle
   - DB2
   - MySQL
   - Apache Derby
   - Microsoft SQL Server
<br/><br/>
2. Before you can monitor a broker, you need to register the Zookeeper server associated with it:

![register zookeeper](/img/register-zookeeper.png)

Filling in the form and clicking on *Connect* will register the Zookeeper server. Once the console has successfully established a connection with the registered Zookeeper server, it can retrieve all necessary information about brokers, topics, and consumers.

![zookeepers](/img/zookeepers.png)

Support
---
Please [report](http://github.com/claudemamo/kafka-web-console/issues) any bugs or desired features.