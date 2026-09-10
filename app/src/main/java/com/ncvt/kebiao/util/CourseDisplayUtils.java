package com.ncvt.kebiao.util;

import com.ncvt.kebiao.model.Course;

public final class CourseDisplayUtils {
    private static final String[] PERIOD_TIMES = {
            "08:00", "08:40", "08:50", "09:30", "09:40", "10:20",
            "10:30", "11:10", "11:20", "12:00", "14:40", "15:20",
            "15:30", "16:10", "16:20", "17:00", "19:00", "19:40",
            "19:50", "20:30", "20:40", "21:20", "21:30", "22:10"
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
        return timeAt(course.startPeriod) + "-" + timeAt(end < PERIOD_TIMES.length ? end + 1 : end);
    }

    private static String timeAt(int period) {
        return period >= 1 && period <= PERIOD_TIMES.length ? PERIOD_TIMES[period - 1] : "--:--";
    }
}
