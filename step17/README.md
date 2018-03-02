# Objective 

Cdc, Avro, KSql and Joins !

```sh

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
        "database.history.kafka.topic": "schema-changes.mydb",
        "include.schema.changes": "true" ,
        "transforms": "unwrap",
        "transforms.unwrap.type": "io.debezium.transforms.UnwrapFromEnvelope",
        "transforms.unwrap.drop.tombstones":"false"
      }        
}'
```

Let's see the data

```
$ docker-compose exec kafka-1 kafka-topics --zookeeper zookeeper:2181 --list
__consumer_offsets
__ksql_compatibility_check
_schemas
connect-config
connect-offsets
connect-status
dbserver1
dbserver1.mydb.member
dbserver1.mydb.team
ksql__commands
schema-changes.mydb

$ docker-compose exec schema-registry kafka-avro-console-consumer \
    --bootstrap-server kafka-1:9092,kafka-2:9092,kafka-3:9092 \
    --topic dbserver1.mydb.member \
    --from-beginning \
    --property schema.registry.url=http://localhost:8082 \
    --property print.key=true \
    --key-deserializer org.apache.kafka.common.serialization.StringDeserializer
Struct{id=1}	{"id":1,"name":"jun@confluent.io","team_id":1}

$ docker-compose exec schema-registry kafka-avro-console-consumer \
    --bootstrap-server kafka-1:9092,kafka-2:9092,kafka-3:9092 \
    --topic dbserver1.mydb.team \
    --from-beginning \
    --property schema.registry.url=http://localhost:8082 \
    --property print.key=true \
    --key-deserializer org.apache.kafka.common.serialization.StringDeserializer
Struct{id=1}	{"id":1,"name":"kafka","email":"kafka@apache.org","last_modified":1520003701000}
```


Use ksql

```
$ docker-compose exec ksql-cli ksql-cli remote http://ksql-server:8080
ksql> PRINT 'dbserver1.mydb.member' FROM BEGINNING;
3/2/18 9:34:15 AM UTC, , {"id": 1, "name": "jun@confluent.io", "team_id": 1}
^CTopic printing ceased
ksql> PRINT 'dbserver1.mydb.team' FROM BEGINNING;
3/2/18 9:30:01 AM UTC, , {"id": 1, "name": "kafka", "email": "kafka@apache.org", "last_modified": 1519982897000}
^CTopic printing ceased
ksql> SET 'auto.offset.reset' = 'earliest';
Successfully changed local property 'auto.offset.reset' from 'null' to 'earliest'
ksql> CREATE TABLE team WITH (KAFKA_TOPIC='dbserver1.mydb.team', VALUE_FORMAT='AVRO', KEY='id');

 Message
----------------------------
 Stream created and running
---------------------------
ksql> DESCRIBE team;

 Field         | Type
-------------------------------------------
 ROWTIME       | BIGINT           (system)
 ROWKEY        | VARCHAR(STRING)  (system)
 ID            | INTEGER
 NAME          | VARCHAR(STRING)
 EMAIL         | VARCHAR(STRING)
 LAST_MODIFIED | BIGINT
-------------------------------------------
For runtime statistics and query details run: DESCRIBE EXTENDED <Stream,Table>;
ksql> SELECT * FROM team;
1519983001682 |  | 1 | kafka | kafka@apache.org | 1519982897000
^CQuery terminated
ksql> CREATE STREAM member WITH (KAFKA_TOPIC='dbserver1.mydb.member', VALUE_FORMAT='AVRO');

 Message
----------------------------
 Stream created and running
---------------------------
ksql> DESCRIBE member;

 Field   | Type
-------------------------------------
 ROWTIME | BIGINT           (system)
 ROWKEY  | VARCHAR(STRING)  (system)
 ID      | INTEGER
 NAME    | VARCHAR(STRING)
 TEAM_ID | INTEGER
-------------------------------------
For runtime statistics and query details run: DESCRIBE EXTENDED <Stream,Table>;
ksql> SELECT * FROM member;
1519983051212 |  | 1 | jun@confluent.io | 1
^CQuery terminated
ksql> SELECT m.name, t.name \
      FROM member m LEFT JOIN team t \
      ON m.team_id = t.id;
jun@confluent.io | null      
```
