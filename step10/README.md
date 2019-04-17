# Objective 

Getting Kafka Connect and Schema registry setup 


# Kafka Connect

Let's post it

```
$ docker-compose exec connect curl -s -XPOST -H "Content-Type: application/json; charset=UTF-8" http://localhost:8083/connectors/ -d '
{
    "name": "my-mysql-connector",
    "config": {
      "connector.class":"io.confluent.connect.jdbc.JdbcSourceConnector",
      "tasks.max":"10",
      "connection.url":"jdbc:mysql://mysql:3306/db?user=user&password=password&useSSL=false",
      "table.whitelist":"application",
      "mode":"timestamp+incrementing",
      "timestamp.column.name":"last_modified",
      "incrementing.column.name":"id",
      "topic.prefix":"mysql-",
      "key.ignore": true,
      "key.converter.schema.registry.url": "http://schema-registry:8082",
      "value.converter": "io.confluent.connect.avro.AvroConverter",
      "value.converter.schema.registry.url": "http://schema-registry:8082",
      "schema.ignore": true

    }
}
' | jq .
```


Let's see its status

```
$ docker-compose exec connect curl -s localhost:8083/connectors/my-mysql-connector/status | jq .
{
  "name": "my-mysql-connector",
  "connector": {
    "state": "RUNNING",
    "worker_id": "connect:8083"
  },
  "tasks": [
    {
      "state": "RUNNING",
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
$ docker-compose exec schema-registry kafka-avro-console-consumer -bootstrap-server kafka-1:9092,kafka-2:9092,kafka-3:9092 --topic mysql-application --from-beginning --property schema.registry.url=http://localhost:8082
  [2018-02-13 19:09:49,930] INFO ConsumerConfig values:
  	auto.commit.interval.ms = 5000
  	auto.offset.reset = earliest
  	bootstrap.servers = [kafka-1:9092, kafka-2:9092, kafka-3:9092]

...
{"id":1,"name":"kafka","team_email":"kafka@apache.org","last_modified":1518544661000}
...
```

So much better with a Schema Registry!

Let's add another element in the application 

``` 
$ docker-compose exec mysql mysql --user=root --password=password --database=db -e "
INSERT INTO application (   \
  id,   \
  name, \
  team_email,   \
  last_modified \
) VALUES (  \
  2,    \
  'another',  \
  'another@apache.org',   \
  NOW() \
); "
```

```
$ docker-compose exec mysql bash -c "mysql --user=root --password=password --database=db -e 'select * from application'"
mysql: [Warning] Using a password on the command line interface can be insecure.
+----+---------+--------------------+---------------------+
| id | name    | team_email         | last_modified       |
+----+---------+--------------------+---------------------+
|  1 | kafka   | kafka@apache.org   | 2018-02-25 11:25:23 |
|  2 | another | another@apache.org | 2018-02-25 11:31:10 |
+----+---------+--------------------+---------------------+
```

Let's verify that we have them in our topic 

```
$ docker-compose exec schema-registry kafka-avro-console-consumer -bootstrap-server kafka-1:9092,kafka-2:9092,kafka-3:9092 --topic mysql-application --from-beginning --property schema.registry.url=http://localhost:8082 --property print.key=true
  [2018-02-13 19:09:49,930] INFO ConsumerConfig values:
  	auto.commit.interval.ms = 5000
  	auto.offset.reset = earliest
  	bootstrap.servers = [kafka-1:9092, kafka-2:9092, kafka-3:9092]
...
{"id":1,"name":"kafka","team_email":"kafka@apache.org","last_modified":1519557923000}
{"id":2,"name":"another","team_email":"another@apache.org","last_modified":1519558270000}
```

What about update ? 

```
$ docker-compose exec mysql mysql --user=root --password=password --database=db -e "UPDATE application set name='another2', last_modified = NOW() where id = '2'"
```


```
$ docker-compose exec mysql bash -c "mysql --user=root --password=password --database=db -e 'select * from application'"
mysql: [Warning] Using a password on the command line interface can be insecure.
+----+---------+--------------------+---------------------+
| id | name     | team_email         | last_modified       |
+----+----------+--------------------+---------------------+
|  1 | kafka    | kafka@apache.org   | 2018-02-25 11:25:23 |
|  2 | another2 | another@apache.org | 2018-02-25 11:36:10 |
+----+----------+--------------------+---------------------+
```

Let's verify that we the update reflected in the topic 

```
$ docker-compose exec schema-registry kafka-avro-console-consumer -bootstrap-server kafka-1:9092,kafka-2:9092,kafka-3:9092 --topic mysql-application --from-beginning --property schema.registry.url=http://localhost:8082
  [2018-02-13 19:09:49,930] INFO ConsumerConfig values:
  	auto.commit.interval.ms = 5000
  	auto.offset.reset = earliest
  	bootstrap.servers = [kafka-1:9092, kafka-2:9092, kafka-3:9092]
...
{"id":1,"name":"kafka","team_email":"kafka@apache.org","last_modified":1519557923000}
{"id":2,"name":"another","team_email":"another@apache.org","last_modified":1519558270000}
{"id":2,"name":"another2","team_email":"another@apache.org","last_modified":1519568679000}
```

What about deletion ?

```
$ docker-compose exec mysql mysql --user=root --password=password --database=db -e "DELETE FROM application where id = '2'"
```

```
$ docker-compose exec schema-registry kafka-avro-console-consumer -bootstrap-server kafka-1:9092,kafka-2:9092,kafka-3:9092 --topic mysql-application --from-beginning --property schema.registry.url=http://localhost:8082
  [2018-02-13 19:09:49,930] INFO ConsumerConfig values:
  	auto.commit.interval.ms = 5000
  	auto.offset.reset = earliest
  	bootstrap.servers = [kafka-1:9092, kafka-2:9092, kafka-3:9092]
...
{"id":1,"name":"kafka","team_email":"kafka@apache.org","last_modified":1519557923000}
{"id":2,"name":"another","team_email":"another@apache.org","last_modified":1519558270000}
{"id":2,"name":"another2","team_email":"another@apache.org","last_modified":1519568679000}
```

Nope, no new event ! With this method, either you load all data using `batch` or you need to use soft-delete to support deletion.

See https://docs.confluent.io/current/connect/connect-jdbc/docs/source_config_options.html#mode

Enter Change data capture.

