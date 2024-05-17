package com.github.imrafaelmerino.kafkacli;

import java.time.Instant;

 record KafkaResponse(Instant receivedAt,
                            long offset,
                            int partition) {

  @Override
  public String toString() {
    return
        STR."""
        offset=\{offset}, partition=\{partition}, receivedAt=\{receivedAt}""";
  }
}
