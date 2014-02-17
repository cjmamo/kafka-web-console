package kafka.javaapi.consumer

import kafka.message.MessageAndMetadata


class EventHandler[K, V](val topic: String,
                         val cb: MessageAndMetadata[K, V] => Unit)

