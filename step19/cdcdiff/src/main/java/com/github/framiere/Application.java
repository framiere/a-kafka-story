package com.github.framiere;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.*;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.processor.WallclockTimestampExtractor;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class Application {
    private static final List<String> TOPICS = Arrays.asList("dbserver1.mydb.Team", "dbserver1.mydb.Member", "dbserver1.mydb.Address");
    private final CdcChange cdcChange = new CdcChange();

    public static void main(String[] args) throws Exception {
        new Application().stream(args.length == 1 ? args[0] : "localhost:9092");
    }

    public void stream(String bootstrapServers) throws Exception {
        waitForTopics(bootstrapServers);

        StreamsBuilder builder = new StreamsBuilder();
        builder
                .stream(TOPICS, Consumed.with(Serdes.String(), Serdes.String()))
                .mapValues(cdcChange::toTelegraf)
                .filter((k, v) -> v != null && !v.isEmpty())
                .peek((k, v) -> System.out.println(v))
                .to("telegraf", Produced.with(Serdes.String(), Serdes.String()));

        Topology build = builder.build();

        System.out.println(build.describe());

        KafkaStreams kafkaStreams = new KafkaStreams(build, buildProducerProperties(bootstrapServers));
        kafkaStreams.cleanUp();
        kafkaStreams.start();
    }

    private void waitForTopics(String bootstrapServers) throws Exception {
        while (true) {
            TimeUnit.SECONDS.sleep(5);
            Properties properties = new Properties();
            properties.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
            AdminClient adminClient = AdminClient.create(properties);
            if (adminClient.listTopics().names().get().containsAll(TOPICS)) {
                return;
            }
            System.out.println("Waiting for data");
        }
    }

    private Properties buildProducerProperties(String bootstrapServers) {
        Properties properties = new Properties();
        properties.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(StreamsConfig.NUM_STREAM_THREADS_CONFIG, 3);
        properties.put(StreamsConfig.APPLICATION_ID_CONFIG, "cdc-change-stream");
        properties.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 5 * 1000);
        properties.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 0);
        properties.put(StreamsConfig.DEFAULT_TIMESTAMP_EXTRACTOR_CLASS_CONFIG, WallclockTimestampExtractor.class);
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ProducerConfig.BATCH_SIZE_CONFIG, "1");
        properties.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");
        return properties;
    }
}