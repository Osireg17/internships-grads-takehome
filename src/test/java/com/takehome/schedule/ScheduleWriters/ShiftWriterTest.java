package com.takehome.schedule.ScheduleWriters;

import com.takehome.schedule.Models.Entry;
import com.takehome.schedule.Models.Schedule;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ShiftWriterTest {

    private static final List<String> USERS = List.of("alice", "bob", "charlie");
    private static final Instant HANDOVER_START = Instant.parse("2026-11-06T17:00:00Z");
    private static final int INTERVAL_DAYS = 7;

    private Schedule schedule;

    @BeforeEach
    void setUp() {
        schedule = new Schedule(USERS, HANDOVER_START, INTERVAL_DAYS);
    }

    @Test
    void windowInsideOneShift() {
        List<Entry> actual = ShiftWriter.generate(
                schedule, Instant.parse("2026-11-06T18:00:00Z"), Instant.parse("2026-11-06T23:59:59Z"));

        assertEquals(List.of(shift("alice", 0)), actual);
    }

    @Test
    void windowSpanningSeveralShifts() {
        List<Entry> actual = ShiftWriter.generate(
                schedule, Instant.parse("2026-11-06T18:00:00Z"), Instant.parse("2026-11-20T17:00:01Z"));

        assertEquals(List.of(shift("alice", 0), shift("bob", 1), shift("charlie", 2)), actual);
    }

    @Test
    void usersWrapAroundAfterTheLast() {
        List<Entry> actual = ShiftWriter.generate(
                schedule, Instant.parse("2026-11-06T18:00:00Z"), Instant.parse("2026-12-04T17:00:01Z"));

        assertEquals(List.of(
                shift("alice", 0), shift("bob", 1), shift("charlie", 2),
                shift("alice", 3), shift("bob", 4)
        ), actual);
    }

    @Test
    void fromMidShiftIncludesThatShiftWithNaturalStart() {
        List<Entry> actual = ShiftWriter.generate(
                schedule, Instant.parse("2026-11-24T10:00:00Z"), Instant.parse("2026-12-04T16:59:59Z"));

        assertEquals(List.of(shift("charlie", 2), shift("alice", 3)), actual);
    }

    @Test
    void fromExactlyOnHandoverExcludesPreviousShift() {
        List<Entry> actual = ShiftWriter.generate(
                schedule, Instant.parse("2026-11-20T17:00:00Z"), Instant.parse("2026-12-04T16:59:59Z"));

        assertEquals(List.of(shift("charlie", 2), shift("alice", 3)), actual);
    }

    @Test
    void untilExactlyOnHandoverExcludesNextShift() {
        List<Entry> actual = ShiftWriter.generate(
                schedule, Instant.parse("2026-11-06T18:00:00Z"), Instant.parse("2026-11-20T17:00:00Z"));

        assertEquals(List.of(shift("alice", 0), shift("bob", 1)), actual);
    }

    @Test
    void fromEqualToFirstHandoverIsAllowed() {
        List<Entry> actual = ShiftWriter.generate(
                schedule, HANDOVER_START, Instant.parse("2026-11-20T16:59:59Z"));

        assertEquals(List.of(shift("alice", 0), shift("bob", 1)), actual);
    }

    @Test
    void singleUserScheduleIsNotMerged() {
        Schedule singleUserSchedule = new Schedule(List.of("alice"), HANDOVER_START, INTERVAL_DAYS);
        List<Entry> actual = ShiftWriter.generate(
                singleUserSchedule, HANDOVER_START, Instant.parse("2026-11-20T17:00:00Z"));

        assertEquals(List.of(shift("alice", 0), shift("alice", 1)), actual);
    }

    @Test
    void rejectsFromEqualToUntil() {
        assertThrows(IllegalArgumentException.class,
                () -> ShiftWriter.generate(schedule, HANDOVER_START, HANDOVER_START));
    }

    @Test
    void rejectsFromAfterUntil() {
        assertThrows(IllegalArgumentException.class,
                () -> ShiftWriter.generate(schedule, HANDOVER_START.plusSeconds(1), HANDOVER_START));
    }

    @Test
    void rejectsFromBeforeFirstHandover() {
        assertThrows(IllegalArgumentException.class,
                () -> ShiftWriter.generate(schedule, HANDOVER_START.minusSeconds(1), HANDOVER_START.plusSeconds(1)));
    }

    private static Entry shift(String user, int index) {
        Instant start = HANDOVER_START.plusSeconds(index * INTERVAL_DAYS * 24L * 60L * 60L);
        return new Entry(user, start, start.plusSeconds(INTERVAL_DAYS * 24L * 60L * 60L));
    }
}
