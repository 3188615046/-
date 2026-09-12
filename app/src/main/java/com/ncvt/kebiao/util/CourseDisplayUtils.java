package com.ncvt.kebiao.util;

import com.ncvt.kebiao.model.Course;
import java.time.LocalTime;

public final class CourseDisplayUtils {
    private static final String[] PERIOD_START_TIMES = {
            "08:00", "08:50", "09:40", "10:30", "11:20", "14:40",
            "15:30", "16:20", "19:00", "19:50", "20:40", "21:30"
    };
    private static final String[] PERIOD_END_TIMES = {
            "08:40", "09:30", "10:20", "11:10", "12:00", "15:20",
            "16:10", "17:00", "19:40", "20:30", "21:20", "22:10"
    };
    private static final String[] DAYS = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};

    private CourseDisplayUtils() {}

    public static String semesterDisplay(String semester) {
        switch (semester.trim()) {
            case "3": return "1";
            case "12": return "2";
            case "16": return "短";
            default: return semester.trim().isEmpty() ? "-" : semester;
        }
    }

    public static String dayOfWeekLabel(int dayOfWeek) {
        return dayOfWeek >= 1 && dayOfWeek <= 7 ? DAYS[dayOfWeek - 1] : "未知";
    }

    public static String periodLabel(Course course) {
        return "第" + course.startPeriod + "-" + (course.startPeriod + course.duration - 1) + "节";
    }

    public static String timeRangeLabel(Course course) {
        int end = course.startPeriod + course.duration - 1;
        return startTimeLabel(course.startPeriod) + "-" + endTimeLabel(end);
    }

    public static LocalTime startTime(int period) {
        return period >= 1 && period <= PERIOD_START_TIMES.length
                ? LocalTime.parse(PERIOD_START_TIMES[period - 1]) : null;
    }

    public static String startTimeLabel(int period) {
        return period >= 1 && period <= PERIOD_START_TIMES.length
                ? PERIOD_START_TIMES[period - 1] : "--:--";
    }

    private static String endTimeLabel(int period) {
        return period >= 1 && period <= PERIOD_END_TIMES.length
                ? PERIOD_END_TIMES[period - 1] : "--:--";
    }
}
