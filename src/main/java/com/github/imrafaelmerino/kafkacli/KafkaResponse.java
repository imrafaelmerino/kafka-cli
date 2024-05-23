package com.github.imrafaelmerino.kafkacli;

import java.time.Instant;

record KafkaResponse(long timestamp,
                     long offset,
                     int partition) {


    String getResponseReceivedMessage(String topic) {
        return STR."""
        Publish response received:
          Topic: \{topic}
          Offset: \{offset}
          Partition: \{partition}
          Timestamp: \{Instant.ofEpochMilli(timestamp)}""";
    }


}
