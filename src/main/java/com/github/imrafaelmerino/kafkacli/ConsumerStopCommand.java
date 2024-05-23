package com.github.imrafaelmerino.kafkacli;

import jio.IO;
import jio.cli.Command;
import jio.cli.State;
import jsonvalues.JsObj;

import java.util.function.Function;

import static jio.cli.ConsolePrograms.ASK_FOR_INPUT;

class ConsumerStopCommand extends Command {

    static final String CLOSE_CONSUMER_COMMAND = "consumer-stop";
    private static final String USAGE = """
            Usage: consumer-stop [consumer-name]

            Description:
            The `consumer-stop` command stops a running Kafka consumer.

            Parameters:
            - consumer-name (optional): The name of the consumer to stop. If not provided, the user will be prompted to select from a list of available consumers.

            Steps:
            1. Without a consumer name:
               - The command will list all available consumers.
               - The user will be prompted to type the name of one of the listed consumers.
               - If the input is invalid, the user will have three attempts to provide a correct name.

            2. With a consumer name:
               - The command will directly attempt to stop the specified consumer.

            Output:
            - Success: "Consumer `<consumer-name>` closed!"
            - Failure: Appropriate error message if the consumer is not found or is already closed.

            Example:
            1. Interactive mode (prompt user for consumer name):
               $ consumer-stop
               consumer1
               consumer2
               consumer3
               Type the consumer name (choose one of the above):

            2. Direct mode (provide consumer name):
               $ consumer-stop consumer1

            Note:
            Ensure that the consumer is currently running before attempting to stop it.
            """;
    private final KafkaConsumers consumers;

    ConsumerStopCommand(final KafkaConsumers consumers) {
        super(CLOSE_CONSUMER_COMMAND,
              USAGE,
              tokens -> tokens[0].equals(CLOSE_CONSUMER_COMMAND));
        this.consumers = consumers;
    }

    @Override
    public Function<String[], IO<String>> apply(final JsObj conf,
                                                final State state
                                               ) {
        return args -> {
            if (args.length == 1) {
                var allConsumers = ConfigurationQueries.getConsumers(conf);
                return ASK_FOR_INPUT(Prompts.ASK_FOR_CONSUMER_PARAMS.apply(allConsumers))
                        .then(this::stop);
            }
            return stop(args[1]);
        };
    }

    private IO<String> stop(final String consumerName) {
        return IO.lazy(() -> {
            if (!consumers.isStarted(consumerName)) {
                return "Consumer `%s` already closed!".formatted(consumerName);
            }
            consumers.closeConsumer(consumerName);
            return "Consumer `%s` closed!".formatted(consumerName);
        });
    }
}
