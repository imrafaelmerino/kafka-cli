package com.github.imrafaelmerino.kafkacli;

import java.util.function.Function;
import jio.IO;
import jio.console.Command;
import jio.console.State;
import jsonvalues.JsObj;

class ConsumerStopCommand extends Command {

  static final String CLOSE_CONSUMER_COMMAND = "consumer-stop";

  private final KafkaConsumers consumers;

  ConsumerStopCommand(final KafkaConsumers consumers) {
    super(CLOSE_CONSUMER_COMMAND,
          "",
          tokens -> tokens[0].equals(CLOSE_CONSUMER_COMMAND));
    this.consumers = consumers;
  }

  @Override
  public Function<String[], IO<String>> apply(final JsObj conf,
                                              final State state) {
    return args -> IO.lazy(() -> {
      String consumerName = args[1];
      if (!consumers.isStarted(consumerName)) {
        return "Consumer `%s` already closed!".formatted(consumerName);
      }
      consumers.stopConsumer(consumerName);
      return "Consumer `%s` closed!".formatted(consumerName);
    });
  }
}
