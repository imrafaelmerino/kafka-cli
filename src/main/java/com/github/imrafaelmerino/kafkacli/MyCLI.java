package com.github.imrafaelmerino.kafkacli;

import fun.gen.Gen;
import fun.gen.StrGen;
import java.util.HashMap;
import java.util.Map;
import jsonvalues.gen.JsIntGen;
import jsonvalues.gen.JsObjGen;
import jsonvalues.gen.JsStrGen;

public class MyCLI {

  public static void main(String[] args) {

    Map<String, Gen<?>> generators = new HashMap<>();
    generators.put("id-flight",
                   JsObjGen.of("_id",
                               JsStrGen.alphabetic()));
    generators.put("flight-stats",
                   JsObjGen.of("flights",
                               JsIntGen.arbitrary(0,
                                                  1000),
                               "origin",
                               JsStrGen.alphabetic(),
                               "destination",
                               JsStrGen.alphabetic()
                              )
                  );
    generators.put("message",
                   StrGen.alphabetic(10,100));

    new CLI(generators).start(args);

  }
}
