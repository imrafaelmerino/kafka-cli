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
        generators.put("client_id",
                       JsObjGen.of("_id",
                                   JsStrGen.alphabetic()));
        generators.put("client_profile",
                       JsObjGen.of("a",
                                   JsIntGen.arbitrary(0,
                                                      1000),
                                   "b",
                                   JsStrGen.alphabetic(),
                                   "c",
                                   JsStrGen.alphabetic()
                                  )
                      );
        generators.put("text",
                       StrGen.alphabetic(10, 100));

        new KafkaCLI(generators).start(args);

    }
}
