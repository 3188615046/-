package com.ncvt.kebiao.viewmodel;

import android.app.Application;
import android.net.Uri;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.ncvt.kebiao.KeBiaoApplication;
import com.ncvt.kebiao.data.remote.ScrapeResult;
import com.ncvt.kebiao.data.repository.CourseRepository;
import com.ncvt.kebiao.data.repository.SettingsRepository;
import com.ncvt.kebiao.model.AppSettings;
import com.ncvt.kebiao.util.WeekCalculator;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class SettingsViewModel extends AsyncViewModel {
    private final SettingsRepository settingsRepository;
    private final CourseRepository courseRepository;
    private final MutableLiveData<AppSettings> settings = new MutableLiveData<>();
    private final MutableLiveData<Event<Boolean>> saveResult = new MutableLiveData<>();
    private final MutableLiveData<Event<ScrapeResult>> importResult = new MutableLiveData<>();
    private final MutableLiveData<Boolean> importing = new MutableLiveData<>(false);

    public SettingsViewModel(Application application) {
        super(application);
        settingsRepository = new SettingsRepository(application);
        courseRepository = new CourseRepository(((KeBiaoApplication) application).getDatabase());
        loadSettings();
    }

    public LiveData<AppSettings> getSettings() { return settings; }
    public LiveData<Event<Boolean>> getSaveResult() { return saveResult; }
    public LiveData<Event<ScrapeResult>> getImportResult() { return importResult; }
    public LiveData<Boolean> getImporting() { return importing; }

    public void loadSettings() { settings.setValue(settingsRepository.getSettings()); }

    public void saveSettings(AppSettings value) {
        AppSettings current = settingsRepository.getSettings();
        AppSettings.Builder builder = value.toBuilder();
        builder.semesterStartDate = WeekCalculator.normalizeSemesterStartDate(value.semesterStartDate);
        builder.showNonCurrentWeekCourses = current.showNonCurrentWeekCourses;
        builder.nightModeEnabled = current.nightModeEnabled;
        builder.deepCourseCardEnabled = current.deepCourseCardEnabled;
        AppSettings normalized = builder.build();
        settingsRepository.saveSettings(normalized);
        settings.setValue(normalized);
        saveResult.setValue(new Event<>(true));
    }

    public void importPyqtJson(Uri uri) {
        if (Boolean.TRUE.equals(importing.getValue())) return;
        importing.setValue(true);
        execute(() -> courseRepository.importCoursesFromPyqtJson(readJson(uri)), this::finishImport,
                error -> finishImport(new ScrapeResult.Error("读取 JSON 文件失败: " + error.getMessage())));
    }

    private String readJson(Uri uri) throws IOException {
        try (InputStream input = getApplication().getContentResolver().openInputStream(uri)) {
            if (input == null) throw new IOException("无法打开文件");
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                StringBuilder text = new StringBuilder();
                char[] buffer = new char[8192];
                int count;
                while ((count = reader.read(buffer)) != -1) text.append(buffer, 0, count);
                if (text.length() == 0) throw new IOException("文件为空");
                if (text.charAt(0) == '\uFEFF') text.deleteCharAt(0);
                return text.toString();
            }
        }
    }

    private void finishImport(ScrapeResult result) {
        importing.setValue(false);
        if (result instanceof ScrapeResult.CoursesFetched) {
            ScrapeResult.CoursesFetched fetched = (ScrapeResult.CoursesFetched) result;
            AppSettings.Builder builder = settingsRepository.getSettings().toBuilder();
            if (fetched.resolvedConfig != null) {
                builder.academicYear = fetched.resolvedConfig.academicYear;
                builder.semester = fetched.resolvedConfig.semester;
                if (!fetched.resolvedConfig.menuCode.isEmpty()) builder.menuCode = fetched.resolvedConfig.menuCode;
            }
            AppSettings updated = builder.build();
            settingsRepository.saveSettings(updated);
            settings.setValue(updated);
        }
        importResult.setValue(new Event<>(result));
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        courseRepository.close();
    }
}
