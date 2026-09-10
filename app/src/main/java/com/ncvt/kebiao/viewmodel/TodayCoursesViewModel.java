package com.ncvt.kebiao.viewmodel;

import android.app.Application;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.ncvt.kebiao.KeBiaoApplication;
import com.ncvt.kebiao.data.repository.CourseRepository;
import com.ncvt.kebiao.data.repository.SettingsRepository;
import com.ncvt.kebiao.model.AppSettings;
import com.ncvt.kebiao.model.Course;
import com.ncvt.kebiao.util.WeekCalculator;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class TodayCoursesViewModel extends AsyncViewModel {
    private final CourseRepository courseRepository;
    private final SettingsRepository settingsRepository;
    private final MutableLiveData<List<Course>> todayCourses = new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<String> dateDisplay = new MutableLiveData<>("");
    private final MutableLiveData<Boolean> isEmpty = new MutableLiveData<>(true);
    private int loadGeneration;

    public TodayCoursesViewModel(Application application) {
        super(application);
        courseRepository = new CourseRepository(((KeBiaoApplication) application).getDatabase());
        settingsRepository = new SettingsRepository(application);
        loadTodayCourses();
    }

    public LiveData<List<Course>> getTodayCourses() { return todayCourses; }
    public LiveData<String> getDateDisplay() { return dateDisplay; }
    public LiveData<Boolean> getIsEmpty() { return isEmpty; }

    public void loadTodayCourses() {
        int generation = ++loadGeneration;
        AppSettings settings = settingsRepository.getSettings();
        LocalDate today = LocalDate.now();
        int day = today.getDayOfWeek().getValue();
        dateDisplay.setValue(today.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日 EEEE", Locale.CHINESE)));
        if (settings.academicYear.trim().isEmpty() || settings.semester.trim().isEmpty()) {
            publish(Collections.emptyList());
            return;
        }
        LocalDate start = WeekCalculator.resolveSemesterStartDate(
                settings.semesterStartDate, settings.academicYear, settings.semester);
        int week = start == null ? 1 : WeekCalculator.getCurrentWeek(start);
        execute(() -> courseRepository.getCoursesForDay(settings.academicYear, settings.semester, day, week),
                courses -> { if (generation == loadGeneration) publish(courses); },
                error -> { if (generation == loadGeneration) publish(Collections.emptyList()); });
    }

    private void publish(List<Course> courses) {
        todayCourses.setValue(courses);
        isEmpty.setValue(courses.isEmpty());
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        courseRepository.close();
    }
}
