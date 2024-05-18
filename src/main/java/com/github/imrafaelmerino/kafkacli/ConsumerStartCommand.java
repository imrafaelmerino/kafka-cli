package com.github.imrafaelmerino.kafkacli;

import static com.github.imrafaelmerino.kafkacli.ConfigurationFields.COMMON_PROPS;
import static com.github.imrafaelmerino.kafkacli.ConfigurationFields.CONSUMERS;
import static com.github.imrafaelmerino.kafkacli.ConfigurationFields.CONSUMER_PROPS;
import static com.github.imrafaelmerino.kafkacli.ConfigurationFields.KAFKA;
import static com.github.imrafaelmerino.kafkacli.ConfigurationFields.TOPICS;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import jio.IO;
import jio.RetryPolicies;
import jio.console.Command;
import jio.console.ConsolePrograms;
import jio.console.ConsolePrograms.AskForInputParams;
import jio.console.State;
import jsonvalues.JsObj;
import jsonvalues.JsPath;

class ConsumerStartCommand extends Command {

  static final String CREATE_CONSUMER_COMMAND = "consumer-start";

  private final KafkaConsumers consumers;

  ConsumerStartCommand(final KafkaConsumers consumers) {
    super(CREATE_CONSUMER_COMMAND,
          "",
          tokens -> tokens[0].equals(CREATE_CONSUMER_COMMAND));
    this.consumers = consumers;
  }

  @Override
  public Function<String[], IO<String>> apply(final JsObj conf,
                                              final State state) {
    return args -> {
      if (args.length == 1) {
        var allConsumers = ConfigurationQueries.getConsumers(conf);
        return ConsolePrograms.ASK_FOR_PAIR(new AskForInputParams("%s\n%s".formatted(String.join("\n",
                                                                                                 allConsumers),
                                                                                     "Type the consumer name (choose one "
                                                                                     + "of the above):"),
                                                                  allConsumers::contains,
                                                                  "Invalid consumer name.",
                                                                  RetryPolicies.limitRetries(3)),
                                            new AskForInputParams(
                                                "Do you want a verbose output of the received messages? "
                                                + "(yes | no)",
                                                resp -> resp.equalsIgnoreCase("yes")
                                                        || resp.equalsIgnoreCase("no"),
                                                "Invalid response.",
                                                RetryPolicies.limitRetries(3)
                                            )
                                           )
                              .then(pair -> start(conf,
                                                  pair.first(),
                                                  "yes".equalsIgnoreCase(pair.second()))
                                   );
      }
      var consumerName = args[1];
      boolean verbose = Arrays.stream(args)
                              .toList()
                              .contains("-verbose");
      return start(conf,
                   consumerName,
                   verbose);
    };
  }

  private IO<String> start(final JsObj conf,
                           final String consumerName,
                           final boolean verbose) {
    return IO.lazy(() -> {
      JsPath pathConf = JsPath.fromKey(KAFKA)
                              .key(CONSUMERS)
                              .key(consumerName);
      JsObj consumerConf = conf.getObj(pathConf);
      if (consumerConf == null) {
        return "The configuration associated to the the consumer `%s` wasn't found at %s".formatted(consumerName,
                                                                                                    pathConf);
      }
      var consumerProps = consumerConf.getObj(CONSUMER_PROPS);
      if (consumerProps == null) {
        return "The kafka configuration associated to the the consumer `%s` wasn't found at %s".formatted(consumerName,
                                                                                                          pathConf.key(CONSUMER_PROPS));
      }
      if (consumers.isStarted(consumerName)) {
        return "Consumer `%s` already started!".formatted(consumerName);
      }
      var common = conf.getObj(JsPath.fromKey(KAFKA)
                                     .key(COMMON_PROPS));

      Duration pollTimeout = Duration.ofSeconds(consumerConf.getInt(ConfigurationFields.POLL_TIMEOUT_SEC));
      List<String> topics = consumerConf.getArray(TOPICS)
                                        .streamOfValues()
                                        .map(it -> it.toJsStr().value)
                                        .toList();
      consumers.startConsumer(common,
                              consumerName,
                              consumerProps,
                              topics,
                              pollTimeout,
                              verbose);
      return "Consumer `%s` started!".formatted(consumerName);
    });
  }
}
