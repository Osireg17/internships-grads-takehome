package com.takehome.schedule.Models;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ScheduleTest {

    private List<String> users;
    private Instant handoverStartAt;
    private int handoverIntervalDays;

    @BeforeEach
    void setUp() {
        users = new ArrayList<>(List.of("alice", "bob", "charlie"));
        handoverStartAt = Instant.parse("2026-11-06T17:00:00Z");
        handoverIntervalDays = 7;
    }

    @Test
    void createsValidSchedule() {
        Schedule expected = new Schedule(users, handoverStartAt, handoverIntervalDays);
        Schedule actual = new Schedule(users, handoverStartAt, handoverIntervalDays);

        assertEquals(expected, actual);
    }

    @Test
    void rejectsNoUsers() {
        assertThrows(IllegalArgumentException.class,
                () -> new Schedule(List.of(), handoverStartAt, handoverIntervalDays));
    }

    @Test
    void rejectsMissingUsers() {
        assertThrows(IllegalArgumentException.class,
                () -> new Schedule(null, handoverStartAt, handoverIntervalDays));
    }

    @Test
    void rejectsBlankUserName() {
        assertThrows(IllegalArgumentException.class,
                () -> new Schedule(List.of("alice", " "), handoverStartAt, handoverIntervalDays));
    }

    @Test
    void rejectsMissingUserName() {
        users.add(null);

        assertThrows(IllegalArgumentException.class,
                () -> new Schedule(users, handoverStartAt, handoverIntervalDays));
    }

    @Test
    void rejectsZeroInterval() {
        assertThrows(IllegalArgumentException.class,
                () -> new Schedule(users, handoverStartAt, 0));
    }

    @Test
    void rejectsNegativeInterval() {
        assertThrows(IllegalArgumentException.class,
                () -> new Schedule(users, handoverStartAt, -1));
    }

    @Test
    void rejectsMissingHandoverStart() {
        assertThrows(IllegalArgumentException.class,
                () -> new Schedule(users, null, handoverIntervalDays));
    }

    @Test
    void copiesUsersPassedToConstructor() {
        Schedule schedule = new Schedule(users, handoverStartAt, handoverIntervalDays);

        users.set(0, "dave");

        assertEquals(List.of("alice", "bob", "charlie"), schedule.users());
    }

    @Test
    void exposesAnUnmodifiableUsersList() {
        Schedule schedule = new Schedule(users, handoverStartAt, handoverIntervalDays);

        assertThrows(UnsupportedOperationException.class,
                () -> schedule.users().add("dave"));
    }
}
