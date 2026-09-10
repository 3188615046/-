package com.ncvt.kebiao.util;

import org.junit.Test;
import java.time.LocalDate;
import static org.junit.Assert.*;

public class WeekCalculatorTest {
    @Test
    public void emptyPatternAppliesToEveryWeek() {
        assertTrue(WeekCalculator.matchesWeek(null, 7));
        assertTrue(WeekCalculator.matchesWeek("  ", 7));
    }

    @Test
    public void rangeIncludesBothEnds() {
        assertTrue(WeekCalculator.matchesWeek("1-16周", 1));
        assertTrue(WeekCalculator.matchesWeek("1-16周", 16));
        assertFalse(WeekCalculator.matchesWeek("1-16周", 17));
    }

    @Test
    public void oddAndEvenApplyToTheirOwnSegments() {
        String pattern = "1-8周（单），10-16周（双）,19周";
        assertTrue(WeekCalculator.matchesWeek(pattern, 3));
        assertFalse(WeekCalculator.matchesWeek(pattern, 4));
        assertTrue(WeekCalculator.matchesWeek(pattern, 10));
        assertFalse(WeekCalculator.matchesWeek(pattern, 11));
        assertTrue(WeekCalculator.matchesWeek(pattern, 19));
        assertFalse(WeekCalculator.matchesWeek(pattern, 18));
    }

    @Test
    public void invalidPatternDoesNotMatch() {
        assertFalse(WeekCalculator.matchesWeek("unknown", 1));
        assertFalse(WeekCalculator.matchesWeek("16-1", 3));
    }

    @Test
    public void supportedDateFormatsNormalizeToIso() {
        for (String input : new String[]{"2026-03-02", "2026-3-2", "2026/3/2", "2026.3.2"}) {
            assertEquals("2026-03-02", WeekCalculator.normalizeSemesterStartDate(input));
        }
    }

    @Test
    public void explicitStartTakesPriorityOverGuess() {
        assertEquals(LocalDate.of(2026, 3, 2), WeekCalculator.resolveSemesterStartDate("2026/3/2", "2025", "12"));
        assertEquals(LocalDate.of(2026, 2, 23), WeekCalculator.guessSemesterStartDate("2025-2026", "12"));
        assertNull(WeekCalculator.guessSemesterStartDate("invalid", "12"));
        assertNull(WeekCalculator.guessSemesterStartDate("2025", "invalid"));
    }

    @Test
    public void weekBoundariesAndFutureStartAreStable() {
        LocalDate today = LocalDate.now();
        assertEquals(1, WeekCalculator.getCurrentWeek(today.plusDays(7)));
        assertEquals(1, WeekCalculator.getCurrentWeek(today.minusDays(6)));
        assertEquals(2, WeekCalculator.getCurrentWeek(today.minusDays(7)));
        assertEquals(LocalDate.of(2026, 3, 9), WeekCalculator.getWeekStartDate(LocalDate.of(2026, 3, 2), 2));
    }
}
