package com.github.imrafaelmerino.kafkacli;

import static com.github.imrafaelmerino.kafkacli.ConfigurationFields.TOPICS;

import fun.gen.Gen;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import jio.IO;
import jio.console.Command;
import jio.console.Console;
import jio.console.GenerateCommand;
import jio.console.State;
import jsonvalues.JsObj;
import jsonvalues.JsPath;
import jsonvalues.spec.JsObjSpecParser;

public class CLI {

  final Map<String, Gen<?>> generators;

  public CLI(final Map<String, Gen<?>> generators) {
    this.generators = generators;
  }

  public CLI() {
    this(new HashMap<>());
  }


  public static final String LS_PRODUCERS_COMMAND = "list-producers";
  public static final String LS_CONSUMERS_COMMAND = "list-consumers";
  public static final String LS_CHANNELS_COMMAND = "list-channels";
  public static final String CREATE_PRODUCER_COMMAND = "create-producer";
  public static final String CREATE_CONSUMER_COMMAND = "create-consumer";
  public static final String CLOSE_CONSUMER_COMMAND = "close-consumer";
  public static final String CLOSE_PRODUCER_COMMAND = "close-producer";
  public static final String COMMIT_CONSUMER_COMMAND = "commit-consumer";

  public void start(String[] args) {
    JsObj conf;
    try {
      conf = parseConf(args);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    List<Command> myCommands = new ArrayList<>();

    KafkaProducers producers = new KafkaProducers();
    KafkaConsumers consumers = new KafkaConsumers();

    AvroSchemas avroSchemas = new AvroSchemas(conf);

    myCommands.add(new SendGeneratedVal(generators,
                                        producers,
                                        avroSchemas));
    myCommands.add(new Command(CREATE_PRODUCER_COMMAND,
                               "",
                               tokens -> tokens[0].equals(CREATE_PRODUCER_COMMAND)) {
      @Override
      public Function<String[], IO<String>> apply(final JsObj conf,
                                                  final State state) {
        return args -> {
          var common = conf.getObj(JsPath.fromKey("kafka")
                                         .key("props"));
          var producerName = args[1];
          var producerConf = conf.getObj(JsPath.fromKey("kafka")
                                               .key("producers")
                                               .key(producerName)
                                               .key("props"));
          producers.createProducer(common,
                                   producerName,
                                   producerConf);
          return IO.succeed("Producer started!");
        };
      }
    });
    myCommands.add(new Command(COMMIT_CONSUMER_COMMAND,
                               "",
                               tokens -> tokens[0].equals(COMMIT_CONSUMER_COMMAND)) {
      @Override
      public Function<String[], IO<String>> apply(final JsObj conf,
                                                  final State state) {
        return args -> {
          consumers.commitAsync(args[1]);
          return IO.succeed("Commit request sent!");
        };
      }
    });

    myCommands.add(new Command(CLOSE_PRODUCER_COMMAND,
                               "",
                               tokens -> tokens[0].equals(CLOSE_PRODUCER_COMMAND)) {
      @Override
      public Function<String[], IO<String>> apply(final JsObj conf,
                                                  final State state) {
        return args -> {
          producers.closeProducer(args[1]);
          return IO.succeed("Producer closed!");
        };
      }
    });

    myCommands.add(new Command(CLOSE_CONSUMER_COMMAND,
                               "",
                               tokens -> tokens[0].equals(CLOSE_CONSUMER_COMMAND)) {
      @Override
      public Function<String[], IO<String>> apply(final JsObj conf,
                                                  final State state) {
        return args -> {
          consumers.stopConsumer(args[1]);
          return IO.succeed("Consumer closed!");
        };
      }
    });

    myCommands.add(new Command(CREATE_CONSUMER_COMMAND,
                               "",
                               tokens -> tokens[0].equals(CREATE_CONSUMER_COMMAND)) {
      @Override
      public Function<String[], IO<String>> apply(final JsObj conf,
                                                  final State state) {
        return args -> {
          var common = conf.getObj(JsPath.fromKey("kafka")
                                         .key("props"));
          var consumerName = args[1];
          JsObj consumerConf = conf.getObj(JsPath.fromKey("kafka")
                                                 .key("consumers")
                                                 .key(consumerName));
          var consumerProps = consumerConf.getObj("props");

          Duration pollTimeout = Duration.ofSeconds(consumerConf.getInt(ConfigurationFields.POLL_TIMEOUT_SEC));
          List<String> topics = consumerConf.getArray(TOPICS)
                                            .streamOfValues()
                                            .map(it -> it.toJsStr().value)
                                            .toList();
          consumers.createConsumer(common,
                                   consumerName,
                                   consumerProps,
                                   topics,
                                   pollTimeout);
          return IO.succeed("Consumer started!");
        };
      }
    });

    myCommands.add(new

                       Command(LS_CONSUMERS_COMMAND,
                               "",
                               tokens -> tokens[0].

                                   equals(LS_CONSUMERS_COMMAND)) {
                         @Override
                         public Function<String[], IO<String>> apply(final JsObj conf,
                                                                     final State state) {
                           return args -> {
                             JsObj consumers = conf.getObj(JsPath.fromKey("kafka")
                                                                 .key("consumers"));
                             String result = consumers.keySet()
                                                      .stream()
                                                      .collect(Collectors.joining(","));
                             return IO.succeed(result);
                           };
                         }
                       });
    myCommands.add(new Command(LS_PRODUCERS_COMMAND,
                               "Prints out the list of producers defined in the conf file",
                               tokens -> tokens[0].

                                   equals(LS_PRODUCERS_COMMAND)) {
      @Override
      public Function<String[], IO<String>> apply(final JsObj conf,
                                                  final State state) {
        return args -> {
          JsObj producers = conf.getObj(JsPath.fromKey("kafka")
                                              .key("producers"));
          String result = producers.keySet()
                                   .stream()
                                   .collect(Collectors.joining(","));
          return IO.succeed(result);
        };
      }
    });
    myCommands.add(new

                       Command(LS_CHANNELS_COMMAND,
                               "Prints out the list of consumers defined in the conf file",
                               tokens -> tokens[0].

                                   equals(LS_CHANNELS_COMMAND)) {
                         @Override
                         public Function<String[], IO<String>> apply(final JsObj conf,
                                                                     final State state) {
                           return args -> {
                             JsObj channels = conf.getObj(JsPath.fromKey("channels"));
                             String result = channels.keySet()
                                                     .stream()
                                                     .map(key -> String.format("%s (topic:%s, producer: %s) ",
                                                                               key,
                                                                               channels.getObj(key)
                                                                                       .getStr("topic"),
                                                                               channels.getObj(key)
                                                                                       .getStr("producer")
                                                                              ))
                                                     .collect(Collectors.joining(","));
                             return IO.succeed(result);
                           };
                         }
                       });
    for (String genName : generators.keySet()) {
      myCommands.add(new GenerateCommand(genName,
                                         "",
                                         generators.get(genName)
                                                   .map(Object::toString)));
    }
    Console cli = new Console(myCommands);

    cli.eval(conf);

  }

  private static JsObj parseConf(final String[] args) throws IOException {
    JsObjSpecParser parser = JsObjSpecParser.of(ConfigurationSpec.global);
    if (args.length == 0) {
      throw new IllegalArgumentException("Pass in the configuration file");
    }
    var path = Path.of(args[0]);
    if (!path.toFile()
             .exists()) {
      throw new IllegalArgumentException("File " + path + " not found");
    }
    return parser.parse(Files.readAllBytes(path));
  }

}
