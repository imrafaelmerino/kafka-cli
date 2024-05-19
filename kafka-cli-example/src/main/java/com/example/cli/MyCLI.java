package com.example.cli;

import com.github.imrafaelmerino.kafkacli.KafkaCLI;
import fun.gen.Gen;
import fun.gen.StrGen;
import jsonvalues.gen.JsIntGen;
import jsonvalues.gen.JsObjGen;
import jsonvalues.gen.JsStrGen;

import java.util.HashMap;
import java.util.Map;

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
                       StrGen.alphabetic(10, 100));

        new KafkaCLI(generators).start(args);

    }
}
