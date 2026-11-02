package com.takehome.schedule.Models;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EntryTest {

    private String user;
    private Instant start;
    private Instant end;

    @BeforeEach
    void setUp() {
        user = "testUser";
        start = Instant.ofEpochMilli(1000L);
        end = Instant.ofEpochMilli(2000L);
    }

    @Test
    void createsValidEntry() {
        Entry expected = new Entry(user, start, end);
        Entry actual = new Entry(user, start, end);

        assertEquals(expected, actual);
    }

    @Test
    void rejectsStartEqualToEnd() {
        Instant equalTime = start;

        assertThrows(IllegalArgumentException.class, () -> new Entry(user, equalTime, equalTime));
    }

    @Test
    void rejectsStartAfterEnd() {
        assertThrows(IllegalArgumentException.class, () -> new Entry(user, end, start));

    }

    @Test
    void rejectsMissingOrBlankUser() {
        assertThrows(IllegalArgumentException.class, () -> new Entry(null, start, end));
        assertThrows(IllegalArgumentException.class, () -> new Entry("", start, end));
        assertThrows(IllegalArgumentException.class, () -> new Entry("   ", start, end));
    }

    @Test
    void rejectsMissingStartOrEnd() {
        assertThrows(IllegalArgumentException.class, () -> new Entry(user, null, end));
        assertThrows(IllegalArgumentException.class, () -> new Entry(user, start, null));
    }
}
