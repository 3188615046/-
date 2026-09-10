package com.ncvt.kebiao.data.repository;

import com.ncvt.kebiao.data.local.AppDatabase;
import com.ncvt.kebiao.data.local.CourseDao;
import com.ncvt.kebiao.data.local.CourseEntity;
import com.ncvt.kebiao.data.remote.CourseJsonParser;
import com.ncvt.kebiao.data.remote.CourseRaw;
import com.ncvt.kebiao.data.remote.ResolvedTimetableConfig;
import com.ncvt.kebiao.data.remote.ScrapeResult;
import com.ncvt.kebiao.data.remote.ZhengfangScraper;
import com.ncvt.kebiao.model.AppSettings;
import com.ncvt.kebiao.model.Course;
import com.ncvt.kebiao.util.ColorUtils;
import com.ncvt.kebiao.util.WeekCalculator;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class CourseRepository implements AutoCloseable {
    private final CourseDao dao;
    private ZhengfangScraper scraper;

    public CourseRepository(AppDatabase database) {
        dao = database.courseDao();
    }

    private synchronized ZhengfangScraper scraper() {
        if (scraper == null) scraper = new ZhengfangScraper();
        return scraper;
    }

    public List<Course> getCoursesBySemester(String year, String semester) {
        List<Course> courses = new ArrayList<>();
        for (CourseEntity entity : dao.getAllBySemester(year, semester)) courses.add(entity.toDomainModel());
        return courses;
    }

    public List<Course> getCoursesForWeek(String year, String semester, int week) {
        List<Course> courses = getCoursesBySemester(year, semester);
        courses.removeIf(course -> !WeekCalculator.matchesWeek(course.weekPattern, week));
        return courses;
    }

    public List<Course> getCoursesForDay(String year, String semester, int day, int week) {
        List<Course> courses = getCoursesForWeek(year, semester, week);
        courses.removeIf(course -> course.dayOfWeek != day);
        courses.sort(Comparator.comparingInt(course -> course.startPeriod));
        return courses;
    }

    public ScrapeResult initLogin(String eduUrl) {
        return scraper().fetchLoginPage(eduUrl);
    }

    public byte[] getCaptchaImage() {
        return scraper().fetchCaptcha();
    }

    public ScrapeResult updateCoursesFromEdu(AppSettings settings, String captcha) {
        ScrapeResult loginResult = scraper().login(settings.account, settings.password, captcha);
        if (!(loginResult instanceof ScrapeResult.LoginSuccess)) return loginResult;
        ScrapeResult lastFailure = new ScrapeResult.Error("已登录，但未能确定学年、学期或课表入口");
        for (ResolvedTimetableConfig config : scraper().resolveTimetableConfigs(
                settings.academicYear, settings.semester, settings.menuCode)) {
            if (Thread.currentThread().isInterrupted()) return new ScrapeResult.Error("更新已取消");
            ScrapeResult result = scraper().fetchCourses(config.academicYear, config.semester, config.menuCode);
            if (result instanceof ScrapeResult.CoursesFetched) {
                List<CourseRaw> courses = ((ScrapeResult.CoursesFetched) result).courses;
                saveRawCourses(courses, config.academicYear, config.semester);
                return new ScrapeResult.CoursesFetched(courses, config);
            }
            lastFailure = result;
        }
        return lastFailure;
    }

    public ScrapeResult importCoursesFromPyqtJson(String rawJson) {
        try {
            JSONObject root = new JSONObject(rawJson);
            JSONObject student = root.optJSONObject("xsxx");
            String year = CourseJsonParser.firstNonBlank(student, "XNMC", "XNM");
            String semester = CourseJsonParser.firstNonBlank(student, "XQM");
            if (year.isEmpty() || semester.isEmpty()) {
                return new ScrapeResult.Error("JSON 缺少学年或学期信息，请确认选择的是 PyQt 导出的课程表原始 JSON");
            }
            if (root.optJSONArray("kbList") == null) return new ScrapeResult.Error("JSON 中没有找到 kbList 课表数据");
            List<CourseRaw> courses = CourseJsonParser.parseCourses(root, true);
            if (courses.isEmpty()) return new ScrapeResult.Error("JSON 已读取成功，但没有解析到课程数据");
            saveRawCourses(courses, year, semester);
            return new ScrapeResult.CoursesFetched(courses, new ResolvedTimetableConfig(year, semester, "N2151"));
        } catch (Exception e) {
            return new ScrapeResult.Error("解析 PyQt JSON 失败: " + e.getMessage());
        }
    }

    private void saveRawCourses(List<CourseRaw> rawCourses, String year, String semester) {
        List<CourseEntity> entities = new ArrayList<>();
        for (CourseRaw raw : rawCourses) {
            CourseEntity entity = new CourseEntity();
            entity.name = raw.name;
            entity.teacher = raw.teacher;
            entity.classroom = raw.classroom;
            entity.dayOfWeek = raw.dayOfWeek;
            entity.startPeriod = raw.startPeriod;
            entity.duration = raw.endPeriod - raw.startPeriod + 1;
            entity.weekPattern = raw.weekPattern;
            entity.color = ColorUtils.colorForCourse(raw.name);
            entity.academicYear = year;
            entity.semester = semester;
            entities.add(entity);
        }
        dao.replaceBySemester(year, semester, entities);
    }

    @Override
    public synchronized void close() {
        if (scraper != null) scraper.close();
    }
}
