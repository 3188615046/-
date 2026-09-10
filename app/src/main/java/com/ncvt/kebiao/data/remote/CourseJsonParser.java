package com.ncvt.kebiao.data.remote;

import com.ncvt.kebiao.util.WeekCalculator;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public final class CourseJsonParser {
    private CourseJsonParser() {}

    public static List<CourseRaw> parseCourses(JSONObject root, boolean importedFile) {
        List<CourseRaw> courses = new ArrayList<>();
        JSONArray items = root.optJSONArray("kbList");
        if (items == null) return courses;
        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.optJSONObject(i);
            if (item == null) continue;
            String name = firstNonBlank(item, "kcmc");
            int day = item.optInt("xqj", 0);
            String periods = importedFile ? firstNonBlank(item, "jcs", "jc", "jcor")
                    : firstNonBlank(item, "jc", "jcs", "jcor");
            if (name.isEmpty() || day < 1 || day > 7 || periods.isEmpty()) continue;
            List<Integer> values = WeekCalculator.numbersIn(periods);
            if (values.isEmpty()) continue;
            int start = values.get(0);
            int end = values.get(values.size() - 1);
            if (start < 1 || end < start) continue;
            String weekPattern = firstNonBlank(item, "zcd", "zcmc");
            List<Integer> weeks = new ArrayList<>();
            if (!weekPattern.isEmpty()) {
                for (int week = 1; week <= 53; week++) {
                    if (WeekCalculator.matchesWeek(weekPattern, week)) weeks.add(week);
                }
            }
            courses.add(new CourseRaw(name, firstNonBlank(item, "xm", "jsxm", "rkjs"),
                    firstNonBlank(item, "cdmc"), day, start, end, weekPattern, weeks));
        }
        return courses;
    }

    public static String firstNonBlank(JSONObject object, String... keys) {
        if (object == null) return "";
        for (String key : keys) {
            if (object.isNull(key)) continue;
            String value = object.optString(key, "").trim();
            if (!value.isEmpty()) return value;
        }
        return "";
    }
}
