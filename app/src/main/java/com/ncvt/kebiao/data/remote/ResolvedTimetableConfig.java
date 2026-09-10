package com.ncvt.kebiao.data.remote;

import java.util.Objects;

public final class ResolvedTimetableConfig {
    public final String academicYear;
    public final String semester;
    public final String menuCode;
    public final String semesterStartDate;

    public ResolvedTimetableConfig(String academicYear, String semester, String menuCode) {
        this(academicYear, semester, menuCode, "");
    }

    public ResolvedTimetableConfig(String academicYear, String semester, String menuCode,
                                   String semesterStartDate) {
        this.academicYear = academicYear;
        this.semester = semester;
        this.menuCode = menuCode;
        this.semesterStartDate = semesterStartDate;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof ResolvedTimetableConfig)) return false;
        ResolvedTimetableConfig that = (ResolvedTimetableConfig) other;
        return academicYear.equals(that.academicYear) && semester.equals(that.semester)
                && menuCode.equals(that.menuCode) && semesterStartDate.equals(that.semesterStartDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(academicYear, semester, menuCode, semesterStartDate);
    }
}
