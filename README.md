# Kafka Connect Sound

A source connector for reading data from a microphone and streaming it into [Apache Kafka], and a
sink connector for streaming data from [Apache Kafka] straight to your computer's speakers, both
implemented on top of the [Kafka Connect] framework

1. [Overview](#overview)
1. [Installation](#installation)
1. [Configuration](#configuration)
1. [Quickstart](#quickstart)
1. [Data Format](#data-format)
1. [Offsets Tracking](#offsets-tracking)
1. [Issue Tracking](#issue-tracking)
1. [TODO](#todo)

## Overview

These connectors use the [Java sound API] available in the standard library to record and play
headerless audio of various formats.

Note that an earlier version of these connectors required [SoX] to run; current versions do not.


## Installation


### Local build

The connector can be built locally by running the following command:

```bash
mvn package
```

and then copying and unzipping the zip archive generated in the `target/components/packages`
directory onto the plugin path or classpath for your Kafka Connect worker(s).

### Maven Central

In the near future, new releases will be published to  [Maven Central]. This is
currently blocked on receiving permission to publish to the `io.github.c0urante`
namespace, which should be granted in the next few days (those folks tend to be
very responsive).

### Confluent Hub
Releases to Confluent Hub have been discontinued.


## Configuration

[Microphone Source Docs](docs/source-connector-config.md)

[Microphone Source Example](config/kafka-connect-microphone.properties)

[Speakers Sink Docs](docs/sink-connector-config.md)

[Speakers Sink Example](config/kafka-connect-speakers.properties)


## Quickstart

Assumptions:

- Maven 3+ is installed
- Zookeeper is running and listening on localhost:2181
- Kafka is running and listening on localhost:9092
- Current directory is the root of the repository

### Source and sink: record yourself, and play it back through your speakers
```bash
# Build the project
mvn clean package

# Create the topic that the connectors will write to and read from (important: only need one partition)
kafka-topics --zookeeper localhost:2181 --create --topic voice-wav --partitions 1 --replication-factor 1

# Run the microphone source connector, and make some noise! Close down the worker with ctrl+C when you're finished
connect-standalone config/connect-standalone.properties config/kafka-connect-microphone.properties

# Run the speakers sink connector, and listen as the noise you made is played back out your speakers!
connect-standalone config/connect-standalone.properties config/kafka-connect-speakers.properties
```

### Sink only: import an audio file into Kafka, and play it through your speakers

Additional assumptions:

- [Kafka Tools] is installed and available on the current `$PATH`

```bash
# Build the project
mvn clean package

# Create the topic that the connector will read from (important: only need one partition)
kafka-topics --bootstrap-server localhost:9092 --create --topic queen --partitions 1 --replication-factor 1

# Read an audio file into Kafka
# Recorded with SoX, using the default audio format (sample size, channels, etc.) for the connectors:
#   sox --buffer 2048 -d -t raw -b 16 -L -e signed-integer -r 44100 -c 1 src/test/resources/audio/queen.raw
# To play this with SoX for debugging purposes:
#   sox --buffer 2048 -t raw -b 16 -L -e signed-integer -r 44100 -c 1 src/test/resources/audio/queen.raw -d
kafka-binary-producer --topic queen --broker-list localhost:9092 < src/test/resources/audio/queen.raw

# Run the speakers sink connector, and listen to some music!
connect-standalone config/connect-standalone.properties config/kafka-connect-speakers-music.properties
```


## Data Format

### Microphone source

The microphone source produces data with a fixed byte array schema.

### Speakers sink

The speakers sink connector can consume data with a type of either byte or byte array. Anything else
will cause the connector to fail.

### Note on converters

Because these connectors are hardcoded to work exclusively with bytes and byte arrays, it's
recommended that the `ByteArrayConverter` be used with them unless there are upstream or downstream
limitations that require data to be serialized in a specific format.


## Offsets Tracking

### Behavior
These connectors do not perform offsets tracking.

For the source connector, this means that its tasks will only record audio produced in real
time when they are running. Rebalances, worker restarts, and task failures will all run the
risk of dropping audio.

For the sink connector, this means that its tasks will always resume reading from Kafka
based on the value for their consumers' [auto.offset.reset] property. For Kafka Connect,
the default behavior is to read from the beginning of topics. To override this for all sink
connectors on a cluster and cause them to read from the ends of topics, add this property
to your Kafka Connect worker config file:
```properties
consumer.auto.offset.reset = latest
```

And, for Kafka Connect clusters version 3.0.0 and beyond, you can override this for a
single sink connector at a time by adding this property to the connector's config file:
```properties
consumer.override.auto.offset.reset = latest
```
(for standalone Connect workers)

```json
{
  "consumer.override.auto.offset.reset": "latest"
}
```
(for distributed Connect clusters)

### Rationale

For the source connector, this is a matter of feasibility: we can't go back in time
and re-record audio that we failed to record while a task wasn't running.

For the sink connector, we could track the offsets of records that we were able to
successfully write to the operating system, and commit those periodically. However,
the implementation complexity of this would be non-trivial since a single large record may
be broken down into several buffers that are written individually to the OS, and several
smaller records may be grouped together into a single buffer. Additionally, there's no
guarantee that audio that has been dispatched to the OS has actually been played out
of the speakers on the machine. We could possibly look into tracking this more precisely,
but it doesn't seem worth the bother.


## Issue Tracking

Issues are tracked on GitHub. If there's a problem you're running into
with the connector or a feature missing that you'd like to see, please
open an issue.

If there's a small bug or typo that you'd like to fix, feel free to open
a PR without filing an issue first and tag @C0urante for review.


## TODO

- [x] Sink connector that writes to speakers
- [x] Source connector that reads from a microphone
- [x] Easy way to read static audio files into Kafka ([Kafka Tools])
- [ ] Publish to [Maven Central]

PRs welcome and encouraged!

[Kafka Connect]: https://docs.confluent.io/current/connect
[Apache Kafka]: https://kafka.apache.org
[Maven Central]: https://search.maven.org
[SoX]: http://sox.sourceforge.net
[Kafka Tools]: https://github.com/C0urante/kafka-tools
[ksqlDB]: https://github.com/confluentinc/ksql
[Java sound API]: https://docs.oracle.com/javase/8/docs/technotes/guides/sound/programmer_guide/contents.html
[auto.offset.reset]: https://kafka.apache.org/documentation.html#consumerconfigs_auto.offset.reset