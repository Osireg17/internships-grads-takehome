package com.takehome.schedule.ScheduleWriters;

import com.takehome.schedule.Models.Entry;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WindowWriterTest {

    private List<Entry> entries;

    @BeforeEach
    void setUp() {
        entries = new ArrayList<>();
        entries.add(new Entry("alice", Instant.parse("2026-11-07T13:00:00Z"), Instant.parse("2026-11-09T13:00:00Z")));
        entries.add(new Entry("bob", Instant.parse("2026-11-13T13:00:00Z"), Instant.parse("2026-11-20T13:00:00Z")));
        entries.add(new Entry("charlie", Instant.parse("2026-11-20T13:00:00Z"), Instant.parse("2026-11-27T13:00:00Z")));
    }

    @Test
    void entryStickingOutAtStartIsCutAtFrom() {
        Instant from = Instant.parse("2026-11-08T14:00:00Z");
        Instant until = Instant.parse("2026-11-09T14:00:00Z");
        List<Entry> result = WindowWriter.truncate(entries, from, until);
        assertEquals(List.of(new Entry("alice", from, Instant.parse("2026-11-09T13:00:00Z"))), result);
    }

    @Test
    void entryInsideWindowIsUnchanged() {
        Instant from = Instant.parse("2026-11-13T13:00:00Z");
        Instant until = Instant.parse("2026-11-20T13:00:00Z");

        List<Entry> result = WindowWriter.truncate(entries, from, until);

        assertEquals(List.of(entries.get(1)), result);
    }

    @Test
    void entryStickingOutAtEndIsCutAtUntil() {
        Instant from = Instant.parse("2026-11-20T13:00:00Z");
        Instant until = Instant.parse("2026-11-25T13:00:00Z");

        List<Entry> result = WindowWriter.truncate(entries, from, until);

        assertEquals(List.of(new Entry("charlie", Instant.parse("2026-11-20T13:00:00Z"), until)), result);
    }

    @Test
    void entryCoveringWholeWindowIsCutAtBothEnds() {
        Instant from = Instant.parse("2026-11-24T10:00:00Z");
        Instant until = Instant.parse("2026-11-25T10:00:00Z");

        List<Entry> result = WindowWriter.truncate(entries, from, until);

        assertEquals(List.of(new Entry("charlie", from, until)), result);
    }

    @Test
    void entryEntirelyBeforeWindowIsRemoved() {

        Instant from = Instant.parse("2026-11-24T10:00:00Z");
        Instant until = Instant.parse("2026-11-25T10:00:00Z");

        List<Entry> result = WindowWriter.truncate(entries, from, until);

        assertEquals(List.of(new Entry("charlie", from, until)), result);
    }

    @Test
    void entryEntirelyAfterWindowIsRemoved() {
        Instant from = Instant.parse("2026-11-07T10:00:00Z");
        Instant until = Instant.parse("2026-11-08T10:00:00Z");

        List<Entry> result = WindowWriter.truncate(entries, from, until);

        assertEquals(List.of(new Entry("alice", Instant.parse("2026-11-07T13:00:00Z"), until)), result);
    }

    @Test
    void entryEndingExactlyAtFromIsRemoved() {
        Instant from = Instant.parse("2026-11-09T13:00:00Z");
        Instant until = Instant.parse("2026-11-10T13:00:00Z");

        List<Entry> result = WindowWriter.truncate(entries, from, until);

        assertEquals(List.of(), result);
    }

    @Test
    void entryStartingExactlyAtUntilIsRemoved() {
        Instant from = Instant.parse("2026-11-06T13:00:00Z");
        Instant until = Instant.parse("2026-11-07T13:00:00Z");

        List<Entry> result = WindowWriter.truncate(entries, from, until);

        assertEquals(List.of(), result);
    }

    @Test
    void emptyInputGivesEmptyOutput() {
        Instant from = Instant.parse("2026-11-06T13:00:00Z");
        Instant until = Instant.parse("2026-11-07T13:00:00Z");

        List<Entry> result = WindowWriter.truncate(new ArrayList<>(), from, until);

        assertEquals(List.of(), result);
    }

    @Test
    void preservesOrderAndDoesNotModifyInput() {
        Instant from = Instant.parse("2026-11-08T13:00:00Z");
        Instant until = Instant.parse("2026-11-21T13:00:00Z");
        List<Entry> originalEntries = List.copyOf(entries);

        List<Entry> result = WindowWriter.truncate(entries, from, until);

        assertEquals(List.of(
                new Entry("alice", from, Instant.parse("2026-11-09T13:00:00Z")),
                entries.get(1),
                new Entry("charlie", Instant.parse("2026-11-20T13:00:00Z"), until)
        ), result);

        assertEquals(originalEntries, entries);
    }
}
