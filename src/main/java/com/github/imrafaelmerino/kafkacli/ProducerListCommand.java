package com.github.imrafaelmerino.kafkacli;

import jio.IO;
import jio.cli.Command;
import jio.cli.State;
import jsonvalues.JsObj;

import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

class ProducerListCommand extends Command {

    static final String LS_PRODUCERS_COMMAND = "producer-list";
    private static final String USAGE = """
            Usage: producer-list

            Description:
            The `producer-list` command lists all Kafka producers along with their statuses (up or down).

            Output:
            - The list of producers with their names and statuses.

            Example:
            $ producer-list
            Name                 Status
            producer1            up
            producer2            down
            producer3            up

            Note:
            Ensure that the producer configurations are correctly set in the configuration file to accurately reflect their statuses.
            """;


    final KafkaProducers kafkaProducers;

    public ProducerListCommand(final KafkaProducers kafkaProducers) {
        super(LS_PRODUCERS_COMMAND,
              USAGE,
              tokens -> tokens[0].equals(LS_PRODUCERS_COMMAND));
        this.kafkaProducers = kafkaProducers;
    }

    @Override
    public Function<String[], IO<String>> apply(final JsObj conf,
                                                final State state
                                               ) {
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
