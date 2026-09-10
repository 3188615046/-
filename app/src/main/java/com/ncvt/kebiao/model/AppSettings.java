package com.ncvt.kebiao.model;

public final class AppSettings {
    public static final String DEFAULT_EDU_URL =
            "http://jw.sub.ncvt.net/jwglxt/xtgl/login_slogin.html";

    public final String eduUrl;
    public final String account;
    public final String password;
    public final String academicYear;
    public final String semester;
    public final String menuCode;
    public final String semesterStartDate;
    public final boolean showNonCurrentWeekCourses;
    public final boolean nightModeEnabled;
    public final boolean deepCourseCardEnabled;

    public AppSettings() {
        this(new Builder());
    }

    private AppSettings(Builder builder) {
        eduUrl = builder.eduUrl;
        account = builder.account;
        password = builder.password;
        academicYear = builder.academicYear;
        semester = builder.semester;
        menuCode = builder.menuCode;
        semesterStartDate = builder.semesterStartDate;
        showNonCurrentWeekCourses = builder.showNonCurrentWeekCourses;
        nightModeEnabled = builder.nightModeEnabled;
        deepCourseCardEnabled = builder.deepCourseCardEnabled;
    }

    public boolean isValid() {
        return !eduUrl.trim().isEmpty() && !account.trim().isEmpty()
                && !password.trim().isEmpty();
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public static final class Builder {
        public String eduUrl = DEFAULT_EDU_URL;
        public String account = "";
        public String password = "";
        public String academicYear = "";
        public String semester = "";
        public String menuCode = "";
        public String semesterStartDate = "";
        public boolean showNonCurrentWeekCourses = true;
        public boolean nightModeEnabled;
        public boolean deepCourseCardEnabled;

        public Builder() {}

        public Builder(AppSettings settings) {
            eduUrl = settings.eduUrl;
            account = settings.account;
            password = settings.password;
            academicYear = settings.academicYear;
            semester = settings.semester;
            menuCode = settings.menuCode;
            semesterStartDate = settings.semesterStartDate;
            showNonCurrentWeekCourses = settings.showNonCurrentWeekCourses;
            nightModeEnabled = settings.nightModeEnabled;
            deepCourseCardEnabled = settings.deepCourseCardEnabled;
        }

        public AppSettings build() {
            return new AppSettings(this);
        }
    }
}
