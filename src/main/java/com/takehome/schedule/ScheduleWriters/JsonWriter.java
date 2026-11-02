package com.takehome.schedule.ScheduleWriters;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.core.util.Separators;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.takehome.schedule.Models.Entry;
import com.takehome.schedule.Models.Schedule;

public final class JsonWriter {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final DefaultPrettyPrinter PRINTER = new DefaultPrettyPrinter(
            Separators.createDefaultInstance().withObjectFieldValueSpacing(Separators.Spacing.AFTER))
            .withArrayIndenter(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE);

    private JsonWriter() {
    }

    public static Schedule readSchedule(Path file) {
        JsonNode schedule = readJson(file);
        if (schedule == null || !schedule.isObject()) {
            throw new IllegalArgumentException("JSON is not a single object in file: " + file);
        }

        JsonNode usersNode = schedule.get("users");
        if (usersNode == null || !usersNode.isArray()) {
            throw new IllegalArgumentException("Missing or invalid 'users' field in file: " + file);
        }

        List<String> users = new ArrayList<>();
        for (JsonNode userNode : usersNode) {
            if (!userNode.isTextual()) {
                throw new IllegalArgumentException("Invalid user in file: " + file);
            }
            users.add(userNode.textValue());
        }

        Instant handoverStartAt = readInstant(schedule, "handover_start_at", file);
        JsonNode intervalNode = schedule.get("handover_interval_days");
        if (intervalNode == null || !intervalNode.isInt()) {
            throw new IllegalArgumentException("Missing or invalid 'handover_interval_days' field in file: " + file);
        }

        return new Schedule(users, handoverStartAt, intervalNode.intValue());
    }

    public static List<Entry> readOverrides(Path file) {
        JsonNode overridesNode = readJson(file);
        if (overridesNode == null || !overridesNode.isArray()) {
            throw new IllegalArgumentException("JSON is not an array in file: " + file);
        }

        List<Entry> overrides = new ArrayList<>();
        for (JsonNode overrideNode : overridesNode) {
            if (!overrideNode.isObject()) {
                throw new IllegalArgumentException("Override is not an object in file: " + file);
            }

            JsonNode userNode = overrideNode.get("user");
            if (userNode == null || !userNode.isTextual()) {
                throw new IllegalArgumentException("Missing or invalid 'user' field in file: " + file);
            }

            overrides.add(new Entry(
                    userNode.textValue(),
                    readInstant(overrideNode, "start_at", file),
                    readInstant(overrideNode, "end_at", file)
            ));
        }

        return List.copyOf(overrides);
    }

    public static String writeEntries(List<Entry> entries) {
        if (entries.isEmpty()) {
            return "[]";
        }

        ArrayNode entriesNode = MAPPER.createArrayNode();
        for (Entry entry : entries) {
            ObjectNode entryNode = entriesNode.addObject();
            entryNode.put("user", entry.user());
            entryNode.put("start_at", entry.start().toString());
            entryNode.put("end_at", entry.end().toString());
        }

        try {
            return MAPPER.writer(PRINTER).writeValueAsString(entriesNode);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not write schedule entries", exception);
        }
    }

    private static JsonNode readJson(Path file) {
        String contents;
        try {
            contents = Files.readString(file);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Cannot read file: " + file, exception);
        }

        try {
            return MAPPER.readTree(contents);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Invalid JSON in file: " + file, exception);
        }
    }

    private static Instant readInstant(JsonNode objectNode, String fieldName, Path file) {
        JsonNode timestampNode = objectNode.get(fieldName);
        if (timestampNode == null || !timestampNode.isTextual()) {
            throw new IllegalArgumentException("Missing or invalid '" + fieldName + "' field in file: " + file);
        }

        try {
            return Instant.parse(timestampNode.textValue());
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("Invalid '" + fieldName + "' timestamp in file: " + file, exception);
        }
    }
}
