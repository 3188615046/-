package com.ncvt.kebiao.model;

import java.io.Serializable;
import java.util.Objects;

public final class Course implements Serializable {
    private static final long serialVersionUID = 1L;

    public final long id;
    public final String name;
    public final String teacher;
    public final String classroom;
    public final int dayOfWeek;
    public final int startPeriod;
    public final int duration;
    public final String weekPattern;
    public final int color;
    public final String academicYear;
    public final String semester;

    public Course(long id, String name, String teacher, String classroom, int dayOfWeek,
                  int startPeriod, int duration, String weekPattern, int color,
                  String academicYear, String semester) {
        this.id = id;
        this.name = name;
        this.teacher = teacher;
        this.classroom = classroom;
        this.dayOfWeek = dayOfWeek;
        this.startPeriod = startPeriod;
        this.duration = duration;
        this.weekPattern = weekPattern;
        this.color = color;
        this.academicYear = academicYear;
        this.semester = semester;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof Course)) return false;
        Course that = (Course) other;
        return id == that.id && dayOfWeek == that.dayOfWeek && startPeriod == that.startPeriod
                && duration == that.duration && color == that.color && Objects.equals(name, that.name)
                && Objects.equals(teacher, that.teacher) && Objects.equals(classroom, that.classroom)
                && Objects.equals(weekPattern, that.weekPattern)
                && Objects.equals(academicYear, that.academicYear) && Objects.equals(semester, that.semester);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, teacher, classroom, dayOfWeek, startPeriod, duration,
                weekPattern, color, academicYear, semester);
    }
}
