# render-schedule

A command-line tool that renders an on-call schedule: a rotation of users, with temporary overrides applied, cut to a time window.

## Requirements

- JDK 17 or newer
- Maven 3.9+

## Install and build

The ZIP already contains a built `target/render-schedule.jar`, so with a JDK 17+ you can skip straight to [Run](#run).
To rebuild it from source (needs Maven):

```console
$ mvn package
```

This compiles the code, runs the tests, and builds `target/render-schedule.jar`.
Add `-DskipTests` to skip the tests. The `./render-schedule` launcher needs the jar, so rebuild if it is missing.

## Run

### Example input

`examples/schedule.json` is a single object holding the rotation rule:

```json
{
  "users": ["alice", "bob", "charlie"],
  "handover_start_at": "2026-11-06T17:00:00Z",
  "handover_interval_days": 7
}
```

`examples/overrides.json` is an array of overrides. Here Charlie covers 5pm to 10pm on Monday 9 November:

```json
[
  {
    "user": "charlie",
    "start_at": "2026-11-09T17:00:00Z",
    "end_at": "2026-11-09T22:00:00Z"
  }
]
```

### Command and output

```console
$ ./render-schedule \
    --schedule=examples/schedule.json \
    --overrides=examples/overrides.json \
    --from='2026-11-06T17:00:00Z' \
    --until='2026-11-20T17:00:00Z'
[
  {
    "user": "alice",
    "start_at": "2026-11-06T17:00:00Z",
    "end_at": "2026-11-09T17:00:00Z"
  },
  {
    "user": "charlie",
    "start_at": "2026-11-09T17:00:00Z",
    "end_at": "2026-11-09T22:00:00Z"
  },
  {
    "user": "alice",
    "start_at": "2026-11-09T22:00:00Z",
    "end_at": "2026-11-13T17:00:00Z"
  },
  {
    "user": "bob",
    "start_at": "2026-11-13T17:00:00Z",
    "end_at": "2026-11-20T17:00:00Z"
  }
]
```

- All four flags are required, in the `--name=value` form (flag order doesn't matter).
- `examples/overrides-tricky.json` has out-of-order overrides, one spanning a handover, and one inside its own user's shift.
- Run the tests alone with `mvn test`.

### Errors

Any bad input prints `render-schedule: <message>` to stderr, writes nothing to stdout, and exits with code 1. No stack traces. Examples:

- a missing, repeated, unknown or malformed flag
- an unreadable file, invalid JSON, or a missing field
- an unparsable timestamp
- `--from` not before `--until`
- `--from` earlier than the schedule's `handover_start_at`

Everything is a pure function over immutable values. Only `Main` and `JsonWriter` touch the outside world.

### Worked example

The README example, one stage at a time. Alice, Bob and Charlie rotate weekly from 6 Nov 17:00, and Charlie has an override on 9 Nov from 17:00 to 22:00.

```text
            6 Nov 17:00      9 Nov 17:00   22:00    13 Nov 17:00           20 Nov 17:00
            |                |             |        |                      |
1 shifts    [ alice ...................................... ][ bob ........... ]
2 overrides [ alice ......... ][ charlie ][ alice ...... ][ bob ........... ]
3 window    nothing to cut: the window is exactly 6 Nov 17:00 to 20 Nov 17:00
```

1. `ShiftWriter` produces two whole shifts, Alice then Bob.
2. `OverrideWriter` splits Alice's shift around Charlie's override: before, override, after.
3. `WindowWriter` clamps to `--from` and `--until`. With a narrower window, the first and last entries would be shortened and anything outside dropped.

### Data types

| Type | Shape | Notes |
| --- | --- | --- |
| `Entry` | `record(String user, Instant start, Instant end)` | One block of cover, half-open `[start, end)`. Rejects a blank user, null times, or `start >= end`. Used for shifts, overrides and output. |
| `Schedule` | `record(List<String> users, Instant handoverStartAt, int handoverIntervalDays)` | The rotation definition. Validates its fields and copies the list. |

### Main APIs

| API | Does |
| --- | --- |
| `Main.run(args, out, err)` | Parses flags, runs the pipeline, prints the result. The one place errors are caught; returns the exit code. |
| `JsonWriter.readSchedule(Path)` / `readOverrides(Path)` | Read a file and validate it at the boundary. Bad input becomes an `IllegalArgumentException` that names the file. |
| `JsonWriter.writeEntries(List<Entry>)` | Renders entries as the pretty-printed JSON array in the task's format (`[]` when empty). |
| `ShiftWriter.generate(schedule, from, until)` | Builds the base rotation covering the window. The user for shift `n` is `users[n mod userCount]`, counted from `handover_start_at`. |
| `OverrideWriter.apply(baseEntries, overrides)` | Cuts the shifts that an override covers, inserts the override, and returns a sorted timeline. |
| `WindowWriter.truncate(entries, from, until)` | Clamps each entry to the window and drops anything left empty. |

### Design decisions

- **Half-open intervals.** `[start, end)` means back-to-back shifts never overlap.
- **`Instant` everywhere.** All times are UTC instants, so there is no time-zone or DST handling.
- **Overrides split shifts.** An override in the middle of a shift leaves a piece before and a piece after.
- **Merging only where an override creates the adjacency.** If a neighbouring piece belongs to the same user as an override, the two merge into one entry (for example, Bob overriding the last day before his own shift). Untouched back-to-back shifts of the same user are never merged, which matters for a one-person rotation.
- **One error handler.** Every layer throws `IllegalArgumentException` with a useful message, and `Main.run` turns it into the stderr line and the exit code.

### Not handled (by choice)

- Overlapping overrides are accepted rather than rejected; the task guarantees they don't happen.
- `--from` before `handover_start_at` and `--from == --until` are errors rather than being clamped or printing `[]`.
