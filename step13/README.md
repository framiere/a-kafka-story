# Objective 

Getting CDC to S3 

```
$ docker-compose exec connect curl  -XPOST -H "Content-Type: application/json; charset=UTF-8" http://localhost:8083/connectors/ -d '
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
        "database.history.kafka.topic": "schema-changes.mydb"
    }
}'
```

Let's see its status
 
```sh
$ docker-compose exec connect curl -s localhost:8083/connectors/debezium-connector/status | jq .
{
  "name": "debezium-connector",
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

Let's see if we have our topic 

```sh 
$ docker-compose exec kafka-1 kafka-topics --zookeeper zookeeper:2181 --list
  __consumer_offsets
  _schemas
  connect-config
  connect-offsets
  connect-status
  dbserver1
  dbserver1.mydb.team
  schema-changes.mydb
```

Let's dig into out team topic

```
$ docker-compose exec kafka-1 kafka-console-consumer --bootstrap-server kafka-1:9092 --topic dbserver1.mydb.team --from-beginning

kafka kafka@apache.org����X
0.7.3dbserver1 mysql-bin.000003mydteamc����X
```


Start the S3 connector

```
$ docker-compose exec connect curl  -XPOST -H "Content-Type: application/json; charset=UTF-8" http://localhost:8083/connectors/ -d '
{
    "name": "s3-sink",
    "config": {
        "connector.class": "io.confluent.connect.s3.S3SinkConnector",
        "tasks.max": "1",
        "topics": "dbserver1.mydb.team",
        "s3.bucket.name": "cdc",
        "s3.part.size": "5242880",
        "store.url": "http://minio:9000",
        "flush.size": "3",
        "storage.class": "io.confluent.connect.s3.storage.S3Storage",
        "format.class": "io.confluent.connect.s3.format.avro.AvroFormat",
        "schema.generator.class": "io.confluent.connect.storage.hive.schema.DefaultSchemaGenerator",
        "partitioner.class": "io.confluent.connect.storage.partitioner.DefaultPartitioner",
        "schema.compatibility": "NONE",
        "name": "s3-sink"
    }
}'
```

Let's see its status
 
```sh
$ docker-compose exec connect curl -s localhost:8083/connectors/s3-sink/status | jq .
{
  "name": "s3-sink",
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
  "type": "sink"
}
```
 

Let's use the avro consumers

```
$ docker-compose exec schema-registry kafka-avro-console-consumer -bootstrap-server kafka-1:9092,kafka-2:9092,kafka-3:9092 --topic dbserver1.mydb.team --from-beginning --property schema.registry.url=http://localhost:8082
{"before":null,"after":{"dbserver1.mydb.team.Value":{"id":1,"name":"kafka","email":"kafka@apache.org","last_modified":1519584693000}},"source":{"version":{"string":"0.7.3"},"name":"dbserver1","server_id":0,"ts_sec":0,"gtid":null,"file":"mysql-bin.000003","pos":154,"row":0,"snapshot":{"boolean":true},"thread":null,"db":{"string":"mydb"},"table":{"string":"team"}},"op":"c","ts_ms":{"long":1519584821699}}
```

Let's add some data

```
while [ true ] ; do docker-compose exec mysql mysql --user=root --password=password --database=mydb -e "
INSERT INTO team (   \
  name, \
  email,   \
  last_modified \
) VALUES (  \
  'another',  \
  'another@apache.org',   \
  NOW() \
); "
done

```

Perfect!

# The full action ?

[![screencast](https://asciinema.org/a/2FLP4VXblCfmi6jxILIhMmq5a.png)](https://asciinema.org/a/2FLP4VXblCfmi6jxILIhMmq5a?autoplay=1)
