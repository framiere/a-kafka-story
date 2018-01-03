package com.github.framiere;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class SimpleProducer {
    public static void main(String[] args) throws InterruptedException {
        Properties props = new Properties();
        props.put("bootstrap.servers", "kafka-1:9092,kafka-2:9092,kafka-3:3902");
        props.put("acks", "all");
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        String topic = "sample";
        System.out.println("Sending data to " + topic);
        try (Producer<String, String> producer = new KafkaProducer<>(props)) {
            int i = 0;
            while (true) {
                ProducerRecord<String, String> record = new ProducerRecord<>(topic, "key " + i, "Value " + i);
                System.out.println("Sending " + record.key() + " " + record.value());
                producer.send(record);
                i++;
                TimeUnit.SECONDS.sleep(1);
            }
        }
    }
}
