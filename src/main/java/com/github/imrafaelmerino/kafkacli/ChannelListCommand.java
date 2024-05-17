package com.github.imrafaelmerino.kafkacli;

import java.util.function.Function;
import jio.IO;
import jio.console.Command;
import jio.console.State;
import jsonvalues.JsObj;

class ChannelListCommand extends Command {

  static final String LS_CHANNELS_COMMAND = "channel-list";
  private final KafkaProducers producers;

  ChannelListCommand(KafkaProducers producers) {
    super(LS_CHANNELS_COMMAND,
          "Prints out the list of consumers defined in the conf file",
          tokens -> tokens[0].

              equals(LS_CHANNELS_COMMAND));
    this.producers = producers;
  }

  @Override
  public Function<String[], IO<String>> apply(final JsObj conf,
                                              final State state) {
    return _ -> IO.lazy(() -> ConfigurationQueries.getChannelsInfo(conf,
                                                                   producers));
  }


}
