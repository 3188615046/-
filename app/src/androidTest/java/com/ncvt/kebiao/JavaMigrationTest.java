package com.ncvt.kebiao;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteConstraintException;
import androidx.room.Room;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.ncvt.kebiao.data.local.AppDatabase;
import com.ncvt.kebiao.data.local.CourseDao;
import com.ncvt.kebiao.data.local.CourseEntity;
import com.ncvt.kebiao.data.remote.CourseJsonParser;
import com.ncvt.kebiao.data.remote.CourseRaw;
import com.ncvt.kebiao.data.remote.ScrapeResult;
import com.ncvt.kebiao.data.repository.CourseRepository;
import com.ncvt.kebiao.data.repository.FortuneRepository;
import com.ncvt.kebiao.ui.timetable.WeekPickerDialog;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class JavaMigrationTest {
    private Context testContext() {
        return InstrumentationRegistry.getInstrumentation().getTargetContext();
    }

    @Test
    public void parserHandlesAliasesAndMixedWeekPatterns() throws Exception {
        JSONObject json = new JSONObject("{\"kbList\":[{\"kcmc\":\"Java\",\"jsxm\":\"Teacher\","
                + "\"cdmc\":\"A101\",\"xqj\":\"2\",\"jcs\":\"3-4节\",\"zcmc\":\"1-8周(单),10-16周(双)\"}]}");
        List<CourseRaw> courses = CourseJsonParser.parseCourses(json, true);
        assertEquals(1, courses.size());
        CourseRaw course = courses.get(0);
        assertEquals(3, course.startPeriod);
        assertEquals(4, course.endPeriod);
        assertEquals("Teacher", course.teacher);
        assertTrue(course.weeks.contains(10));
        assertFalse(course.weeks.contains(11));
    }

    @Test
    public void parserSkipsInvalidRows() throws Exception {
        JSONObject json = new JSONObject("{\"kbList\":[{\"kcmc\":\"bad\",\"xqj\":8,\"jcs\":\"1-2\"},"
                + "{\"kcmc\":\"bad\",\"xqj\":1,\"jcs\":\"4-2\"},{\"kcmc\":null}]}");
        assertTrue(CourseJsonParser.parseCourses(json, true).isEmpty());
    }

    @Test
    public void importAndInvalidImportPreserveStoredCourses() {
        AppDatabase database = Room.inMemoryDatabaseBuilder(testContext(), AppDatabase.class).build();
        try (CourseRepository repository = new CourseRepository(database)) {
            String json = "{\"xsxx\":{\"XNMC\":\"2025-2026\",\"XQM\":\"12\"},\"kbList\":["
                    + "{\"kcmc\":\"Java\",\"xqj\":1,\"jcs\":\"1-2\",\"zcd\":\"1-16\"}]}";
            assertTrue(repository.importCoursesFromPyqtJson(json) instanceof ScrapeResult.CoursesFetched);
            assertEquals(1, repository.getCoursesForDay("2025-2026", "12", 1, 2).size());
            assertTrue(repository.getCoursesForDay("2025-2026", "12", 2, 2).isEmpty());
            assertTrue(repository.importCoursesFromPyqtJson("invalid") instanceof ScrapeResult.Error);
            assertEquals(1, repository.getCoursesBySemester("2025-2026", "12").size());
        } finally {
            database.close();
        }
    }

    @Test
    public void failedReplacementRollsBackDeletion() {
        AppDatabase database = Room.inMemoryDatabaseBuilder(testContext(), AppDatabase.class).build();
        try {
            CourseDao dao = database.courseDao();
            CourseEntity original = new CourseEntity();
            original.name = "Existing";
            original.academicYear = "2026";
            original.semester = "3";
            dao.insertAll(Collections.singletonList(original));
            CourseEntity invalid = new CourseEntity();
            invalid.name = null;
            assertThrows(SQLiteConstraintException.class,
                    () -> dao.replaceBySemester("2026", "3", Collections.singletonList(invalid)));
            assertEquals("Existing", dao.getAllBySemester("2026", "3").get(0).name);
        } finally {
            database.close();
        }
    }

    @Test
    public void kotlinVersionDatabaseOpensWithoutMigration() {
        String name = "kotlin-compatibility-test";
        testContext().deleteDatabase(name);
        try {
            try (SQLiteDatabase old = testContext().openOrCreateDatabase(name, Context.MODE_PRIVATE, null)) {
                old.execSQL("CREATE TABLE courses (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL,"
                        + " teacher TEXT NOT NULL, classroom TEXT NOT NULL, dayOfWeek INTEGER NOT NULL,"
                        + " startPeriod INTEGER NOT NULL, duration INTEGER NOT NULL, weekPattern TEXT NOT NULL,"
                        + " color INTEGER NOT NULL, academicYear TEXT NOT NULL, semester TEXT NOT NULL)");
                old.execSQL("CREATE TABLE room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT)");
                old.execSQL("INSERT INTO room_master_table VALUES (42, '5f2d79db52cc6b574c1fe2d3c32062e4')");
                old.execSQL("INSERT INTO courses VALUES (7,'Existing','Teacher','A101',1,1,2,'1-16',0,'2026','3')");
                old.setVersion(1);
            }
            AppDatabase database = Room.databaseBuilder(testContext(), AppDatabase.class, name).build();
            try {
                assertEquals("Existing", database.courseDao().getAll().get(0).name);
            } finally {
                database.close();
            }
        } finally {
            testContext().deleteDatabase(name);
        }
    }

    @Test
    public void dailyFortuneIsStableWithinOneDay() {
        Context context = new ContextWrapper(testContext()) {
            @Override public Context getApplicationContext() { return this; }
            @Override public SharedPreferences getSharedPreferences(String name, int mode) {
                return super.getSharedPreferences("java_migration_test_" + name, mode);
            }
        };
        FortuneRepository repository = new FortuneRepository(context);
        LocalDate date = LocalDate.of(2026, 9, 8);
        String id = repository.drawTodayFortune(date).id;
        assertEquals(id, new FortuneRepository(context).drawTodayFortune(date).id);
        assertNull(repository.getTodayFortune(date.plusDays(1)));
    }

    @Test
    public void activityAndWeekPickerRestoreWithoutDuplicatePages() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                BottomNavigationView nav = activity.findViewById(R.id.bottom_nav);
                nav.setSelectedItemId(R.id.nav_today);
                activity.getSupportFragmentManager().executePendingTransactions();
            });
            scenario.recreate();
            scenario.onActivity(activity -> {
                assertEquals(3, activity.getSupportFragmentManager().getFragments().size());
                assertEquals(R.id.nav_today, ((BottomNavigationView) activity.findViewById(R.id.bottom_nav)).getSelectedItemId());
                ((BottomNavigationView) activity.findViewById(R.id.bottom_nav)).setSelectedItemId(R.id.nav_week_schedule);
                activity.getSupportFragmentManager().executePendingTransactions();
                WeekPickerDialog.newInstance(30).showNow(activity.getSupportFragmentManager()
                        .findFragmentByTag("week_schedule").getChildFragmentManager(), "WeekPicker");
            });
            scenario.recreate();
            scenario.onActivity(activity -> {
                assertEquals(3, activity.getSupportFragmentManager().getFragments().size());
                assertNotNull(activity.getSupportFragmentManager().findFragmentByTag("week_schedule")
                        .getChildFragmentManager().findFragmentByTag("WeekPicker"));
            });
        }
    }
}
