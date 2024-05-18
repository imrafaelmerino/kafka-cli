package com.github.imrafaelmerino.kafkacli;

import fun.gen.Gen;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.random.RandomGenerator;
import jio.IO;
import jio.ListExp;
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

class PublishFileCommand extends Command {

  private final static RandomGenerator keySeed = new Random();
  private final static RandomGenerator valueSeed = new Random();
  private final KafkaProducers producers;
  private final AvroSchemas avroSchemas;
  Map<String, Gen<?>> generators;
  private static final String USAGE = """   
      Publishes records from a file to the specified Kafka channel.

      Usage: publish-file {channel} {file_path}
         
      {channel}: The name of the Kafka channel to publish to. Choose one of the channels listed below.
         
      {file_path}: The absolute path of the file containing records to publish.
         
      The file should have the following format:
        - Each record should be separated by a new line.
        - Each record consists of one or more lines, starting with either "headers:", "key:", or "value:".
        - headers and key are optional.
        - headers must be a Json object.
         
      Examples:
        publish-file (prompts the user to input the channel name and file absolute path)
        publish-file channel1 /path/to/another_records.txt
      """;


  public PublishFileCommand(final Map<String, Gen<?>> generators,
                            final KafkaProducers producers,
                            final AvroSchemas avroSchemas) {
    super("publish-file",
          USAGE,
          args -> args[0].equals("publish-file"));
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
        return ConsolePrograms.ASK_FOR_PAIR(new AskForInputParams("%s\n%s".formatted(channelsInfo,
                                                                                       "Type the channel name (choose one of the above with an `up` Status):".formatted()),
                                                                  channel -> ConfigurationQueries.existChannel(conf,
                                                                                                               channel)
                                                                             &&
                                                                             ConfigurationQueries.isChannelUp(conf,
                                                                                                              channel,
                                                                                                              producers),
                                                                  "Invalid channel name.",
                                                                  RetryPolicies.limitRetries(3)),
                                            new AskForInputParams("%s\n%s".formatted(channelsInfo,
                                                                                       "Type the file absolute path:"),
                                                                  path -> Files.exists(Path.of(path)),
                                                                  "File doesn't exist",
                                                                  RetryPolicies.limitRetries(3))
                                           )
                              .then(pair -> sendFile(conf,
                                                     pair.first(),
                                                     pair.second())
                                   );

      } else {
        String channel = args[1];
        String path = args[2];

        return sendFile(conf,
                        channel,
                        path);

      }
    };

  }

  private IO<String> sendFile(JsObj conf,
                              String channelName,
                              String path) {
    JsObj channels = conf.getObj(ConfigurationFields.CHANNELS);
    JsObj channel = channels.getObj(channelName);
    String producerName = channel.getStr(ConfigurationFields.PRODUCER);
    String topic = channel.getStr(ConfigurationFields.TOPIC);

    KafkaProducer<Object, Object> producer =
        producers.apply(producerName);
    if (producer == null) {
      return IO.fail(new IllegalArgumentException(String.format("Producer `%%s` not started. Use the command `start-producer %%s`".formatted(),
                                                                producerName,
                                                                producerName)));
    }

    var records = FileParser.parseRecordsFromFile(path);

    ListExp<String> list = ListExp.seq();
    for (Message record : records) {

      if (record.key() == null) {
        list.append(sendValue(record.value(),
                              channelName,
                              producer,
                              topic));
      }

      list.append(sendKeyAndValue(record.key(),
                                  record.value(),
                                  channelName,
                                  producer,
                                  topic));


    }

    return list.map(it -> String.join("\n",
                                      it));


  }

  IO<String> sendKeyAndValue(final String key,
                             final String value,
                             final String channelName,
                             final KafkaProducer<Object, Object> producer,
                             final String topic) {

    Schema keySchema = avroSchemas.keySchemasPerChannel.get(channelName);
    Schema valueSchema = avroSchemas.valueSchemasPerChannel.get(channelName);
    if (valueSchema != null && keySchema != null) {
      return sendRecordTask(producer,
                            new ProducerRecord<>(topic,
                                                 JsonToAvro.convert(JsObj.parse(key),
                                                                    keySchema),
                                                 JsonToAvro.convert(JsObj.parse(value),
                                                                    valueSchema)
                            ));
    } else if (valueSchema != null) {
      return sendRecordTask(producer,
                            new ProducerRecord<>(topic,
                                                 key,
                                                 JsonToAvro.convert(JsObj.parse(value),
                                                                    valueSchema)
                            ));
    } else if (keySchema != null) {
      return sendRecordTask(producer,
                            new ProducerRecord<>(topic,
                                                 JsonToAvro.convert(JsObj.parse(key),
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
    return ConsolePrinter.PRINT_NEW_LINE(Fun.getMessageSent(record))
                         .then(_ ->
                                   IO.effect(() -> producer.send(record
                                                                )
                                            )
                                     .map(it -> new KafkaResponse(Instant.now(),
                                                                  it.offset(),
                                                                  it.partition()
                                     ).getResponseReceivedMessage(record.topic()))
                              );
  }

  IO<String> sendValue(final String value,
                       final String channelName,
                       final KafkaProducer<Object, Object> producer,
                       final String topic) {

    Schema valueSchema = avroSchemas.valueSchemasPerChannel.get(channelName);

    if (valueSchema != null) {
      return sendRecordTask(producer,
                            new ProducerRecord<>(topic,
                                                 JsonToAvro.convert(JsObj.parse(value),
                                                                    valueSchema)));

    } else {
      return sendRecordTask(producer,
                            new ProducerRecord<>(topic,
                                                 value));
    }
  }
}
