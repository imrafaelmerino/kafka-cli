package com.github.imrafaelmerino.kafkacli;

import static com.github.imrafaelmerino.kafkacli.ConfigurationFields.COMMON_PROPS;
import static com.github.imrafaelmerino.kafkacli.ConfigurationFields.KAFKA;
import static com.github.imrafaelmerino.kafkacli.ConfigurationFields.PRODUCERS;
import static com.github.imrafaelmerino.kafkacli.ConfigurationFields.PRODUCER_PROPS;

import java.util.function.Function;
import jio.IO;
import jio.console.Command;
import jio.console.State;
import jsonvalues.JsObj;
import jsonvalues.JsPath;

class ProducerCreateCommand extends Command {

  static final String CREATE_PRODUCER_COMMAND = "producer-start";

  private final KafkaProducers producers;

  public ProducerCreateCommand(final KafkaProducers producers) {
    super(CREATE_PRODUCER_COMMAND,
          "",
          tokens -> tokens[0].equals(CREATE_PRODUCER_COMMAND));
    this.producers = producers;
  }

  @Override
  public Function<String[], IO<String>> apply(final JsObj conf,
                                              final State state) {
    return args -> IO.task(() -> {
      var producerName = args[1];
      JsPath confPath = JsPath.fromKey(KAFKA)
                              .key(PRODUCERS)
                              .key(producerName)
                              .key(PRODUCER_PROPS);

      var producerConf = conf.getObj(confPath);
      if (producerConf == null) {
        return
            "The configuration associated to the the producer `%s` wasn't found at %s".formatted(producerName,
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
