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
import jio.console.ConsolePrinter;
import jio.console.ConsolePrograms;
import jio.console.ConsolePrograms.AskForInputParams;
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
  private final Map<String, Gen<?>> generators;
  private static final String USAGE = """
      Publishes messages to a specified Kafka topic using predefined generators for message keys and values.
      The command can interactively prompt for the channel name if not provided.
            
      Usage:
      publish
      publish {channel_name}
            
      Examples:
      publish (prompts the user to input the channel name)
      publish my_channel (publishes messages to the specified channel)
            
      Details:
      - The command uses channel configurations to determine the Kafka producer, topic, and generators for keys and values.
      - If an Avro schema is defined for the channel, the messages will be converted to Avro format before sending.
      - The user will be prompted interactively for input if the required arguments are not provided.
            
      Note:
      Ensure that the Kafka producer for the specified channel is started before using this command.
      Use the command `start-producer {producer_name}` to start the producer if it's not running.
      """;


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
        return ConsolePrograms.ASK_FOR_INPUT(new AskForInputParams("%s\n%s".formatted(channelsInfo,
                                                                                      "Type the channel name (choose one of the above with an `up` Status):"),
                                                                   channel -> ConfigurationQueries.existChannel(conf,
                                                                                                                channel)
                                                                              &&
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
    var channels = conf.getObj(ConfigurationFields.CHANNELS);
    var channel = channels.getObj(channelName);
    var producerName = channel.getStr(ConfigurationFields.PRODUCER);
    var topic = channel.getStr(ConfigurationFields.TOPIC);

    KafkaProducer<Object, Object> producer =
        producers.apply(producerName);
    if (producer == null) {
      return IO.fail(
          new IllegalArgumentException("Producer `%s` not started. Use the command `start-producer %s`".formatted(producerName,
                                                                                                                  producerName)));
    }

    var keyGenName = channel.getStr(ConfigurationFields.KEY_GEN);
    var valueGenName = channel.getStr(ConfigurationFields.VALUE_GEN);

    Object value = generators.get(valueGenName)
                             .apply(valueSeed)
                             .get();
    if (keyGenName == null) {
      return sendValue(value,
                       channelName,
                       producer,
                       topic);
    }
    Object key = generators.get(keyGenName)
                           .apply(keySeed)
                           .get();
    return sendKeyAndValue(key,
                           value,
                           channelName,
                           producer,
                           topic);
  }

  IO<String> sendKeyAndValue(final Object key,
                             final Object value,
                             final String channelName,
                             final KafkaProducer<Object, Object> producer,
                             final String topic) {
    Schema keySchema = avroSchemas.keySchemasPerChannel.get(channelName);
    Schema valueSchema = avroSchemas.valueSchemasPerChannel.get(channelName);
    if (valueSchema != null && keySchema != null) {
      return sendRecordTask(producer,
                            new ProducerRecord<>(topic,
                                                 JsonToAvro.convert(((JsObj) key),
                                                                    keySchema),
                                                 JsonToAvro.convert(((JsObj) value),
                                                                    valueSchema)
                            ));
    } else if (valueSchema != null) {
      return sendRecordTask(producer,
                            new ProducerRecord<>(topic,
                                                 key,
                                                 JsonToAvro.convert(((JsObj) value),
                                                                    valueSchema)
                            ));
    } else if (keySchema != null) {
      return sendRecordTask(producer,
                            new ProducerRecord<>(topic,
                                                 JsonToAvro.convert((JsObj) key,
                                                                    keySchema),
                                                 value
                            ));
    } else {
      return sendRecordTask(producer,
                            new ProducerRecord<>(topic,
                                                 key,
                                                 value
                            ));
    }
  }

  private IO<String> sendRecordTask(final KafkaProducer<Object, Object> producer,
                                    final ProducerRecord<Object, Object> record) {
    return
        ConsolePrinter.PRINT_NEW_LINE(Fun.getMessageSent(record))
                      .then(_ ->
                                IO.effect(() -> producer.send(record))
                                  .map(it -> new KafkaResponse(Instant.now(),
                                                               it.offset(),
                                                               it.partition()).getResponseReceivedMessage(record.topic())));
  }

  IO<String> sendValue(final Object value,
                       final String channelName,
                       final KafkaProducer<Object, Object> producer,
                       final String topic) {

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
