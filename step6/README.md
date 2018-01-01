# Objective

1. Setup our dev environment for local development

# Docker

Before that all the operations on the cluster where made in docker-land.

As we will do development, we want to access the cluster from the outside.

To simplify things, we'll use the `host` [network_mode](https://docs.docker.com/compose/compose-file/#network_mode) on all components

Note: I remapped the ports too for the docker4mac users out there that are hitting the [`host` limitations](https://docs.docker.com/docker-for-mac/networking/#known-limitations-use-cases-and-workarounds)

As we are now using localhost, we need to adverstise the right name for the kafka brokers:

```yml
  kafka-1:
    image: confluentinc/cp-kafka
    hostname: kafka-1
    depends_on:
      - zookeeper
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka-1:19092
      KAFKA_DEFAULT_REPLICATION_FACTOR: 3
```

becomes

```yml
  kafka-1:
    image: confluentinc/cp-kafka
    network_mode: host
    ports: 
      - "29092:29092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:19092
      KAFKA_DEFAULT_REPLICATION_FACTOR: 3
```

For the telegraf.conf we need to adapt the broker list

```
[agent]
interval = "10s"

[[inputs.docker]]
endpoint = "unix:///tmp/docker.sock"

[[outputs.kafka]]
brokers = ["localhost:19092","localhost:29092","localhost:39092"]
topic = "telegraf"
```

And same for the `telegraf-topic` 

```yml
  telegraf-topic:
    image: confluentinc/cp-kafka
    network_mode: host
    command: kafka-topics --zookeeper localhost:2181 --create --topic telegraf --partitions 20 --replication-factor 3 
    depends_on:
      - zookeeper
```

We can now access to kafka via our development environment.