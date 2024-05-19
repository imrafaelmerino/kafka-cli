package com.github.imrafaelmerino.kafkacli;

import jio.IO;
import jio.cli.Command;
import jio.cli.State;
import jsonvalues.JsObj;

import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

class ConsumerListCommand extends Command {

    static final String LS_CONSUMERS_COMMAND = "consumer-list";
    private static final String USAGE = """
            Usage: consumer-list

            Description:
            The `consumer-list` command lists all Kafka consumers along with their statuses (up or down).

            Output:
            - The list of consumers with their names and statuses.

            Example:
            $ consumer-list
            Name                 Status
            consumer1            up
            consumer2            down
            consumer3            up

            Note:
            Ensure that the consumer configurations are correctly set in the configuration file to accurately reflect their statuses.
            """;
    final KafkaConsumers kafkaConsumers;

    ConsumerListCommand(final KafkaConsumers kafkaConsumers) {
        super(LS_CONSUMERS_COMMAND,
              USAGE,
              tokens -> tokens[0].

                      equals(LS_CONSUMERS_COMMAND));
        this.kafkaConsumers = kafkaConsumers;
    }

    @Override
    public Function<String[], IO<String>> apply(final JsObj conf,
                                                final State state
                                               ) {
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
