package com.takehome.schedule.ScheduleWriters;

import com.takehome.schedule.Models.Entry;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OverrideWriterTest {

    private List<Entry> baseEntries;

    @BeforeEach
    void setUp() {
        baseEntries = List.of(
                entry("alice", "2026-11-06T17:00:00Z", "2026-11-13T17:00:00Z"),
                entry("bob", "2026-11-13T17:00:00Z", "2026-11-20T17:00:00Z"),
                entry("charlie", "2026-11-20T17:00:00Z", "2026-11-27T17:00:00Z")
        );
    }

    @Test
    void noOverridesLeavesBaseUnchanged() {
        assertEquals(baseEntries, OverrideWriter.apply(baseEntries, List.of()));
    }

    @Test
    void overrideInMiddleOfShiftSplitsIntoThree() {
        List<Entry> overrides = List.of(entry("charlie", "2026-11-09T17:00:00Z", "2026-11-09T22:00:00Z"));

        assertEquals(List.of(
                entry("alice", "2026-11-06T17:00:00Z", "2026-11-09T17:00:00Z"),
                entry("charlie", "2026-11-09T17:00:00Z", "2026-11-09T22:00:00Z"),
                entry("alice", "2026-11-09T22:00:00Z", "2026-11-13T17:00:00Z"),
                baseEntries.get(1), baseEntries.get(2)
        ), OverrideWriter.apply(baseEntries, overrides));
    }

    @Test
    void overrideStartingOnShiftStartLeavesNoEmptyPiece() {
        List<Entry> overrides = List.of(entry("charlie", "2026-11-06T17:00:00Z", "2026-11-06T22:00:00Z"));

        assertEquals(List.of(
                entry("charlie", "2026-11-06T17:00:00Z", "2026-11-06T22:00:00Z"),
                entry("alice", "2026-11-06T22:00:00Z", "2026-11-13T17:00:00Z"),
                baseEntries.get(1), baseEntries.get(2)
        ), OverrideWriter.apply(baseEntries, overrides));
    }

    @Test
    void overrideEndingOnShiftEndLeavesNoEmptyPiece() {
        List<Entry> overrides = List.of(entry("charlie", "2026-11-12T22:00:00Z", "2026-11-13T17:00:00Z"));

        assertEquals(List.of(
                entry("alice", "2026-11-06T17:00:00Z", "2026-11-12T22:00:00Z"),
                entry("charlie", "2026-11-12T22:00:00Z", "2026-11-13T17:00:00Z"),
                baseEntries.get(1), baseEntries.get(2)
        ), OverrideWriter.apply(baseEntries, overrides));
    }

    @Test
    void overrideCoveringWholeShiftReplacesIt() {
        List<Entry> overrides = List.of(entry("charlie", "2026-11-06T17:00:00Z", "2026-11-13T17:00:00Z"));

        assertEquals(List.of(
                entry("charlie", "2026-11-06T17:00:00Z", "2026-11-13T17:00:00Z"),
                baseEntries.get(1), baseEntries.get(2)
        ), OverrideWriter.apply(baseEntries, overrides));
    }

    @Test
    void overrideSpanningTwoShiftsReplacesTailAndHead() {
        List<Entry> overrides = List.of(entry("charlie", "2026-11-13T15:00:00Z", "2026-11-14T03:00:00Z"));

        assertEquals(List.of(
                entry("alice", "2026-11-06T17:00:00Z", "2026-11-13T15:00:00Z"),
                entry("charlie", "2026-11-13T15:00:00Z", "2026-11-14T03:00:00Z"),
                entry("bob", "2026-11-14T03:00:00Z", "2026-11-20T17:00:00Z"),
                baseEntries.get(2)
        ), OverrideWriter.apply(baseEntries, overrides));
    }

    @Test
    void overrideByNextShiftsUserMergesIntoOneEntry() {
        List<Entry> overrides = List.of(entry("bob", "2026-11-12T17:00:00Z", "2026-11-13T17:00:00Z"));

        assertEquals(List.of(
                entry("alice", "2026-11-06T17:00:00Z", "2026-11-12T17:00:00Z"),
                entry("bob", "2026-11-12T17:00:00Z", "2026-11-20T17:00:00Z"),
                baseEntries.get(2)
        ), OverrideWriter.apply(baseEntries, overrides));
    }

    @Test
    void overrideByTheSameUserInsideTheirOwnShiftChangesNothing() {
        List<Entry> overrides = List.of(entry("alice", "2026-11-09T17:00:00Z", "2026-11-09T22:00:00Z"));

        assertEquals(baseEntries, OverrideWriter.apply(baseEntries, overrides));
    }

    @Test
    void multipleUnsortedOverridesGiveChronologicalResult() {
        List<Entry> overrides = List.of(
                entry("charlie", "2026-11-09T17:00:00Z", "2026-11-09T22:00:00Z"),
                entry("bob", "2026-11-08T17:00:00Z", "2026-11-08T22:00:00Z")
        );

        assertEquals(List.of(
                entry("alice", "2026-11-06T17:00:00Z", "2026-11-08T17:00:00Z"),
                entry("bob", "2026-11-08T17:00:00Z", "2026-11-08T22:00:00Z"),
                entry("alice", "2026-11-08T22:00:00Z", "2026-11-09T17:00:00Z"),
                entry("charlie", "2026-11-09T17:00:00Z", "2026-11-09T22:00:00Z"),
                entry("alice", "2026-11-09T22:00:00Z", "2026-11-13T17:00:00Z"),
                baseEntries.get(1), baseEntries.get(2)
        ), OverrideWriter.apply(baseEntries, overrides));
    }

    @Test
    void overrideOutsideBaseEntriesIsStillReturned() {
        List<Entry> overrides = List.of(entry("charlie", "2026-11-28T17:00:00Z", "2026-11-29T17:00:00Z"));

        assertEquals(List.of(baseEntries.get(0), baseEntries.get(1), baseEntries.get(2), overrides.get(0)),
                OverrideWriter.apply(baseEntries, overrides));
    }

    @Test
    void untouchedBackToBackShiftsOfSameUserAreNotMerged() {
        List<Entry> singleUserBase = List.of(
                entry("alice", "2026-11-06T17:00:00Z", "2026-11-13T17:00:00Z"),
                entry("alice", "2026-11-13T17:00:00Z", "2026-11-20T17:00:00Z")
        );

        assertEquals(singleUserBase, OverrideWriter.apply(singleUserBase, List.of()));
    }

    @Test
    void doesNotModifyItsInputs() {
        List<Entry> overrides = List.of(entry("charlie", "2026-11-09T17:00:00Z", "2026-11-09T22:00:00Z"));
        List<Entry> originalBaseEntries = List.copyOf(baseEntries);
        List<Entry> originalOverrides = List.copyOf(overrides);

        OverrideWriter.apply(baseEntries, overrides);

        assertEquals(originalBaseEntries, baseEntries);
        assertEquals(originalOverrides, overrides);
    }

    @Test
    void overrideInsideOneSingleUserShiftDoesNotMergeAcrossTheNextShift() {
        List<Entry> singleUserBase = List.of(
                entry("alice", "2026-11-06T17:00:00Z", "2026-11-13T17:00:00Z"),
                entry("alice", "2026-11-13T17:00:00Z", "2026-11-20T17:00:00Z")
        );
        List<Entry> overrides = List.of(entry("alice", "2026-11-09T17:00:00Z", "2026-11-10T17:00:00Z"));

        assertEquals(singleUserBase, OverrideWriter.apply(singleUserBase, overrides));
    }

    @Test
    void overrideEndingOnASingleUserShiftBoundaryDoesNotMergeTheNextShift() {
        List<Entry> singleUserBase = List.of(
                entry("alice", "2026-11-06T17:00:00Z", "2026-11-13T17:00:00Z"),
                entry("alice", "2026-11-13T17:00:00Z", "2026-11-20T17:00:00Z")
        );
        List<Entry> overrides = List.of(entry("alice", "2026-11-09T17:00:00Z", "2026-11-13T17:00:00Z"));

        assertEquals(singleUserBase, OverrideWriter.apply(singleUserBase, overrides));
    }

    @Test
    void touchingOverridesForTheSameUserAreMerged() {
        List<Entry> overrides = List.of(
                entry("bob", "2026-11-08T17:00:00Z", "2026-11-09T17:00:00Z"),
                entry("bob", "2026-11-09T17:00:00Z", "2026-11-10T17:00:00Z")
        );

        assertEquals(List.of(
                entry("alice", "2026-11-06T17:00:00Z", "2026-11-08T17:00:00Z"),
                entry("bob", "2026-11-08T17:00:00Z", "2026-11-10T17:00:00Z"),
                entry("alice", "2026-11-10T17:00:00Z", "2026-11-13T17:00:00Z"),
                baseEntries.get(1), baseEntries.get(2)
        ), OverrideWriter.apply(baseEntries, overrides));
    }

    private static Entry entry(String user, String start, String end) {
        return new Entry(user, Instant.parse(start), Instant.parse(end));
    }
}
