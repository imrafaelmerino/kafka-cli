package com.github.imrafaelmerino.kafkacli;

import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import jio.IO;
import jio.console.Command;
import jio.console.State;
import jsonvalues.JsObj;

class ConsumerListCommand extends Command {

  static final String LS_CONSUMERS_COMMAND = "consumer-list";

  final KafkaConsumers kafkaConsumers;

  ConsumerListCommand(final KafkaConsumers kafkaConsumers) {
    super(LS_CONSUMERS_COMMAND,
          "",
          tokens -> tokens[0].

              equals(LS_CONSUMERS_COMMAND));
    this.kafkaConsumers = kafkaConsumers;
  }

  @Override
  public Function<String[], IO<String>> apply(final JsObj conf,
                                              final State state) {
    return _ -> IO.lazy(() -> {
      Set<String> result = ConfigurationQueries.getConsumers(conf);
      return result.stream()
                   .map(consumer -> String.format("%-20s %s",
                                                  consumer,
                                                  kafkaConsumers.apply(consumer) != null ? "up" : "down")
                       )
                   .collect(Collectors.joining("\n",
                                               String.format("%-20s %s",
                                                             "Name",
                                                             "Status\n"),
                                               ""));
    });
  }

}
