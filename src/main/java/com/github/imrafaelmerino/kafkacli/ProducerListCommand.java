package com.github.imrafaelmerino.kafkacli;

import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import jio.IO;
import jio.console.Command;
import jio.console.State;
import jsonvalues.JsObj;

class ProducerListCommand extends Command {

  static final String LS_PRODUCERS_COMMAND = "producer-list";


  final KafkaProducers kafkaProducers;

  public ProducerListCommand(final KafkaProducers kafkaProducers) {
    super(LS_PRODUCERS_COMMAND,
          "Prints out the list of producers defined in the conf file",
          tokens -> tokens[0].
              equals(LS_PRODUCERS_COMMAND));
    this.kafkaProducers = kafkaProducers;
  }

  @Override
  public Function<String[], IO<String>> apply(final JsObj conf,
                                              final State state) {
    return _ -> IO.lazy(() -> {
      Set<String> result = ConfigurationQueries.getProducers(conf);
      return result.stream()
                   .map(producer -> String.format("%-20s %s",
                                                  producer,
                                                  kafkaProducers.apply(producer) != null ? "up" : "down")
                       )
                   .collect(Collectors.joining("\n",
                                               String.format("%-20s %s",
                                                             "Name",
                                                             "Status\n"),
                                               ""));
    });
  }

}
