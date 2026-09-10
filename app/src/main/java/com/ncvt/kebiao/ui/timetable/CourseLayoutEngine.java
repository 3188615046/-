package com.ncvt.kebiao.ui.timetable;

import com.ncvt.kebiao.model.Course;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Assigns vertical lanes to courses which occupy overlapping class periods.
 * Each connected overlap group shares the same number of lanes, so cards stay
 * inside their original time range while never covering one another.
 */
public final class CourseLayoutEngine {
    private static final Pattern FIRST_NUMBER = Pattern.compile("\\d+");

    private CourseLayoutEngine() {}

    public static List<CourseLayout> layout(List<Course> courses) {
        Map<Integer, List<Course>> byDay = new HashMap<>();
        for (Course course : courses) {
            if (course == null || course.duration <= 0) continue;
            List<Course> dayCourses = byDay.get(course.dayOfWeek);
            if (dayCourses == null) {
                dayCourses = new ArrayList<>();
                byDay.put(course.dayOfWeek, dayCourses);
            }
            dayCourses.add(course);
        }

        List<CourseLayout> result = new ArrayList<>();
        for (List<Course> dayCourses : byDay.values()) {
            dayCourses.sort(courseOrder());
            int groupStart = 0;
            int groupEnd = Integer.MIN_VALUE;
            for (int index = 0; index < dayCourses.size(); index++) {
                Course course = dayCourses.get(index);
                if (index > groupStart && course.startPeriod >= groupEnd) {
                    addGroupLayouts(result, dayCourses.subList(groupStart, index));
                    groupStart = index;
                    groupEnd = Integer.MIN_VALUE;
                }
                groupEnd = Math.max(groupEnd, endPeriodExclusive(course));
            }
            if (groupStart < dayCourses.size()) {
                addGroupLayouts(result, dayCourses.subList(groupStart, dayCourses.size()));
            }
        }
        result.sort(Comparator.comparingInt((CourseLayout item) -> item.course.dayOfWeek)
                .thenComparingInt(item -> item.course.startPeriod)
                .thenComparingInt(item -> item.laneIndex)
                .thenComparingLong(item -> item.course.id));
        return result;
    }

    private static void addGroupLayouts(List<CourseLayout> result, List<Course> group) {
        List<Integer> laneEnds = new ArrayList<>();
        List<LaneAssignment> assignments = new ArrayList<>();
        for (Course course : group) {
            int lane = firstAvailableLane(laneEnds, course.startPeriod);
            if (lane == laneEnds.size()) laneEnds.add(endPeriodExclusive(course));
            else laneEnds.set(lane, endPeriodExclusive(course));
            assignments.add(new LaneAssignment(course, lane));
        }
        int laneCount = laneEnds.size();
        for (LaneAssignment assignment : assignments) {
            result.add(new CourseLayout(assignment.course, assignment.laneIndex, laneCount));
        }
    }

    private static int firstAvailableLane(List<Integer> laneEnds, int startPeriod) {
        for (int lane = 0; lane < laneEnds.size(); lane++) {
            if (laneEnds.get(lane) <= startPeriod) return lane;
        }
        return laneEnds.size();
    }

    private static Comparator<Course> courseOrder() {
        return Comparator.comparingInt((Course course) -> course.startPeriod)
                .thenComparingInt(CourseLayoutEngine::endPeriodExclusive)
                .thenComparingInt(course -> firstWeek(course.weekPattern))
                .thenComparing(course -> course.weekPattern == null ? "" : course.weekPattern)
                .thenComparing(course -> course.name == null ? "" : course.name)
                .thenComparingLong(course -> course.id);
    }

    private static int endPeriodExclusive(Course course) {
        return course.startPeriod + Math.max(1, course.duration);
    }

    private static int firstWeek(String weekPattern) {
        if (weekPattern == null) return Integer.MAX_VALUE;
        Matcher matcher = FIRST_NUMBER.matcher(weekPattern);
        return matcher.find() ? Integer.parseInt(matcher.group()) : Integer.MAX_VALUE;
    }

    public static final class CourseLayout {
        public final Course course;
        public final int laneIndex;
        public final int laneCount;

        private CourseLayout(Course course, int laneIndex, int laneCount) {
            this.course = course;
            this.laneIndex = laneIndex;
            this.laneCount = laneCount;
        }
    }

    private static final class LaneAssignment {
        final Course course;
        final int laneIndex;

        LaneAssignment(Course course, int laneIndex) {
            this.course = course;
            this.laneIndex = laneIndex;
        }
    }
}
