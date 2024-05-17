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
import jio.ExceptionFun;
import jsonvalues.JsObj;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

class KafkaConsumers implements
                     Function<String, KafkaConsumer<Object, Object>> {


  public KafkaConsumers() {
    consumers = new HashMap<>();
    service = Executors.newCachedThreadPool();
    Runtime.getRuntime()
           .addShutdownHook(new Thread(() -> {
             if (consumers != null) {
               for (KafkaConsumer<Object, Object> consumer : consumers.values()) {
                 try {
                   consumer.close();
                 } catch (Exception e) {

                 }
               }
             }
           }));
  }

  private final Map<String, KafkaConsumer<Object, Object>> consumers;


  ExecutorService service;

  public boolean isStarted(String consumerName) {
    return consumers.containsKey(consumerName);
  }

  public void startConsumer(JsObj kafkaCommonConf,
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
            printRecords(consumerName,
                         topics,
                         records);
          }
        }
      }
    });

  }

  private static void printRecords(final String consumerName,
                                   final List<String> topics,
                                   final ConsumerRecords<Object, Object> records) {
    StringBuilder output = new StringBuilder();

    output.append("\n");
    output.append(String.format("Received %d records from topics `%s` in consumer `%s`%n",
                                records.count(),
                                topics,
                                consumerName));

    Iterator<ConsumerRecord<Object, Object>> iterator = records.iterator();
    int n = 1;
    while (iterator.hasNext()) {
      ConsumerRecord<Object, Object> next = iterator.next();
      output.append(String.format("Record %d:%n",
                                  n++));
      output.append(String.format("  Offset: %d%n",
                                  next.offset()));
      output.append(String.format("  Key: %s%n",
                                  next.key() != null ? next.key() : "null"));
      output.append(String.format("  Value: %s%n",
                                  next.value()));
      output.append(String.format("  Partition: %d%n",
                                  next.partition()));
      output.append(String.format("  Timestamp: %d%n",
                                  next.timestamp()));
      output.append("\n");
    }

    System.out.print(output.toString());
  }


  public void commitAsync(String consumerName) {
    this.consumers.get(consumerName)
                  .commitAsync((_, exception) -> {
                    if (exception != null) {
                      Fun.lockedPrintln("Commit request from consumer `%s` completed".formatted(consumerName));
                    } else {
                      Fun.lockedPrintln(STR."Commit request from consumer `\{consumerName}` failed: \{ExceptionFun.findUltimateCause(exception)
                                                                                                                  .toString()}");
                    }
                  });
  }

  public void stopConsumer(String consumerName) {

    KafkaConsumer<Object, Object> consumer = this.consumers.get(consumerName);
    if (consumer != null) {
      consumer.close();
      this.consumers.remove(consumerName);
    }
  }

  @Override
  public KafkaConsumer<Object, Object> apply(final String consumerName) {
    return consumers.get(consumerName);
  }
}
