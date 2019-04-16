package com.github.framiere;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.*;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.processor.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * ./confluent destroy
 * ./confluent start
 * ./kafka-topics --zookeeper localhost:2181 --create --topic telegraf --partitions 3 --replication-factor 1
 * run application java -jar simplestream*.jar
 * seq 10000 | ./kafka-console-producer --broker-list localhost:9092 --topic telegraf
 * or type yourself words : ./kafka-console-producer --broker-list localhost:9092 --topic telegraf
 * ./kafka-topics --zookeeper localhost:2181 --list
 * ./kafka-console-consumer --bootstrap-server localhost:9092 --topic telegraf-input-by-thread --from-beginning
 * ./kafka-console-consumer --bootstrap-server localhost:9092 --topic telegraf-10s-window-count --property print.key=true --value-deserializer org.apache.kafka.common.serialization.LongDeserializer --from-beginning
 */
public class SimpleStream {

    public void stream(String bootstrapServers) {
        Properties properties = new Properties();
        properties.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(StreamsConfig.NUM_STREAM_THREADS_CONFIG, 3);
        properties.put(StreamsConfig.APPLICATION_ID_CONFIG, "simple-stream");
        properties.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 5 * 1000);
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 0);
        properties.put(StreamsConfig.DEFAULT_TIMESTAMP_EXTRACTOR_CLASS_CONFIG, WallclockTimestampExtractor.class);

        StreamsBuilder builder = new StreamsBuilder();
        KStream<String, String> input = builder.stream("telegraf", Consumed.with(Serdes.String(), Serdes.String()));

        // map each value and add the thread that processed it
        input
                .mapValues(v -> Thread.currentThread().getName() + " " + v)
                .to("telegraf-input-by-thread", Produced.with(Serdes.String(), Serdes.String()));

        // grab the first word as a key, and make a global count out of it, and push the changes to telegraf-global-count
        input
                .map((key, value) -> new KeyValue<>(value.split("[, ]")[0], 0L))
                .groupByKey(Grouped.with(Serdes.String(), Serdes.Long()))
                .count()
                .toStream()
                .to("telegraf-global-count", Produced.with(Serdes.String(), Serdes.Long()));

        // check with ./kafka-console-consumer --bootstrap-server localhost:9092 --topic telegraf-10s-window-count --property print.key=true --value-deserializer org.apache.kafka.common.serialization.LongDeserializer
        // count the first word on a sliding window, and push the changes to telegraf-10s-window-count
        input
                .map((key, value) -> new KeyValue<>(value.split("[, ]")[0], 1L))
                .groupByKey(Grouped.with(Serdes.String(), Serdes.Long()))
                .windowedBy(TimeWindows.of(Duration.ofSeconds(10)))
                .count()
                .toStream((windowedRegion, count) -> windowedRegion.toString())
                .to("telegraf-10s-window-count", Produced.with(Serdes.String(), Serdes.Long()));

        // you can branch to multiple destinations, please note that the branching happens on first-match:
        // A record in the original stream is assigned to the corresponding result
        // stream for the first predicate that evaluates to true, and is assigned to this stream only.
        // A record will be dropped if none of the predicates evaluate to true.
        KStream<String, String>[] branch = input.branch(
                (key, value) -> (value.length() % 3) == 0,
                (key, value) -> (value.length() % 5) == 0,
                (key, value) -> true);

        branch[0].to("telegraf-length-divisible-by-3", Produced.with(Serdes.String(), Serdes.String()));
        branch[1].to("telegraf-length-divisible-by-5", Produced.with(Serdes.String(), Serdes.String()));
        branch[2].to("telegraf-length-divisible-by-neither-3-nor-5", Produced.with(Serdes.String(), Serdes.String()));

        // You can also use the low level APIs if you need to handle complex use cases,
        input
                .process(() -> new AbstractProcessor<String, String>() {
                    private final List<String> batch = new ArrayList<>();

                    @Override
                    public void init(ProcessorContext context) {
                        super.init(context);
                        // Punctuator function will be called on the same thread
                        context().schedule(TimeUnit.SECONDS.toMillis(10), PunctuationType.WALL_CLOCK_TIME, this::flush);
                    }

                    private void flush(long timestamp) {
                        if (!batch.isEmpty()) {
                            // sending to an external system ?
                            System.out.println(timestamp + " " + Thread.currentThread().getName() + " Flushing batch of " + batch.size());
                            batch.clear();
                        }
                    }

                    @Override
                    public void process(String key, String value) {
                        batch.add(value);
                        context().forward(key, value);
                    }
                });


        Topology build = builder.build();

        System.out.println(build.describe());

        KafkaStreams kafkaStreams = new KafkaStreams(build, properties);
        kafkaStreams.cleanUp();
        kafkaStreams.start();
    }

    public static void main(String[] args) {
        String bootstrapServers = args.length == 1 ? args[0] : "localhost:9092";
        System.out.println(bootstrapServers);
        new SimpleStream().stream(bootstrapServers);
    }
}
