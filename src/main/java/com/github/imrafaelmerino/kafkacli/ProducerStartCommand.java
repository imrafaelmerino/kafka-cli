package com.github.imrafaelmerino.kafkacli;

import static com.github.imrafaelmerino.kafkacli.ConfigurationFields.COMMON_PROPS;
import static com.github.imrafaelmerino.kafkacli.ConfigurationFields.KAFKA;
import static com.github.imrafaelmerino.kafkacli.ConfigurationFields.PRODUCERS;
import static com.github.imrafaelmerino.kafkacli.ConfigurationFields.PRODUCER_PROPS;

import java.util.Set;
import java.util.function.Function;
import jio.IO;
import jio.RetryPolicies;
import jio.console.Command;
import jio.console.ConsolePrograms;
import jio.console.ConsolePrograms.AskForInputParams;
import jio.console.State;
import jsonvalues.JsObj;
import jsonvalues.JsPath;

class ProducerStartCommand extends Command {

  static final String CREATE_PRODUCER_COMMAND = "producer-start";

  private final KafkaProducers producers;

  public ProducerStartCommand(final KafkaProducers producers) {
    super(CREATE_PRODUCER_COMMAND,
          "",
          tokens -> tokens[0].equals(CREATE_PRODUCER_COMMAND));
    this.producers = producers;
  }

  @Override
  public Function<String[], IO<String>> apply(final JsObj conf,
                                              final State state) {
    return args -> {
      if (args.length == 1) {
        Set<String> allProducers = ConfigurationQueries.getProducers(conf);
        return ConsolePrograms.ASK_FOR_INPUT(new AskForInputParams("%s\n%s".formatted(String.join("\n",
                                                                                                  allProducers),
                                                                                      "Type the producer name (One of the "
                                                                                      + "above):"),
                                                                   allProducers::contains,
                                                                   "Invalid producer name.",
                                                                   RetryPolicies.limitRetries(3))
                                            )
                              .then(producer -> start(conf,
                                                      producer)
                                   );
      }

      return start(conf,
                   args[1]);

    };
  }

  private IO<String> start(final JsObj conf,
                           final String producerName) {
    return IO.task(() -> {
      JsPath confPath = JsPath.fromKey(KAFKA)
                              .key(PRODUCERS)
                              .key(producerName)
                              .key(PRODUCER_PROPS);

      var producerConf = conf.getObj(confPath);
      if (producerConf == null) {
        return "The configuration associated to the the producer `%s` wasn't found at %s".formatted(producerName,
                                                                                                    confPath);
      }
      if (producers.isStarted(producerName)) {
        return "Producer `%s` already started!".formatted(producerName);
      }
      var common = conf.getObj(JsPath.fromKey(KAFKA)
                                     .key(COMMON_PROPS));

      producers.startProducer(common,
                              producerName,
                              producerConf);
      return "Producer `%s` started!".formatted(producerName);
    });
  }
}
