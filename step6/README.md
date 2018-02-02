# Objective

1. Streams

# Docker

```
$ docker-compose build
$ docker-compose up -d
$ docker-compose ps
$ docker-compose exec kafka-1 kafka-topics \
    --zookeeper zookeeper:2181 \ 
    --list
$ docker-compose exec kafka-1 kafka-console-consumer \
    --bootstrap-server localhost:9092 \
    --topic telegraf-input-by-thread \
    --from-beginning
$ docker-compose exec kafka-1 kafka-console-consumer \
    --bootstrap-server localhost:9092 \
    --topic telegraf-10s-window-count \
    --property print.key=true \
    --value-deserializer org.apache.kafka.common.serialization.LongDeserializer \
    --from-beginning
```

# The full action ?

[![screencast](https://asciinema.org/a/jfXXzDMSrGNplXj3MLdPOGEAt.png)](https://asciinema.org/a/jfXXzDMSrGNplXj3MLdPOGEAt?autoplay=1)
