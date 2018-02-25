# Objective 

Getting Kafka Connect up and ready with a mysql input

# Mysql

```yml
version: '3.4'
services:
  mysql:
    image: mysql:5.7
    volumes:
      - ./mysql-init.sql:/docker-entrypoint-initdb.d/mysql-init.sql
    environment:
      MYSQL_ROOT_PASSWORD: password
      MYSQL_DATABASE: db
      MYSQL_USER: user
      MYSQL_PASSWORD: password

volumes:
  mysql:
```

with a `mysql-init.sql`


```sql 
CREATE DATABASE IF NOT EXISTS db;

USE db;

CREATE TABLE IF NOT EXISTS applications (
  name VARCHAR(255) NOT NULL PRIMARY KEY,
  team_email VARCHAR(255) NOT NULL,
  production_release DATE
);


INSERT INTO applications VALUES (
  'kafka',
  'kafka@team.co',
  DATE('2017-12-01')
);
```

Let's test that:

```
$ docker-compose exec mysql bash -c "mysql --user=root --password=password --database=db -e 'describe application'"
mysql: [Warning] Using a password on the command line interface can be insecure.
+---------------+--------------+------+-----+---------+----------------+
| Field         | Type         | Null | Key | Default | Extra          |
+---------------+--------------+------+-----+---------+----------------+
| id            | int(11)      | NO   | PRI | NULL    | auto_increment |
| name          | varchar(255) | NO   |     | NULL    |                |
| team_email    | varchar(255) | NO   |     | NULL    |                |
| last_modified | date         | YES  |     | NULL    |                |
+---------------+--------------+------+-----+---------+----------------+
```

```
$ docker-compose exec mysql bash -c "mysql --user=root --password=password --database=db -e 'select * from application'"
mysql: [Warning] Using a password on the command line interface can be insecure.
+-------+---------------+--------------------+
| name  | team_email    | production_release |
+-------+---------------+--------------------+
| kafka | kafka@team.co | 2017-12-01         |
+-------+---------------+--------------------+
```

All good, let's setup kafka connect now.

# Kafka Connect


api : https://docs.confluent.io/current/connect/restapi.html


http http://localhost:8083/connector-plugins/


```
curl -s -XPUT -H "Content-Type: application/json; charset=UTF-8" http://localhost:8083/connector-plugins/JdbcSourceConnector/config/validate -d '
{
  "connector.class":"io.confluent.connect.jdbc.JdbcSourceConnector",
  "tasks.max":"10",
  "connection.url":"jdbc:mysql://mysql:3306/db?user=user&password=password&useSSL=false",
  "table.whitelist":"application",
  "topic.prefix":"mysql-"
}
' | jq ".error_count"
4
```

a valid one

```
$ curl -s -XPUT -H "Content-Type: application/json; charset=UTF-8" http://localhost:8083/connector-plugins/JdbcSourceConnector/config/validate -d '
{
  "connector.class":"io.confluent.connect.jdbc.JdbcSourceConnector",
  "name":"my-mysql-connector",
  "tasks.max":"10",
  "connection.url":"jdbc:mysql://mysql:3306/db?user=user&password=password&useSSL=false",
  "table.whitelist":"application",
  "mode":"timestamp+incrementing",
  "timestamp.column.name":"last_modified",
  "incrementing.column.name":"id",
  "topic.prefix":"mysql-"
}
' | jq ".error_count"
0
```

Let's post it

```
curl -s -XPOST -H "Content-Type: application/json; charset=UTF-8" http://localhost:8083/connectors/ -d '
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
      "topic.prefix":"mysql-"
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

Let's see all the stages of this failed connect worker in `connect-status`:

```sh
$ docker-compose exec kafka-1 kafka-console-consumer --bootstrap-server kafka-1:9092 --topic connect-status --from-beginning
{"state":"RUNNING","trace":null,"worker_id":"connect:8083","generation":2}
{"state":"UNASSIGNED","trace":null,"worker_id":"connect:8083","generation":2}
{"state":"FAILED","trace":"org.apache.kafka.connect.errors.ConnectException: java.sql.SQLException: No suitable driver found for jdbc:mysql://mysql:3306/db?user=user&password=password\n\tat io.confluent.connect.jdbc.util.CachedConnectionProvider.getValidConnection(CachedConnectionProvider.java:75)\n\tat io.confluent.connect.jdbc.JdbcSourceConnector.start(JdbcSourceConnector.java:95)\n\tat org.apache.kafka.connect.runtime.WorkerConnector.doStart(WorkerConnector.java:108)\n\tat org.apache.kafka.connect.runtime.WorkerConnector.start(WorkerConnector.java:133)\n\tat org.apache.kafka.connect.runtime.WorkerConnector.transitionTo(WorkerConnector.java:192)\n\tat org.apache.kafka.connect.runtime.Worker.startConnector(Worker.java:211)\n\tat org.apache.kafka.connect.runtime.distributed.DistributedHerder.startConnector(DistributedHerder.java:894)\n\tat org.apache.kafka.connect.runtime.distributed.DistributedHerder.access$1300(DistributedHerder.java:108)\n\tat org.apache.kafka.connect.runtime.distributed.DistributedHerder$15.call(DistributedHerder.java:910)\n\tat org.apache.kafka.connect.runtime.distributed.DistributedHerder$15.call(DistributedHerder.java:906)\n\tat java.util.concurrent.FutureTask.run(FutureTask.java:266)\n\tat java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1142)\n\tat java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:617)\n\tat java.lang.Thread.run(Thread.java:745)\nCaused by: java.sql.SQLException: No suitable driver found for jdbc:mysql://mysql:3306/db?user=user&password=password\n\tat java.sql.DriverManager.getConnection(DriverManager.java:689)\n\tat java.sql.DriverManager.getConnection(DriverManager.java:247)\n\tat io.confluent.connect.jdbc.util.CachedConnectionProvider.newConnection(CachedConnectionProvider.java:85)\n\tat io.confluent.connect.jdbc.util.CachedConnectionProvider.getValidConnection(CachedConnectionProvider.java:68)\n\t... 13 more\n","worker_id":"connect:8083","generation":1}
```

add driver to folder

```sh
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
      "trace": "org.apache.kafka.connect.errors.ConnectException: Cannot make incremental queries using timestamp column last_modified on application because this column is nullable.\n\tat io.confluent.connect.jdbc.source.JdbcSourceTask.validateNonNullable(JdbcSourceTask.java:287)\n\tat io.confluent.connect.jdbc.source.JdbcSourceTask.start(JdbcSourceTask.java:130)\n\tat org.apache.kafka.connect.runtime.WorkerSourceTask.execute(WorkerSourceTask.java:157)\n\tat org.apache.kafka.connect.runtime.WorkerTask.doRun(WorkerTask.java:170)\n\tat org.apache.kafka.connect.runtime.WorkerTask.run(WorkerTask.java:214)\n\tat java.util.concurrent.Executors$RunnableAdapter.call(Executors.java:511)\n\tat java.util.concurrent.FutureTask.run(FutureTask.java:266)\n\tat java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1142)\n\tat java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:617)\n\tat java.lang.Thread.run(Thread.java:745)\n",
      "id": 0,
      "worker_id": "connect:8083"
    }
  ],
  "type": "source"
}

``` 


Ok let's fix the data structure, then

```sh
$ docker-compose exec mysql bash -c "mysql --user=root --password=password --database=db -e 'describe application'"
mysql: [Warning] Using a password on the command line interface can be insecure.
+---------------+--------------+------+-----+---------+----------------+
| Field         | Type         | Null | Key | Default | Extra          |
+---------------+--------------+------+-----+---------+----------------+
| id            | int(11)      | NO   | PRI | NULL    | auto_increment |
| name          | varchar(255) | NO   |     | NULL    |                |
| team_email    | varchar(255) | NO   |     | NULL    |                |
| last_modified | date         | YES  |     | NULL    |                |
+---------------+--------------+------+-----+---------+----------------+
```

Let's recreate it


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
      "trace": "java.lang.ClassCastException: java.sql.Date cannot be cast to java.sql.Timestamp\n\tat io.confluent.connect.jdbc.source.TimestampIncrementingTableQuerier.extractOffset(TimestampIncrementingTableQuerier.java:231)\n\tat io.confluent.connect.jdbc.source.TimestampIncrementingTableQuerier.extractRecord(TimestampIncrementingTableQuerier.java:207)\n\tat io.confluent.connect.jdbc.source.JdbcSourceTask.poll(JdbcSourceTask.java:230)\n\tat org.apache.kafka.connect.runtime.WorkerSourceTask.execute(WorkerSourceTask.java:179)\n\tat org.apache.kafka.connect.runtime.WorkerTask.doRun(WorkerTask.java:170)\n\tat org.apache.kafka.connect.runtime.WorkerTask.run(WorkerTask.java:214)\n\tat java.util.concurrent.Executors$RunnableAdapter.call(Executors.java:511)\n\tat java.util.concurrent.FutureTask.run(FutureTask.java:266)\n\tat java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1142)\n\tat java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:617)\n\tat java.lang.Thread.run(Thread.java:745)\n",
      "id": 0,
      "worker_id": "connect:8083"
    }
  ],
  "type": "source"
}
``` 

Ok date is not what is required, it's a `timestamp`. let's update the datastructure.

```sh
$ docker-compose exec mysql bash -c "mysql --user=root --password=password --database=db -e 'describe application'"
mysql: [Warning] Using a password on the command line interface can be insecure.
+---------------+--------------+------+-----+---------+----------------+
| Field         | Type         | Null | Key | Default | Extra          |
+---------------+--------------+------+-----+---------+----------------+
| id            | int(11)      | NO   | PRI | NULL    | auto_increment |
| name          | varchar(255) | NO   |     | NULL    |                |
| team_email    | varchar(255) | NO   |     | NULL    |                |
| last_modified | datetime     | NO   |     | NULL    |                |
+---------------+--------------+------+-----+---------+----------------+
```


```sh
$ curl -s localhost:8083/connectors/my-mysql-connector/status | jq .
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

```sh
$ docker-compose exec kafka-1 kafka-topics --zookeeper zookeeper:2181 --list
__consumer_offsets
connect-config
connect-offsets
connect-status
mysql-application

$ docker-compose exec kafka-1 kafka-topics --zookeeper zookeeper:2181 --topic mysql-application --describe
Topic:mysql-application	PartitionCount:1	ReplicationFactor:3	Configs:
	Topic: mysql-application	Partition: 0	Leader: 1	Replicas: 1,2,3	Isr: 1,2,3
```

Let's see the data

```sh
$ docker-compose exec kafka-1 kafka-console-consumer --bootstrap-server kafka-1:9092 --topic mysql-application --from-beginning
{"schema":{"type":"struct","fields":[{"type":"int32","optional":false,"field":"id"},{"type":"string","optional":false,"field":"name"},{"type":"string","optional":false,"field":"team_email"},{"type":"int64","optional":false,"name":"org.apache.kafka.connect.data.Timestamp","version":1,"field":"last_modified"}],"optional":false,"name":"application"},"payload":{"id":1,"name":"kafka","team_email":"kafka@apache.org","last_modified":1518540778000}}
^CProcessed a total of 1 messages
```

Let's zoom

```json
{
  "schema": {
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
        "field": "team_email"
      },
      {
        "type": "int64",
        "optional": false,
        "name": "org.apache.kafka.connect.data.Timestamp",
        "version": 1,
        "field": "last_modified"
      }
    ],
    "optional": false,
    "name": "application"
  },
  "payload": {
    "id": 1,
    "name": "kafka",
    "team_email": "kafka@apache.org",
    "last_modified": 1518540791000
  }
}
```

What ? What is this schema ? I want only the payload !

Let's introduce the Schema Registry ! 
