package com.github.imrafaelmerino.kafkacli;

import java.util.Properties;
import jsonvalues.JsBool;
import jsonvalues.JsInt;
import jsonvalues.JsLong;
import jsonvalues.JsObj;
import jsonvalues.JsObjPair;
import jsonvalues.JsStr;
import jsonvalues.JsValue;

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

  static void lockedPrint(String message){
    synchronized (System.out){
      System.out.print(message);
    }
  }

  static void lockedPrintln(String message){
    synchronized (System.out){
      System.out.println(message);
    }
  }

}



