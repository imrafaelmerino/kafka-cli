package com.github.imrafaelmerino.kafkacli;

import java.util.Set;
import java.util.function.Function;
import jio.IO;
import jio.RetryPolicies;
import jio.console.Command;
import jio.console.ConsolePrograms;
import jio.console.ConsolePrograms.AskForInputParams;
import jio.console.State;
import jsonvalues.JsObj;

class ProducerStopCommand extends Command {

  static final String CLOSE_PRODUCER_COMMAND = "producer-stop";

  private final KafkaProducers producers;

  public ProducerStopCommand(final KafkaProducers producers) {
    super(CLOSE_PRODUCER_COMMAND,
          "",
          tokens -> tokens[0].equals(CLOSE_PRODUCER_COMMAND));
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
                              .then(this::stop);
      }
      return stop(args[1]);
    };
  }

  private IO<String> stop(final String producerName) {
    return IO.lazy(() -> {
      if (!producers.isStarted(producerName)) {
        return "Producer `%s` already closed!".formatted(producerName);
      }
      producers.closeProducer(producerName);
      return "Producer `%s` closed!".formatted(producerName);
    });
  }
}
