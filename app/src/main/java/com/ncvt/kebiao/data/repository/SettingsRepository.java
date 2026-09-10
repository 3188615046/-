package com.ncvt.kebiao.data.repository;

import android.content.Context;
import android.content.SharedPreferences;
import com.ncvt.kebiao.model.AppSettings;

public final class SettingsRepository {
    private final SharedPreferences prefs;

    public SettingsRepository(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences("kebiao_settings", Context.MODE_PRIVATE);
    }

    public AppSettings getSettings() {
        AppSettings.Builder builder = new AppSettings.Builder();
        builder.eduUrl = prefs.getString("edu_url", AppSettings.DEFAULT_EDU_URL);
        builder.account = prefs.getString("account", "");
        builder.password = prefs.getString("password", "");
        builder.academicYear = prefs.getString("academic_year", "");
        builder.semester = prefs.getString("semester", "");
        builder.menuCode = prefs.getString("menu_code", "");
        builder.semesterStartDate = prefs.getString("semester_start_date", "");
        builder.showNonCurrentWeekCourses = prefs.getBoolean("show_non_current_week", true);
        builder.nightModeEnabled = prefs.getBoolean("night_mode", false);
        builder.deepCourseCardEnabled = prefs.getBoolean("deep_course_card", false);
        return builder.build();
    }

    public void saveSettings(AppSettings settings) {
        prefs.edit()
                .putString("edu_url", settings.eduUrl)
                .putString("account", settings.account)
                .putString("password", settings.password)
                .putString("academic_year", settings.academicYear)
                .putString("semester", settings.semester)
                .putString("menu_code", settings.menuCode)
                .putString("semester_start_date", settings.semesterStartDate)
                .putBoolean("show_non_current_week", settings.showNonCurrentWeekCourses)
                .putBoolean("night_mode", settings.nightModeEnabled)
                .putBoolean("deep_course_card", settings.deepCourseCardEnabled)
                .apply();
    }
}
