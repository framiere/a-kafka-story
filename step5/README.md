# Objective

1. Better kafka default settings

# Telegraf

We want to have better topic defaults, let's look into https://kafka.apache.org/documentation/#brokerconfigs

Let's add `KAFKA_DEFAULT_REPLICATION_FACTOR: 3` configuration to all kafka brokers, and disable the automatic topic creation with `KAFKA_AUTO_CREATE_TOPICS_ENABLED`

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
      KAFKA_AUTO_CREATE_TOPICS_ENABLED: "false"
```

We also create the `telegraf` topic with the right number of partitions from the get-go, and we rely on the `cub` tool to wait for zookeeper to be up before that.

```yml
  telegraf-topic:
    image: confluentinc/cp-kafka
    command: bash -c "cub kafka-ready -z zookeeper:2181 1 30 && kafka-topics --zookeeper zookeeper:2181 --create --topic telegraf --partitions 10 --replication-factor 3"
    depends_on:
      - zookeeper
```

Let' run it `docker-compose up` and verify

```
$ docker-compose exec kafka-1 kafka-topics  \
    --zookeeper zookeeper:2181 \
    --describe \
    --topic telegraf
Topic:telegraf	PartitionCount:10	ReplicationFactor:3	Configs:
	Topic: telegraf	Partition: 0	Leader: 2	Replicas: 2,1,3	Isr: 2,1,3
	Topic: telegraf	Partition: 1	Leader: 3	Replicas: 3,2,1	Isr: 3,2,1
	Topic: telegraf	Partition: 2	Leader: 1	Replicas: 1,3,2	Isr: 1,3,2
	Topic: telegraf	Partition: 3	Leader: 2	Replicas: 2,3,1	Isr: 2,3,1
	Topic: telegraf	Partition: 4	Leader: 3	Replicas: 3,1,2	Isr: 3,1,2
	Topic: telegraf	Partition: 5	Leader: 1	Replicas: 1,3,2	Isr: 1,3,2
	Topic: telegraf	Partition: 6	Leader: 2	Replicas: 2,3,1	Isr: 2,3,1
	Topic: telegraf	Partition: 7	Leader: 3	Replicas: 3,1,2	Isr: 3,1,2
	Topic: telegraf	Partition: 8	Leader: 1	Replicas: 1,2,3	Isr: 1,2,3
	Topic: telegraf	Partition: 9	Leader: 2	Replicas: 2,1,3	Isr: 2,1,3
```

All good!

# The full action ?

[![screencast](https://asciinema.org/a/RLEDOaMNKJs0SoUPuo5GPHab9.png)](https://asciinema.org/a/RLEDOaMNKJs0SoUPuo5GPHab9?autoplay=1)
