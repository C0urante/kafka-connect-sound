# Kafka Connect Sound

A source connector for reading data from a microphone and streaming it into [Apache Kafka], and a
sink connector for streaming data from [Apache Kafka] straight to your computer's speakers, both
implemented on top of the [Kafka Connect] framework

1. [Overview](#overview)
1. [Installation](#installation)
1. [Configuration](#configuration)
1. [Quickstart](#quickstart)
1. [Offset Tracking](#offset-tracking)
1. [Data Format](#data-format)
1. [Issue Tracking](#issue-tracking)
1. [TODO](#todo)

## Overview

These connectors are essentially thin wrappers around [SoX] that work by invoking its `play` and
`rec` commands.

In order to run either connector, you must have `SoX` installed onto your machine and available on
the worker's `$PATH` with the names `play` and `rec`. See the [SoX Installation] page if you do not
already have `SoX` installed on your machine. 


## Installation


### Via Confluent Hub

TODO

### Local build

The connector can be built locally by running the following command:

```bash
mvn package
```

and then copying and unzipping the zip archive generated in the `target/components/packages`
directory onto the plugin path or classpath for your Kafka Connect worker(s).

## Configuration


[Microphone Source Docs](docs/source-connector-config.md)

[Microphone Source Example](config/kafka-connect-microphone.properties)

[Speakers Sink Docs](docs/sink-connector-config.md)

[Speakers Sink Example](config/kafka-connect-speakers-voice.properties)

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
connect-standalone config/connect-standalone.properties config/kafka-connect-speakers-voice.properties
```

### Sink only: import an audio file into Kafka, and play it through your speakers

Additional assumptions:

- [Kafka Tools] is installed and available on the current `$PATH`

```bash
# Build the project
mvn clean package

# Create the topic that the connectors will write to and read from (important: only need one partition)
kafka-topics --zookeeper localhost:2181 --create --topic music-mp3 --partitions 1 --replication-factor 1

# Run the microphone source connector, and make some noise! Close down the worker with ctrl+C when you're finished
connect-standalone config/connect-standalone.properties config/kafka-connect-microphone.properties

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
- [ ] Add configurability for location of sox executable
- [ ] Look into streaming consecutive separate files into same sink task
- [ ] Look into picking up from the middle of an audio file (currently breaks because headers are not present)
- [ ] Publish to [Confluent Hub]
- [ ] Maybe write some nifty SMTs, streams jobs, and/or [ksqlDB] queries to do things like distortion, voice recognition, etc.?

PRs welcome and encouraged!

[Kafka Connect]: https://docs.confluent.io/current/connect
[Apache Kafka]: https://kafka.apache.org
[Confluent Hub]: https://confluent.io/hub
[SoX]: http://sox.sourceforge.net
[SoX Installation]: https://sourceforge.net/projects/sox/files/sox
[Kafka Tools]: https://github.com/C0urante/kafka-tools
[ksqlDB]: https://github.com/confluentinc/ksql