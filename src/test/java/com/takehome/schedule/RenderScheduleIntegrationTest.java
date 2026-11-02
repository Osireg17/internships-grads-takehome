package com.takehome.schedule;

import com.takehome.schedule.Models.Entry;

import com.takehome.schedule.ScheduleWriters.JsonWriter;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static com.takehome.schedule.TestSupport.writeFile;

class RenderScheduleIntegrationTest {

    private static final String SCHEDULE_JSON = """
            {
              "users": ["alice", "bob", "charlie"],
              "handover_start_at": "2026-11-06T17:00:00Z",
              "handover_interval_days": 7
            }
            """;

    private static final String OVERRIDES_JSON = """
            [
              {
                "user": "charlie",
                "start_at": "2026-11-09T17:00:00Z",
                "end_at": "2026-11-09T22:00:00Z"
              }
            ]
            """;

    @TempDir
    Path tempDir;

    private Path scheduleFile;
    private Path overridesFile;

    @BeforeEach
    void setUp() throws IOException {
        scheduleFile = writeFile(tempDir, "schedule.json", SCHEDULE_JSON);
        overridesFile = writeFile(tempDir, "overrides.json", OVERRIDES_JSON);
    }

    @Test
    void readmeExampleProducesExpectedOutput() {
        TestSupport.CommandResult actual = TestSupport.runCommand(
                "--schedule=" + scheduleFile,
                "--overrides=" + overridesFile,
                "--from=2026-11-06T17:00:00Z",
                "--until=2026-11-20T17:00:00Z"
        );

        List<Entry> expectedEntries = List.of(
                new Entry(
                        "alice",
                        Instant.parse("2026-11-06T17:00:00Z"),
                        Instant.parse("2026-11-09T17:00:00Z")
                ),
                new Entry(
                        "charlie",
                        Instant.parse("2026-11-09T17:00:00Z"),
                        Instant.parse("2026-11-09T22:00:00Z")
                ),
                new Entry(
                        "alice",
                        Instant.parse("2026-11-09T22:00:00Z"),
                        Instant.parse("2026-11-13T17:00:00Z")
                ),
                new Entry(
                        "bob",
                        Instant.parse("2026-11-13T17:00:00Z"),
                        Instant.parse("2026-11-20T17:00:00Z")
                )
        );

        TestSupport.CommandResult expected = new TestSupport.CommandResult(
                0,
                JsonWriter.writeEntries(expectedEntries) + System.lineSeparator(),
                ""
        );

        assertEquals(expected, actual);
    }

    @Test
    void windowCutsFirstAndLastEntries() throws IOException {
        TestSupport.CommandResult actual = TestSupport.runCommand(
                "--schedule=" + scheduleFile,
                "--overrides=" + overridesFile,
                "--from=2026-11-07T17:00:00Z",
                "--until=2026-11-19T17:00:00Z"
        );

        List<Entry> expectedEntries = List.of(
                new Entry(
                        "alice",
                        Instant.parse("2026-11-07T17:00:00Z"),
                        Instant.parse("2026-11-09T17:00:00Z")
                ),
                new Entry(
                        "charlie",
                        Instant.parse("2026-11-09T17:00:00Z"),
                        Instant.parse("2026-11-09T22:00:00Z")
                ),
                new Entry(
                        "alice",
                        Instant.parse("2026-11-09T22:00:00Z"),
                        Instant.parse("2026-11-13T17:00:00Z")
                ),
                new Entry(
                        "bob",
                        Instant.parse("2026-11-13T17:00:00Z"),
                        Instant.parse("2026-11-19T17:00:00Z")
                )
        );

        TestSupport.CommandResult expected = new TestSupport.CommandResult(
                0,
                JsonWriter.writeEntries(expectedEntries) + System.lineSeparator(),
                ""
        );

        assertEquals(expected, actual);
    }

    @Test
    void overrideOutsideWindowDoesNotAppear() throws IOException {
        String overridesJson = """
                [
                  {
                    "user": "charlie",
                    "start_at": "2026-11-21T17:00:00Z",
                    "end_at": "2026-11-21T22:00:00Z"
                  }
                ]
                """;

        Path overridesFile = writeFile(tempDir, "overrides.json", overridesJson);

        TestSupport.CommandResult actual = TestSupport.runCommand(
                "--schedule=" + scheduleFile,
                "--overrides=" + overridesFile,
                "--from=2026-11-06T17:00:00Z",
                "--until=2026-11-20T17:00:00Z"
        );

        List<Entry> expectedEntries = List.of(
                new Entry(
                        "alice",
                        Instant.parse("2026-11-06T17:00:00Z"),
                        Instant.parse("2026-11-13T17:00:00Z")
                ),
                new Entry(
                        "bob",
                        Instant.parse("2026-11-13T17:00:00Z"),
                        Instant.parse("2026-11-20T17:00:00Z")
                )
        );

        TestSupport.CommandResult expected = new TestSupport.CommandResult(
                0,
                JsonWriter.writeEntries(expectedEntries) + System.lineSeparator(),
                ""
        );

        assertEquals(expected, actual);
    }

    @Test
    void overrideStartingBeforeFromIsCut() throws IOException {
        String overridesJson = """
                [
                  {
                    "user": "charlie",
                    "start_at": "2026-11-05T17:00:00Z",
                    "end_at": "2026-11-07T17:00:00Z"
                  }
                ]
                """;

        Path overridesFile = writeFile(tempDir, "overrides.json", overridesJson);

        TestSupport.CommandResult actual = TestSupport.runCommand(
                "--schedule=" + scheduleFile,
                "--overrides=" + overridesFile,
                "--from=2026-11-06T17:00:00Z",
                "--until=2026-11-20T17:00:00Z"
        );

        List<Entry> expectedEntries = List.of(
                new Entry(
                        "charlie",
                        Instant.parse("2026-11-06T17:00:00Z"),
                        Instant.parse("2026-11-07T17:00:00Z")
                ),
                new Entry(
                        "alice",
                        Instant.parse("2026-11-07T17:00:00Z"),
                        Instant.parse("2026-11-13T17:00:00Z")
                ),
                new Entry(
                        "bob",
                        Instant.parse("2026-11-13T17:00:00Z"),
                        Instant.parse("2026-11-20T17:00:00Z")
                )
        );

        TestSupport.CommandResult expected = new TestSupport.CommandResult(
                0,
                JsonWriter.writeEntries(expectedEntries) + System.lineSeparator(),
                ""
        );

        assertEquals(expected, actual);
    }

    @Test
    void flagOrderDoesNotMatter() {
        TestSupport.CommandResult actual = TestSupport.runCommand(
                "--from=2026-11-06T17:00:00Z",
                "--until=2026-11-20T17:00:00Z",
                "--overrides=" + overridesFile,
                "--schedule=" + scheduleFile
        );

        TestSupport.CommandResult expected = TestSupport.runCommand(
                "--schedule=" + scheduleFile,
                "--overrides=" + overridesFile,
                "--from=2026-11-06T17:00:00Z",
                "--until=2026-11-20T17:00:00Z"
        );

        assertEquals(expected, actual);
    }

    static Stream<Arguments> invalidFlagCases() {
        return Stream.of(
                Arguments.of("missing flag --until", new String[]{
            "--schedule=s.json", "--overrides=o.json", "--from=2026-11-06T17:00:00Z"}),
                Arguments.of("repeated flag schedule", new String[]{
            "--schedule=s.json", "--schedule=s.json", "--overrides=o.json",
            "--from=2026-11-06T17:00:00Z", "--until=2026-11-20T17:00:00Z"}),
                Arguments.of("unknown flag verbose", new String[]{
            "--schedule=s.json", "--overrides=o.json", "--from=2026-11-06T17:00:00Z",
            "--until=2026-11-20T17:00:00Z", "--verbose=true"}),
                Arguments.of("invalid flag format --schedule", new String[]{
            "--schedule", "s.json", "--overrides=o.json",
            "--from=2026-11-06T17:00:00Z", "--until=2026-11-20T17:00:00Z"})
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidFlagCases")
    void invalidFlagsFail(String caseName, String[] args) {
        TestSupport.CommandResult actual = TestSupport.runCommand(args);

        assertFailure(actual);
        assertEquals("render-schedule: " + caseName + System.lineSeparator(), actual.stderr());
    }

    @Test
    void unparsableTimestampFlagFails() {
        TestSupport.CommandResult actual = TestSupport.runCommand(
                "--schedule=" + scheduleFile,
                "--overrides=" + overridesFile,
                "--from=not-a-timestamp",
                "--until=2026-11-20T17:00:00Z"
        );

        assertFailure(actual);
        assertTrue(actual.stderr().contains("not-a-timestamp"));
    }

    @Test
    void fromNotBeforeUntilFails() {
        TestSupport.CommandResult actual = TestSupport.runCommand(
                "--schedule=" + scheduleFile,
                "--overrides=" + overridesFile,
                "--from=2026-11-20T17:00:00Z",
                "--until=2026-11-06T17:00:00Z"
        );

        assertFailure(actual);
        assertTrue(actual.stderr().contains("Window is empty or backwards"));
    }

    @Test
    void fromBeforeFirstHandoverFails() {
        TestSupport.CommandResult actual = TestSupport.runCommand(
                "--schedule=" + scheduleFile,
                "--overrides=" + overridesFile,
                "--from=2026-11-05T17:00:00Z",
                "--until=2026-11-20T17:00:00Z"
        );

        assertFailure(actual);
        assertTrue(actual.stderr().contains("first handover"));
    }

    @Test
    void inputFileProblemFails() {
        TestSupport.CommandResult actual = TestSupport.runCommand(
                "--schedule=" + tempDir.resolve("missing-schedule.json"),
                "--overrides=" + overridesFile,
                "--from=2026-11-06T17:00:00Z",
                "--until=2026-11-20T17:00:00Z"
        );

        assertFailure(actual);
        assertTrue(actual.stderr().contains("Cannot read file"));
    }

    private static void assertFailure(TestSupport.CommandResult result) {
        assertEquals(1, result.exitCode());
        assertEquals("", result.stdout());
        assertTrue(result.stderr().startsWith("render-schedule: "));
        assertTrue(result.stderr().endsWith(System.lineSeparator()));
    }
}
