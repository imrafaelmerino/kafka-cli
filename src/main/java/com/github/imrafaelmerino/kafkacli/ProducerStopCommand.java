package com.github.imrafaelmerino.kafkacli;

import jio.IO;
import jio.cli.Command;
import jio.cli.State;
import jsonvalues.JsObj;

import java.util.Set;
import java.util.function.Function;

class ProducerStopCommand extends Command {

    static final String CLOSE_PRODUCER_COMMAND = "producer-stop";
    static String USAGE = """
            Usage: producer-stop [producer-name]

            Description:
            The `producer-stop` command stops a running Kafka producer.

            Parameters:
            - producer-name (optional): The name of the producer to stop. If not provided, the user will be prompted to select from a list of available producers.

            Steps:
            1. Without a producer name:
               - The command will list all available producers.
               - The user will be prompted to type the name of one of the listed producers.
               - If the input is invalid, the user will have three attempts to provide a correct name.

            2. With a producer name:
               - The command will directly attempt to stop the specified producer.

            Output:
            - Success: "Producer `<producer-name>` closed!"
            - Failure: Appropriate error message if the producer is not found or is already closed.

            Example:
            1. Interactive mode (prompt user for producer name):
               $ producer-stop
               producer1
               producer2
               producer3
               Type the producer name (One of the above):

            2. Direct mode (provide producer name):
               $ producer-stop producer1

            Note:
            Ensure that the producer is currently running before attempting to stop it.
            """;

    private final KafkaProducers producers;

    public ProducerStopCommand(final KafkaProducers producers) {
        super(CLOSE_PRODUCER_COMMAND,
              USAGE,
              tokens -> tokens[0].equals(CLOSE_PRODUCER_COMMAND));
        this.producers = producers;
    }

    @Override
    public Function<String[], IO<String>> apply(final JsObj conf,
                                                final State state
                                               ) {
        return args -> {
            if (args.length == 1) {
                Set<String> allProducers = ConfigurationQueries.getProducers(conf);
                return Prompts.ASK_FOR_PRODUCER.apply(allProducers)
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
