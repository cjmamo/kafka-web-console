/**
 * Copyright (C) 2014 the original author or authors and Enernoc Inc.
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
 * Authors: Andrew Zafft | azafty468, Isaac Banner | ibanner56
 */

package kafka.consumer;

import kafka.api.PartitionOffsetRequestInfo;
import kafka.common.TopicAndPartition;
import kafka.javaapi.*;
import kafka.javaapi.consumer.SimpleConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;
import java.util.HashMap;

public class LowLevelConsumer {
    static final Logger log = LoggerFactory.getLogger(LowLevelConsumer.class);

    private SimpleConsumer consumer;
    private String leadBroker;
    private String clientName;
    private final String topic;
    private final int partition;
    private final int port;

    public LowLevelConsumer(String topic, int partition, String seedBroker, int port) {
        this.topic = topic;
        this.partition = partition;
        this.leadBroker = seedBroker;
        this.port = port;
        clientName = "Client_" + topic + "_" + partition;

        consumer = new SimpleConsumer(leadBroker, port, 1000000, 64 * 1024, clientName);
    }

    public long startingOffset() {
        long offset;
        try {
            offset = getLastOffset(consumer, topic, partition, kafka.api.OffsetRequest.EarliestTime(), clientName);
        } catch (Exception e) {
            e.printStackTrace();
            return 0L;
        }
        return offset;
    }

    public long endingOffset() {
        long offset;
        try {
            offset = getLastOffset(consumer, topic, partition, kafka.api.OffsetRequest.LatestTime(), clientName);
        } catch (Exception e) {
            e.printStackTrace();
            return 0L;
        }
        return offset;
    }

    public void closeConsumers() {
        if (consumer != null) consumer.close();
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
}
