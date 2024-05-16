package com.github.imrafaelmerino.kafkacli;


import static com.github.imrafaelmerino.kafkacli.ConfigurationFields.CHANNELS;
import static com.github.imrafaelmerino.kafkacli.ConfigurationFields.COMMON_PROPS;
import static com.github.imrafaelmerino.kafkacli.ConfigurationFields.CONSUMERS;
import static com.github.imrafaelmerino.kafkacli.ConfigurationFields.CONSUMER_PROPS;
import static com.github.imrafaelmerino.kafkacli.ConfigurationFields.KAFKA_ENTRIES;
import static com.github.imrafaelmerino.kafkacli.ConfigurationFields.KEY_SCHEMA;
import static com.github.imrafaelmerino.kafkacli.ConfigurationFields.POLL_TIMEOUT_SEC;
import static com.github.imrafaelmerino.kafkacli.ConfigurationFields.PRODUCER;
import static com.github.imrafaelmerino.kafkacli.ConfigurationFields.PRODUCERS;
import static com.github.imrafaelmerino.kafkacli.ConfigurationFields.PRODUCER_PROPS;
import static com.github.imrafaelmerino.kafkacli.ConfigurationFields.SCHEMA_VALUE;
import static com.github.imrafaelmerino.kafkacli.ConfigurationFields.TOPIC;
import static com.github.imrafaelmerino.kafkacli.ConfigurationFields.TOPICS;
import static com.github.imrafaelmerino.kafkacli.ConfigurationFields.VALUE_SCHEMA;

import jsonvalues.JsNull;
import jsonvalues.spec.JsObjSpec;
import jsonvalues.spec.JsSpec;
import jsonvalues.spec.JsSpecs;

public final class ConfigurationSpec {


  private ConfigurationSpec() {
  }


  /**
   * any kind of map of values, where a value is a string, boolean or integer number In our case we model all the kafka
   * properties with this spec
   */
  public static final JsSpec propsSpec =
      JsSpecs.mapOfSpec(JsSpecs.oneSpecOf(JsSpecs.str(),
                                          JsSpecs.bool(),
                                          JsSpecs.integer(),
                                          JsSpecs.longInteger(),
                                          JsSpecs.cons(JsNull.NULL))
                       );
  /**
   * Kafka producer spec
   */
  public static final JsSpec producerSpec =
      JsObjSpec.of(PRODUCER_PROPS,
                   propsSpec);

  public static final JsSpec consumerSpec =
      JsObjSpec.of(CONSUMER_PROPS,
                   propsSpec,
                   POLL_TIMEOUT_SEC,
                   JsSpecs.integer(n -> n > 1),
                   TOPICS,
                   JsSpecs.arrayOfStr()
                  );

  public static final JsObjSpec schemaSpec =
      JsObjSpec.of(SCHEMA_VALUE,
                   JsSpecs.str()
                  );

  public static final JsSpec channelSpec =
      JsObjSpec.of(PRODUCER,
                   JsSpecs.str(),
                   TOPIC,
                   JsSpecs.str(),
                   KEY_SCHEMA,
                   JsSpecs.str(),
                   VALUE_SCHEMA,
                   JsSpecs.str()
                  )
               .withOptKeys(KEY_SCHEMA,
                            VALUE_SCHEMA);


  public static final JsObjSpec global =
      JsObjSpec.of(KAFKA_ENTRIES,
                   JsObjSpec.of(COMMON_PROPS,
                                propsSpec,
                                PRODUCERS,
                                JsSpecs.mapOfSpec(producerSpec),
                                CONSUMERS,
                                JsSpecs.mapOfSpec(consumerSpec)

                               ),
                   CHANNELS,
                   JsSpecs.mapOfSpec(channelSpec)
                  );
}
