package com.github.imrafaelmerino.kafkacli;

import jsonvalues.*;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.Properties;

final class Fun {

    static Object kafkaPropValueToObj(JsValue propValue) {

        return switch (propValue) {
            case JsInt n -> n.value;
            case JsLong n -> n.value;
            case JsStr n -> n.value;
            case JsBool n -> n.value;
            default -> null;
        };
    }

    static Properties toProperties(JsObj props) {
        Properties result = new Properties();
        for (JsObjPair pair : props) {
            String propName = pair.key();
            JsValue propValue = pair.value();
            Object objValue = Fun.kafkaPropValueToObj(propValue);
            if (objValue != null && !(objValue instanceof String s && (s.isEmpty() || s.isBlank()))) {
                result.put(propName,
                           objValue);
            }
        }
        return result;
    }


    static String getMessageSent(final ProducerRecord<Object, Object> record) {

        return record.key() != null ? """
                
                Publish request sent:
                  Topic: %s
                  Key: %s
                  Value: %s
                """.formatted(record.topic(),
                              record.key(),
                              record.value()) :
                """
                        
                        Publish request sent:
                          Topic: %s
                          Value: %s
                        """.formatted(record.topic(),
                                      record.value());
    }
}



