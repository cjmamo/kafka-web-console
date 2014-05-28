/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Copyright 2014 Claude Mamo
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
 */

package kafka.javaapi.consumer;

import kafka.consumer.TopicFilter;
import kafka.message.MessageAndMetadata;
import kafka.serializer.Decoder;
import org.I0Itec.zkclient.ZkClient;
import scala.Function1;
import scala.Unit;

import java.util.Map;

public interface AsyncConsumerConnector {
    /**
     * Create a list of MessageStreams of type T for each topic.
     *
     * @param topicCountMap a map of (topic, callback) pair
     * @param keyDecoder   a decoder that decodes the message key
     * @param valueDecoder a decoder that decodes the message itself
     * @return a map of (topic, list of  KafkaStream) pairs.
     * The number of items in the list is #streams. Each stream supports
     * an iterator over message/metadata pairs.
     */
    public <K, V> void
    createMessageStreams(Map<EventHandler<K, V>, Integer> topicCountMap, Decoder<K> keyDecoder, Decoder<V> valueDecoder);

    public void createMessageStreams(Map<EventHandler<byte[], byte[]>, Integer> topicCountMap);

    /**
     * Create a list of MessageAndTopicStreams containing messages of type T.
     *
     * @param topicFilter  a TopicFilter that specifies which topics to
     *                     subscribe to (encapsulates a whitelist or a blacklist).
     * @param numStreams   the number of message streams to return.
     * @param keyDecoder   a decoder that decodes the message key
     * @param valueDecoder a decoder that decodes the message itself
     * @return a list of KafkaStream. Each stream supports an
     * iterator over its MessageAndMetadata elements.
     */
    public <K, V> void
    createMessageStreamsByFilter(TopicFilter topicFilter, int numStreams, Decoder<K> keyDecoder, Decoder<V> valueDecoder,
                                 Function1<MessageAndMetadata<K, V>, Unit> cb);

    public void createMessageStreamsByFilter(TopicFilter topicFilter, int numStreams, Function1<MessageAndMetadata<byte[], byte[]>, Unit> cb);

    public void createMessageStreamsByFilter(TopicFilter topicFilter, Function1<MessageAndMetadata<byte[], byte[]>, Unit> cb);

    /**
     * Commit the offsets of all broker partitions connected by this connector.
     */
    public void commitOffsets();

    /**
     * Shut down the connector
     */
    public void shutdown();
}
