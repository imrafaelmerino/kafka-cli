package com.github.imrafaelmerino.kafkacli;

import jio.IO;
import jio.RetryPolicies;
import jio.cli.Command;
import jio.cli.ConsolePrograms;
import jio.cli.ConsolePrograms.AskForInputParams;
import jio.cli.State;
import jsonvalues.JsObj;
import jsonvalues.JsPath;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import static com.github.imrafaelmerino.kafkacli.ConfigurationFields.*;

class ConsumerStartCommand extends Command {

    static final String CREATE_CONSUMER_COMMAND = "consumer-start";
    private static final String USAGE = """
            Usage: consumer-start [consumer-name] [-verbose]

            Description:
            The `consumer-start` command starts a Kafka consumer using the provided configuration.

            Parameters:
            - consumer-name (optional): The name of the consumer to start. If not provided, the user will be prompted to select from a list of available consumers.
            - -verbose (optional): If provided, the consumer will output a verbose log of the received messages.

            Steps:
            1. Without a consumer name:
               - The command will list all available consumers.
               - The user will be prompted to type the name of one of the listed consumers.
               - The user will be asked if they want a verbose output of the received messages (yes | no).
               - If the input is invalid, the user will have three attempts to provide a correct name and response.

            2. With a consumer name:
               - The command will directly attempt to start the specified consumer.
               - If `-verbose` is provided, the consumer will output a verbose log of the received messages.

            Output:
            - Success: "Consumer `<consumer-name>` started!"
            - Failure: Appropriate error message if the configuration is not found or if the consumer is already started.

            Example:
            1. Interactive mode (prompt user for consumer name):
               $ consumer-start
               consumer1
               consumer2
               consumer3
               Type the consumer name (choose one of the above):
               Do you want a verbose output of the received messages? (yes | no):

            2. Direct mode (provide consumer name and verbose flag):
               $ consumer-start consumer1 -verbose

            Note:
            Ensure that the consumer configurations are correctly set in the configuration file before starting a consumer.
            """;
    private final KafkaConsumers consumers;


    ConsumerStartCommand(final KafkaConsumers consumers) {
        super(CREATE_CONSUMER_COMMAND,
              USAGE,
              tokens -> tokens[0].equals(CREATE_CONSUMER_COMMAND));
        this.consumers = consumers;
    }

    @Override
    public Function<String[], IO<String>> apply(final JsObj conf,
                                                final State state
                                               ) {
        return args -> {
            if (args.length == 1) {
                var allConsumers = ConfigurationQueries.getConsumers(conf);
                return ConsolePrograms.ASK_FOR_PAIR(Prompts.ASK_FOR_CONSUMER_PARAMS.apply(allConsumers),
                                                    new AskForInputParams(
                                                            "\nDo you want a verbose output of the received messages? "
                                                            + "(yes | no)",
                                                            resp -> resp.equalsIgnoreCase("yes")
                                                                    || resp.equalsIgnoreCase("no"),
                                                            "\nInvalid response.",
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
                             final boolean verbose
                            ) {
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
