package com.github.imrafaelmerino.kafkacli;

import java.util.function.Function;
import jio.IO;
import jio.RetryPolicies;
import jio.console.Command;
import jio.console.ConsolePrograms;
import jio.console.ConsolePrograms.AskForInputParams;
import jio.console.State;
import jsonvalues.JsObj;

class ConsumerStopCommand extends Command {

  static final String CLOSE_CONSUMER_COMMAND = "consumer-stop";

  private final KafkaConsumers consumers;

  ConsumerStopCommand(final KafkaConsumers consumers) {
    super(CLOSE_CONSUMER_COMMAND,
          "",
          tokens -> tokens[0].equals(CLOSE_CONSUMER_COMMAND));
    this.consumers = consumers;
  }

  @Override
  public Function<String[], IO<String>> apply(final JsObj conf,
                                              final State state) {
    return args -> {
      if (args.length == 1) {
        var allConsumers = ConfigurationQueries.getConsumers(conf);
        return ConsolePrograms.ASK_FOR_INPUT(new AskForInputParams("%s\n%s".formatted(String.join("\n",
                                                                                                  allConsumers),
                                                                                      "Type the consumer name (choose one "
                                                                                      + "of the above):"),
                                                                   consumer -> allConsumers.contains(consumer),
                                                                   "Invalid consumer name.",
                                                                   RetryPolicies.limitRetries(3))
                                            )
                              .then(name -> stop(name));
      }
      return stop(args[1]);
    };
  }

  private IO<String> stop(final String consumerName) {
    return IO.lazy(() -> {
      if (!consumers.isStarted(consumerName)) {
        return "Consumer `%s` already closed!".formatted(consumerName);
      }
      consumers.stopConsumer(consumerName);
      return "Consumer `%s` closed!".formatted(consumerName);
    });
  }
}
