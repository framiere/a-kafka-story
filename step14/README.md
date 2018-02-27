# Objective 

Ksql using the standalone version 

Let's create simple data 

```
echo '{"name":"simpson","firstname":"lisa","age":8}
{"name":"simpson","firstname":"bart","age":10}
{"name":"simpson","firstname":"maggie","age":1}
{"name":"simpson","firstname":"homer","age":39}
{"name":"simpson","firstname":"marge","age":36}
{"name":"wayne","firstname":"bruce","age":39}' \
    | docker exec -i $(docker-compose ps -q kafka-1) kafka-console-producer --broker-list kafka-1:9092 --topic heroes
```

Let's our topics 

```
$ docker-compose exec kafka-1 kafka-topics --zookeeper zookeeper:2181 --list
heroes
```

Let's kickstart KSQL

```
$ docker-compose exec ksql-cli ksql-cli local --bootstrap-server kafka-1:9092
```

Specify to get to earliest data

```
ksql> CREATE STREAM heroes (name varchar, firstname varchar, age bigint) \
        WITH ( kafka_topic='heroes',value_format='JSON');

 Message
----------------------------
 Stream created and running
---------------------------
ksql> SELECT * FROM heroes;
...
```

There's nothing because no new data is flowing to the system, just kill it with Ctrl-C.
And update `auto.offset.reset` 

```
ksql> SET 'auto.offset.reset' = 'earliest';
Successfully changed local property 'auto.offset.reset' from 'null' to 'earliest'
ksql> SELECT * FROM heroes;
1519766414175 | null | simpson | lisa | 8
1519766414189 | null | simpson | bart | 10
1519766414189 | null | simpson | maggie | 1
1519766414189 | null | simpson | homer | 39
1519766414189 | null | simpson | marge | 36
1519766414189 | null | wayne | bruce | 39
^CQuery terminated
ksql> exit
```

Let's our topics again 

```
$ docker-compose exec kafka-1 kafka-topics --zookeeper zookeeper:2181 --list
__consumer_offsets
heroes
ksql__commands
```

`heroes` has been created by us
`ksql__commands` is created by ksql
`__consumer_offsets` has been created via the consumer used by ksql

Let's look into `ksql__commands`

```
$ docker-compose exec kafka-1 kafka-console-consumer --bootstrap-server kafka-1:9092 --topic ksql__commands --from-beginning
  {"statement":"CREATE STREAM heroes (name varchar, firstname varchar, age bigint)         WITH ( kafka_topic='heroes',value_format='JSON');","streamsProperties":{}}

```

Ok makes sense.

Fine, let's create a new stream to get only the simpsons

```
$ docker-compose exec ksql-cli ksql-cli local --bootstrap-server kafka-1:9092
ksql> SET 'auto.offset.reset' = 'earliest';
ksql> CREATE STREAM simpsons \
        AS SELECT * FROM heroes \
            WHERE name = 'simpson';

 Message
----------------------------
 Stream created and running
---------------------------
ksql> SELECT * FROM simpsons;
1519766414189 | null | simpson | bart | 10
1519766414189 | null | simpson | maggie | 1
1519766414175 | null | simpson | lisa | 8
1519766414189 | null | simpson | marge | 36
1519766414189 | null | simpson | homer | 39
^CQuery terminated
```

Let's add some random heroes 

```
ksql> CREATE STREAM random_heroes \
        AS SELECT * FROM heroes \
            WHERE name LIKE 'random%';

 Message
----------------------------
 Stream created and running
---------------------------
ksql> SELECT * FROM random_heroes;

```

While this stream is running, let's create random heroes

```sh
$ while [ true ] ; do
echo '{"name":"random-'$(( RANDOM % 5 ))'","firstname":"random","age":'$(( RANDOM % 80 ))'}' \
    | docker exec -i $(docker-compose ps -q kafka-1) kafka-console-producer --broker-list kafka-1:9092 --topic heroes
done
```

Let's see the topics again

```sh
$ docker-compose exec kafka-1 kafka-topics --zookeeper zookeeper:2181 --list
RANDOM_HEROES
SIMPSONS
__consumer_offsets
heroes
ksql__commands
```

2 new topics that represent our created topics.

We can also see 

Let's do now some more non trivial computation now:

```
ksql> SET 'auto.offset.reset' = 'earliest';
ksql> SELECT \
        name, \
        SUM(age)/COUNT(*) AS average, \
        SUM(age) AS sumAge, \
        COUNT(*) AS count, \
        MAX(age) AS maxAge, \
        MIN(age) AS minAge \
      FROM \
        random_heroes \
      GROUP BY \
         name;
random-4 | 34 | 34 | 1 | 34 | 34
random-3 | 49 | 348 | 7 | 67 | 20
random-1 | 34 | 69 | 2 | 62 | 7
random-2 | 29 | 59 | 2 | 58 | 1
random-4 | 29 | 59 | 2 | 34 | 25
random-4 | 21 | 65 | 3 | 34 | 6
random-3 | 50 | 402 | 8 | 67 | 20
^CQuery terminated    
```

Fine, let's create a table out of it !

```
ksql> CREATE TABLE heroes_ages AS \
      SELECT \
          name, \
          SUM(age)/COUNT(*) AS average, \
          SUM(age) AS sumAge, \
          COUNT(*) AS count, \
          MAX(age) AS maxAge, \
          MIN(age) AS minAge \
      FROM \
          heroes \
      GROUP BY \
          name;
 Message
------------------------------------
 Statement written to command topic
------------------------------------          
```

This computation is now ready to be consumed in the `heroes_ages` topic
 
```sh
$ docker-compose exec kafka-1 kafka-topics --zookeeper zookeeper:2181 --list
HEROES_AGES
RANDOM_HEROES
SIMPSONS
__consumer_offsets
heroes
ksql__commands
ksql_query_CTAS_HEROES_AGES-KSTREAM-AGGREGATE-STATE-STORE-0000000005-changelog
ksql_query_CTAS_HEROES_AGES-KSTREAM-AGGREGATE-STATE-STORE-0000000005-repartition
```

As we have stateful computation (GROUP BY) we have intermediary topics that store this computation and enable resiliency.


We can also use windowing functions !

```sh
ksql> SET 'auto.offset.reset' = 'earliest';
ksql> CREATE TABLE random_heroes_count AS \
        SELECT name, COUNT(*) AS count \
        FROM random_heroes \
        WINDOW HOPPING (SIZE 20 SECONDS, ADVANCE BY 20 SECONDS) \
        GROUP BY name;
ksql> 
1519773520000 | random-0 : Window{start=1519773520000 end=-} | random-0 | 2
1519773500000 | random-0 : Window{start=1519773500000 end=-} | random-0 | 2
1519773480000 | random-3 : Window{start=1519773480000 end=-} | random-3 | 2
1519773460000 | random-3 : Window{start=1519773460000 end=-} | random-3 | 1
1519773500000 | random-3 : Window{start=1519773500000 end=-} | random-3 | 1
1519773520000 | random-3 : Window{start=1519773520000 end=-} | random-3 | 1
1519773480000 | random-4 : Window{start=1519773480000 end=-} | random-4 | 2
1519773500000 | random-4 : Window{start=1519773500000 end=-} | random-4 | 4
1519773520000 | random-4 : Window{start=1519773520000 end=-} | random-4 | 3
1519773500000 | random-1 : Window{start=1519773500000 end=-} | random-1 | 2
1519773460000 | random-1 : Window{start=1519773460000 end=-} | random-1 | 1
...
```

Let's see the data from the outside :

```
$ docker-compose exec kafka-1 kafka-console-consumer --bootstrap-server kafka-1:9092 --topic RANDOM_HEROES_COUNT --from-beginning
{"COUNT":1,"NAME":"random-0"}
{"COUNT":1,"NAME":"random-3"}
{"COUNT":1,"NAME":"random-3"}
{"COUNT":1,"NAME":"random-4"}
{"COUNT":2,"NAME":"random-0"}
{"COUNT":1,"NAME":"random-2"}
{"COUNT":1,"NAME":"random-1"}
{"COUNT":2,"NAME":"random-4"}
```

We can see our created tables and streams

```
sql> SHOW TABLES;
 Table Name          | Kafka Topic         | Format | Windowed
---------------------------------------------------------------
 RANDOM_HEROES_COUNT | RANDOM_HEROES_COUNT | JSON   | true
 HEROES_AGES         | HEROES_AGES         | JSON   | false
---------------------------------------------------------------
ksql> SHOW STREAMS;
 Stream Name   | Kafka Topic   | Format
----------------------------------------
 SIMPSONS      | SIMPSONS      | JSON
 HEROES        | heroes        | JSON
 RANDOM_HEROES | RANDOM_HEROES | JSON
----------------------------------------
```

Let's add some nested data

```
echo '{"name":"one","obj":{"subField":1}}
{"name":"one","obj":{"subField":2}}
{"name":"two","obj":{"subField":3}}' \
    | docker exec -i $(docker-compose ps -q kafka-1) kafka-console-producer --broker-list kafka-1:9092 --topic nested
```

And query the nested data

```
ksql> SET 'auto.offset.reset' = 'earliest';
ksql> CREATE STREAM nested (name varchar, obj varchar) \
            WITH ( kafka_topic='nested',value_format='JSON');
ksql> SELECT name, COUNT(EXTRACTJSONFIELD(obj,'$.subField')) FROM nested GROUP BY name;
one | 2
two | 1
```

# The full action ?

[![screencast](https://asciinema.org/a/74rGpmh3SfCjYbi4InpEswA0z.png)](https://asciinema.org/a/74rGpmh3SfCjYbi4InpEswA0z?autoplay=1)
