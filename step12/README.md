# Objective 

Getting Change data capture ready with mysql and debezium with Avro 


Let's add add Kafka Connect configuration

```
      CONNECT_KEY_CONVERTER: io.confluent.connect.avro.AvroConverter
      CONNECT_VALUE_CONVERTER: io.confluent.connect.avro.AvroConverter
      CONNECT_INTERNAL_KEY_CONVERTER: org.apache.kafka.connect.json.JsonConverter
      CONNECT_INTERNAL_VALUE_CONVERTER: org.apache.kafka.connect.json.JsonConverter
      CONNECT_KEY_CONVERTER_SCHEMA_REGISTRY_URL: http://schema-registry:8082
      CONNECT_VALUE_CONVERTER_SCHEMA_REGISTRY_URL: http://schema-registry:8082
```
 
```
$ docker-compose exec connect curl -s -XPOST -H "Content-Type: application/json; charset=UTF-8" http://localhost:8083/connectors/ -d '
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

Let's use the avro consumers

```
$ docker-compose exec schema-registry kafka-avro-console-consumer -bootstrap-server kafka-1:9092,kafka-2:9092,kafka-3:9092 --topic dbserver1.mydb.team --from-beginning --property schema.registry.url=http://localhost:8082
{"before":null,"after":{"dbserver1.mydb.team.Value":{"id":1,"name":"kafka","email":"kafka@apache.org","last_modified":1519584693000}},"source":{"version":{"string":"0.7.3"},"name":"dbserver1","server_id":0,"ts_sec":0,"gtid":null,"file":"mysql-bin.000003","pos":154,"row":0,"snapshot":{"boolean":true},"thread":null,"db":{"string":"mydb"},"table":{"string":"team"}},"op":"c","ts_ms":{"long":1519584821699}}
```

Perfect!

# The full action ?

[![screencast](https://asciinema.org/a/cuBXz5JjrzvM2j2jMdQWm2TWn.png)](https://asciinema.org/a/cuBXz5JjrzvM2j2jMdQWm2TWn?autoplay=1)
