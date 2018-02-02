# Objective 

Grafana


# Metrics -> Influxdb

The metrics captured by telegraf to shipped to kafka, we still need to push them to influxdb.

Let's do that with 

```yml
  kafka-to-influxdb:
    image: telegraf:1.5
    restart: unless-stopped
    volumes:
      - ./telegraf-kafka-to-influxdb.conf:/etc/telegraf/telegraf.conf:ro
    depends_on:
      - kafka-1
      - kafka-2
      - kafka-3
      - influxdb
```

The telegraf configuration is as simple as this

```
[agent]
interval = "5s"
flush_interval= "5s"

[[inputs.kafka_consumer]]
topics = ["telegraf"]
brokers = ["kafka-1:9092","kafka-2:9092","kafka-3:9092"]
consumer_group = "telegraf-kafka-to-influxdb"
offset = "oldest"

[[outputs.influxdb]]
urls = ["http://influxdb:8086"]
database = "telegraf"
```

# Grafana



