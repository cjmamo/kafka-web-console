/**
* Copyright (C) 2014 the original author or authors.
* See the LICENCE.txt file distributed with this work for additional
* information regarding copyright ownership.
*
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy of
* the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations under
* the License.
 *
 * Author: Andrew Zafft | azafty468
*/

package kafka.consumer;

import kafka.api.FetchRequest;
import kafka.api.FetchRequestBuilder;
import kafka.api.PartitionOffsetRequestInfo;
import kafka.common.TopicAndPartition;
import kafka.javaapi.*;
import kafka.javaapi.consumer.SimpleConsumer;
import kafka.message.MessageAndOffset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.*;

public class LowLevelConsumer {
    static final Logger log = LoggerFactory.getLogger(LowLevelConsumer.class);

    private SimpleConsumer consumer;
    private List<String> replicaBrokers = new ArrayList<>();
    private String leadBroker;
    private String clientName;
    private final String topic;
    private final int partition;
    private final int port;

    public LowLevelConsumer(String topic, int partition, String seedBroker, int port, boolean findLeader) {
        this.topic = topic;
        this.partition = partition;
        List<String> seedBrokers = new ArrayList<>();
        seedBrokers.add(seedBroker);
        this.port = port;
        replicaBrokers = new ArrayList<>();

        // find the meta data about the topic and partition we are interested in
        PartitionMetadata metadata = findLeader(seedBrokers, port, topic, partition);
        if (metadata == null) {
            System.out.println("Can't find metadata for Topic and Partition. Exiting");
            return;
        }
        if (metadata.leader() == null) {
            System.out.println("Can't find Leader for Topic and Partition. Exiting");
            return;
        }

        clientName = "Client_" + topic + "_" + partition;
        if (findLeader) {
            leadBroker = metadata.leader().host();
            consumer = new SimpleConsumer(leadBroker, port, 1000000, 64 * 1024, clientName);
        }
        else {
            leadBroker = seedBroker;
            consumer = new SimpleConsumer(leadBroker, port, 1000000, 64 * 1024, clientName);
        }
    }

    public long startingOffset() {
        long offset;
        try {
            offset = getLastOffset(consumer, topic, partition, kafka.api.OffsetRequest.EarliestTime(), clientName);
        } catch(Exception e) {
            e.printStackTrace();
            return 0L;
        }
        return offset;
    }

    public long endingOffset() {
        long offset;
        try {
            offset = getLastOffset(consumer, topic, partition, kafka.api.OffsetRequest.LatestTime(), clientName);
        } catch(Exception e) {
            e.printStackTrace();
            return 0L;
        }
        return offset;
    }

    public Set<String> retrieveData(long offsetToRead, int bytesToRead) {
        long readOffset = offsetToRead;
        Set<String> messages = new LinkedHashSet<>();
        FetchRequest req = new FetchRequestBuilder()
                .clientId(clientName)
                .addFetch(topic, partition, readOffset, bytesToRead)
                .build();
        FetchResponse fetchResponse = consumer.fetch(req);

        if (fetchResponse.hasError()) {
            short code = fetchResponse.errorCode(topic, partition);
            log.error("Error fetching data from the Broker:" + leadBroker + " Reason: " + code);
            return messages;
        }

        for (MessageAndOffset messageAndOffset : fetchResponse.messageSet(topic, partition)) {
            long currentOffset = messageAndOffset.offset();
            if (currentOffset < readOffset) {
                log.error("Found an old offset: " + currentOffset + " Expecting: " + readOffset);
                continue;
            }
            readOffset = messageAndOffset.nextOffset();
            ByteBuffer payload = messageAndOffset.message().payload();

            byte[] bytes = new byte[payload.limit()];
            payload.get(bytes);
            try {
                messages.add(new String(bytes, "UTF-8"));
            } catch (Exception e) {
                log.error("Failed to append message.", e);
            }
        }
        return messages;
    }

    public void closeConsumers() {
        if(consumer != null) consumer.close();
    }

    private static long getLastOffset(SimpleConsumer consumer, String topic, int partition, long whichTime, String clientName) {
        TopicAndPartition topicAndPartition = new TopicAndPartition(topic, partition);
        Map<TopicAndPartition, PartitionOffsetRequestInfo> requestInfo = new HashMap<>();
        requestInfo.put(topicAndPartition, new PartitionOffsetRequestInfo(whichTime, 1));
        kafka.javaapi.OffsetRequest request = new kafka.javaapi.OffsetRequest(
                requestInfo, kafka.api.OffsetRequest.CurrentVersion(), clientName);
        OffsetResponse response = consumer.getOffsetsBefore(request);

        if (response.hasError()) {
            log.error("Error fetching data Offset Data the Broker. Reason: " + response.errorCode(topic, partition));
            return 0;
        }
        long[] offsets = response.offsets(topic, partition);
        return offsets[0];
    }

    private String findNewLeader(String oldLeader, String topic, int partition, int port) throws Exception {
        for (int i = 0; i < 3; i++) {
            PartitionMetadata metadata = findLeader(replicaBrokers, port, topic, partition);
            if (metadata != null &&
                    metadata.leader() != null &&
                    !(oldLeader.equalsIgnoreCase(metadata.leader().host()) && i == 0)) {

                // first time through if the leader hasn't changed give ZooKeeper a second to recover
                // second time, assume the broker did recover before failover, or it was a non-Broker issue

                return metadata.leader().host();
            }

            try {
                Thread.sleep(500);
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            }
        }
        log.error("Unable to find new leader after Broker failure. Exiting");
        throw new Exception("Unable to find new leader after Broker failure. Exiting");
    }

    private PartitionMetadata findLeader(List<String> seedBrokers, int port, String topic, int partition) {
        PartitionMetadata returnMetaData = null;
        loop:
        for (String seed : seedBrokers) {
            SimpleConsumer tempConsumer = null;
            try {
                tempConsumer = new SimpleConsumer(seed, port, 100000, 64 * 1024, "leaderLookup");
                List<String> topics = Collections.singletonList(topic);
                TopicMetadataRequest request= new TopicMetadataRequest(topics);
                kafka.javaapi.TopicMetadataResponse resp = tempConsumer.send(request);

                List<TopicMetadata> metaData = resp.topicsMetadata();
                for (TopicMetadata item : metaData) {
                    for (PartitionMetadata part : item.partitionsMetadata()) {
                        if (part.partitionId() == partition) {
                            returnMetaData = part;
                            break loop;
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Error communicating with Broker [" + seed + "] to find Leader for [" + topic
                        + ", " + partition + "]", e);
            } finally {
                if (tempConsumer != null)
                    tempConsumer.close();
            }
        }
        if (returnMetaData != null) {
            replicaBrokers.clear();
            for (kafka.cluster.Broker replica : returnMetaData.replicas()) {
                replicaBrokers.add(replica.host());
            }
        }
        return returnMetaData;
    }
}