# Objective 

Getting Change data capture ready with mysql and debezium 


# Mysql

Let's setup binlog, 

```
# ----------------------------------------------
# Enable the binlog for replication & CDC
# ----------------------------------------------

# Enable binary replication log and set the prefix, expiration, and log format.
# The prefix is arbitrary, expiration can be short for integration tests but would
# be longer on a production system. Row-level info is required for ingest to work.
# Server ID is required, but this will vary on production systems
server-id         = 223344
log_bin           = mysql-bin
expire_logs_days  = 1
binlog_format     = row
```

More info here : https://github.com/debezium/docker-images/blob/master/examples/mysql/0.8/mysql.cnf

Let's map this file to our mysql instance

```yml
  mysql:
    image: mysql:5.7
    volumes:
      - ./mysql.cnf:/etc/mysql/conf.d/mysql.cnf
      - ../step9/mysql-init.sql:/docker-entrypoint-initdb.d/mysql-init.sql
    environment:
      MYSQL_ROOT_PASSWORD: password
      MYSQL_DATABASE: db
      MYSQL_USER: user
      MYSQL_PASSWORD: password
 ```
 
Let' setup the debezium user too in the `mysql-init.sql`  

```sql 
  GRANT REPLICATION SLAVE, REPLICATION CLIENT ON *.* TO 'replicator' IDENTIFIED BY 'replpass';
  GRANT SELECT, RELOAD, SHOW DATABASES, REPLICATION SLAVE, REPLICATION CLIENT  ON *.* TO 'debezium' IDENTIFIED BY 'dbz';
```
 
 # Kafka Connect
 
 Let's download debezium connect plugin  
 
 ```
$ wget https://repo1.maven.org/maven2/io/debezium/debezium-connector-mysql/0.7.3/debezium-connector-mysql-0.7.3-plugin.tar.gz
$ tar -xvf debezium-connector-mysql-0.7.3-plugin.tar.gz
 ```
 
 Let's map it to connect
 
```yml
     volumes:
       - ../step9/mysql-connector-java-5.1.45-bin.jar:/usr/share/java/kafka-connect-jdbc/mysql-connector-java-5.1.45-bin.jar
       - ./debezium-connector-mysql:/usr/share/java/debezium-connector-mysql

```
 
Let's verify we have our connector ready to be used

```
$ docker-compose exec connect curl localhost:8083/connector-plugins | jq .
[
  {
    "class": "io.confluent.connect.elasticsearch.ElasticsearchSinkConnector",
    "type": "sink",
    "version": "4.0.0"
  },
 ...
   {
     "class": "io.debezium.connector.mysql.MySqlConnector",
     "type": "source",
     "version": "0.7.3"
   },
  ...
]
```

Perfect, let's use it
 
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
{"schema":{"type":"struct","fields":[{"type":"struct","fields":[{"type":"int32","optional":false,"field":"id"},{"type":"string","optional":false,"field":"name"},{"type":"string","optional":false,"field":"email"},{"type":"int64","optional":false,"name":"io.debezium.time.Timestamp","version":1,"field":"last_modified"}],"optional":true,"name":"dbserver1.mydb.team.Value","field":"before"},{"type":"struct","fields":[{"type":"int32","optional":false,"field":"id"},{"type":"string","optional":false,"field":"name"},{"type":"string","optional":false,"field":"email"},{"type":"int64","optional":false,"name":"io.debezium.time.Timestamp","version":1,"field":"last_modified"}],"optional":true,"name":"dbserver1.mydb.team.Value","field":"after"},{"type":"struct","fields":[{"type":"string","optional":true,"field":"version"},{"type":"string","optional":false,"field":"name"},{"type":"int64","optional":false,"field":"server_id"},{"type":"int64","optional":false,"field":"ts_sec"},{"type":"string","optional":true,"field":"gtid"},{"type":"string","optional":false,"field":"file"},{"type":"int64","optional":false,"field":"pos"},{"type":"int32","optional":false,"field":"row"},{"type":"boolean","optional":true,"field":"snapshot"},{"type":"int64","optional":true,"field":"thread"},{"type":"string","optional":true,"field":"db"},{"type":"string","optional":true,"field":"table"}],"optional":false,"name":"io.debezium.connector.mysql.Source","field":"source"},{"type":"string","optional":false,"field":"op"},{"type":"int64","optional":true,"field":"ts_ms"}],"optional":false,"name":"dbserver1.mydb.team.Envelope"},"payload":{"before":null,"after":{"id":1,"name":"kafka","email":"kafka@apache.org","last_modified":1519581055000},"source":{"version":"0.7.3","name":"dbserver1","server_id":0,"ts_sec":0,"gtid":null,"file":"mysql-bin.000003","pos":154,"row":0,"snapshot":true,"thread":null,"db":"mydb","table":"team"},"op":"c","ts_ms":1519581125772}}
```

Here's the formatted version : 

```json
{
  "schema": {
    "type": "struct",
    "fields": [
      {
        "type": "struct",
        "fields": [
          {
            "type": "int32",
            "optional": false,
            "field": "id"
          },
          {
            "type": "string",
            "optional": false,
            "field": "name"
          },
          {
            "type": "string",
            "optional": false,
            "field": "email"
          },
          {
            "type": "int64",
            "optional": false,
            "name": "io.debezium.time.Timestamp",
            "version": 1,
            "field": "last_modified"
          }
        ],
        "optional": true,
        "name": "dbserver1.mydb.team.Value",
        "field": "before"
      },
      {
        "type": "struct",
        "fields": [
          {
            "type": "int32",
            "optional": false,
            "field": "id"
          },
          {
            "type": "string",
            "optional": false,
            "field": "name"
          },
          {
            "type": "string",
            "optional": false,
            "field": "email"
          },
          {
            "type": "int64",
            "optional": false,
            "name": "io.debezium.time.Timestamp",
            "version": 1,
            "field": "last_modified"
          }
        ],
        "optional": true,
        "name": "dbserver1.mydb.team.Value",
        "field": "after"
      },
      {
        "type": "struct",
        "fields": [
          {
            "type": "string",
            "optional": true,
            "field": "version"
          },
          {
            "type": "string",
            "optional": false,
            "field": "name"
          },
          {
            "type": "int64",
            "optional": false,
            "field": "server_id"
          },
          {
            "type": "int64",
            "optional": false,
            "field": "ts_sec"
          },
          {
            "type": "string",
            "optional": true,
            "field": "gtid"
          },
          {
            "type": "string",
            "optional": false,
            "field": "file"
          },
          {
            "type": "int64",
            "optional": false,
            "field": "pos"
          },
          {
            "type": "int32",
            "optional": false,
            "field": "row"
          },
          {
            "type": "boolean",
            "optional": true,
            "field": "snapshot"
          },
          {
            "type": "int64",
            "optional": true,
            "field": "thread"
          },
          {
            "type": "string",
            "optional": true,
            "field": "db"
          },
          {
            "type": "string",
            "optional": true,
            "field": "table"
          }
        ],
        "optional": false,
        "name": "io.debezium.connector.mysql.Source",
        "field": "source"
      },
      {
        "type": "string",
        "optional": false,
        "field": "op"
      },
      {
        "type": "int64",
        "optional": true,
        "field": "ts_ms"
      }
    ],
    "optional": false,
    "name": "dbserver1.mydb.team.Envelope"
  },
  "payload": {
    "before": null,
    "after": {
      "id": 1,
      "name": "kafka",
      "email": "kafka@apache.org",
      "last_modified": 1519581055000
    },
    "source": {
      "version": "0.7.3",
      "name": "dbserver1",
      "server_id": 0,
      "ts_sec": 0,
      "gtid": null,
      "file": "mysql-bin.000003",
      "pos": 154,
      "row": 0,
      "snapshot": true,
      "thread": null,
      "db": "mydb",
      "table": "team"
    },
    "op": "c",
    "ts_ms": 1519581125772
  }
}
```

That's easy to understand, but let's outline the operation with `jq '{time_ms: .payload.ts_ms, op: .payload.op, table: .payload.source.table}`

```
$ docker-compose exec kafka-1 kafka-console-consumer --bootstrap-server kafka-1:9092 --topic dbserver1.mydb.team --from-beginning | jq '{time_ms: .payload.ts_ms, op: .payload.op, table: .payload.source.table}'
{
   "time_ms": 1519582917064,
                              "op": "c",
                                          "table": "team"
                                                         }
```


Let's add another element in team 

```
$ docker-compose exec mysql mysql --user=root --password=password --database=mydb -e "
INSERT INTO team (   \
  name, \
  email,   \
  last_modified \
) VALUES (  \
  'another',  \
  'another@apache.org',   \
  NOW() \
); "

$ docker-compose exec mysql mysql --user=root --password=password --database=mydb -e "SELECT * FROM team";
mysql: [Warning] Using a password on the command line interface can be insecure.
+----+---------+--------------------+---------------------+
| id | name    | email              | last_modified       |
+----+---------+--------------------+---------------------+
|  1 | kafka   | kafka@apache.org   | 2018-02-25 18:19:44 |
|  2 | another | another@apache.org | 2018-02-25 18:22:56 |
+----+---------+--------------------+---------------------+
```

Yes the insert has been captured 

```
$ docker-compose exec kafka-1 kafka-console-consumer --bootstrap-server kafka-1:9092 --topic dbserver1.mydb.team --from-beginning
{"schema":{"type":"struct","fields":[{"type":"struct","fields":[{"type":"int32","optional":false,"field":"id"},{"type":"string","optional":false,"field":"name"},{"type":"string","optional":false,"field":"email"},{"type":"int64","optional":false,"name":"io.debezium.time.Timestamp","version":1,"field":"last_modified"}],"optional":true,"name":"dbserver1.mydb.team.Value","field":"before"},{"type":"struct","fields":[{"type":"int32","optional":false,"field":"id"},{"type":"string","optional":false,"field":"name"},{"type":"string","optional":false,"field":"email"},{"type":"int64","optional":false,"name":"io.debezium.time.Timestamp","version":1,"field":"last_modified"}],"optional":true,"name":"dbserver1.mydb.team.Value","field":"after"},{"type":"struct","fields":[{"type":"string","optional":true,"field":"version"},{"type":"string","optional":false,"field":"name"},{"type":"int64","optional":false,"field":"server_id"},{"type":"int64","optional":false,"field":"ts_sec"},{"type":"string","optional":true,"field":"gtid"},{"type":"string","optional":false,"field":"file"},{"type":"int64","optional":false,"field":"pos"},{"type":"int32","optional":false,"field":"row"},{"type":"boolean","optional":true,"field":"snapshot"},{"type":"int64","optional":true,"field":"thread"},{"type":"string","optional":true,"field":"db"},{"type":"string","optional":true,"field":"table"}],"optional":false,"name":"io.debezium.connector.mysql.Source","field":"source"},{"type":"string","optional":false,"field":"op"},{"type":"int64","optional":true,"field":"ts_ms"}],"optional":false,"name":"dbserver1.mydb.team.Envelope"},"payload":{"before":null,"after":{"id":1,"name":"kafka","email":"kafka@apache.org","last_modified":1519582784000},"source":{"version":"0.7.3","name":"dbserver1","server_id":0,"ts_sec":0,"gtid":null,"file":"mysql-bin.000003","pos":154,"row":0,"snapshot":true,"thread":null,"db":"mydb","table":"team"},"op":"c","ts_ms":1519582917064}}
{"schema":{"type":"struct","fields":[{"type":"struct","fields":[{"type":"int32","optional":false,"field":"id"},{"type":"string","optional":false,"field":"name"},{"type":"string","optional":false,"field":"email"},{"type":"int64","optional":false,"name":"io.debezium.time.Timestamp","version":1,"field":"last_modified"}],"optional":true,"name":"dbserver1.mydb.team.Value","field":"before"},{"type":"struct","fields":[{"type":"int32","optional":false,"field":"id"},{"type":"string","optional":false,"field":"name"},{"type":"string","optional":false,"field":"email"},{"type":"int64","optional":false,"name":"io.debezium.time.Timestamp","version":1,"field":"last_modified"}],"optional":true,"name":"dbserver1.mydb.team.Value","field":"after"},{"type":"struct","fields":[{"type":"string","optional":true,"field":"version"},{"type":"string","optional":false,"field":"name"},{"type":"int64","optional":false,"field":"server_id"},{"type":"int64","optional":false,"field":"ts_sec"},{"type":"string","optional":true,"field":"gtid"},{"type":"string","optional":false,"field":"file"},{"type":"int64","optional":false,"field":"pos"},{"type":"int32","optional":false,"field":"row"},{"type":"boolean","optional":true,"field":"snapshot"},{"type":"int64","optional":true,"field":"thread"},{"type":"string","optional":true,"field":"db"},{"type":"string","optional":true,"field":"table"}],"optional":false,"name":"io.debezium.connector.mysql.Source","field":"source"},{"type":"string","optional":false,"field":"op"},{"type":"int64","optional":true,"field":"ts_ms"}],"optional":false,"name":"dbserver1.mydb.team.Envelope"},"payload":{"before":null,"after":{"id":2,"name":"another","email":"another@apache.org","last_modified":1519582976000},"source":{"version":"0.7.3","name":"dbserver1","server_id":223344,"ts_sec":1519582976,"gtid":null,"file":"mysql-bin.000003","pos":354,"row":0,"snapshot":null,"thread":7,"db":"mydb","table":"team"},"op":"c","ts_ms":1519582976582}}```
```

Update is here 

```
$ docker-compose exec kafka-1 kafka-console-consumer --bootstrap-server kafka-1:9092 --topic dbserver1.mydb.team --from-beginning | jq '{time_ms: .payload.ts_ms, op: .payload.op, table: .payload.source.table}'

   "time_ms": 1519582917064,
                              "op": "c",
                                          "table": "team"
                                                         }
                                                          {
                                                             "time_ms": 1519582976582,
                                                                                        "op": "c",
                                                                                                    "table": "team"
                                                                                                                   }
```

Let's update it

```
$ docker-compose exec mysql mysql --user=root --password=password --database=mydb -e "UPDATE team set name='another name', last_modified = NOW() where id = '2'"

$ docker-compose exec mysql mysql --user=root --password=password --database=mydb -e "SELECT * FROM team";
mysql: [Warning] Using a password on the command line interface can be insecure.
+----+--------------+--------------------+---------------------+
| id | name         | email              | last_modified       |
+----+--------------+--------------------+---------------------+
|  1 | kafka        | kafka@apache.org   | 2018-02-25 18:19:44 |
|  2 | another name | another@apache.org | 2018-02-25 18:25:06 |
+----+--------------+--------------------+---------------------+

$ docker-compose exec kafka-1 kafka-console-consumer --bootstrap-server kafka-1:9092 --topic dbserver1.mydb.team --from-beginning | jq '.payload.op'
"c"
   "c"
      "u"
```

Perfect, now let's delete it !
 

```
$ docker-compose exec mysql mysql --user=root --password=password --database=mydb -e "DELETE FROM team WHERE id = 2"

$ docker-compose exec mysql mysql --user=root --password=password --database=mydb -e "SELECT * FROM team";
mysql: [Warning] Using a password on the command line interface can be insecure.
+----+-------+------------------+---------------------+
| id | name  | email            | last_modified       |
+----+-------+------------------+---------------------+
|  1 | kafka | kafka@apache.org | 2018-02-25 18:19:44 |
+----+-------+------------------+---------------------+

$ docker-compose exec kafka-1 kafka-console-consumer --bootstrap-server kafka-1:9092 --topic dbserver1.mydb.team --from-beginning | jq '.payload.op'
"c"
   "c"
      "u"
         "d"
            null
```

We have `d` so there deletion is here.... but what is the null ?


```
$ docker-compose exec kafka-1 kafka-console-consumer --bootstrap-server kafka-1:9092 --topic dbserver1.mydb.team --from-beginning
{"schema":{"type":"struct","fields":[{"type":"struct","fields":[{"type":"int32","optional":false,"field":"id"},{"type":"string","optional":false,"field":"name"},{"type":"string","optional":false,"field":"email"},{"type":"int64","optional":false,"name":"io.debezium.time.Timestamp","version":1,"field":"last_modified"}],"optional":true,"name":"dbserver1.mydb.team.Value","field":"before"},{"type":"struct","fields":[{"type":"int32","optional":false,"field":"id"},{"type":"string","optional":false,"field":"name"},{"type":"string","optional":false,"field":"email"},{"type":"int64","optional":false,"name":"io.debezium.time.Timestamp","version":1,"field":"last_modified"}],"optional":true,"name":"dbserver1.mydb.team.Value","field":"after"},{"type":"struct","fields":[{"type":"string","optional":true,"field":"version"},{"type":"string","optional":false,"field":"name"},{"type":"int64","optional":false,"field":"server_id"},{"type":"int64","optional":false,"field":"ts_sec"},{"type":"string","optional":true,"field":"gtid"},{"type":"string","optional":false,"field":"file"},{"type":"int64","optional":false,"field":"pos"},{"type":"int32","optional":false,"field":"row"},{"type":"boolean","optional":true,"field":"snapshot"},{"type":"int64","optional":true,"field":"thread"},{"type":"string","optional":true,"field":"db"},{"type":"string","optional":true,"field":"table"}],"optional":false,"name":"io.debezium.connector.mysql.Source","field":"source"},{"type":"string","optional":false,"field":"op"},{"type":"int64","optional":true,"field":"ts_ms"}],"optional":false,"name":"dbserver1.mydb.team.Envelope"},"payload":{"before":null,"after":{"id":1,"name":"kafka","email":"kafka@apache.org","last_modified":1519582784000},"source":{"version":"0.7.3","name":"dbserver1","server_id":0,"ts_sec":0,"gtid":null,"file":"mysql-bin.000003","pos":154,"row":0,"snapshot":true,"thread":null,"db":"mydb","table":"team"},"op":"c","ts_ms":1519582917064}}
{"schema":{"type":"struct","fields":[{"type":"struct","fields":[{"type":"int32","optional":false,"field":"id"},{"type":"string","optional":false,"field":"name"},{"type":"string","optional":false,"field":"email"},{"type":"int64","optional":false,"name":"io.debezium.time.Timestamp","version":1,"field":"last_modified"}],"optional":true,"name":"dbserver1.mydb.team.Value","field":"before"},{"type":"struct","fields":[{"type":"int32","optional":false,"field":"id"},{"type":"string","optional":false,"field":"name"},{"type":"string","optional":false,"field":"email"},{"type":"int64","optional":false,"name":"io.debezium.time.Timestamp","version":1,"field":"last_modified"}],"optional":true,"name":"dbserver1.mydb.team.Value","field":"after"},{"type":"struct","fields":[{"type":"string","optional":true,"field":"version"},{"type":"string","optional":false,"field":"name"},{"type":"int64","optional":false,"field":"server_id"},{"type":"int64","optional":false,"field":"ts_sec"},{"type":"string","optional":true,"field":"gtid"},{"type":"string","optional":false,"field":"file"},{"type":"int64","optional":false,"field":"pos"},{"type":"int32","optional":false,"field":"row"},{"type":"boolean","optional":true,"field":"snapshot"},{"type":"int64","optional":true,"field":"thread"},{"type":"string","optional":true,"field":"db"},{"type":"string","optional":true,"field":"table"}],"optional":false,"name":"io.debezium.connector.mysql.Source","field":"source"},{"type":"string","optional":false,"field":"op"},{"type":"int64","optional":true,"field":"ts_ms"}],"optional":false,"name":"dbserver1.mydb.team.Envelope"},"payload":{"before":null,"after":{"id":2,"name":"another","email":"another@apache.org","last_modified":1519582976000},"source":{"version":"0.7.3","name":"dbserver1","server_id":223344,"ts_sec":1519582976,"gtid":null,"file":"mysql-bin.000003","pos":354,"row":0,"snapshot":null,"thread":7,"db":"mydb","table":"team"},"op":"c","ts_ms":1519582976582}}
{"schema":{"type":"struct","fields":[{"type":"struct","fields":[{"type":"int32","optional":false,"field":"id"},{"type":"string","optional":false,"field":"name"},{"type":"string","optional":false,"field":"email"},{"type":"int64","optional":false,"name":"io.debezium.time.Timestamp","version":1,"field":"last_modified"}],"optional":true,"name":"dbserver1.mydb.team.Value","field":"before"},{"type":"struct","fields":[{"type":"int32","optional":false,"field":"id"},{"type":"string","optional":false,"field":"name"},{"type":"string","optional":false,"field":"email"},{"type":"int64","optional":false,"name":"io.debezium.time.Timestamp","version":1,"field":"last_modified"}],"optional":true,"name":"dbserver1.mydb.team.Value","field":"after"},{"type":"struct","fields":[{"type":"string","optional":true,"field":"version"},{"type":"string","optional":false,"field":"name"},{"type":"int64","optional":false,"field":"server_id"},{"type":"int64","optional":false,"field":"ts_sec"},{"type":"string","optional":true,"field":"gtid"},{"type":"string","optional":false,"field":"file"},{"type":"int64","optional":false,"field":"pos"},{"type":"int32","optional":false,"field":"row"},{"type":"boolean","optional":true,"field":"snapshot"},{"type":"int64","optional":true,"field":"thread"},{"type":"string","optional":true,"field":"db"},{"type":"string","optional":true,"field":"table"}],"optional":false,"name":"io.debezium.connector.mysql.Source","field":"source"},{"type":"string","optional":false,"field":"op"},{"type":"int64","optional":true,"field":"ts_ms"}],"optional":false,"name":"dbserver1.mydb.team.Envelope"},"payload":{"before":{"id":2,"name":"another","email":"another@apache.org","last_modified":1519582976000},"after":{"id":2,"name":"another name","email":"another@apache.org","last_modified":1519583106000},"source":{"version":"0.7.3","name":"dbserver1","server_id":223344,"ts_sec":1519583106,"gtid":null,"file":"mysql-bin.000003","pos":657,"row":0,"snapshot":null,"thread":9,"db":"mydb","table":"team"},"op":"u","ts_ms":1519583106645}}
{"schema":{"type":"struct","fields":[{"type":"struct","fields":[{"type":"int32","optional":false,"field":"id"},{"type":"string","optional":false,"field":"name"},{"type":"string","optional":false,"field":"email"},{"type":"int64","optional":false,"name":"io.debezium.time.Timestamp","version":1,"field":"last_modified"}],"optional":true,"name":"dbserver1.mydb.team.Value","field":"before"},{"type":"struct","fields":[{"type":"int32","optional":false,"field":"id"},{"type":"string","optional":false,"field":"name"},{"type":"string","optional":false,"field":"email"},{"type":"int64","optional":false,"name":"io.debezium.time.Timestamp","version":1,"field":"last_modified"}],"optional":true,"name":"dbserver1.mydb.team.Value","field":"after"},{"type":"struct","fields":[{"type":"string","optional":true,"field":"version"},{"type":"string","optional":false,"field":"name"},{"type":"int64","optional":false,"field":"server_id"},{"type":"int64","optional":false,"field":"ts_sec"},{"type":"string","optional":true,"field":"gtid"},{"type":"string","optional":false,"field":"file"},{"type":"int64","optional":false,"field":"pos"},{"type":"int32","optional":false,"field":"row"},{"type":"boolean","optional":true,"field":"snapshot"},{"type":"int64","optional":true,"field":"thread"},{"type":"string","optional":true,"field":"db"},{"type":"string","optional":true,"field":"table"}],"optional":false,"name":"io.debezium.connector.mysql.Source","field":"source"},{"type":"string","optional":false,"field":"op"},{"type":"int64","optional":true,"field":"ts_ms"}],"optional":false,"name":"dbserver1.mydb.team.Envelope"},"payload":{"before":{"id":2,"name":"another name","email":"another@apache.org","last_modified":1519583106000},"after":null,"source":{"version":"0.7.3","name":"dbserver1","server_id":223344,"ts_sec":1519583172,"gtid":null,"file":"mysql-bin.000003","pos":995,"row":0,"snapshot":null,"thread":11,"db":"mydb","table":"team"},"op":"d","ts_ms":1519583172741}}
{"schema":null,"payload":null}
```

The deletion payload has the following form 

```json
{
  "payload": {
    "before": {
      "id": 2,
      "name": "another name",
      "email": "another@apache.org",
      "last_modified": 1519583106000
    },
    "after": null,
    "source": {
      "version": "0.7.3",
      "name": "dbserver1",
      "server_id": 223344,
      "ts_sec": 1519583172,
      "gtid": null,
      "file": "mysql-bin.000003",
      "pos": 995,
      "row": 0,
      "snapshot": null,
      "thread": 11,
      "db": "mydb",
      "table": "team"
    },
    "op": "d",
    "ts_ms": 1519583172741
  }
}
```

# The full action ?

[![screencast](https://asciinema.org/a/lUa5RhZUeJOsPB4XSbM0VnVy3.png)](https://asciinema.org/a/lUa5RhZUeJOsPB4XSbM0VnVy3?autoplay=1)
