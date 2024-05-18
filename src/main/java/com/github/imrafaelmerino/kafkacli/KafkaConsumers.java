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
import jio.console.ConsoleLogger;
import jio.console.ConsolePrinter;
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
                            Duration pollTimeout,
                            boolean verbose) {
    Properties kafkaCommonProps = Fun.toProperties(kafkaCommonConf);

    Properties consumerProps = Fun.toProperties(consumerConf);
    consumerProps.putAll(kafkaCommonProps);

    KafkaConsumer<Object, Object> consumer = new KafkaConsumer<>(consumerProps);

    consumer.subscribe(topics);

    this.consumers.put(consumerName,
                       consumer
                      );
    var unused = service.submit(() -> {
      while (true) {
        var records = consumer.poll(pollTimeout);
        if (!records.isEmpty()) {
          printRecords(consumerName,
                       topics,
                       records,
                       verbose);
        }
      }
    });

  }

  private static void printRecords(final String consumerName,
                                   final List<String> topics,
                                   final ConsumerRecords<Object, Object> records,
                                   final boolean verbose) {
    StringBuilder summary = new StringBuilder();

    summary.append("\n");
    summary.append(String.format("Received %d records from topics `%s` in consumer `%s`%n",
                                 records.count(),
                                 topics,
                                 consumerName));

    ConsolePrinter.synchronizedPrintln(summary.toString());
    ConsoleLogger.log(summary.toString());

    if (verbose) {
      StringBuilder all = new StringBuilder();

      Iterator<ConsumerRecord<Object, Object>> iterator = records.iterator();
      int n = 1;
      while (iterator.hasNext()) {
        ConsumerRecord<Object, Object> next = iterator.next();
        all.append(String.format("Record %d:%n",
                                 n++));
        all.append(String.format("  Offset: %d%n",
                                 next.offset()));
        all.append(String.format("  Key: %s%n",
                                 next.key() != null ? next.key() : "null"));
        all.append(String.format("  Value: %s%n",
                                 next.value()));
        all.append(String.format("  Partition: %d%n",
                                 next.partition()));
        all.append(String.format("  Timestamp: %d%n",
                                 next.timestamp()));
        all.append("\n");
      }

      ConsolePrinter.synchronizedPrintln(all.toString());
      ConsoleLogger.log(all.toString());

    }

  }


  public void commitAsync(String consumerName) {
    this.consumers.get(consumerName)
                  .commitAsync((_, exception) -> {
                    if (exception == null) {
                      ConsolePrinter.synchronizedPrintln("Commit request from consumer `%s` completed".formatted(consumerName));
                    } else {
                      ConsolePrinter.synchronizedPrintln("Commit request from consumer `%s` failed: %s".formatted(consumerName,
                                                                                                                  ExceptionFun.findUltimateCause(exception)));
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
