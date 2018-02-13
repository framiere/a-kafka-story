# Objective 

Getting Kafka Connect and Schema registry setup 


# Kafka Connect

Let's post it

```
$ curl -s -XPOST -H "Content-Type: application/json; charset=UTF-8" http://localhost:8083/connectors/ -d '
{
    "name": "my-mysql-connector3",
    "config": {
      "connector.class":"io.confluent.connect.jdbc.JdbcSourceConnector",
      "tasks.max":"10",
      "connection.url":"jdbc:mysql://mysql:3306/db?user=user&password=password&useSSL=false",
      "table.whitelist":"application",
      "mode":"timestamp+incrementing",
      "timestamp.column.name":"last_modified",
      "incrementing.column.name":"id",
      "topic.prefix":"mysql2-",
      "key.ignore": true,
      "key.converter.schema.registry.url": "http://schema-registry:8082",
      "value.converter": "io.confluent.connect.avro.AvroConverter",
      "value.converter.schema.registry.url": "http://schema-registry:8082",
      "schema.ignore": true

    }
}
' | jq .
{
  "name": "my-mysql-connector",
  "config": {
    "connector.class": "io.confluent.connect.jdbc.JdbcSourceConnector",
    "tasks.max": "10",
    "connection.url": "jdbc:mysql://mysql:3306/db?user=user&password=password&useSSL=false",
    "table.whitelist": "application",
    "mode": "timestamp+incrementing",
    "timestamp.column.name": "last_modified",
    "incrementing.column.name": "id",
    "topic.prefix": "mysql-",
    "name": "my-mysql-connector"
  },
  "tasks": [],
  "type": null
}
```


Let's see its status

```
$ curl -s localhost:8083/connectors/my-mysql-connector/status | jq .
{
  "name": "my-mysql-connector",
  "connector": {
    "state": "RUNNING",
    "worker_id": "connect:8083"
  },
  "tasks": [
    {
      "state": "FAILED",
      "trace": "org.apache.kafka.connect.errors.ConnectException: java.sql.SQLException: No suitable driver found for jdbc:mysql://mysql:3306/db?user=user&password=password\n\tat io.confluent.connect.jdbc.util.CachedConnectionProvider.getValidConnection(CachedConnectionProvider.java:75)\n\tat io.confluent.connect.jdbc.JdbcSourceConnector.start(JdbcSourceConnector.java:95)\n\tat org.apache.kafka.connect.runtime.WorkerConnector.doStart(WorkerConnector.java:108)\n\tat org.apache.kafka.connect.runtime.WorkerConnector.start(WorkerConnector.java:133)\n\tat org.apache.kafka.connect.runtime.WorkerConnector.transitionTo(WorkerConnector.java:192)\n\tat org.apache.kafka.connect.runtime.Worker.startConnector(Worker.java:211)\n\tat org.apache.kafka.connect.runtime.distributed.DistributedHerder.startConnector(DistributedHerder.java:894)\n\tat org.apache.kafka.connect.runtime.distributed.DistributedHerder.access$1300(DistributedHerder.java:108)\n\tat org.apache.kafka.connect.runtime.distributed.DistributedHerder$15.call(DistributedHerder.java:910)\n\tat org.apache.kafka.connect.runtime.distributed.DistributedHerder$15.call(DistributedHerder.java:906)\n\tat java.util.concurrent.FutureTask.run(FutureTask.java:266)\n\tat java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1142)\n\tat java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:617)\n\tat java.lang.Thread.run(Thread.java:745)\nCaused by: java.sql.SQLException: No suitable driver found for jdbc:mysql://mysql:3306/db?user=user&password=password\n\tat java.sql.DriverManager.getConnection(DriverManager.java:689)\n\tat java.sql.DriverManager.getConnection(DriverManager.java:247)\n\tat io.confluent.connect.jdbc.util.CachedConnectionProvider.newConnection(CachedConnectionProvider.java:85)\n\tat io.confluent.connect.jdbc.util.CachedConnectionProvider.getValidConnection(CachedConnectionProvider.java:68)\n\t... 13 more\n",
      "id": 0,
      "worker_id": "connect:8083"
    }
  ],
  "type": "source"
}

``` 


We have our data 

```
$ docker-compose exec kafka-1 kafka-topics --zookeeper zookeeper:2181 --list
__consumer_offsets
connect-config
connect-offsets
connect-status
mysql-application
```

and 

```
$ docker-compose exec kafka-1 kafka-topics --zookeeper zookeeper:2181 --topic mysql-application --describe
Topic:mysql-application	PartitionCount:1	ReplicationFactor:3	Configs:
	Topic: mysql-application	Partition: 0	Leader: 1	Replicas: 1,2,3	Isr: 1,2,3
```

Let's see the data

```
$ docker-compose exec schema-registry kafka-avro-console-consumer -bootstrap-server kafka-1:9092,kafka-2:9092,kafka-3:9092 --topic mysql2-application --from-beginning --property schema.registry.url=http://localhost:8082
  [2018-02-13 19:09:49,930] INFO ConsumerConfig values:
  	auto.commit.interval.ms = 5000
  	auto.offset.reset = earliest
  	bootstrap.servers = [kafka-1:9092, kafka-2:9092, kafka-3:9092]

...
{"id":1,"name":"kafka","team_email":"kafka@team.co","last_modified":1518544661000}
{"id":2,"name":"kafka","team_email":"kafka@team.co","last_modified":1518544666000}
{"id":3,"name":"kafka","team_email":"kafka@team.co","last_modified":1518544679000}
{"id":4,"name":"kafka","team_email":"kafka@team.co","last_modified":1518545834000}
...
```

So much better with a Schema Registry
