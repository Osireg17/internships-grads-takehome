package com.takehome.schedule.ScheduleWriters;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.takehome.schedule.Models.Entry;
import com.takehome.schedule.Models.Schedule;

public final class ShiftWriter {

    public static List<Entry> generate(Schedule schedule, Instant from, Instant until) {

        if (from.isAfter(until) || from.equals(until)) {
            throw new IllegalArgumentException("Window is empty or backwards");
        }

        if (from.isBefore(schedule.handoverStartAt())) {
            throw new IllegalArgumentException("Window opens before the schedule's first handover");
        }

        List<Entry> shifts = new ArrayList<>();

        long shiftLengthSeconds = schedule.handoverIntervalDays() * 24L * 60L * 60L;

        Instant currentShiftStart = schedule.handoverStartAt();
        int currentPersonIndex = 0;

        while (currentShiftStart.isBefore(until)) {
            Instant currentShiftEnd = currentShiftStart.plusSeconds(shiftLengthSeconds);

            if (currentShiftEnd.isAfter(from)) {
                String currentUser = schedule.users().get(currentPersonIndex);
                shifts.add(new Entry(currentUser, currentShiftStart, currentShiftEnd));
            }

            currentShiftStart = currentShiftEnd;
            currentPersonIndex = (currentPersonIndex + 1) % schedule.users().size();
        }

        return shifts;
    }
}
