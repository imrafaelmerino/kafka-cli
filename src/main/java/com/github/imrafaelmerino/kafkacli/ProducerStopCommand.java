package com.github.imrafaelmerino.kafkacli;

import java.util.function.Function;
import jio.IO;
import jio.console.Command;
import jio.console.State;
import jsonvalues.JsObj;

class ProducerStopCommand extends Command {

  static final String CLOSE_PRODUCER_COMMAND = "producer-stop";

  private final KafkaProducers producers;

  public ProducerStopCommand(final KafkaProducers producers) {
    super(CLOSE_PRODUCER_COMMAND,
          "",
          tokens -> tokens[0].equals(CLOSE_PRODUCER_COMMAND));
    this.producers = producers;
  }

  @Override
  public Function<String[], IO<String>> apply(final JsObj conf,
                                              final State state) {
    return args -> IO.lazy(() -> {
      String producerName = args[1];
      if(!producers.isStarted(producerName)){
        return "Producer `%s` already closed!".formatted(producerName);
      }
      producers.closeProducer(producerName);
      return "Producer `%s` closed!".formatted(producerName);
    });
  }
}
