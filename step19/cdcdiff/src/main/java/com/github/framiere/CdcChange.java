package com.github.framiere;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.concurrent.atomic.LongAdder;

public class CdcChange {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public String toTelegraf(String json) {
        return toTelegraf(json.getBytes());
    }

    public String toTelegraf(byte[] json) {
        try {
            JsonNode payload = OBJECT_MAPPER.readTree(json).get("payload");
            if (payload == null || payload.has("op") == false || payload.has("source") == false || payload.has("before") == false) {
                return null;
            }
            String op = payload.get("op").asText();
            String table = payload.get("source").get("table").asText();
            JsonNode beforeNode = payload.get("before");
            JsonNode afterNode = payload.get("after");

            String line = "cdc,table=" + table + ",operation=";
            switch (op) {
                case "u":
                    return line + "update" + getId(afterNode) + toUpdate(beforeNode, afterNode);
                case "d":
                    return line + "delete" + getId(beforeNode);
                default:
                    return line + "create" + getId(afterNode);
            }
        } catch (IOException e) {
            return "";
        }
    }

    private String getId(JsonNode afterNode) {
        String id = "";
        if (afterNode.has("id")) {
            id = ",id=" + afterNode.get("id").asText();
        }
        return id;
    }

    private String toUpdate(JsonNode beforeNode, JsonNode afterNode) {
        StringBuilder line = new StringBuilder();
        LongAdder nbChanges = new LongAdder();
        beforeNode.fieldNames()
                .forEachRemaining(f -> {
                            if (beforeNode.get(f).asText().equals(afterNode.get(f).asText())) {
                                line.append("," + f + "=true");
                                nbChanges.increment();
                            }
                        }
                );
        return line + " nbChanges=" + nbChanges;
    }
}
