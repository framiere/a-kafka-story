package com.github.framiere;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.concurrent.atomic.LongAdder;

public class CdcChange {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public String toTelegraf(String json) {
        return json == null ? null : toTelegraf(json.getBytes());
    }

    public String toTelegraf(byte[] json) {
        try {
            JsonNode payload = OBJECT_MAPPER.readTree(json);
            if (payload == null || !payload.has("op") || !payload.has("source") || !payload.has("before")) {
                System.err.println("Payload has not the required fields");
                return null;
            }
            String op = payload.get("op").asText();
            String table = payload.get("source").get("table").asText();
            JsonNode beforeNode = payload.get("before");
            JsonNode afterNode = payload.get("after");

            String line = "cdc,table=" + table + ",operation=";
            switch (op) {
                case "u":
                    return line + "update" + getId(afterNode) + toUpdate(beforeNode, afterNode) + ",found=1 " + getTimeInS(payload);
                case "d":
                    return line + "delete" + getId(beforeNode) + " found=1 " + getTimeInS(payload);
                default:
                    return line + "insert" + getId(afterNode) + " found=1 " + getTimeInS(payload);
            }
        } catch (IOException e) {
            return "";
        }
    }

    private String getTimeInS(JsonNode payload) {
        return payload.get("ts_ms").asText() + "000000";
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
                            if (!beforeNode.get(f).asText().equals(afterNode.get(f).asText())) {
                                line.append("," + f + "=updated");
                                nbChanges.increment();
                            }
                        }
                );
        return line + " nbChanges=" + nbChanges;
    }
}
