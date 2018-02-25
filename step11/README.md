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
      - ./mysql-init.sql:/mysql-init.sql
      - ./mysql.cnf:/etc/mysql/conf.d/mysql.cnf
    environment:
      MYSQL_ROOT_PASSWORD: password
      MYSQL_DATABASE: db
      MYSQL_USER: user
      MYSQL_PASSWORD: password
    command: --init-file /mysql-init.sql
 ```
 
 Let' setup the debezium user 

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
        "database.server.name": "db",
        "database.whitelist": "application",
        "database.history.kafka.bootstrap.servers": "kafka-1:9092",
        "database.history.kafka.topic": "schema-changes.application"
    }
}
 '
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
db
schema-changes.application
```

