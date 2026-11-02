package com.takehome.schedule;

import java.io.PrintStream;
import java.nio.file.Path;
import java.time.DateTimeException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.takehome.schedule.Models.Entry;
import com.takehome.schedule.Models.Schedule;
import com.takehome.schedule.ScheduleWriters.JsonWriter;
import com.takehome.schedule.ScheduleWriters.OverrideWriter;
import com.takehome.schedule.ScheduleWriters.ShiftWriter;
import com.takehome.schedule.ScheduleWriters.WindowWriter;

public final class Main {

    public static void main(String[] args) {

        int exitCode = run(args, System.out, System.err);
        System.exit(exitCode);

    }

    public static int run(String[] args, PrintStream out, PrintStream err) {
        try {
            Map<String, String> commandLineValues = new HashMap<>();

            for (String arg : args) {
                if (!arg.startsWith("--")) {
                    throw new IllegalArgumentException("unknown flag " + arg);
                }
                String[] parts = arg.substring(2).split("=", 2);
                if (parts.length != 2) {
                    throw new IllegalArgumentException("invalid flag format " + arg);
                }
                String name = parts[0];
                String value = parts[1];

                if (commandLineValues.containsKey(name)) {
                    throw new IllegalArgumentException("repeated flag " + name);
                }
                if (!name.equals("schedule") && !name.equals("overrides") && !name.equals("from") && !name.equals("until")) {
                    throw new IllegalArgumentException("unknown flag " + name);
                }
                commandLineValues.put(name, value);
            }

            String schedulePath = commandLineValues.get("schedule");
            String overridesPath = commandLineValues.get("overrides");
            String fromValue = commandLineValues.get("from");
            String untilValue = commandLineValues.get("until");

            if (schedulePath == null) {
                throw new IllegalArgumentException("missing flag --schedule");
            }
            if (overridesPath == null) {
                throw new IllegalArgumentException("missing flag --overrides");
            }
            if (fromValue == null) {
                throw new IllegalArgumentException("missing flag --from");
            }
            if (untilValue == null) {
                throw new IllegalArgumentException("missing flag --until");
            }

            Instant from = Instant.parse(fromValue);
            Instant until = Instant.parse(untilValue);

            Schedule schedule = JsonWriter.readSchedule(Path.of(schedulePath));
            List<Entry> overrides = JsonWriter.readOverrides(Path.of(overridesPath));

            List<Entry> rotationShifts = ShiftWriter.generate(schedule, from, until);
            List<Entry> timeline = OverrideWriter.apply(rotationShifts, overrides);
            List<Entry> visibleTimeline = WindowWriter.truncate(timeline, from, until);

            out.println(JsonWriter.writeEntries(visibleTimeline));
            return 0;
        } catch (IllegalArgumentException | DateTimeException exception) {
            err.println("render-schedule: " + exception.getMessage());
            return 1;
        }
    }
}
