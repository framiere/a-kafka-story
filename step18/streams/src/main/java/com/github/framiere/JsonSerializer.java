package com.github.framiere;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Serializer;

import java.util.Map;

public class JsonSerializer implements Serializer<Object> {
    private final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {

    }

    @Override
    public byte[] serialize(String topic, Object data) {
        try {
            return OBJECT_MAPPER.writeValueAsString(data).getBytes();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() {

    }
}