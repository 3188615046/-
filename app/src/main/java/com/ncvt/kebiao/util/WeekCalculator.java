package com.ncvt.kebiao.util;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class WeekCalculator {
    private static final DateTimeFormatter[] DATE_FORMATS = {
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("yyyy-M-d"),
            DateTimeFormatter.ofPattern("yyyy/M/d"),
            DateTimeFormatter.ofPattern("yyyy.M.d")
    };
    private static final Pattern NUMBER = Pattern.compile("\\d+");

    private WeekCalculator() {}

    public static int getCurrentWeek(LocalDate semesterStartDate) {
        long days = ChronoUnit.DAYS.between(semesterStartDate, LocalDate.now());
        return days < 0 ? 1 : (int) (days / 7) + 1;
    }

    public static LocalDate parseSemesterStartDate(String rawValue) {
        if (rawValue == null || rawValue.trim().isEmpty()) return null;
        for (DateTimeFormatter formatter : DATE_FORMATS) {
            try {
                return LocalDate.parse(rawValue.trim(), formatter);
            } catch (DateTimeException ignored) {
                // Try the remaining supported formats.
            }
        }
        return null;
    }

    public static String normalizeSemesterStartDate(String rawValue) {
        LocalDate date = parseSemesterStartDate(rawValue);
        return date != null ? date.toString() : rawValue.trim();
    }

    public static LocalDate resolveSemesterStartDate(String start, String year, String semester) {
        LocalDate parsed = parseSemesterStartDate(start);
        return parsed != null ? parsed : guessSemesterStartDate(year, semester);
    }

    public static LocalDate guessSemesterStartDate(String academicYear, String semester) {
        try {
            String yearPart = academicYear.split("-", 2)[0];
            int year = Integer.parseInt(yearPart.substring(0, Math.min(4, yearPart.length())));
            LocalDate reference;
            switch (semester) {
                case "3": reference = LocalDate.of(year, 9, 1); break;
                case "12": reference = LocalDate.of(year + 1, 2, 24); break;
                case "16": reference = LocalDate.of(year + 1, 7, 1); break;
                default: return null;
            }
            return reference.minusDays(reference.getDayOfWeek().getValue() - 1L);
        } catch (IllegalArgumentException | DateTimeException e) {
            return null;
        }
    }

    public static LocalDate getWeekStartDate(LocalDate semesterStartDate, int weekNumber) {
        return semesterStartDate.plusWeeks(weekNumber - 1L);
    }

    public static boolean matchesWeek(String weekPattern, int weekNumber) {
        if (weekPattern == null || weekPattern.trim().isEmpty()) return true;
        String normalized = weekPattern.replace("星期", "").replace("周", "")
                .replace(" ", "").replace("（", "(").replace("）", ")")
                .replace("，", ",").trim();
        if (normalized.isEmpty()) return true;
        for (String segment : normalized.split(",")) {
            List<Integer> numbers = numbersIn(segment);
            if (numbers.isEmpty()) continue;
            int first = numbers.get(0);
            int last = numbers.get(numbers.size() - 1);
            if (weekNumber < first || weekNumber > last) continue;
            if (segment.contains("单") && weekNumber % 2 != 1) continue;
            if (!segment.contains("单") && segment.contains("双") && weekNumber % 2 != 0) continue;
            return true;
        }
        return false;
    }

    public static List<Integer> numbersIn(String text) {
        List<Integer> numbers = new ArrayList<>();
        Matcher matcher = NUMBER.matcher(text);
        while (matcher.find()) numbers.add(Integer.parseInt(matcher.group()));
        return numbers;
    }

    public static int getTodayDayOfWeek() {
        return LocalDate.now().getDayOfWeek().getValue();
    }
}
