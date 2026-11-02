package com.takehome.schedule.Models;

import java.time.Instant;

public record Entry(String user, Instant start, Instant end) {

    public Entry {
        if (user == null || user.isBlank()) {
            throw new IllegalArgumentException("User cannot be null or blank");
        }
        if (start == null || end == null) {
            throw new IllegalArgumentException("Start and end cannot be null");
        }
        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("Start must be before end");
        }
    }
}
