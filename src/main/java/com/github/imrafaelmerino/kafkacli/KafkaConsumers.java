package com.github.imrafaelmerino.kafkacli;

import java.time.Duration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import jsonvalues.JsObj;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;

public class KafkaConsumers implements
                            Function<String, KafkaConsumer<Object, Object>> {


  private final Map<String, KafkaConsumer<Object, Object>> consumers = new HashMap<>();

  ExecutorService service = Executors.newCachedThreadPool();

  public void createConsumer(JsObj kafkaCommonConf,
                             String consumerName,
                             JsObj consumerConf,
                             List<String> topics,
                             Duration pollTimeout) {
    Properties kafkaCommonProps = Fun.toProperties(kafkaCommonConf);

    Properties consumerProps = Fun.toProperties(consumerConf);
    consumerProps.putAll(kafkaCommonProps);

    KafkaConsumer<Object, Object> consumer = new KafkaConsumer<>(consumerProps);

    consumer.subscribe(topics);

    this.consumers.put(consumerName,
                       consumer
                      );
    service.submit(() -> {
      while (true) {
        var records = consumer.poll(pollTimeout);
        if (!records.isEmpty()) {
          synchronized (System.out) {
            System.out.println("\n");
            System.out.printf("""
                                  Received %s records from topics %s at consumer %s
                                  %n""",
                              records.count(),
                              topics,
                              consumerName);

            Iterator<ConsumerRecord<Object, Object>> iterator = records.iterator();
            int n = 1;
            while (iterator.hasNext()) {
              ConsumerRecord<Object, Object> next = iterator.next();
              System.out.printf("""
                                    ----------------  %d -------------------
                                    Offset: %d
                                    Value: %s
                                    Key: %s
                                    Headers: %s
                                    Partition: %d
                                    Timestamp: %d
                                    -----------------------------------------
                                    %n""",
                                n++,
                                next.offset(),
                                next.value(),
                                next.key() != null ? next.key() : "",
                                next.headers() != null ? next.headers() : "",
                                next.partition(),
                                next.timestamp());
            }
          }
        }
      }
    });

  }


  public void commitAsync(String consumerName) {
    this.consumers.get(consumerName)
                  .commitAsync((offsets, exception) -> {
                    synchronized (System.out) {
                      if (exception != null) {
                        System.out.println("Commit request completed");
                      } else {
                        System.out.println("Commit request failed: " + exception.toString());
                      }

                    }
                  });
  }

  public void stopConsumer(String consumerName) {
    this.consumers.get(consumerName)
                  .close();
    this.consumers.remove(consumerName);
  }

  @Override
  public KafkaConsumer<Object, Object> apply(final String consumerName) {
    return consumers.get(consumerName);
  }
}
