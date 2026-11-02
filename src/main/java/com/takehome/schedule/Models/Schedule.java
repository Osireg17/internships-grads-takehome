package com.takehome.schedule.Models;

import java.time.Instant;
import java.util.List;

public record Schedule(List<String> users, Instant handoverStartAt, int handoverIntervalDays) {

    public Schedule {
        if (users == null || users.isEmpty()) {
            throw new IllegalArgumentException("Users cannot be null or empty");
        }
        if (users.stream().anyMatch(user -> user == null || user.isBlank())) {
            throw new IllegalArgumentException("Users cannot contain null or blank names");
        }
        if (handoverStartAt == null) {
            throw new IllegalArgumentException("Handover start cannot be null");
        }
        if (handoverIntervalDays <= 0) {
            throw new IllegalArgumentException("Handover interval must be positive");
        }

        users = List.copyOf(users);
    }
}
