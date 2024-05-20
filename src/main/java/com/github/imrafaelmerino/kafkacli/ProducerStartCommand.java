package com.github.imrafaelmerino.kafkacli;

import jio.IO;
import jio.cli.Command;
import jio.cli.State;
import jsonvalues.JsObj;
import jsonvalues.JsPath;

import java.util.Set;
import java.util.function.Function;

import static com.github.imrafaelmerino.kafkacli.ConfigurationFields.*;

class ProducerStartCommand extends Command {

    static final String CREATE_PRODUCER_COMMAND = "producer-start";
    static String USAGE = """
            Usage: producer-start [producer-name]

            Description:
            The `producer-start` command initiates a Kafka producer using the provided configuration.

            Parameters:
            - producer-name (optional): The name of the producer to start. If not provided, the user will be prompted to select from a list of available producers.

            Steps:
            1. Without a producer name:
               - The command will list all available producers.
               - The user will be prompted to type the name of one of the listed producers.
               - If the input is invalid, the user will have three attempts to provide a correct name.

            2. With a producer name:
               - The command will directly attempt to start the specified producer.

            Output:
            - Success: "Producer `<producer-name>` started!"
            - Failure: Appropriate error message if the configuration is not found or if the producer is already started.

            Example:
            1. Interactive mode (prompt user for producer name):
               $ producer-start
               producer1
               producer2
               producer3
               Type the producer name (One of the above):

            2. Direct mode (provide producer name):
               $ producer-start producer1

            Note:
            Ensure that the producer configurations are correctly set in the configuration file before starting a producer.
            """;
    private final KafkaProducers producers;


    public ProducerStartCommand(final KafkaProducers producers) {
        super(CREATE_PRODUCER_COMMAND,
              USAGE,
              tokens -> tokens[0].equals(CREATE_PRODUCER_COMMAND));
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
                                               .then(producer -> start(conf,
                                                                       producer)
                                                    );
            }

            return start(conf,
                         args[1]);

        };
    }

    private IO<String> start(final JsObj conf,
                             final String producerName
                            ) {
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
