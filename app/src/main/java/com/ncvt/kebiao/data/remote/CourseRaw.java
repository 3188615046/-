package com.ncvt.kebiao.data.remote;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class CourseRaw {
    public final String name;
    public final String teacher;
    public final String classroom;
    public final int dayOfWeek;
    public final int startPeriod;
    public final int endPeriod;
    public final String weekPattern;
    public final List<Integer> weeks;

    public CourseRaw(String name, String teacher, String classroom, int dayOfWeek,
                     int startPeriod, int endPeriod, String weekPattern, List<Integer> weeks) {
        this.name = name;
        this.teacher = teacher;
        this.classroom = classroom;
        this.dayOfWeek = dayOfWeek;
        this.startPeriod = startPeriod;
        this.endPeriod = endPeriod;
        this.weekPattern = weekPattern;
        this.weeks = Collections.unmodifiableList(new ArrayList<>(weeks));
    }
}
