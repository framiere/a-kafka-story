# Objective

1. Run a zookeeper and single kafka broker
1. discover how `confluentinc/cp-kafka` is built and run

# `docker-compose`

Let's start with a simple `docker-compose.yml` file

```yml
version: '2'
services:
  zookeeper:
    image: confluentinc/cp-zookeeper
    hostname: zookeeper

  kafka:
    image: confluentinc/cp-kafka
    hostname: kafka
    depends_on:
      - zookeeper
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: 'zookeeper:2181'
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092      
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1          
```

You can discover the configuration options at

* https://docs.confluent.io/current/installation/docker/docs/configuration.html#zookeeper
* https://docs.confluent.io/current/installation/docker/docs/configuration.html#confluent-kafka-cp-kafka

Note: `KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR` is set to 1 as we have a single broker (the default is 3)

then run it 

```sh
$ docker-compose up -d
$ docker-compose ps
      Name                   Command            State                     Ports
--------------------------------------------------------------------------------------------------
step1_kafka-1       /etc/confluent/docker/run   Up
step1_zookeeper_1   /etc/confluent/docker/run   Up      0.0.0.0:2181->2181/tcp, 2888/tcp, 3888/tcp
```

Fine, looks like `zookeeper` and `kafka` are up.


# `confluentinc/cp-kafka` image walkthrough

Let's see how `confluentinc/cp-kafka` is built

You can see at the end of [Dockerfile](https://github.com/confluentinc/cp-docker-images/blob/4.0.x/debian/kafka/Dockerfile) that the container runs `/etc/confluent/docker/run`

```dockerfile
CMD ["/etc/confluent/docker/run"]
```

Let's look into the [`/etc/confluent/docker/run`](https://github.com/confluentinc/cp-docker-images/blob/4.0.x/debian/kafka/include/etc/confluent/docker/run) script 

```sh
echo "===> ENV Variables ..."
env | sort

echo "===> User"
id

echo "===> Configuring ..."
/etc/confluent/docker/configure

echo "===> Running preflight checks ... "
/etc/confluent/docker/ensure

echo "===> Launching ... "
exec /etc/confluent/docker/launch

```

Ok, seems simple. This`ensure` tool seems interesting, let's take a look into [its source](https://github.com/confluentinc/cp-docker-images/blob/4.0.x/debian/kafka/include/etc/confluent/docker/ensure)


```sh
export KAFKA_DATA_DIRS=${KAFKA_DATA_DIRS:-"/var/lib/kafka/data"}
echo "===> Check if $KAFKA_DATA_DIRS is writable ..."
dub path "$KAFKA_DATA_DIRS" writable
```
Ok, this `dub` command checks if the `KAFKA_DATA_DIRS` path is writable.

```
echo "===> Check if Zookeeper is healthy ..."
cub zk-ready "$KAFKA_ZOOKEEPER_CONNECT" "${KAFKA_CUB_ZK_TIMEOUT:-40}"
```
Oh ! this `zk-ready` command is neat as zookeeper readiness that cannot be expressed in `docker-compose`.

As a reminder [`depends_on`](https://docs.docker.com/compose/compose-file/#depends_on) will only wait for the image to start, not for the service within the container to be up & running.


These `dub` and `cub` commands seem very interesting, can they do more ?

# `confluentinc/cp-base` image

These command are installed in the `confluentinc/cp-base` image 

https://github.com/confluentinc/cp-docker-images/blob/4.0.x/debian/base/Dockerfile#L79

```
pip install --no-cache-dir git+https://github.com/confluentinc/confluent-docker-utils@v0.0.20
```

Ok, fine, it's python based, let's follow the code and we stumble on https://github.com/confluentinc/confluent-docker-utils/tree/v0.0.20/confluent/docker_utils

## `dub` : Docker utility belt.

https://github.com/confluentinc/confluent-docker-utils/blob/v0.0.20/confluent/docker_utils/dub.py

1. template : Uses Jinja2 and environment variables to generate configuration files.
2. ensure: ensures that a environment variable is set. Used to ensure required properties.
3. ensure-atleast-one: ensures that atleast one of environment variable is set. Used to ensure required properties.
4. wait: waits for a service to become available on a host:port.
5. path: Checks a path for permissions (read, write, execute, exists)
These commands log any output to stderr and returns with exitcode=0 if successful, exitcode=1 otherwise.

Checkout the [confluent doc](https://docs.confluent.io/current/installation/docker/docs/development.html#docker-utility-belt-dub).


## `cub` : Confluent utility belt 
https://github.com/confluentinc/confluent-docker-utils/blob/v0.0.20/confluent/docker_utils/cub.py

1. kafka-ready : Ensures a Kafka cluster is ready to accept client requests.
2. zk-ready: Ensures that a Zookeeper ensemble is ready to accept client requests.
3. sr-ready: Ensures that Schema Registry is ready to accept client requests.
4. kr-ready: Ensures that Kafka REST Proxy is ready to accept client requests.
5. listeners: Derives the listeners property from advertised.listeners.
6. ensure-topic: Ensure that topic exists and is vaild.

Checkout the [confluent doc](https://docs.confluent.io/current/installation/docker/docs/development.html#confluent-platform-utility-belt-cub).

# Java 

```
$ docker-compose exec kafka java -version
openjdk version "1.8.0_102"
OpenJDK Runtime Environment (Zulu 8.17.0.3-linux64) (build 1.8.0_102-b14)
OpenJDK 64-Bit Server VM (Zulu 8.17.0.3-linux64) (build 25.102-b14, mixed mode)
```

See the [OpenJDK Zulu](https://www.azul.com/downloads/zulu/) based installation in [`confluentinc/cp-base`](https://github.com/confluentinc/cp-docker-images/blob/4.0.x/debian/base/Dockerfile#L81)

# Let's see if everything is working

Let's send a message
```sh
$ docker-compose exec kafka bash -c "echo story | kafka-console-producer --broker-list localhost:9092 --topic sample"
>>%
```

And retrieve it

```sh
$ docker-compose exec kafka kafka-console-consumer --bootstrap-server localhost:9092 --topic sample --from-beginning --max-messages=1
story
Processed a total of 1 messages
```

Ok all good

Let's see the topics

```sh
$ docker-compose exec kafka kafka-topics  --zookeeper zookeeper:2181 --list
__consumer_offsets
sample
```

And focus on `sample`

```
$ docker-compose exec kafka kafka-topics  --zookeeper zookeeper:2181 --describe --topic sample
Topic:sample	PartitionCount:1	ReplicationFactor:1	Configs:
	Topic: sample	Partition: 0	Leader: 1	Replicas: 1	Isr: 1
```    


# Stop the containers
We can stop the containers now

```
$ docker-compose stop
Stopping step1_kafka_1    ...
Stopping step1_zookeeper_1 ...
kafka_1     | [2017-12-31 16:33:47,690] INFO Terminating process due to signal SIGTERM (io.confluent.support.metrics.SupportedKafka)
kafka_1     | [2017-12-31 16:33:47,697] INFO [KafkaServer id=1] shutting down (kafka.server.KafkaServer)
kafka_1     | [2017-12-31 16:33:47,698] INFO [KafkaServer id=1] Starting controlled shutdown (kafka.server.KafkaServer)
kafka_1     | [2017-12-31 16:33:47,712] INFO [Controller id=1] Shutting down broker 1 (kafka.controller.KafkaController)
...
...
Stopping step1_kafka_1    ... done
kafka_1     | [2017-12-31 16:33:51,001] INFO [KafkaServer id=1] shut down completed (kafka.server.KafkaServer)
step1_kafka_1 exited with code 143
Stopping step1_zookeeper_1 ... done
$ docker-compose ps
      Name                   Command             State     Ports
----------------------------------------------------------------
step1_kafka_1      /etc/confluent/docker/run   Exit 143
step1_zookeeper_1   /etc/confluent/docker/run   Exit 143
```

# The full action ?

[![screencast](https://asciinema.org/a/gZsALOy3FH2JEYnQJZfnL0p2N.png)](https://asciinema.org/a/gZsALOy3FH2JEYnQJZfnL0p2N?autoplay=1)
