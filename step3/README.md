# Objective

1. Creating a simple java producer
1. Creating a simple java consumer

# Docker-compose

We are adding only 

```
  producer:
    build: producer/
    depends_on:
      - kafka-1
      - kafka-2
      - kafka-3

  consumer:
    build: consumer/
    depends_on:
      - kafka-1
      - kafka-2
      - kafka-3
```

We are using the [`build`](https://docs.docker.com/compose/compose-file/compose-file-v2/#build) feature of docker compose.

# Docker multi stage buids

in `step3/consumer` you'll see a `Dockerfile` using a [multi stage build](https://docs.docker.com/engine/userguide/eng-image/multistage-build/#use-multi-stage-builds) 

The first step is the following
```Dockerfile
FROM maven:3.5-jdk-8 as mavenBuild
COPY pom.xml pom.xml
COPY src src
RUN ["mvn"]
```

This is using a maven image, then copies the pom and sources, then execute mvn that produces a jar in `target/`

Please note the `as mavenBuild` that will reference all the artefacts of this image.

The second step is 

```Dockerfile
FROM java:8
COPY --from=mavenBuild ./target/*.jar ./
CMD ["bash", "-c", "java -jar *.jar"]
```

Using a java8 image we copy from the jar from the first build using is logical name `mavenBuild`, and specify the CMD. That's it.

That allows to prevent embedding maven and all its artefacts in the target image.

# Java

The consumer is an almost identical copy of https://kafka.apache.org/10/javadoc/index.html?org/apache/kafka/clients/consumer/KafkaConsumer.html

The producer is an almost identical copy of https://kafka.apache.org/10/javadoc/index.html?org/apache/kafka/clients/consumer/KafkaConsumer.html


# Run

You can then run it by `docker-compose up`

```
$ docker-compose up
$ docker-compose ps
      Name                   Command            State               Ports
-------------------------------------------------------------------------------------
step3_consumer_1    bash -c java -jar *.jar     Up
step3_kafka-1_1     /etc/confluent/docker/run   Up       9092/tcp
step3_kafka-2_1     /etc/confluent/docker/run   Up       9092/tcp
step3_kafka-3_1     /etc/confluent/docker/run   Up       9092/tcp
step3_producer_1    bash -c java -jar *.jar     Exit 0
step3_zookeeper_1   /etc/confluent/docker/run   Up       2181/tcp, 2888/tcp, 3888/tcp
$ docker-compose logs producer
...
producer_1   | Sending key 301 Value 301
producer_1   | Sending key 302 Value 302
producer_1   | Sending key 303 Value 303
producer_1   | Sending key 304 Value 304
producer_1   | Sending key 305 Value 305
producer_1   | Sending key 306 Value 306
producer_1   | Sending key 307 Value 307
producer_1   | Sending key 308 Value 308
producer_1   | Sending key 309 Value 309
producer_1   | Sending key 310 Value 310
producer_1   | Sending key 311 Value 311
producer_1   | Sending key 312 Value 312
producer_1   | Sending key 313 Value 313
$ docker-compose logs consumer
Attaching to step3_consumer_1
consumer_1   | log4j:WARN No appenders could be found for logger (org.apache.kafka.clients.consumer.ConsumerConfig).
consumer_1   | log4j:WARN Please initialize the log4j system properly.
consumer_1   | log4j:WARN See http://logging.apache.org/log4j/1.2/faq.html#noconfig for more info.
consumer_1   | Subscribing to sample
consumer_1   | log4j:WARN No appenders could be found for logger (org.apache.kafka.clients.consumer.ConsumerConfig).
consumer_1   | log4j:WARN Please initialize the log4j system properly.
consumer_1   | log4j:WARN See http://logging.apache.org/log4j/1.2/faq.html#noconfig for more info.
consumer_1   | Subscribing to sample
consumer_1   | Received offset = 370729, key = key 1, value = Value 1
consumer_1   | Received offset = 370730, key = key 2, value = Value 2
consumer_1   | Received offset = 370731, key = key 3, value = Value 3
consumer_1   | Received offset = 370732, key = key 4, value = Value 4
consumer_1   | Received offset = 370733, key = key 5, value = Value 5
```

