package com.github.imrafaelmerino.kafkacli;

import static com.github.imrafaelmerino.kafkacli.ConfigurationFields.COMMON_PROPS;
import static com.github.imrafaelmerino.kafkacli.ConfigurationFields.CONSUMERS;
import static com.github.imrafaelmerino.kafkacli.ConfigurationFields.CONSUMER_PROPS;
import static com.github.imrafaelmerino.kafkacli.ConfigurationFields.KAFKA;
import static com.github.imrafaelmerino.kafkacli.ConfigurationFields.TOPICS;

import java.time.Duration;
import java.util.List;
import java.util.function.Function;
import jio.IO;
import jio.console.Command;
import jio.console.State;
import jsonvalues.JsObj;
import jsonvalues.JsPath;

class ConsumerCreateCommand extends Command {

  static final String CREATE_CONSUMER_COMMAND = "consumer-start";

  private final KafkaConsumers consumers;

  ConsumerCreateCommand(final KafkaConsumers consumers) {
    super(CREATE_CONSUMER_COMMAND,
          "",
          tokens -> tokens[0].equals(CREATE_CONSUMER_COMMAND));
    this.consumers = consumers;
  }

  @Override
  public Function<String[], IO<String>> apply(final JsObj conf,
                                              final State state) {
    return args -> IO.task(() -> {

      var consumerName = args[1];
      JsPath pathConf = JsPath.fromKey(KAFKA)
                              .key(CONSUMERS)
                              .key(consumerName);
      JsObj consumerConf = conf.getObj(pathConf);
      if (consumerConf == null) {
        return
            "The configuration associated to the the consumer `%s` wasn't found at %s".formatted(consumerName,
                                                                                                 pathConf);
      }
      var consumerProps = consumerConf.getObj(CONSUMER_PROPS);
      if (consumerProps == null) {
        return
            "The kafka configuration associated to the the consumer `%s` wasn't found at %s".formatted(consumerName,
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
                              pollTimeout);
      return "Consumer `%s` started!".formatted(consumerName);
    });
  }
}
