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
- Play Framework 2.3.x
- Apache Kafka 0.8.x
- Zookeeper 3.3.3 or 3.3.4

Deployment
----
Consult Play!'s documentation for [deployment options and instructions](http://www.playframework.com/documentation/2.3.x/Production).

For instance, build a Debian package with the `debian:packageBin` task in SBT. You'll need to supply a production configuration file at `/etc/kafka-web-console/production.conf` after installing the produced .deb---you may copy `conf/application.conf` from the repo to start with, just change the secret key! Also if you stick with an H2 database, locate the file someplace that the `kafka-web-console` user has write privileges for, such as:

    db.default.url="jdbc:h2:file:/var/run/kafka-web-console/play"

Getting Started
---
1. Kafka Web Console requires a relational database. By default, the server connects to an embedded H2 database and no database installation or configuration is needed. Consult Play!'s documentation to [specify a database for the console](http://www.playframework.com/documentation/2.3.x/ScalaDatabase). The following databases are supported:
   - H2 (default)
   - PostgreSql
   - Oracle
   - DB2
   - MySQL
   - Apache Derby
   - Microsoft SQL Server

   Changing the database might necessitate making minor modifications to the [DDL](conf/evolutions/default) to accommodate the new database.
   
2. Before you can monitor a broker, you need to register the Zookeeper server associated with it:

![register zookeeper](/img/register-zookeeper.png)

Filling in the form and clicking on *Connect* will register the Zookeeper server. Once the console has successfully established a connection with the registered Zookeeper server, it can retrieve all necessary information about brokers, topics, and consumers:

![zookeepers](/img/zookeepers.png)

Support
---
Please [report](http://github.com/claudemamo/kafka-web-console/issues) any bugs or desired features.
