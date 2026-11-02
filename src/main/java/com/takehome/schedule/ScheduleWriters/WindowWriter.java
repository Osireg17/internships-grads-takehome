package com.takehome.schedule.ScheduleWriters;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.takehome.schedule.Models.Entry;

public final class WindowWriter {

    private WindowWriter() {
    }

    public static List<Entry> truncate(List<Entry> entries, Instant from, Instant until) {
        List<Entry> truncatedEntries = new ArrayList<>();

        for (Entry entry : entries) {
            Instant visibleStart = entry.start().isAfter(from) ? entry.start() : from;
            Instant visibleEnd = entry.end().isBefore(until) ? entry.end() : until;

            if (visibleStart.isBefore(visibleEnd)) {
                truncatedEntries.add(new Entry(entry.user(), visibleStart, visibleEnd));
            }
        }

        return truncatedEntries;
    }
}
