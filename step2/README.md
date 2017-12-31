# Objective

1. Run a zookeeper and many kafka brokers

# `docker-compose`

In this example, we'll focus on kafka, no need to setup many zookeeper, it would be the same

```yml
version: '2'
services:
  zookeeper:
    image: confluentinc/cp-zookeeper
    hostname: zookeeper
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181

  kafka-1:
    image: confluentinc/cp-kafka
    hostname: kafka-1
    depends_on:
      - zookeeper
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka-1:19092

  kafka-2:
    image: confluentinc/cp-kafka
    hostname: kafka-2
    depends_on:
      - zookeeper
    environment:
      KAFKA_BROKER_ID: 2
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka-2:29092

  kafka-3:
    image: confluentinc/cp-kafka
    hostname: kafka-3
    depends_on:
      - zookeeper
    environment:
      KAFKA_BROKER_ID: 3
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka-3:39092
```

You can discover the configuration options at

* https://docs.confluent.io/current/installation/docker/docs/configuration.html#zookeeper
* https://docs.confluent.io/current/installation/docker/docs/configuration.html#confluent-kafka-cp-kafka

Note: Please note `KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR` is not defined anymore, we have now enough kafka brokers to satisfy the defaut replication factor of 3.

then run it 

```sh
$ docker-compose up -d
$ docker-compose ps
      Name                   Command            State                     Ports
--------------------------------------------------------------------------------------------------
step2_kafka-1_1     /etc/confluent/docker/run   Up
step2_kafka-2_1     /etc/confluent/docker/run   Up
step2_kafka-3_1     /etc/confluent/docker/run   Up
step2_zookeeper_1   /etc/confluent/docker/run   Up      0.0.0.0:2181->2181/tcp, 2888/tcp, 3888/tcp
```

Fine, looks like `zookeeper` and multiple `kafka` are up.


# Let's see if everything is working

Let's send a message
```sh
$ docker-compose exec kafka-1 bash -c "echo story|kafka-console-producer --broker-list localhost:19092 --topic sample"
>>%
```

And retrieve it

```sh
$ docker-compose exec kafka-1 kafka-console-consumer --bootstrap-server localhost:19092 --topic sample --from-beginning --max-messages=1
story
Processed a total of 1 messages
```

Ok all good

Let's see the topics

```sh
$ docker-compose exec kafka-1 kafka-topics  --zookeeper zookeeper:2181 --list
__consumer_offsets
sample
```

And focus on `sample`

```
$ docker-compose exec kafka-1 kafka-topics  --zookeeper zookeeper:2181 --describe --topic sample
Topic:sample	PartitionCount:1	ReplicationFactor:1	Configs:
	Topic: sample	Partition: 0	Leader: 1	Replicas: 1	Isr: 1
```    

