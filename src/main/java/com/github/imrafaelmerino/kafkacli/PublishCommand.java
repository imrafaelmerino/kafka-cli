package com.github.imrafaelmerino.kafkacli;

import fun.gen.Gen;
import java.time.Instant;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.random.RandomGenerator;
import jio.IO;
import jio.RetryPolicies;
import jio.console.Command;
import jio.console.Programs;
import jio.console.Programs.AskForInputParams;
import jio.console.State;
import jsonvalues.JsObj;
import jsonvalues.spec.JsonToAvro;
import org.apache.avro.Schema;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

class PublishCommand extends Command {

  private final static RandomGenerator keySeed = new Random();
  private final static RandomGenerator valueSeed = new Random();
  private final KafkaProducers producers;
  private final AvroSchemas avroSchemas;
  Map<String, Gen<?>> generators;
  private static final String USAGE = """ 
      Usage: \
      send-gen \
      send-gen channel""";

  public PublishCommand(final Map<String, Gen<?>> generators,
                        final KafkaProducers producers,
                        final AvroSchemas avroSchemas) {
    super("publish",
          USAGE,
          args -> args[0].equals("publish"));
    this.generators = generators;
    this.producers = producers;
    this.avroSchemas = avroSchemas;
  }

  @Override
  public Function<String[], IO<String>> apply(final JsObj conf,
                                              final State state) {
    return args -> {

      if (args.length == 1) {
        String channelsInfo = ConfigurationQueries.getChannelsInfo(conf,
                                                                   producers);
        return Programs.ASK_FOR_INPUT(new AskForInputParams("%s\n\n%s".formatted(channelsInfo,
                                                                                 "Type the channel name (choose one of "
                                                                                 + "the "
                                                                                 + "above with an `up` Status):"),
                                                            channel -> ConfigurationQueries.existChannel(conf,
                                                                                                         channel) &&
                                                                       ConfigurationQueries.isChannelUp(conf,
                                                                                                        channel,
                                                                                                        producers),
                                                            "Invalid channel name.",
                                                            RetryPolicies.limitRetries(3))
                                     )
                       .then(channel -> sendGenerated(conf,
                                                      channel)
                            );

      } else {
        String channelName = args[1];

        return sendGenerated(conf,
                             channelName);

      }
    };

  }

  private IO<String> sendGenerated(final JsObj conf,
                                   final String channelName) {
    JsObj channels = conf.getObj(ConfigurationFields.CHANNELS);
    JsObj channel = channels.getObj(channelName);
    String producerName = channel.getStr(ConfigurationFields.PRODUCER);
    String topic = channel.getStr(ConfigurationFields.TOPIC);

    KafkaProducer<Object, Object> producer =
        producers.apply(producerName);
    if (producer == null) {
      return IO.fail(new IllegalArgumentException(String.format("Producer `%s` not started. Use the "
                                                                + "command "
                                                                + "`start-producer %s`",
                                                                producerName,
                                                                producerName)));
    }

    String keyGenName = channel.getStr(ConfigurationFields.KEY_GEN);
    String valueGenName = channel.getStr(ConfigurationFields.VALUE_GEN);

    if (keyGenName == null) {
      return sendValue(valueGenName,
                       channelName,
                       producer,
                       topic);
    }

    return sendKeyAndValue(keyGenName,
                           valueGenName,
                           channelName,
                           producer,
                           topic);
  }

  private IO<String> sendKeyAndValue(final String keyGenName,
                                     final String valueGenName,
                                     final String channelName,
                                     final KafkaProducer<Object, Object> producer,
                                     final String topic) {
    Schema keySchema = avroSchemas.keySchemasPerChannel.get(channelName);
    Schema valueSchema = avroSchemas.valueSchemasPerChannel.get(channelName);
    if (valueSchema != null && keySchema != null) {
      return sendRecordTask(producer,
                            new ProducerRecord<>(topic,
                                                 JsonToAvro.convert(((JsObj) generators.get(keyGenName)
                                                                                       .apply(keySeed)
                                                                                       .get()),
                                                                    keySchema),
                                                 JsonToAvro.convert(((JsObj) generators.get(valueGenName)
                                                                                       .apply(valueSeed)
                                                                                       .get()),
                                                                    valueSchema)
                            ));
    } else if (valueSchema != null) {
      return sendRecordTask(producer,
                            new ProducerRecord<>(topic,
                                                 generators.get(keyGenName)
                                                           .apply(keySeed)
                                                           .get(),
                                                 JsonToAvro.convert(((JsObj) generators.get(valueGenName)
                                                                                       .apply(valueSeed)
                                                                                       .get()),
                                                                    valueSchema)
                            ));
    } else if (keySchema != null) {
      return sendRecordTask(producer,
                            new ProducerRecord<>(topic,
                                                 JsonToAvro.convert(((JsObj) generators.get(keyGenName)
                                                                                       .apply(keySeed)
                                                                                       .get()),
                                                                    keySchema),
                                                 generators.get(valueGenName)
                                                           .apply(valueSeed)
                                                           .get()
                            ));
    } else {
      return sendRecordTask(producer,
                            new ProducerRecord<>(topic,
                                                 generators.get(keyGenName)
                                                           .apply(keySeed)
                                                           .get(),
                                                 generators.get(valueGenName)
                                                           .apply(valueSeed)
                                                           .get()
                            ));
    }
  }

  private IO<String> sendRecordTask(final KafkaProducer<Object, Object> producer,
                                    final ProducerRecord<Object, Object> record) {
    return IO.task(() -> producer.send(record
                                      )
                                 .get()
                  )
             .map(it -> new KafkaResponse(Instant.now(),
                                          it.offset(),
                                          it.partition()).toString());
  }

  private IO<String> sendValue(final String genName,
                               final String channelName,
                               final KafkaProducer<Object, Object> producer,
                               final String topic) {

    Object value = generators.get(genName)
                             .apply(valueSeed)
                             .get();
    Schema valueSchema = avroSchemas.valueSchemasPerChannel.get(channelName);

    if (valueSchema != null) {
      return sendRecordTask(producer,
                            new ProducerRecord<>(topic,
                                                 JsonToAvro.convert(((JsObj) value),
                                                                    valueSchema)));

    } else {
      return sendRecordTask(producer,
                            new ProducerRecord<>(topic,
                                                 value));
    }
  }
}
