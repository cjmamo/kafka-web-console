Kafka Web Console
=========
Kafka Web Console is a Java web application for monitoring Apache Kafka.

It allows you from your web browser to view:

   - Registered brokers
   
![brokers](/img/brokers.png)
   
   - Topics and their partitions
   
![topics](/img/topics.png)
   
   - Consumer groups, individual consumers, and current partition offsets
    
![topic](/img/topic.png)

   - Latest published topic messages

![topic feed](/img/topic-feed.png)

On top of all this, the console provides a JSON API.

Deployment
----

Consult Play Framework's documentation for [deployment options and instructions](http://www.playframework.com/documentation/2.2.x/Production).

Getting Started
---

Kafka Web Console depends on Apache Zookeeper to retrieve information about brokers, consumer groups, and other entities. Therefore, the first step to configuring the console is to register a Zookeeper server.

![register zookeeper](/img/register-zookeeper.png)

Filling in the form and clicking on *Connect* will register the Zookeeper. Once the console has successfully established a connection with the registered Zookeeper server, it wil retrieve all necessary information about brokers, topics, and consumers.

![zookeepers](/img/zookeepers.png)


Support
---
Please [report](http://github.com/claudemamo/kafka-web-console/issues) bugs or desired features.