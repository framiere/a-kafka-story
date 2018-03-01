# Objective 

Let's add a transform to get only the modified data http://kafka.apache.org/documentation/#connect_transforms

```sh
docker-compose exec connect curl -s -XPOST -H "Content-Type: application/json; charset=UTF-8" http://localhost:8083/connectors/ -d '
{
    "name": "debezium-connector",
    "config": {
        "connector.class": "io.debezium.connector.mysql.MySqlConnector",
        "tasks.max": "1",
        "database.hostname": "mysql",
        "database.port": "3306",
        "database.user": "debezium",
        "database.password": "dbz",
        "database.server.id": "223344",
        "database.server.name": "dbserver1",
        "database.whitelist": "mydb",
        "database.history.kafka.bootstrap.servers": "kafka-1:9092,kafka-2:9092,kafka-3:9092",
        "database.history.kafka.topic": "schema-changes.mydb",
        "include.schema.changes": "true" ,
        "transforms": "unwrap",
        "transforms.unwrap.type": "io.debezium.transforms.UnwrapFromEnvelope",
        "transforms.unwrap.drop.tombstones":"false"
      }        
}'
```

Let's do database manipulation

```
docker-compose exec mysql mysql --user=root --password=password --database=mydb -e "select * from team"
docker-compose exec mysql mysql --user=root --password=password --database=mydb -e "
INSERT INTO team (   \
  name, \
  email,   \
  last_modified \
) VALUES (  \
  'another',  \
  'another@apache.org',   \
  NOW() \
); "
docker-compose exec mysql mysql --user=root --password=password --database=mydb -e "UPDATE team set name='another name', last_modified = NOW() where id = '2'"
docker-compose exec mysql mysql --user=root --password=password --database=mydb -e "DELETE FROM team WHERE id = 2"
```

Use ksql

```
$ docker-compose exec ksql-cli ksql-cli remote http://ksql-server:8080
ksql> SET 'auto.offset.reset' = 'earliest';
Successfully changed local property 'auto.offset.reset' from 'null' to 'earliest'
ksql> CREATE STREAM team WITH (KAFKA_TOPIC='dbserver1.mydb.team', VALUE_FORMAT='AVRO');
1519925080993 |  | 1 | kafka | kafka@apache.org | 1519925014000
1519925111125 |  | 2 | another | another@apache.org | 1519925110000
1519925120123 |  | 2 | another name | another@apache.org | 1519925119000
```

Let's see the real underlying data to detect the deletion

```sh
$ docker-compose exec schema-registry kafka-avro-console-consumer \
    --bootstrap-server kafka-1:9092,kafka-2:9092,kafka-3:9092 \
    --topic dbserver1.mydb.team \
    --from-beginning \
    --property schema.registry.url=http://localhost:8082
{"id":1,"name":"kafka","email":"kafka@apache.org","last_modified":1519925014000}
{"id":2,"name":"another","email":"another@apache.org","last_modified":1519925110000}
{"id":2,"name":"another name","email":"another@apache.org","last_modified":1519925119000}
null    
```

We have a null, this is a tombstone.

Let's see with the key

```sh
$ docker-compose exec schema-registry kafka-avro-console-consumer \
    --bootstrap-server kafka-1:9092,kafka-2:9092,kafka-3:9092 \
    --topic dbserver1.mydb.team \
    --from-beginning \
    --property schema.registry.url=http://localhost:8082 \
    --property print.key=true
{"id":1}        {"id":1,"name":"kafka","email":"kafka@apache.org","last_modified":1519925014000}
{"id":2}        {"id":2,"name":"another","email":"another@apache.org","last_modified":1519925110000}
{"id":2}        {"id":2,"name":"another name","email":"another@apache.org","last_modified":1519925119000}
{"id":2}        null
```

We have detected the deletion, we can make this topic a compacted topic then.

Let's see the topic as of now 

```
$ docker-compose exec kafka-1 kafka-topics --zookeeper zookeeper:2181 --describe --topic dbserver1.mydb.team
Topic:dbserver1.mydb.team	PartitionCount:1	ReplicationFactor:3	Configs:
	Topic: dbserver1.mydb.team	Partition: 0	Leader: 3	Replicas: 3,1,2	Isr: 3,1,2
```

Let's add the compact policy

```
$ docker-compose exec kafka-1 kafka-configs \
    --zookeeper zookeeper:2181 \
    --entity-type topics \
    --entity-name dbserver1.mydb.team \
    --alter \
    --add-config cleanup.policy=compact
Completed Updating config for entity: topic 'dbserver1.mydb.team'.    
```

# The full action ?

[![screencast](https://asciinema.org/a/oOzstwpeKset9fHZYIj3SHXHn.png)](https://asciinema.org/a/oOzstwpeKset9fHZYIj3SHXHn?autoplay=1)

