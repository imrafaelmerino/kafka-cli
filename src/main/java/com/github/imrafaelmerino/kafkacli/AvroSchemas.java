package com.github.imrafaelmerino.kafkacli;

import io.confluent.kafka.schemaregistry.avro.AvroSchema;
import java.util.HashMap;
import java.util.Map;
import jsonvalues.JsObj;
import jsonvalues.JsObjPair;
import org.apache.avro.Schema;
import org.apache.avro.Schema.Parser;

public class AvroSchemas {

  Parser avroParser = new Parser();
  Map<String, Schema> keySchemasPerChannel = new HashMap<>();

  Map<String, Schema> valueSchemasPerChannel = new HashMap<>();



  public AvroSchemas(final JsObj conf) {

    for (JsObjPair pair : conf.getObj("channels")) {
      String channel = pair.key();
      JsObj channelConf = pair.value()
                              .toJsObj();
      String keySchema = channelConf.getStr("key-schema");
      String valueSchema = channelConf.getStr("value-schema");
      if (valueSchema != null) {
        valueSchemasPerChannel.put(channel,
                                   avroParser.parse(valueSchema));
      }
      if (keySchema != null) {
        keySchemasPerChannel.put(channel,
                                 avroParser.parse(keySchema));
      }
    }

  }


}
