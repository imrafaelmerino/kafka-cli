# Kafka CLI Documentation


## Configuration File

The configuration file provided is just an example to illustrate how the Kafka CLI works. You can customize it to fit your needs by adding any number of consumers, channels, and producers, each with their own configurations. This flexibility allows you to tailor the CLI tool to your specific Kafka setup and requirements.

```json
{
  "conf": {
    "aliases": {
      "producer-start": "ps",
      "producer-list": "pls",
      "producer-stop": "pst",
      "producer-publish": "pb",
      "producer-publish-file": "pbf",
      "consumer-start": "cs",
      "consumer-list": "cls",
      "consumer-stop": "cst",
      "channel-list": "chls"
    },
    "welcome_message": "Welcome to kafka CLI! Go to https://github.com/imrafaelmerino/kafka-cli for further info",
    "session_file_dir": "/Users/rmerino/Projects/kafka-cli/",
    "colors": {
      "error": "\u001B[0;31m",
      "result": "\u001B[0;34m",
      "prompt": "\u001B[0;32m"
    }
  },
  "kafka": {
    "props": {
      "bootstrap.servers": "localhost:29092",
      "schema.registry.url": "http://localhost:8081"
    },
    "consumers": {
      "consumer1": {
        "topics": ["topic1"],
        "pollTimeoutSec": 10,
        "props": {
          "key.deserializer": "jsonvalues.spec.deserializers.confluent.ConfluentDeserializer",
          "value.deserializer": "jsonvalues.spec.deserializers.confluent.ConfluentDeserializer",
          "auto.offset.reset": "earliest",
          "enable.auto.commit": true,
          "group.id": "kafka.cli"
        }
      },
      "consumer2": {
        "topics": ["topic2"],
        "pollTimeoutSec": 10,
        "props": {
          "key.deserializer": "org.apache.kafka.common.serialization.StringDeserializer",
          "value.deserializer": "org.apache.kafka.common.serialization.StringDeserializer",
          "auto.offset.reset": "earliest",
          "enable.auto.commit": true,
          "group.id": "kafka.cli"
        }
      }
    },
    "producers": {
      "producer1": {
        "props": {
          "auto.register.schemas": "false",
          "key.serializer": "jsonvalues.spec.serializers.confluent.ConfluentSerializer",
          "value.serializer": "jsonvalues.spec.serializers.confluent.ConfluentSerializer",
          "acks": "all",
          "request.timeout.ms": 3000,
          "delivery.timeout.ms": 10000,
          "linger.ms": 2000
        }
      },
      "producer2": {
        "props": {
          "auto.register.schemas": "false",
          "key.serializer": "org.apache.kafka.common.serialization.StringSerializer",
          "value.serializer": "org.apache.kafka.common.serialization.StringSerializer",
          "acks": "all",
          "request.timeout.ms": 3000,
          "delivery.timeout.ms": 10000,
          "linger.ms": 2000
        }
      }
    }
  },
  "channels": {
    "channel1": {
      "producer": "producer1",
      "topic": "topic1",
      "key-generator": "client_id",
      "value-generator": "client_profile",
      "key-schema": "{\"type\":\"record\",\"name\":\"key\",\"namespace\":\"com.example.key\",\"fields\":[{\"name\":\"_id\",\"type\":\"string\"}]}",
      "value-schema": "{\"type\":\"record\",\"name\":\"record\",\"namespace\":\"com.example.record\",\"fields\":[{\"name\":\"c\",\"type\":\"string\"},{\"name\":\"b\",\"type\":\"string\"},{\"name\":\"a\",\"type\":\"int\"}]}"
    },
    "channel2": {
      "producer": "producer2",
      "value-generator": "text",
      "topic": "topic2"
    }
  }
}
```

### Configuration File Explanation

The configuration file allows you to set up and manage your Kafka CLI tool effectively.

#### `conf` Section

This section includes general configuration settings for the CLI tool. All the conf section is
optional

- **aliases**: Defines shortcuts for commands.

  ```json
  {
    "aliases": {
      "producer-start": "ps",
      "producer-list": "pls",
      "producer-stop": "pst",
      "producer-publish": "pb",
      "producer-publish-file": "pbf",
      "consumer-start": "cs",
      "consumer-list": "cls",
      "consumer-stop": "cst",
      "channel-list": "chls"
    }
  }
  ```

  For example, instead of typing `producer-start`, you can use the alias `ps`.

- **welcome_message**: The message displayed when the CLI is started.

  ```json
  "welcome_message": "Welcome to kafka CLI! Go to https://github.com/imrafaelmerino/kafka-cli for further info"
  ```

- **session_file_dir**: Directory where session files, including all the commands typed by the user
  and their outputs, are stored.

  ```json
  "session_file_dir": "/Users/rmerino/Projects/kafka-cli/"
  ```

- **colors**: 

  The colors in the configuration file are specified using ANSI escape codes. These codes are used
  to control the formatting, color, and other output options on text terminals. Here’s a brief
  explanation of the ANSI escape codes used in the configuration file:

- **Error Messages**: `\u001B[0;31m` - This sets the text color to red.
- **Result Messages**: `\u001B[0;34m` - This sets the text color to blue.
- **Prompt Messages**: `\u001B[0;32m` - This sets the text color to green.

These codes are prefixed with `\u001B` (which represents the escape character) followed by `[`, and
then the color code (e.g., `0;31m` for red). Here’s a breakdown of the codes:

- `0` indicates the default text style.
- `31` sets the text color to red.
- `34` sets the text color to blue.
- `32` sets the text color to green.


#### `kafka` Section

This section includes configuration settings related to Kafka. It is recommended to use just one
producer, as this is the most efficient way of publishing to Kafka. Only create different producers
when you need different configurations. Note that the schema registry is optional and can be removed
from the common section and added only to the producers and consumers that use the registry.

- **props**: General Kafka properties.

  ```json
  "props": {
    "bootstrap.servers": "localhost:29092",
    "schema.registry.url": "http://localhost:8081"
  }
  ```

  - `bootstrap.servers`: The Kafka server address (mandatory)
  - `schema.registry.url`: The URL for the schema registry (if necessary).

- **consumers**: Configuration for Kafka consumers. When using Avro to serialize the data, you must use the deserializers from [avro-spec](https://github.com/imrafaelmerino/avro-spec),
like in the following example in the `consumer1`

  ```json
  "consumers": {
    "consumer1": {
      "topics": [ "topic1" ],
      "pollTimeoutSec": 10,
      "props": {
        "key.deserializer": "jsonvalues.spec.deserializers.confluent.ConfluentDeserializer",
        "value.deserializer": "jsonvalues.spec.deserializers.confluent.ConfluentDeserializer",
        "auto.offset.reset": "earliest",
        "enable.auto.commit": true,
        "group.id": "kafka.cli"
      }
    },
    "consumer2": {
      "topics": [ "topic2" ],
      "pollTimeoutSec": 10,
      "props": {
        "key.deserializer": "org.apache.kafka.common.serialization.StringDeserializer",
        "value.deserializer": "org.apache.kafka.common.serialization.StringDeserializer",
        "auto.offset.reset": "earliest",
        "enable.auto.commit": true,
        "group.id": "kafka.cli"
      }
    }
  }
  ```

  - `topics`: List of topics the consumer subscribes to.
  - `pollTimeoutSec`: Poll timeout in seconds.
  - `props`: Properties specific to each consumer, such as deserializers, offset reset policy, and
    group ID.

- **producers**: Configuration for Kafka producers. When using Avro, you must use the serializers from [avro-spec](https://github.com/imrafaelmerino/avro-spec),
  like in the following example in the `producer1`
  ```json
  "producers": {
    "producer1": {
      "props": {
        "auto.register.schemas": "false",
        "key.serializer": "jsonvalues.spec.serializers.confluent.ConfluentSerializer",
        "value.serializer": "jsonvalues.spec.serializers.confluent.ConfluentSerializer",
        "acks": "all",
        "request.timeout.ms": 3000,
        "delivery.timeout.ms": 10000,
        "linger.ms": 2000
      }
    },
    "producer2": {
      "props": {
        "auto.register.schemas": "false",
        "key.serializer": "org.apache.kafka.common.serialization.StringSerializer",
        "value.serializer": "org.apache.kafka.common.serialization.StringSerializer",
        "acks": "all",
        "request.timeout.ms": 3000,
        "delivery.timeout.ms": 10000,
        "linger.ms": 2000
      }
    }
  }
  ```
  - `props`: Properties specific to each producer, such as serializers, acknowledgments, timeouts,
    and linger settings.

#### `channels` Section

This section defines channels, which are logical groupings that link producers to topics and define
schemas and generators for keys and values. Generators specified in the channels are necessary to
use the `producer-publish` command, which uses them to generate the data.

```json
"channels": {
  "channel1": {
    "producer": "producer1",
    "topic": "topic1",
    "key-generator": "client_id",
    "value-generator": "client_profile",
    "key-schema": "{\"type\":\"record\",\"name\":\"key\",\"namespace\":\"com.example.key\",\"fields\":[{\"name\":\"_id\",\"type\":\"string\"

}]}",
    "value-schema": "{\"type\":\"record\",\"name\":\"record\",\"namespace\":\"com.example.record\",\"fields\":[{\"name\":\"c\",\"type\":\"string\"},{\"name\":\"b\",\"type\":\"string\"},{\"name\":\"a\",\"type\":\"int\"}]}"
  },
  "channel2": {
    "producer": "producer2",
    "value-generator": "text",
    "topic": "topic2"
  }
}
```

- **producer**: The name of the producer associated with this channel.
- **topic**: The Kafka topic this channel publishes to.
- **key-generator**: The generator used for the keys.
- **value-generator**: The generator used for the values.
- **key-schema**: The Avro schema for the keys.
- **value-schema**: The Avro schema for the values.

### Summary

This configuration file allows you to set up and manage your Kafka CLI tool effectively by defining
shortcuts, message formats, Kafka properties, consumer and producer settings, and channels linking
producers to topics with appropriate schemas and generators. Remember, it's best to use a single producer for efficiency unless different configurations are
necessary. For Avro data, use the serializers and deserializers from
[avro-spec](https://github.com/imrafaelmerino/avro-spec), otherwise you'll get an error

## List of Commands

### Producer Commands

- **producer-list** (alias: pls)

  - Usage: `producer-list`
  - Description: Lists all Kafka producers along with their statuses (up or down).
  - Output: The list of producers with their names and statuses.

- **producer-start** (alias: ps)

  - Usage: `producer-start [producer-name]`
  - Description: Starts a Kafka producer using the provided configuration.
  - Parameters:
    - `producer-name` (optional): The name of the producer to start. If not provided, the user will
      be prompted to select from a list of available producers.
  - Output:
    - Success: "Producer `<producer-name>` started!"
    - Failure: Appropriate error message if the configuration is not found or if the producer is
      already started.

- **producer-stop** (alias: pst)

  - Usage: `producer-stop [producer-name]`
  - Description: Stops a running Kafka producer.
  - Parameters:
    - `producer-name` (optional): The name of the producer to stop. If not provided, the user will
      be prompted to select from a list of available producers.
  - Output:
    - Success: "Producer `<producer-name>` closed!"
    - Failure: Appropriate error message if the producer is not found or is already closed.

- **producer-publish** (alias: pb)

  - Usage: `producer-publish [channel-name]`
  - Description: Sends a generated key-value pair or value to a specified Kafka topic using the
    appropriate Kafka producer.
  - Parameters:
    - `channel-name` (optional): The name of the channel to publish to. If not provided, the user
      will be prompted to select from a list of available channels with an `up` status.
  - Output:
    - Success: A message indicating that the record was successfully sent, along with the offset and
      partition details.
    - Failure: Appropriate error message if the producer is not started or if the channel name is
      invalid.

- **producer-publish-file** (alias: pbf)
  - Usage: `producer-publish-file {channel} {file_path}`
  - Description: Publishes records from a file to the specified Kafka channel.
  - Parameters:
    - `channel`: The name of the Kafka channel to publish to.
    - `file_path`: The absolute path of the file containing records to publish.
  - Output:
    - Success: Messages indicating that the records were successfully sent, along with the offset
      and partition details.
    - Failure: Appropriate error message if the producer is not started or if the channel name is
      invalid.

### Consumer Commands

- **consumer-list** (alias: cls)

  - Usage: `consumer-list`
  - Description: Lists all Kafka consumers along with their statuses (up or down).
  - Output: The list of consumers with their names and statuses.

- **consumer-start** (alias: cs)

  - Usage: `consumer-start [consumer-name] [-verbose]`
  - Description: Starts a Kafka consumer using the provided configuration.
  - Parameters:
    - `consumer-name` (optional): The name of the consumer to start. If not provided, the user will
      be prompted to select from a list of available consumers.
    - `-verbose` (optional): If provided, the consumer will output a verbose log of the received
      messages.
  - Output:
    - Success: "Consumer `<consumer-name>` started!"
    - Failure: Appropriate error message if the configuration is not found or if the consumer is
      already started.

- **consumer-stop** (alias: cst)

  - Usage: `consumer-stop [consumer-name]`
  - Description: Stops a running Kafka consumer.
  - Parameters:
    - `consumer-name` (optional): The name of the consumer to stop. If not provided, the user will
      be prompted to select from a list of available consumers.
  - Output:
    - Success: "Consumer `<consumer-name>` closed!"
    - Failure: Appropriate error message if the consumer is not found or is already closed.

- **consumer-commit**
  - Usage: `consumer-commit [consumer-name]`
  - Description: Sends an asynchronous commit request to the specified consumer.
  - Parameters:
    - `consumer-name`: The name of the consumer to commit.
  - Output:
    - Success: "Commit request sent to consumer `<consumer-name>`!"

### Channel Commands

- **channel-list** (alias: chls)
  - Usage: `channel-list`
  - Description: Prints out the list of channels defined in the configuration file.
  - Output: The list of channels with their names, associated producers, statuses, and topics.

## Examples of Commands and Outputs

```plaintext
Welcome to kafka CLI! Go to https://github.com/imrafaelmerino/kafka-cli for further info
~ producer

producer-list             (pls)
producer-publish          (pb)
producer-publish-file     (pbf)
producer-start            (ps)
producer-stop             (pst)

~ producer-list

Name                 Status
producer1            down
producer2            down

~ producer-start

producer1
producer2

Type the producer name (One of the above):
~ producer1

Producer `producer1` started!

~ producer-list

Name                 Status
producer1            up
producer2            down

~ consumer-list

Name                 Status
consumer2            down
consumer1            down

~ consumer-start

consumer2
consumer1

Type the consumer name (choose one of the above):
~ consumer2

Do you want a verbose output of the received messages? (yes | no)
~ yes

Consumer `consumer2` started!

~ consumer-list

Name                 Status
consumer2            up
consumer1            down

~ channel-list

Name                 Producer             Status               Topic
--------------------------------------------------------------------------------
channel1             producer1            up                   topic1
channel2             producer2            down                 topic2
```

---
