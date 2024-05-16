package com.github.imrafaelmerino.kafkacli;

import java.util.function.Function;
import jio.IO;
import jio.console.Command;
import jio.console.State;
import jsonvalues.JsObj;

public class SendVal extends Command {

  public SendVal() {
    super("send-val",
          "",
          args -> args[0].equals("send-val"));
  }

  @Override
  public Function<String[], IO<String>> apply(final JsObj conf,
                                              final State state) {
    return args -> {

      String channel = args[1];
      String value = args[2];
      if (args.length == 3) {
        //send
      } else if (args.length == 4) {
        // can be n or key gen
       String key = args[3];
      }

      return null;

    };
  }
}
