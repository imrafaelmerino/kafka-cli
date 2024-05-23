package com.github.imrafaelmerino.kafkacli;


import jio.ExceptionFun;
import jio.cli.ConsoleLogger;
import jio.cli.ConsolePrinter;
import jsonvalues.JsObj;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

class KafkaConsumers implements
        Function<String, KafkaConsumer<Object, Object>> {


    private final Map<String, KafkaConsumer<Object, Object>> consumers;
    private final ExecutorService service;


    KafkaConsumers() {
        consumers = new HashMap<>();
        service = Executors.newCachedThreadPool();
        Runtime.getRuntime()
               .addShutdownHook(new Thread(() -> {
                   for (KafkaConsumer<Object, Object> consumer : consumers.values()) {
                       try {
                           consumer.close();
                       } catch (Exception e) {
                           ConsoleLogger.log("Exception closing consumer during shutdown hook: %s".formatted(e));

                       }

                   }
               }));
    }

    private static void printRecords(final String consumerName,
                                     final List<String> topics,
                                     final ConsumerRecords<Object, Object> records,
                                     final boolean verbose
                                    ) {

        String summary =
                "\nReceived %d records from topics `%s` in consumer `%s`%n".formatted(records.count(),
                                                                                      topics,
                                                                                      consumerName);

        ConsolePrinter.printlnResult(summary);

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

            ConsolePrinter.printlnResult(all.toString());

        }

    }

    boolean isStarted(String consumerName) {
        return consumers.containsKey(consumerName);
    }

    void startConsumer(JsObj kafkaCommonConf,
                       String consumerName,
                       JsObj consumerConf,
                       List<String> topics,
                       Duration pollTimeout,
                       boolean verbose
                      ) {
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
                try {
                    var records = consumer.poll(pollTimeout);
                    if (!records.isEmpty()) {
                        printRecords(consumerName,
                                     topics,
                                     records,
                                     verbose);
                    }
                }
                catch (WakeupException e){
                    consumer.close();
                    break;
                }
                catch (Exception e) {
                    ConsolePrinter.printlnError("Exception while fetching records from Kafka: %s ".formatted(ExceptionFun.findUltimateCause(e)));
                }
            }
        });

        assert unused != null;

    }

    void commitAsync(String consumerName) {
        this.consumers.get(consumerName)
                      .commitAsync((_, exception) -> {
                          if (exception == null) {
                              ConsolePrinter.printlnResult("Commit request from consumer `%s` completed".formatted(consumerName));
                          } else {
                              ConsolePrinter.printlnError("Commit request from consumer `%s` failed: %s".formatted(consumerName,
                                                                                                                   ExceptionFun.findUltimateCause(exception)));
                          }
                      });
    }

    void closeConsumer(String consumerName) {

        KafkaConsumer<Object, Object> consumer = this.consumers.get(consumerName);
        if (consumer != null) {
            consumer.wakeup();
            this.consumers.remove(consumerName);
        }
    }

    @Override
    public KafkaConsumer<Object, Object> apply(final String consumerName) {
        return consumers.get(consumerName);
    }
}
