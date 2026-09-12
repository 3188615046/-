package com.ncvt.kebiao.util;

import com.ncvt.kebiao.model.Course;
import org.junit.Test;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import static org.junit.Assert.*;

public class CourseReminderTimeCalculatorTest {
    private static final LocalDate SEMESTER_START = LocalDate.of(2026, 9, 7);

    @Test
    public void reminderIsThirtyMinutesBeforeCourse() {
        Course course = course(1, 1, "1-16周");
        assertEquals(LocalDateTime.of(2026, 9, 7, 7, 30),
                CourseReminderTimeCalculator.findNextReminder(course, SEMESTER_START,
                        LocalDateTime.of(2026, 9, 7, 7, 0)));
    }

    @Test
    public void firedReminderAdvancesToNextMatchingWeek() {
        Course course = course(1, 1, "1-16周（单）");
        assertEquals(LocalDateTime.of(2026, 9, 21, 7, 30),
                CourseReminderTimeCalculator.findNextReminder(course, SEMESTER_START,
                        LocalDateTime.of(2026, 9, 7, 7, 30)));
    }

    @Test
    public void invalidCourseTimeDoesNotSchedule() {
        Course course = course(1, 20, "1-16周");
        assertNull(CourseReminderTimeCalculator.findNextReminder(course, SEMESTER_START,
                LocalDateTime.of(2026, 9, 1, 0, 0)));
        assertEquals(LocalTime.of(14, 40), CourseDisplayUtils.startTime(6));
    }

    private Course course(int day, int period, String weeks) {
        return new Course(7, "Java", "Teacher", "A101", day, period, 2,
                weeks, 0xFFDFF3E4, "2026-2027", "3");
    }
}
