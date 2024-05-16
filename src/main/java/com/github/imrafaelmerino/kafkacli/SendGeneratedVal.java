package com.github.imrafaelmerino.kafkacli;

import fun.gen.Gen;
import java.time.Instant;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.random.RandomGenerator;
import jio.IO;
import jio.console.Command;
import jio.console.State;
import jsonvalues.JsObj;
import jsonvalues.spec.JsonToAvro;
import org.apache.avro.Schema;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

public class SendGeneratedVal extends Command {

  private final static RandomGenerator keySeed = new Random();
  private final static RandomGenerator valueSeed = new Random();
  private final KafkaProducers producers;
  private final AvroSchemas avroSchemas;
  Map<String, Gen<?>> generators;
  private static String USAGE = """ 
      Usage: \
      send-gen channel valueGen \
      send-gen channel keyGen valueGen""";

  public SendGeneratedVal(final Map<String, Gen<?>> generators,
                          final KafkaProducers producers,
                          final AvroSchemas avroSchemas) {
    super("send-gen",
          "",
          args -> args[0].equals("send-gen"));
    this.generators = generators;
    this.producers = producers;
    this.avroSchemas = avroSchemas;
  }

  @Override
  public Function<String[], IO<String>> apply(final JsObj conf,
                                              final State state) {
    return args -> {
      String channelName = args[1];

      JsObj channels = conf.getObj("channels");
      if (!channels.containsKey(channelName)) {
        return IO.fail(new IllegalArgumentException("channel `%s` not found in configuration. Use `ls-channels` to see "
                                                    + "available channels"));
      }
      JsObj channel = channels.getObj(channelName);
      String producerName = channel.getStr("producer");
      String topic = channel.getStr("topic");
      KafkaProducer<Object, Object> producer =
          producers.apply(producerName);
      if (producer == null) {
        return IO.fail(new IllegalArgumentException(String.format("producer `%s` not started. Use the "
                                                                  + "command "
                                                                  + "`create-producer %s`",
                                                                  producerName,
                                                                  producerName)));
      }

      if (args.length < 3) {
        IO.fail(new IllegalArgumentException(USAGE));
      }

      if (args.length == 3) {
        String valueGenName = args[2];
        if (!generators.containsKey(valueGenName)) {
          return IO.fail(new IllegalArgumentException(String.format("value generator `%s` not defined.",
                                                                    valueGenName)));
        }
        Object value = generators.get(valueGenName)
                                 .apply(valueSeed)
                                 .get();
        //si tiene schema, pasamos a json object y a generic containter
        Schema valueSchema = avroSchemas.valueSchemasPerChannel.get(channelName);

        if (valueSchema != null) {
          //TODO
          //validate  channel has the proper serializer
          //validate value is an object

          return IO.task(() -> producer.send(new ProducerRecord<>(topic,
                                                                  JsonToAvro.convert(((JsObj) value),
                                                                                     valueSchema))
                                            )
                                       .get()
                        )
                   .map(it -> new KafkaResponse(Instant.now(),
                                                it.offset(),
                                                it.partition()).toString()
                       );

        } else {
          return IO.task(() -> producer.send(new ProducerRecord<>(topic,
                                                                  value)
                                            )
                                       .get()
                        )
                   .map(it -> new KafkaResponse(Instant.now(),
                                                it.offset(),
                                                it.partition()).toString());
        }


      } else if (args.length == 4) {
        String keyGenName = args[2];
        if (!generators.containsKey(keyGenName)) {
          return IO.fail(new IllegalArgumentException(String.format("key generator `%s` not defined.",
                                                                    keyGenName)));
        }
        String valueGenName = args[3];
        if (!generators.containsKey(valueGenName)) {
          return IO.fail(new IllegalArgumentException(String.format("value generator `%s` not defined.",
                                                                    valueGenName)));
        }

        Schema keySchema = avroSchemas.keySchemasPerChannel.get(channelName);
        Schema valueSchema = avroSchemas.valueSchemasPerChannel.get(channelName);
        if (valueSchema != null && keySchema != null) {
          return IO.task(() -> producer.send(new ProducerRecord<>(topic,
                                                                  JsonToAvro.convert(((JsObj) generators.get(keyGenName)
                                                                                                        .apply(keySeed)
                                                                                                        .get()),
                                                                                     keySchema),
                                                                  JsonToAvro.convert(((JsObj) generators.get(valueGenName)
                                                                                                        .apply(valueSeed)
                                                                                                        .get()),
                                                                                     valueSchema)
                                             )
                                            )
                                       .get()
                        )
                   .map(it -> new KafkaResponse(Instant.now(),
                                                it.offset(),
                                                it.partition()).toString());
        } else if (valueSchema != null && keySchema == null) {
          return IO.task(() -> producer.send(new ProducerRecord<>(topic,
                                                                  generators.get(keyGenName)
                                                                            .apply(keySeed)
                                                                            .get(),
                                                                  JsonToAvro.convert(((JsObj) generators.get(valueGenName)
                                                                                                        .apply(valueSeed)
                                                                                                        .get()),
                                                                                     valueSchema)
                                             )
                                            )
                                       .get()
                        )
                   .map(it -> new KafkaResponse(Instant.now(),
                                                it.offset(),
                                                it.partition()).toString());
        } else if (valueSchema == null && keySchema != null) {
          return IO.task(() -> producer.send(new ProducerRecord<>(topic,
                                                                  JsonToAvro.convert(((JsObj) generators.get(keyGenName)
                                                                                                        .apply(keySeed)
                                                                                                        .get()),
                                                                                     keySchema),
                                                                  generators.get(valueGenName)
                                                                            .apply(valueSeed)
                                                                            .get()
                                             )
                                            )
                                       .get()
                        )
                   .map(it -> new KafkaResponse(Instant.now(),
                                                it.offset(),
                                                it.partition()).toString());
        } else {
          return IO.task(() -> producer.send(new ProducerRecord<>(topic,
                                                                  generators.get(keyGenName)
                                                                            .apply(keySeed)
                                                                            .get(),
                                                                  generators.get(valueGenName)
                                                                            .apply(valueSeed)
                                                                            .get()
                                             )
                                            )
                                       .get()
                        )
                   .map(it -> new KafkaResponse(Instant.now(),
                                                it.offset(),
                                                it.partition()).toString()
                       );
        }


      } else {

        return IO.fail(new IllegalArgumentException(USAGE));
      }

    };
  }
}
