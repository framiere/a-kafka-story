# Objective 

Cdc, Avro, Json and Joins !

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
        "database.server.id": "1",
        "database.server.name": "dbserver1",
        "database.whitelist": "mydb",
        "database.history.kafka.bootstrap.servers": "kafka-1:9092,kafka-2:9092,kafka-3:9092",
        "database.history.kafka.topic": "schema-changes.mydb",
        "include.schema.changes": "false" ,
        "value.converter": "org.apache.kafka.connect.json.JsonConverter",
        "value.converter.schemas.enable": true,
        "key.converter": "org.apache.kafka.connect.json.JsonConverter",
        "key.converter.schemas.enable": true
      }        
}'
```

Let's see the data

```
$ docker-compose exec kafka-1 kafka-topics --zookeeper zookeeper:2181 --list
__confluent.support.metrics
__consumer_offsets
_confluent-ksql-default__command_topic
connect-config
connect-offsets
connect-status
dbserver1.mydb.member
dbserver1.mydb.team
schema-changes.mydb

$ docker-compose exec kafka-1 kafka-console-consumer \
    --bootstrap-server localhost:9092 \
    --from-beginning \
    --topic dbserver1.mydb.member 
{"schema":{"type":"struct","fields":[{"type":"struct","fields":[{"type":"int32","optional":false,"field":"id"},{"type":"string","optional":false,"field":"name"},{"type":"int32","optional":false,"field":"team_id"}],"optional":true,"name":"dbserver1.mydb.member.Value","field":"before"},{"type":"struct","fields":[{"type":"int32","optional":false,"field":"id"},{"type":"string","optional":false,"field":"name"},{"type":"int32","optional":false,"field":"team_id"}],"optional":true,"name":"dbserver1.mydb.member.Value","field":"after"},{"type":"struct","fields":[{"type":"string","optional":true,"field":"version"},{"type":"string","optional":false,"field":"name"},{"type":"int64","optional":false,"field":"server_id"},{"type":"int64","optional":false,"field":"ts_sec"},{"type":"string","optional":true,"field":"gtid"},{"type":"string","optional":false,"field":"file"},{"type":"int64","optional":false,"field":"pos"},{"type":"int32","optional":false,"field":"row"},{"type":"boolean","optional":true,"field":"snapshot"},{"type":"int64","optional":true,"field":"thread"},{"type":"string","optional":true,"field":"db"},{"type":"string","optional":true,"field":"table"}],"optional":false,"name":"io.debezium.connector.mysql.Source","field":"source"},{"type":"string","optional":false,"field":"op"},{"type":"int64","optional":true,"field":"ts_ms"}],"optional":false,"name":"dbserver1.mydb.member.Envelope"},"payload":{"before":null,"after":{"id":1,"name":"jun rao","team_id":1},"source":{"version":"0.7.3","name":"dbserver1","server_id":0,"ts_sec":0,"gtid":null,"file":"mysql-bin.000003","pos":154,"row":0,"snapshot":true,"thread":null,"db":"mydb","table":"member"},"op":"c","ts_ms":1555270349671}}

$ docker-compose exec kafka-1 kafka-console-consumer \
    --bootstrap-server localhost:9092 \
    --topic dbserver1.mydb.team \
    --from-beginning
{"schema":{"type":"struct","fields":[{"type":"struct","fields":[{"type":"int32","optional":false,"field":"id"},{"type":"string","optional":false,"field":"name"},{"type":"int64","optional":false,"name":"io.debezium.time.Timestamp","version":1,"field":"last_modified"}],"optional":true,"name":"dbserver1.mydb.team.Value","field":"before"},{"type":"struct","fields":[{"type":"int32","optional":false,"field":"id"},{"type":"string","optional":false,"field":"name"},{"type":"int64","optional":false,"name":"io.debezium.time.Timestamp","version":1,"field":"last_modified"}],"optional":true,"name":"dbserver1.mydb.team.Value","field":"after"},{"type":"struct","fields":[{"type":"string","optional":true,"field":"version"},{"type":"string","optional":false,"field":"name"},{"type":"int64","optional":false,"field":"server_id"},{"type":"int64","optional":false,"field":"ts_sec"},{"type":"string","optional":true,"field":"gtid"},{"type":"string","optional":false,"field":"file"},{"type":"int64","optional":false,"field":"pos"},{"type":"int32","optional":false,"field":"row"},{"type":"boolean","optional":true,"field":"snapshot"},{"type":"int64","optional":true,"field":"thread"},{"type":"string","optional":true,"field":"db"},{"type":"string","optional":true,"field":"table"}],"optional":false,"name":"io.debezium.connector.mysql.Source","field":"source"},{"type":"string","optional":false,"field":"op"},{"type":"int64","optional":true,"field":"ts_ms"}],"optional":false,"name":"dbserver1.mydb.team.Envelope"},"payload":{"before":null,"after":{"id":1,"name":"kafka","last_modified":1555270292000},"source":{"version":"0.7.3","name":"dbserver1","server_id":0,"ts_sec":0,"gtid":null,"file":"mysql-bin.000003","pos":154,"row":0,"snapshot":true,"thread":null,"db":"mydb","table":"team"},"op":"c","ts_ms":1555270349671}}
```


Use ksql

```
$ docker-compose exec ksql ksql
ksql> SET 'auto.offset.reset' = 'earliest';
Successfully changed local property 'auto.offset.reset' from 'null' to 'earliest'
ksql> PRINT 'dbserver1.mydb.member' FROM BEGINNING;
Format:JSON
{"schema":{"type":"struct","fields":[{"type":"struct","fields":[{"type":"int32","optional":false,"field":"id"},{"type":"string","optional":false,"field":"name"},{"type":"int64","optional":false,"name":"io.debezium.time.Timestamp","version":1,"field":"last_modified"}],"optional":true,"name":"dbserver1.mydb.team.Value","field":"before"},{"type":"struct","fields":[{"type":"int32","optional":false,"field":"id"},{"type":"string","optional":false,"field":"name"},{"type":"int64","optional":false,"name":"io.debezium.time.Timestamp","version":1,"field":"last_modified"}],"optional":true,"name":"dbserver1.mydb.team.Value","field":"after"},{"type":"struct","fields":[{"type":"string","optional":true,"field":"version"},{"type":"string","optional":false,"field":"name"},{"type":"int64","optional":false,"field":"server_id"},{"type":"int64","optional":false,"field":"ts_sec"},{"type":"string","optional":true,"field":"gtid"},{"type":"string","optional":false,"field":"file"},{"type":"int64","optional":false,"field":"pos"},{"type":"int32","optional":false,"field":"row"},{"type":"boolean","optional":true,"field":"snapshot"},{"type":"int64","optional":true,"field":"thread"},{"type":"string","optional":true,"field":"db"},{"type":"string","optional":true,"field":"table"}],"optional":false,"name":"io.debezium.connector.mysql.Source","field":"source"},{"type":"string","optional":false,"field":"op"},{"type":"int64","optional":true,"field":"ts_ms"}],"optional":false,"name":"dbserver1.mydb.team.Envelope"},"payload":{"before":null,"after":{"id":1,"name":"kafka","last_modified":1555270292000},"source":{"version":"0.7.3","name":"dbserver1","server_id":0,"ts_sec":0,"gtid":null,"file":"mysql-bin.000003","pos":154,"row":0,"snapshot":true,"thread":null,"db":"mydb","table":"team"},"op":"c","ts_ms":1555270349671}}
{\"type\":\"int32\",\"optional\":false,\"field\":\"id\"}],\"optional\":false,\"name\":\"dbserver1.mydb.member.Key\"},\"payload\":{\"id\":1}}","schema":{"type":"struct","fields":[{"type":"struct","fields":[{"type":"int32","optional":false,"field":"id"},{"type":"string","optional":false,"field":"name"},{"type":"int32","optional":false,"field":"team_id"}],"optional":true,"name":"dbserver1.mydb.member.Value","field":"before"},{"type":"struct","fields":[{"type":"int32","optional":false,"field":"id"},{"type":"string","optional":false,"field":"name"},{"type":"int32","optional":false,"field":"team_id"}],"optional":true,"name":"dbserver1.mydb.member.Value","field":"after"},{"type":"struct","fields":[{"type":"string","optional":true,"field":"version"},{"type":"string","optional":false,"field":"name"},{"type":"int64","optional":false,"field":"server_id"},{"type":"int64","optional":false,"field":"ts_sec"},{"type":"string","optional":true,"field":"gtid"},{"type":"string","optional":false,"field":"file"},{"type":"int64","optional":false,"field":"pos"},{"type":"int32","optional":false,"field":"row"},{"type":"boolean","optional":true,"field":"snapshot"},{"type":"int64","optional":true,"field":"thread"},{"type":"string","optional":true,"field":"db"},{"type":"string","optional":true,"field":"table"}],"optional":false,"name":"io.debezium.connector.mysql.Source","field":"source"},{"type":"string","optional":false,"field":"op"},{"type":"int64","optional":true,"field":"ts_ms"}],"optional":false,"name":"dbserver1.mydb.member.Envelope"},"payload":{"before":null,"after":{"id":1,"name":"jun rao","team_id":1},"source":{"version":"0.7.3","name":"dbserver1","server_id":0,"ts_sec":0,"gtid":null,"file":"mysql-bin.000003","pos":154,"row":0,"snapshot":true,"thread":null,"db":"mydb","table":"member"},"op":"c","ts_ms":1555270349671}}
^CTopic printing ceased
ksql> CREATE STREAM member_cdc (schema varchar, payload varchar) \
    WITH ( kafka_topic='dbserver1.mydb.member',value_format='JSON');

 Message
----------------------------
 Stream created and running
---------------------------
ksql> SELECT * FROM member_cdc;
1555270351035 | {"schema":{"type":"struct","fields":[{"type":"int32","optional":false,"field":"id"}],"optional":false,"name":"dbserver1.mydb.member.Key"},"payload":{"id":1}} | {"name":"dbserver1.mydb.member.Envelope","optional":false,"type":"struct","fields":[{"field":"before","name":"dbserver1.mydb.member.Value","optional":true,"type":"struct","fields":[{"field":"id","optional":false,"type":"int32"},{"field":"name","optional":false,"type":"string"},{"field":"team_id","optional":false,"type":"int32"}]},{"field":"after","name":"dbserver1.mydb.member.Value","optional":true,"type":"struct","fields":[{"field":"id","optional":false,"type":"int32"},{"field":"name","optional":false,"type":"string"},{"field":"team_id","optional":false,"type":"int32"}]},{"field":"source","name":"io.debezium.connector.mysql.Source","optional":false,"type":"struct","fields":[{"field":"version","optional":true,"type":"string"},{"field":"name","optional":false,"type":"string"},{"field":"server_id","optional":false,"type":"int64"},{"field":"ts_sec","optional":false,"type":"int64"},{"field":"gtid","optional":true,"type":"string"},{"field":"file","optional":false,"type":"string"},{"field":"pos","optional":false,"type":"int64"},{"field":"row","optional":false,"type":"int32"},{"field":"snapshot","optional":true,"type":"boolean"},{"field":"thread","optional":true,"type":"int64"},{"field":"db","optional":true,"type":"string"},{"field":"table","optional":true,"type":"string"}]},{"field":"op","optional":false,"type":"string"},{"field":"ts_ms","optional":true,"type":"int64"}]} | {"op":"c","after":{"name":"jun rao","id":1,"team_id":1},"source":{"ts_sec":0,"file":"mysql-bin.000003","pos":154,"name":"dbserver1","row":0,"server_id":0,"version":"0.7.3","snapshot":true,"db":"mydb","table":"member"},"ts_ms":1555270349671}
^CQuery terminated
ksql> CREATE STREAM member_stream \
    AS SELECT \
        EXTRACTJSONFIELD(payload, '$.op') as cdc_operation, \
        CAST(EXTRACTJSONFIELD(payload, '$.after.id') AS INTEGER) as id, \
        EXTRACTJSONFIELD(payload, '$.after.name') as name, \
        CAST(EXTRACTJSONFIELD(payload, '$.after.team_id') AS INTEGER) as team_id \
    FROM member_cdc \
    PARTITION BY id;
 Message
----------------------------
 Stream created and running
----------------------------
ksql> DESCRIBE member_stream;

 Field         | Type
-------------------------------------------
 ROWTIME       | BIGINT           (system)
 ROWKEY        | VARCHAR(STRING)  (system)
 CDC_OPERATION | VARCHAR(STRING)
 ID            | INTEGER
 NAME          | VARCHAR(STRING)
 TEAM_ID       | INTEGER
-------------------------------------------
For runtime statistics and query details run: DESCRIBE EXTENDED <Stream,Table>;
ksql> SELECT * FROM member_stream;
1555270351035 | 1 | c | 1 | jun rao | 1
^CQuery terminated
ksql> CREATE TABLE member_table ( id VARCHAR, name VARCHAR, team_id INTEGER) \
        WITH (VALUE_FORMAT = 'JSON', \
            KAFKA_TOPIC = 'MEMBER_STREAM', \
            KEY = 'id');
 Message
---------------
 Table created
---------------
ksql> CREATE STREAM team_cdc (schema varchar, payload varchar) \
    WITH ( kafka_topic='dbserver1.mydb.team',value_format='JSON');

 Message
----------------------------
 Stream created and running
---------------------------
ksql> SELECT * FROM team_cdc;
1555270351215 | {"schema":{"type":"struct","fields":[{"type":"int32","optional":false,"field":"id"}],"optional":false,"name":"dbserver1.mydb.team.Key"},"payload":{"id":1}} | {"name":"dbserver1.mydb.team.Envelope","optional":false,"type":"struct","fields":[{"field":"before","name":"dbserver1.mydb.team.Value","optional":true,"type":"struct","fields":[{"field":"id","optional":false,"type":"int32"},{"field":"name","optional":false,"type":"string"},{"field":"last_modified","name":"io.debezium.time.Timestamp","optional":false,"type":"int64","version":1}]},{"field":"after","name":"dbserver1.mydb.team.Value","optional":true,"type":"struct","fields":[{"field":"id","optional":false,"type":"int32"},{"field":"name","optional":false,"type":"string"},{"field":"last_modified","name":"io.debezium.time.Timestamp","optional":false,"type":"int64","version":1}]},{"field":"source","name":"io.debezium.connector.mysql.Source","optional":false,"type":"struct","fields":[{"field":"version","optional":true,"type":"string"},{"field":"name","optional":false,"type":"string"},{"field":"server_id","optional":false,"type":"int64"},{"field":"ts_sec","optional":false,"type":"int64"},{"field":"gtid","optional":true,"type":"string"},{"field":"file","optional":false,"type":"string"},{"field":"pos","optional":false,"type":"int64"},{"field":"row","optional":false,"type":"int32"},{"field":"snapshot","optional":true,"type":"boolean"},{"field":"thread","optional":true,"type":"int64"},{"field":"db","optional":true,"type":"string"},{"field":"table","optional":true,"type":"string"}]},{"field":"op","optional":false,"type":"string"},{"field":"ts_ms","optional":true,"type":"int64"}]} | {"op":"c","after":{"name":"kafka","id":1,"last_modified":1555270292000},"source":{"ts_sec":0,"file":"mysql-bin.000003","pos":154,"name":"dbserver1","row":0,"server_id":0,"version":"0.7.3","snapshot":true,"db":"mydb","table":"team"},"ts_ms":1555270349671}
^CQuery terminated
ksql> CREATE STREAM team_stream \
    AS SELECT \
        EXTRACTJSONFIELD(payload, '$.op') as cdc_operation, \
        CAST(EXTRACTJSONFIELD(payload, '$.after.id') AS INTEGER) as id, \
        EXTRACTJSONFIELD(payload, '$.after.name') as name \
    FROM team_cdc \
    PARTITION BY id;
 Message
----------------------------
 Stream created and running
----------------------------
ksql> DESCRIBE team_stream;

 Field         | Type
-------------------------------------------
 ROWTIME       | BIGINT           (system)
 ROWKEY        | VARCHAR(STRING)  (system)
 CDC_OPERATION | VARCHAR(STRING)
 ID            | INTEGER
 NAME          | VARCHAR(STRING)
-------------------------------------------
For runtime statistics and query details run: DESCRIBE EXTENDED <Stream,Table>;
ksql> SELECT * FROM team_stream;
1555270351215 | 1 | c | 1 | kafka
ksql> CREATE TABLE team_table ( id VARCHAR, name VARCHAR) \
        WITH (VALUE_FORMAT = 'JSON', \
            KAFKA_TOPIC = 'TEAM_STREAM', \
            KEY = 'id');
 Message
---------------
 Table created
---------------
ksql> SELECT m.name, t.name \
      FROM member_stream m LEFT JOIN team_table t \
      ON m.team_id = t.id;    
jun rao | kafka
```
```
$ docker-compose exec mysql mysql --user=root --password=password --database=mydb -e "UPDATE member set name='Jay Kreps' where id = '1'"
```

A new line is displayed
```
Jay Kreps | kafka
```

Let's materizalize this stream to make it accessible from the outside

```
ksql> CREATE stream member_team_join AS SELECT m.name, t.name \
       FROM member_stream m LEFT JOIN team_table t \
       ON m.team_id = t.id;

 Message
----------------------------
 Stream created and running
----------------------------
```

We now have one more topic (`MEMBER_TEAM_JOIN`) to consume from

```
$ docker-compose exec kafka-1 kafka-topics --zookeeper zookeeper:2181 --list
MEMBER_STREAM
MEMBER_TEAM_JOIN
TEAM_STREAM
__confluent.support.metrics
__consumer_offsets
_confluent-ksql-default__command_topic
_confluent-ksql-default_query_CSAS_MEMBER_TEAM_JOIN_2-Join-repartition
_confluent-ksql-default_query_CSAS_MEMBER_TEAM_JOIN_2-KafkaTopic_Right-reduce-changelog
connect-config
connect-offsets
connect-status
dbserver1.mydb.member
dbserver1.mydb.team
schema-changes.mydb
```

# The full action ?

[![screencast](https://asciinema.org/a/cKabJiM4U4cP5kH2jxNaDjD02.png)](https://asciinema.org/a/cKabJiM4U4cP5kH2jxNaDjD02?autoplay=1)

