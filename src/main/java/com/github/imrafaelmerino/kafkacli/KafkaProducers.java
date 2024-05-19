package com.github.imrafaelmerino.kafkacli;


import jio.cli.ConsoleLogger;
import jsonvalues.JsObj;
import org.apache.kafka.clients.producer.KafkaProducer;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;

final class KafkaProducers implements Function<String, KafkaProducer<Object, Object>> {


    private final Map<String, KafkaProducer<Object, Object>> producers;

    KafkaProducers() {
        producers = new HashMap<>();
        Runtime.getRuntime()
               .addShutdownHook(new Thread(() -> {
                   for (KafkaProducer<Object, Object> consumer : producers.values()) {
                       try {
                           consumer.close();
                       } catch (Exception e) {
                           ConsoleLogger.log("Exception closing producer during shutdown hook: %s".formatted(e));
                       }
                   }
               }));
    }

    boolean isStarted(String producerName) {
        return producers.containsKey(producerName);
    }

    void startProducer(JsObj kafkaCommonConf,
                       String producerName,
                       JsObj producerConf
                      ) {

        Properties kafkaCommonProps = Fun.toProperties(kafkaCommonConf);

        Properties producerProps = Fun.toProperties(producerConf);
        producerProps.putAll(kafkaCommonProps);

        this.producers.put(producerName,
                           new KafkaProducer<>(producerProps));

    }

    void closeProducer(String producerName) {
        KafkaProducer<Object, Object> producer = this.producers.get(producerName);
        if (producer != null) {
            producer
                    .close();
            this.producers.remove(producerName);
        }
    }


    @Override
    public KafkaProducer<Object, Object> apply(final String name) {

        return producers.get(name);

    }
}
