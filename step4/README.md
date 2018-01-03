# Objective

1. Gather statistics using telegraf

# Telegraf

In this example, we'll add [telegraf](https://github.com/influxdata/telegraf/) that will gather the container metrics

```yml
  telegraf:
    image: telegraf:1.5
    restart: unless-stopped
    volumes:
      - /var/run/docker.sock:/tmp/docker.sock
      - ./telegraf.conf:/etc/telegraf/telegraf.conf:ro
    links:
      - kafka-1
      - kafka-2
      - kafka-3
```

Note: we specified the `unless-stopped` [restart policy](https://docs.docker.com/compose/compose-file/#restart) as telegraf will fail to start if the cluster is not already ready.

Note: In order for telegraf to gather docker metrics, we provide it the docker socket as a volume mapping.

The `telegraf.conf` is the following

```conf
[agent]
interval = "5s"

[[inputs.docker]]
endpoint = "unix:///tmp/docker.sock"

[[outputs.kafka]]
brokers = ["kafka-1:9092","kafka-2:9092","kafka-3:9092"]
topic = "telegraf"
```

# Let's run it

```
$ docker-compose exec kafka-1 kafka-console-consumer --bootstrap-server kafka-1:9092 --topic telegraf
docker_container_mem,container_version=unknown,com.docker.compose.service=kafka-1,io.confluent.docker=true,com.docker.compose.config-hash=50c870843a239004389079d1baf9067fe0fc0339701905f19af63240a358e2a2,com.docker.compose.container-number=1,com.docker.compose.version=1.17.1,com.docker.compose.project=step4,engine_host=moby,container_name=step4_kafka-1_1,container_image=confluentinc/cp-kafka,io.confluent.docker.git.id=e0c39c6,io.confluent.docker.build.number=None,com.docker.compose.oneoff=False,host=af95c0c993d7 total_pgfault=340388i,total_pgpgin=238572i,total_active_file=180224i,unevictable=0i,inactive_file=294912i,pgpgin=238572i,pgpgout=140181i,rss_huge=0i,total_pgmajfault=0i,total_rss_huge=0i,active_file=180224i,hierarchical_memory_limit=9223372036854771712i,pgmajfault=0i,total_rss=402534400i,total_mapped_file=65536i,max_usage=416272384i,total_active_anon=402452480i,total_inactive_anon=0i,total_inactive_file=294912i,total_unevictable=0i,active_anon=402452480i,rss=402534400i,total_cache=475136i,total_writeback=0i,limit=8096448512i,container_id="37f9bc055227429ee9e0cbb5444c1af3c99746ccda1e17b532e3428f6b969c00",cache=475136i,inactive_anon=0i,mapped_file=65536i,pgfault=340388i,total_pgpgout=140181i,writeback=0i,usage=410337280i,usage_percent=5.062246037784721 1514759002000000000
docker_container_cpu,container_image=telegraf,com.docker.compose.oneoff=False,host=af95c0c993d7,container_name=step4_telegraf_1,com.docker.compose.config-hash=1f7ea37af395ca2db227212f76765d1970dfc55b618b26e39c63b977caa6e015,com.docker.compose.container-number=1,com.docker.compose.service=telegraf,cpu=cpu-total,container_version=1.5,com.docker.compose.project=step4,com.docker.compose.version=1.17.1,engine_host=moby usage_total=150416778i,usage_in_kernelmode=70000000i,throttling_periods=0i,throttling_throttled_periods=0i,usage_percent=0.1428634020618557,usage_in_usermode=80000000i,usage_system=94619910000000i,throttling_throttled_time=0i,container_id="af95c0c993d72f43ca2145af32d723d5ec92dbd387c330491e643286687b05b3" 1514759002000000000

docker_container_cpu,com.docker.compose.container-number=1,host=af95c0c993d7,container_version=1.5,com.docker.compose.project=step4,com.docker.compose.version=1.17.1,engine_host=moby,container_name=step4_telegraf_1,com.docker.compose.config-hash=1f7ea37af395ca2db227212f76765d1970dfc55b618b26e39c63b977caa6e015,cpu=cpu0,container_image=telegraf,com.docker.compose.oneoff=False,com.docker.compose.service=telegraf usage_total=3819059i,container_id="af95c0c993d72f43ca2145af32d723d5ec92dbd387c330491e643286687b05b3" 1514759002000000000

docker_container_cpu,com.docker.compose.oneoff=False,com.docker.compose.project=step4,host=af95c0c993d7,container_name=step4_telegraf_1,com.docker.compose.config-hash=1f7ea37af395ca2db227212f76765d1970dfc55b618b26e39c63b977caa6e015,com.docker.compose.service=telegraf,com.docker.compose.version=1.17.1,container_image=telegraf,container_version=1.5,cpu=cpu1,engine_host=moby,com.docker.compose.container-number=1 container_id="af95c0c993d72f43ca2145af32d723d5ec92dbd387c330491e643286687b05b3",usage_total=45071361i 1514759002000000000
...
...
```

Fine ! We have real data that mean something.

# Telegraf topic 

Let's describe the telegraf topic

```
$ docker-compose exec kafka-1 kafka-topics  --zookeeper zookeeper:2181 --describe --topic telegraf
Topic:telegraf	PartitionCount:1	ReplicationFactor:1	Configs:
	Topic: telegraf	Partition: 0	Leader: 3	Replicas: 3	Isr: 3
```

`PartitionCount:1` and 	`ReplicationFactor:1` ? Not a good configuration.

Let's change that  https://docs.confluent.io/current/kafka/post-deployment.html#tweaking-configs-dynamically
and https://docs.confluent.io/current/kafka/post-deployment.html#modifying-topics


```
$ docker-compose exec kafka-1 kafka-topics  --zookeeper zookeeper:2181 --alter --topic telegraf --partitions 10
WARNING: If partitions are increased for a topic that has a key, the partition logic or ordering of the messages will be affected
Adding partitions succeeded!
```

Fine let's do the same for the `replication-factor` right ?

Well, by reading the [increasing replication factor documentation](https://docs.confluent.io/current/kafka/post-deployment.html#increasing-replication-factor) it seems like it's more complicated than thought.

Please take the time to understand why it's not as simple as addition partition.

Do not forget to read the [scaling-the-cluster](https://docs.confluent.io/current/kafka/post-deployment.html#scaling-the-cluster) chapter.
