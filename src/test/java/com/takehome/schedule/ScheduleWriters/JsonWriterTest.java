package com.takehome.schedule.ScheduleWriters;

import com.takehome.schedule.Models.Entry;
import com.takehome.schedule.Models.Schedule;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JsonWriterTest {

    private static final String SCHEDULE_JSON = """
            {
              "users": ["alice", "bob", "carol"],
              "handover_start_at": "2023-01-01T00:00:00Z",
              "handover_interval_days": 7
            }
            """;

    private static final String OVERRIDES_JSON = """
            [
              {
                "user": "dave",
                "start_at": "2023-01-02T00:00:00Z",
                "end_at": "2023-01-03T00:00:00Z"
              },
              {
                "user": "eve",
                "start_at": "2023-01-04T00:00:00Z",
                "end_at": "2023-01-05T00:00:00Z"
              }
            ]
            """;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        writeFile("schedule.json", SCHEDULE_JSON);
        writeFile("overrides.json", OVERRIDES_JSON);
    }

    @Test
    void readsValidSchedule() {
        Path scheduleFile = tempDir.resolve("schedule.json");
        Schedule expectedSchedule = new Schedule(
                List.of("alice", "bob", "carol"),
                Instant.parse("2023-01-01T00:00:00Z"),
                7
        );

        Schedule actualSchedule = JsonWriter.readSchedule(scheduleFile);

        assertEquals(expectedSchedule, actualSchedule);
    }

    @Test
    void scheduleFileMissing() {
        Path missingFile = tempDir.resolve("missing_schedule.json");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> JsonWriter.readSchedule(missingFile));

        assertEquals("Cannot read file: " + missingFile, exception.getMessage());
    }

    @Test
    void scheduleNotValidJson() throws IOException {
        Path invalidJsonFile = writeFile("invalid_schedule.json", "{ invalid json }");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> JsonWriter.readSchedule(invalidJsonFile));

        assertEquals("Invalid JSON in file: " + invalidJsonFile, exception.getMessage());
    }

    @Test
    void scheduleNotAnObject() throws IOException {
        Path arrayJsonFile = writeFile("array_schedule.json", "[1, 2, 3]");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> JsonWriter.readSchedule(arrayJsonFile));

        assertEquals("JSON is not a single object in file: " + arrayJsonFile, exception.getMessage());
    }

    @Test
    void scheduleMissingRequiredField() throws IOException {
        Path missingFieldFile = writeFile("missing_field_schedule.json", """
                {
                  "users": ["alice", "bob", "carol"],
                  "handover_start_at": "2023-01-01T00:00:00Z"
                }
                """);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> JsonWriter.readSchedule(missingFieldFile));

        assertEquals("Missing or invalid 'handover_interval_days' field in file: " + missingFieldFile, exception.getMessage());
    }

    @Test
    void scheduleUnparsableTimestamp() throws IOException {
        Path unparsableTimestampFile = writeFile("unparsable_timestamp_schedule.json", """
                {
                  "users": ["alice", "bob", "carol"],
                  "handover_start_at": "not-a-timestamp",
                  "handover_interval_days": 7
                }
                """);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> JsonWriter.readSchedule(unparsableTimestampFile));

        assertEquals("Invalid 'handover_start_at' timestamp in file: " + unparsableTimestampFile, exception.getMessage());
    }

    @Test
    void readsValidOverrides() {
        List<Entry> expectedOverrides = List.of(
                new Entry("dave", Instant.parse("2023-01-02T00:00:00Z"), Instant.parse("2023-01-03T00:00:00Z")),
                new Entry("eve", Instant.parse("2023-01-04T00:00:00Z"), Instant.parse("2023-01-05T00:00:00Z"))
        );

        assertEquals(expectedOverrides, JsonWriter.readOverrides(tempDir.resolve("overrides.json")));
    }

    @Test
    void emptyOverridesArrayGivesEmptyList() throws IOException {
        Path emptyOverridesFile = writeFile("empty_overrides.json", "[]");

        assertEquals(List.of(), JsonWriter.readOverrides(emptyOverridesFile));
    }

    @Test
    void overridesFileMissing() {
        Path missingFile = tempDir.resolve("missing_overrides.json");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> JsonWriter.readOverrides(missingFile));

        assertEquals("Cannot read file: " + missingFile, exception.getMessage());
    }

    @Test
    void overridesNotAnArray() throws IOException {
        Path objectOverridesFile = writeFile("object_overrides.json", "{}");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> JsonWriter.readOverrides(objectOverridesFile));

        assertEquals("JSON is not an array in file: " + objectOverridesFile, exception.getMessage());
    }

    @Test
    void overrideWithMissingFieldOrBadTimestamp() throws IOException {
        Path missingFieldFile = writeFile("missing_field_override.json", """
                [{"user": "alice", "start_at": "2023-01-02T00:00:00Z"}]
                """);
        Path badTimestampFile = writeFile("bad_timestamp_override.json", """
                [{"user": "alice", "start_at": "not-a-timestamp", "end_at": "2023-01-03T00:00:00Z"}]
                """);

        assertEquals("Missing or invalid 'end_at' field in file: " + missingFieldFile,
                assertThrows(IllegalArgumentException.class, () -> JsonWriter.readOverrides(missingFieldFile)).getMessage());
        assertEquals("Invalid 'start_at' timestamp in file: " + badTimestampFile,
                assertThrows(IllegalArgumentException.class, () -> JsonWriter.readOverrides(badTimestampFile)).getMessage());
    }

    @Test
    void writesEntriesInReadmeShape() throws IOException {
        String output = JsonWriter.writeEntries(List.of(
                new Entry("alice", Instant.parse("2023-01-01T00:00:00Z"), Instant.parse("2023-01-02T00:00:00Z")),
                new Entry("bob", Instant.parse("2023-01-02T00:00:00Z"), Instant.parse("2023-01-03T00:00:00Z"))
        ));

        JsonNode expected = new ObjectMapper().readTree("""
                [
                  {"user":"alice","start_at":"2023-01-01T00:00:00Z","end_at":"2023-01-02T00:00:00Z"},
                  {"user":"bob","start_at":"2023-01-02T00:00:00Z","end_at":"2023-01-03T00:00:00Z"}
                ]
                """);

        assertEquals(expected, new ObjectMapper().readTree(output));
        assertEquals(true, output.contains("\n"));
    }

    @Test
    void writesEmptyListAsEmptyArray() {
        assertEquals("[]", JsonWriter.writeEntries(List.of()));
    }

    private Path writeFile(String filename, String contents) throws IOException {
        Path file = tempDir.resolve(filename);
        Files.writeString(file, contents);
        return file;
    }
}
