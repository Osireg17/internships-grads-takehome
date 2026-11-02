package com.takehome.schedule.ScheduleWriters;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.takehome.schedule.Models.Entry;

public final class OverrideWriter {

    private record TimelinePiece(
            Entry entry,
            boolean startsWithOverride,
            boolean endsWithOverride,
            Integer baseEntryIndex
            ) {}

    public static List<Entry> apply(List<Entry> baseEntries, List<Entry> overrides) {

        List<Entry> sortedOverrides = overrides.stream()
                .sorted(Comparator.comparing(Entry::start))
                .toList();

        List<TimelinePiece> pieces = new ArrayList<>();

        for (int baseEntryIndex = 0; baseEntryIndex < baseEntries.size(); baseEntryIndex++) {
            Entry baseEntry = baseEntries.get(baseEntryIndex);
            Instant currentPosition = baseEntry.start();

            for (Entry override : sortedOverrides) {
                if (override.end().isAfter(baseEntry.start()) && override.start().isBefore(baseEntry.end())) {
                    Instant overlapStart = currentPosition.isAfter(override.start()) ? currentPosition : override.start();
                    Instant overlapEnd = baseEntry.end().isBefore(override.end()) ? baseEntry.end() : override.end();

                    if (currentPosition.isBefore(overlapStart)) {
                        pieces.add(new TimelinePiece(
                                new Entry(baseEntry.user(), currentPosition, overlapStart),
                                false,
                                false,
                                baseEntryIndex
                        ));
                    }

                    currentPosition = currentPosition.isAfter(overlapEnd) ? currentPosition : overlapEnd;
                }
            }

            if (currentPosition.isBefore(baseEntry.end())) {
                pieces.add(new TimelinePiece(
                        new Entry(baseEntry.user(), currentPosition, baseEntry.end()),
                        false,
                        false,
                        baseEntryIndex
                ));
            }
        }

        for (Entry override : sortedOverrides) {
            pieces.add(new TimelinePiece(override, true, true, null));
        }

        pieces.sort(Comparator.comparing(piece -> piece.entry().start()));
        List<TimelinePiece> finalTimeline = new ArrayList<>();

        for (TimelinePiece piece : pieces) {
            if (!finalTimeline.isEmpty()) {
                TimelinePiece lastFinalPiece = finalTimeline.get(finalTimeline.size() - 1);
                if (lastFinalPiece.entry().end().equals(piece.entry().start())
                        && lastFinalPiece.entry().user().equals(piece.entry().user())
                        && (lastFinalPiece.endsWithOverride() || piece.startsWithOverride())
                        && !crossesSameUserBaseShiftBoundary(lastFinalPiece, piece, baseEntries)) {
                    finalTimeline.set(finalTimeline.size() - 1,
                            new TimelinePiece(new Entry(lastFinalPiece.entry().user(),
                                    lastFinalPiece.entry().start(), piece.entry().end()),
                                    lastFinalPiece.startsWithOverride(),
                                    piece.endsWithOverride(),
                                    firstBaseEntryIndex(lastFinalPiece, piece)));
                    continue;
                }
            }
            finalTimeline.add(piece);
        }

        return finalTimeline.stream().map(TimelinePiece::entry).toList();
    }

    private static boolean crossesSameUserBaseShiftBoundary(
            TimelinePiece left,
            TimelinePiece right,
            List<Entry> baseEntries
    ) {
        if (left.baseEntryIndex() == null || right.baseEntryIndex() == null
                || left.baseEntryIndex().equals(right.baseEntryIndex())) {
            return false;
        }

        return baseEntries.get(left.baseEntryIndex()).user().equals(baseEntries.get(right.baseEntryIndex()).user());
    }

    private static Integer firstBaseEntryIndex(TimelinePiece left, TimelinePiece right) {
        return left.baseEntryIndex() != null ? left.baseEntryIndex() : right.baseEntryIndex();
    }
}
