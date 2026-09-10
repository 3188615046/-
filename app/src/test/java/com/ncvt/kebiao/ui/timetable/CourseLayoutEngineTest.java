package com.ncvt.kebiao.ui.timetable;

import com.ncvt.kebiao.model.Course;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class CourseLayoutEngineTest {
    @Test
    public void sameTimeCoursesUseSeparateLanesInWeekOrder() {
        List<CourseLayoutEngine.CourseLayout> layouts = CourseLayoutEngine.layout(Arrays.asList(
                course(2, "第3-4周", 1, 1, 2), course(1, "第1-2周", 1, 1, 2)));

        assertEquals(2, layouts.size());
        assertEquals(1, layouts.get(0).course.id);
        assertEquals(0, layouts.get(0).laneIndex);
        assertEquals(2, layouts.get(0).laneCount);
        assertEquals(2, layouts.get(1).course.id);
        assertEquals(1, layouts.get(1).laneIndex);
        assertEquals(2, layouts.get(1).laneCount);
    }

    @Test
    public void adjacentAndDifferentDayCoursesKeepFullHeight() {
        List<CourseLayoutEngine.CourseLayout> layouts = CourseLayoutEngine.layout(Arrays.asList(
                course(1, "1-16周", 1, 1, 2), course(2, "1-16周", 1, 3, 2),
                course(3, "1-16周", 2, 1, 2)));

        for (CourseLayoutEngine.CourseLayout layout : layouts) {
            assertEquals(0, layout.laneIndex);
            assertEquals(1, layout.laneCount);
        }
    }

    @Test
    public void partialOverlapSharesTwoLanesWithoutCoveringCards() {
        List<CourseLayoutEngine.CourseLayout> layouts = CourseLayoutEngine.layout(Arrays.asList(
                course(1, "第1周", 1, 1, 2), course(2, "第2周", 1, 2, 2),
                course(3, "第3周", 1, 3, 2)));

        assertEquals(2, layouts.get(0).laneCount);
        assertEquals(2, layouts.get(1).laneCount);
        assertEquals(2, layouts.get(2).laneCount);
        assertEquals(0, layouts.get(0).laneIndex);
        assertEquals(1, layouts.get(1).laneIndex);
        assertEquals(0, layouts.get(2).laneIndex);
    }

    @Test
    public void threeSimultaneousCoursesUseThreeLanes() {
        List<CourseLayoutEngine.CourseLayout> layouts = CourseLayoutEngine.layout(Arrays.asList(
                course(1, "第1周", 1, 5, 2), course(2, "第2周", 1, 5, 2),
                course(3, "第3周", 1, 5, 2)));

        assertEquals(3, layouts.get(0).laneCount);
        assertEquals(3, layouts.get(1).laneCount);
        assertEquals(3, layouts.get(2).laneCount);
    }

    private Course course(long id, String weekPattern, int day, int start, int duration) {
        return new Course(id, "课程" + id, "教师", "教室", day, start, duration,
                weekPattern, 0xFFDFF3E4, "2026", "3");
    }
}
