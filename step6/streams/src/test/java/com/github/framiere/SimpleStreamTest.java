package com.github.framiere;

import com.github.charithe.kafka.EphemeralKafkaBroker;
import com.github.charithe.kafka.KafkaJunitRule;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class SimpleStreamTest {
    private static final long DEFAULT_TIMEOUT = 30 * 1000L;

    @ClassRule
    public static KafkaJunitRule kafkaRule = new KafkaJunitRule(EphemeralKafkaBroker.create());

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void filterGroupAndReduce() throws IOException, InterruptedException {
        String bootstrapServers = kafkaRule.helper().producerConfig().getProperty(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG);

        String inputTopic = "inputTopic";
        String outputTopic = "outputTopic";

            System.out.println("Step 1: Create topology");
            KafkaStreams kafkaStreams = buildTopology(inputTopic, outputTopic, streamConfig(bootstrapServers));
        System.out.println("Step 2: Start topology");
        kafkaStreams.start();

        System.out.println("Step 3: Produce some input data to the input topic");
        sendInputValues(bootstrapServers, inputTopic, Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));

        System.out.println("Step 4: Verify the application's output data");
        List<Integer> actualValues = readOutputValues(bootstrapServers, outputTopic, 1);
        assertThat(actualValues)
                .hasSize(1)
                .containsExactly(30);
    }

    private KafkaStreams buildTopology(String inputTopic, String outputTopic, Properties properties) {
        StreamsBuilder builder = new StreamsBuilder();
        builder.<Integer, Integer>stream(inputTopic)
                .filter((k, v) -> v % 2 == 0)
                .selectKey((k, v) -> 1)
                .groupByKey()
                .reduce((v1, v2) -> v1 + v2)
                .toStream()
                .to(outputTopic);
        return new KafkaStreams(builder.build(), properties);
    }

    private <T> List<T> readOutputValues(String bootstrapServers, String topic, int nbExpectedValues) throws InterruptedException {
        Properties consumerConfig = new Properties();
        consumerConfig.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        consumerConfig.put(ConsumerConfig.GROUP_ID_CONFIG, String.valueOf(new Random().nextLong()));
        consumerConfig.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerConfig.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, IntegerDeserializer.class);
        consumerConfig.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, IntegerDeserializer.class);
        return waitUntilMinValuesRecordsReceived(consumerConfig, topic, nbExpectedValues);
    }

    private void sendInputValues(String bootstrapServers, String inputTopic, List<Integer> inputValues) {
        Properties producerConfig = new Properties();
        producerConfig.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        producerConfig.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, IntegerSerializer.class);
        producerConfig.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, IntegerSerializer.class);


        Producer<Integer, Integer> producer = new KafkaProducer<>(producerConfig);
        for (Integer inputValue : inputValues) {
            producer.send(new ProducerRecord<>(inputTopic, inputValue));
        }
        producer.flush();
    }

    private Properties streamConfig(String bootstrapServers) throws IOException {
        Properties properties = new Properties();
        properties.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(StreamsConfig.STATE_DIR_CONFIG, temporaryFolder.newFolder().getAbsolutePath());
        properties.put(StreamsConfig.APPLICATION_ID_CONFIG, String.valueOf(new Random().nextLong()));
        properties.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.Integer().getClass().getName());
        properties.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.Integer().getClass().getName());
        properties.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 10 * 1000);
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return properties;
    }


    public static <V> List<V> waitUntilMinValuesRecordsReceived(Properties consumerConfig,
                                                                String topic,
                                                                int expectedNumRecords) throws InterruptedException {

        return waitUntilMinValuesRecordsReceived(consumerConfig, topic, expectedNumRecords, DEFAULT_TIMEOUT);
    }

    public static <V> List<V> waitUntilMinValuesRecordsReceived(Properties consumerConfig,
                                                                String topic,
                                                                int expectedNumRecords,
                                                                long waitTime) throws InterruptedException {
        List<V> accumData = new ArrayList<>();
        long startTime = System.currentTimeMillis();
        while (true) {
            List<V> readData = readValues(topic, consumerConfig, expectedNumRecords);
            accumData.addAll(readData);
            if (accumData.size() >= expectedNumRecords) {
                return accumData;
            }
            if (System.currentTimeMillis() > startTime + waitTime) {
                throw new AssertionError("Expected " + expectedNumRecords +
                        " but received only " + accumData.size() +
                        " records before timeout " + waitTime + " ms");
            }
            Thread.sleep(Math.min(waitTime, 100L));
        }
    }

    public static <K, V> List<V> readValues(String topic, Properties consumerConfig, int maxMessages) {
        List<KeyValue<K, V>> kvs = readKeyValues(topic, consumerConfig, maxMessages);
        return kvs.stream().map(kv -> kv.value).collect(Collectors.toList());
    }

    public static <K, V> List<KeyValue<K, V>> readKeyValues(String topic, Properties consumerConfig, int maxMessages) {
        KafkaConsumer<K, V> consumer = new KafkaConsumer<>(consumerConfig);
        consumer.subscribe(Collections.singletonList(topic));
        int pollIntervalMs = 100;
        int maxTotalPollTimeMs = 2000;
        int totalPollTimeMs = 0;
        List<KeyValue<K, V>> consumedValues = new ArrayList<>();
        while (totalPollTimeMs < maxTotalPollTimeMs && continueConsuming(consumedValues.size(), maxMessages)) {
            totalPollTimeMs += pollIntervalMs;
            ConsumerRecords<K, V> records = consumer.poll(pollIntervalMs);
            for (ConsumerRecord<K, V> record : records) {
                consumedValues.add(new KeyValue<>(record.key(), record.value()));
            }
        }
        consumer.close();
        return consumedValues;
    }

    private static boolean continueConsuming(int messagesConsumed, int maxMessages) {
        return maxMessages <= 0 || messagesConsumed < maxMessages;
    }
}