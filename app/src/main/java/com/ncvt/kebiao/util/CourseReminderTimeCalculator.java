package com.ncvt.kebiao.util;

import com.ncvt.kebiao.model.Course;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public final class CourseReminderTimeCalculator {
    public static final int ADVANCE_MINUTES = 30;
    private static final int DEFAULT_MAX_WEEKS = 30;
    private static final int ABSOLUTE_MAX_WEEKS = 60;

    private CourseReminderTimeCalculator() {}

    public static LocalDateTime findNextReminder(Course course, LocalDate semesterStartDate,
                                                  LocalDateTime after) {
        if (course == null || semesterStartDate == null || after == null
                || course.dayOfWeek < 1 || course.dayOfWeek > 7) return null;
        LocalTime startTime = CourseDisplayUtils.startTime(course.startPeriod);
        if (startTime == null) return null;

        int lastWeek = DEFAULT_MAX_WEEKS;
        List<Integer> weeks = WeekCalculator.numbersIn(course.weekPattern == null ? "" : course.weekPattern);
        for (int week : weeks) lastWeek = Math.max(lastWeek, week);
        lastWeek = Math.min(lastWeek, ABSOLUTE_MAX_WEEKS);

        for (int week = 1; week <= lastWeek; week++) {
            if (!WeekCalculator.matchesWeek(course.weekPattern, week)) continue;
            LocalDate courseDate = semesterStartDate.plusWeeks(week - 1L)
                    .plusDays(course.dayOfWeek - 1L);
            LocalDateTime reminderAt = LocalDateTime.of(courseDate, startTime)
                    .minusMinutes(ADVANCE_MINUTES);
            if (reminderAt.isAfter(after)) return reminderAt;
        }
        return null;
    }
}
