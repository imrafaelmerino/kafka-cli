package com.github.imrafaelmerino.kafkacli;

import jio.IO;
import jio.cli.Command;
import jio.cli.State;
import jsonvalues.JsObj;

import java.util.function.Function;


class ConsumerAsyncCommitCommand extends Command {

    static final String COMMIT_CONSUMER_COMMAND = "consumer-commit";

    private final KafkaConsumers consumers;

    ConsumerAsyncCommitCommand(final KafkaConsumers consumers) {
        super(COMMIT_CONSUMER_COMMAND,
              "",
              tokens -> tokens[0].equals(COMMIT_CONSUMER_COMMAND));
        this.consumers = consumers;
    }

    @Override
    public Function<String[], IO<String>> apply(final JsObj conf,
                                                final State state
                                               ) {
        return args -> {
            String consumerName = args[1];
            consumers.commitAsync(consumerName);
            return IO.succeed("Commit request sent to consumer `%s`!".formatted(consumerName));
        };
    }
}
