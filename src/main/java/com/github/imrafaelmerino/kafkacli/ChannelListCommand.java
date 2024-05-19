package com.github.imrafaelmerino.kafkacli;

import jio.IO;
import jio.cli.Command;
import jio.cli.State;
import jsonvalues.JsObj;

import java.util.function.Function;

class ChannelListCommand extends Command {

    static final String LS_CHANNELS_COMMAND = "channel-list";
    private final KafkaProducers producers;

    ChannelListCommand(KafkaProducers producers) {
        super(LS_CHANNELS_COMMAND,
              "Prints out the list of channels defined in the conf file",
              tokens -> tokens[0].

                      equals(LS_CHANNELS_COMMAND));
        this.producers = producers;
    }

    @Override
    public Function<String[], IO<String>> apply(final JsObj conf,
                                                final State state
                                               ) {
        return _ -> IO.lazy(() -> ConfigurationQueries.getChannelsInfo(conf,
                                                                       producers));
    }


}
