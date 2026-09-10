package com.ncvt.kebiao.viewmodel;

import android.app.Application;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.ncvt.kebiao.KeBiaoApplication;
import com.ncvt.kebiao.data.remote.ResolvedTimetableConfig;
import com.ncvt.kebiao.data.remote.ScrapeResult;
import com.ncvt.kebiao.data.repository.CourseRepository;
import com.ncvt.kebiao.data.repository.SettingsRepository;
import com.ncvt.kebiao.model.AppSettings;
import com.ncvt.kebiao.model.Course;
import com.ncvt.kebiao.util.WeekCalculator;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class WeekScheduleViewModel extends AsyncViewModel {
    private final CourseRepository courseRepository;
    private final SettingsRepository settingsRepository;
    private final MutableLiveData<Integer> currentWeek = new MutableLiveData<>(1);
    private final MutableLiveData<List<Course>> weekCourses = new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<AppSettings> settings = new MutableLiveData<>();
    private final MutableLiveData<ScrapeState> scrapeState = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private AppSettings currentSettings = new AppSettings();
    private AppSettings updateSettings;
    private List<Course> semesterCourses = Collections.emptyList();
    private int loadGeneration;
    private int updateGeneration;

    public WeekScheduleViewModel(Application application) {
        super(application);
        courseRepository = new CourseRepository(((KeBiaoApplication) application).getDatabase());
        settingsRepository = new SettingsRepository(application);
        loadSettingsAndCourses();
    }

    public LiveData<Integer> getCurrentWeek() { return currentWeek; }
    public LiveData<List<Course>> getWeekCourses() { return weekCourses; }
    public LiveData<AppSettings> getSettings() { return settings; }
    public LiveData<ScrapeState> getScrapeState() { return scrapeState; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }

    public void loadSettingsAndCourses() {
        currentSettings = settingsRepository.getSettings();
        settings.setValue(currentSettings);
        currentWeek.setValue(resolveCurrentWeek(currentSettings));
        loadSemesterCourses();
    }

    public void switchToWeek(int week) {
        currentWeek.setValue(Math.max(1, Math.min(25, week)));
        publishVisibleCourses();
    }

    public void previousWeek() { switchToWeek(selectedWeek() - 1); }
    public void nextWeek() { switchToWeek(selectedWeek() + 1); }
    public void goToCurrentWeek() {
        currentWeek.setValue(resolveCurrentWeek(currentSettings));
        publishVisibleCourses();
    }

    private int selectedWeek() { return currentWeek.getValue() == null ? 1 : currentWeek.getValue(); }

    public void setShowNonCurrentWeekCourses(boolean show) {
        if (currentSettings.showNonCurrentWeekCourses == show) return;
        AppSettings.Builder builder = settingsRepository.getSettings().toBuilder();
        builder.showNonCurrentWeekCourses = show;
        saveDisplaySettings(builder.build());
    }

    public void setNightModeEnabled(boolean enabled) {
        if (currentSettings.nightModeEnabled == enabled) return;
        AppSettings.Builder builder = settingsRepository.getSettings().toBuilder();
        builder.nightModeEnabled = enabled;
        saveDisplaySettings(builder.build());
        AppCompatDelegate.setDefaultNightMode(enabled
                ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
    }

    public void setDeepCourseCardEnabled(boolean enabled) {
        if (currentSettings.deepCourseCardEnabled == enabled) return;
        AppSettings.Builder builder = settingsRepository.getSettings().toBuilder();
        builder.deepCourseCardEnabled = enabled;
        saveDisplaySettings(builder.build());
    }

    private void saveDisplaySettings(AppSettings updated) {
        currentSettings = updated;
        settingsRepository.saveSettings(updated);
        settings.setValue(updated);
        publishVisibleCourses();
    }

    public void startUpdateCourses() {
        if (Boolean.TRUE.equals(isLoading.getValue())) return;
        currentSettings = settingsRepository.getSettings();
        settings.setValue(currentSettings);
        if (!currentSettings.isValid()) {
            fail("请先填写账号和密码");
            return;
        }
        updateSettings = currentSettings;
        AppSettings snapshot = updateSettings;
        int generation = ++updateGeneration;
        isLoading.setValue(true);
        scrapeState.setValue(ScrapeState.Loading.INSTANCE);
        execute(() -> courseRepository.initLogin(snapshot.eduUrl), result -> {
            if (generation != updateGeneration) return;
            if (result instanceof ScrapeResult.LoginPageReady) {
                if (((ScrapeResult.LoginPageReady) result).requiresCaptcha) loadCaptcha(generation);
                else performUpdate("", generation);
            } else {
                fail(result instanceof ScrapeResult.Error ? ((ScrapeResult.Error) result).message : "登录初始化失败");
            }
        }, error -> { if (generation == updateGeneration) fail(error.getMessage()); });
    }

    private void loadCaptcha(int generation) {
        execute(courseRepository::getCaptchaImage, bytes -> {
            if (generation != updateGeneration) return;
            if (bytes == null || bytes.length == 0) fail("获取验证码失败，请检查网络连接");
            else scrapeState.setValue(new ScrapeState.CaptchaRequired(bytes));
        }, error -> { if (generation == updateGeneration) fail(error.getMessage()); });
    }

    public void refreshCaptcha() {
        if (updateSettings == null) {
            cancelUpdate();
            startUpdateCourses();
            return;
        }
        if (scrapeState.getValue() instanceof ScrapeState.CaptchaRequired) {
            scrapeState.setValue(ScrapeState.Loading.INSTANCE);
            loadCaptcha(updateGeneration);
        }
    }

    public void submitCaptchaAndFetch(String captcha) {
        if (updateSettings == null) {
            cancelUpdate();
            startUpdateCourses();
            return;
        }
        if (!(scrapeState.getValue() instanceof ScrapeState.CaptchaRequired)) return;
        scrapeState.setValue(ScrapeState.Loading.INSTANCE);
        performUpdate(captcha, updateGeneration);
    }

    public void cancelUpdate() {
        updateGeneration++;
        updateSettings = null;
        isLoading.setValue(false);
        scrapeState.setValue(null);
    }

    public void consumeMessage() {
        if (scrapeState.getValue() instanceof ScrapeState.Success
                || scrapeState.getValue() instanceof ScrapeState.Error) scrapeState.setValue(null);
    }

    private void performUpdate(String captcha, int generation) {
        AppSettings snapshot = updateSettings;
        execute(() -> courseRepository.updateCoursesFromEdu(snapshot, captcha), result -> {
            if (generation != updateGeneration) return;
            isLoading.setValue(false);
            if (result instanceof ScrapeResult.CoursesFetched) {
                ScrapeResult.CoursesFetched fetched = (ScrapeResult.CoursesFetched) result;
                ResolvedTimetableConfig config = fetched.resolvedConfig;
                if (config != null) {
                    AppSettings.Builder builder = settingsRepository.getSettings().toBuilder();
                    builder.academicYear = config.academicYear;
                    builder.semester = config.semester;
                    builder.menuCode = config.menuCode;
                    if (builder.semesterStartDate.trim().isEmpty()) builder.semesterStartDate = config.semesterStartDate;
                    currentSettings = builder.build();
                    settingsRepository.saveSettings(currentSettings);
                    settings.setValue(currentSettings);
                    currentWeek.setValue(resolveCurrentWeek(currentSettings));
                }
                loadSemesterCourses();
                scrapeState.setValue(new ScrapeState.Success("课表更新成功，共 " + fetched.courses.size() + " 门课程"));
            } else if (result instanceof ScrapeResult.LoginFailed) {
                fail(((ScrapeResult.LoginFailed) result).message);
            } else {
                fail(result instanceof ScrapeResult.Error ? ((ScrapeResult.Error) result).message : "课表更新失败");
            }
        }, error -> { if (generation == updateGeneration) fail(error.getMessage()); });
    }

    private void fail(String message) {
        isLoading.setValue(false);
        scrapeState.setValue(new ScrapeState.Error(message == null ? "操作失败，请重试" : message));
    }

    private int resolveCurrentWeek(AppSettings value) {
        LocalDate date = WeekCalculator.resolveSemesterStartDate(
                value.semesterStartDate, value.academicYear, value.semester);
        return date == null ? 1 : WeekCalculator.getCurrentWeek(date);
    }

    private void loadSemesterCourses() {
        int generation = ++loadGeneration;
        String year = currentSettings.academicYear;
        String semester = currentSettings.semester;
        if (year.trim().isEmpty() || semester.trim().isEmpty()) {
            semesterCourses = Collections.emptyList();
            publishVisibleCourses();
            return;
        }
        execute(() -> {
            List<Course> courses = courseRepository.getCoursesBySemester(year, semester);
            courses.sort(Comparator.comparingInt((Course course) -> course.dayOfWeek)
                    .thenComparingInt(course -> course.startPeriod).thenComparing(course -> course.name));
            return courses;
        }, courses -> {
            if (generation != loadGeneration) return;
            semesterCourses = courses;
            publishVisibleCourses();
        }, error -> {
            if (generation != loadGeneration) return;
            semesterCourses = Collections.emptyList();
            publishVisibleCourses();
            fail("读取课表失败: " + error.getMessage());
        });
    }

    private void publishVisibleCourses() {
        List<Course> visible = new ArrayList<>();
        for (Course course : semesterCourses) {
            if (currentSettings.showNonCurrentWeekCourses
                    || WeekCalculator.matchesWeek(course.weekPattern, selectedWeek())) visible.add(course);
        }
        weekCourses.setValue(visible);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        courseRepository.close();
    }
}
