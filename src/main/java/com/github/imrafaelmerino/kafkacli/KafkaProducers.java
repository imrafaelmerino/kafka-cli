package com.github.imrafaelmerino.kafkacli;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;
import jsonvalues.JsObj;
import org.apache.kafka.clients.producer.KafkaProducer;

public final class KafkaProducers implements
                                  Function<String, KafkaProducer<Object, Object>> {


  private final Map<String, KafkaProducer<Object, Object>> producers = new HashMap<>();


  public void createProducer(JsObj kafkaCommonConf,
                             String producerName,
                             JsObj producerConf) {

    Properties kafkaCommonProps = Fun.toProperties(kafkaCommonConf);

    Properties producerProps = Fun.toProperties(producerConf);
    producerProps.putAll(kafkaCommonProps);

    this.producers.put(producerName,
                       new KafkaProducer<>(producerProps));

  }

  public void closeProducer(String producerName) {
    this.producers.get(producerName)
                  .close();
    this.producers.remove(producerName);
  }


  @Override
  public KafkaProducer<Object, Object> apply(final String name) {

    return producers.get(name);

  }
}
