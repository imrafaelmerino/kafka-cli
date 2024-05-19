package com.github.imrafaelmerino.kafkacli;


import jsonvalues.JsObj;

public record Message(String key,
                      String value,
                      JsObj headers) {

    public Message {
        if (value == null || value.isEmpty())
            throw new IllegalArgumentException("Message value can not be null or empty");
    }
}



