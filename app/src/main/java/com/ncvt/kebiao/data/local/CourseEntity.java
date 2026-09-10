package com.ncvt.kebiao.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import com.ncvt.kebiao.model.Course;

@Entity(tableName = "courses")
public class CourseEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;
    @NonNull public String name = "";
    @NonNull public String teacher = "";
    @NonNull public String classroom = "";
    public int dayOfWeek;
    public int startPeriod;
    public int duration;
    @NonNull public String weekPattern = "";
    public int color;
    @NonNull public String academicYear = "";
    @NonNull public String semester = "";

    public Course toDomainModel() {
        return new Course(id, name, teacher, classroom, dayOfWeek, startPeriod, duration,
                weekPattern, color, academicYear, semester);
    }
}
