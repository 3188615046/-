package com.ncvt.kebiao.reminder;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import com.ncvt.kebiao.R;
import com.ncvt.kebiao.data.local.AppDatabase;
import com.ncvt.kebiao.data.local.CourseEntity;
import com.ncvt.kebiao.data.repository.SettingsRepository;
import com.ncvt.kebiao.model.AppSettings;
import com.ncvt.kebiao.model.Course;
import com.ncvt.kebiao.util.CourseDisplayUtils;
import com.ncvt.kebiao.util.CourseReminderTimeCalculator;
import com.ncvt.kebiao.util.WeekCalculator;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public final class CourseReminderScheduler {
    public static final String CHANNEL_ID = "course_reminders";
    public static final String ACTION_SHOW_REMINDER = "com.ncvt.kebiao.action.SHOW_COURSE_REMINDER";
    public static final String EXTRA_COURSE_ID = "course_id";
    public static final String EXTRA_COURSE_NAME = "course_name";
    public static final String EXTRA_TEACHER = "teacher";
    public static final String EXTRA_CLASSROOM = "classroom";
    public static final String EXTRA_START_TIME = "start_time";

    private static final String PREFS_NAME = "course_reminder_alarms";
    private static final String KEY_SCHEDULED_IDS = "scheduled_course_ids";
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "course-reminder-scheduler");
            thread.setDaemon(true);
            return thread;
        }
    });

    private CourseReminderScheduler() {}

    public static void ensureNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) return;
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                context.getString(R.string.course_reminder_channel), NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription(context.getString(R.string.course_reminder_channel_description));
        channel.enableVibration(true);
        manager.createNotificationChannel(channel);
    }

    public static void syncAsync(Context context) {
        syncAsync(context, null);
    }

    public static void syncAsync(Context context, Runnable completion) {
        Context appContext = context.getApplicationContext();
        EXECUTOR.execute(() -> {
            try {
                syncNow(appContext);
            } finally {
                if (completion != null) completion.run();
            }
        });
    }

    public static void scheduleFollowingAsync(Context context, long courseId, Runnable completion) {
        Context appContext = context.getApplicationContext();
        EXECUTOR.execute(() -> {
            try {
                scheduleFollowing(appContext, courseId);
            } finally {
                if (completion != null) completion.run();
            }
        });
    }

    static void syncNow(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;
        SharedPreferences alarmPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> oldIds = new HashSet<>(alarmPrefs.getStringSet(KEY_SCHEDULED_IDS, new HashSet<>()));
        for (String rawId : oldIds) {
            try {
                cancel(alarmManager, context, Long.parseLong(rawId));
            } catch (NumberFormatException ignored) {
                // Ignore invalid state left by an older app build.
            }
        }

        Set<String> scheduledIds = new HashSet<>();
        AppSettings settings = new SettingsRepository(context).getSettings();
        LocalDate semesterStart = WeekCalculator.resolveSemesterStartDate(
                settings.semesterStartDate, settings.academicYear, settings.semester);
        if (settings.courseReminderEnabled && semesterStart != null
                && !settings.academicYear.trim().isEmpty() && !settings.semester.trim().isEmpty()) {
            List<CourseEntity> entities = AppDatabase.getInstance(context).courseDao()
                    .getAllBySemester(settings.academicYear, settings.semester);
            LocalDateTime now = LocalDateTime.now();
            for (CourseEntity entity : entities) {
                Course course = entity.toDomainModel();
                if (scheduleNext(alarmManager, context, course, semesterStart, now)) {
                    scheduledIds.add(Long.toString(course.id));
                }
            }
        }
        alarmPrefs.edit().putStringSet(KEY_SCHEDULED_IDS, scheduledIds).commit();
    }

    private static void scheduleFollowing(Context context, long courseId) {
        SharedPreferences alarmPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> scheduledIds = new HashSet<>(
                alarmPrefs.getStringSet(KEY_SCHEDULED_IDS, new HashSet<>()));
        String rawId = Long.toString(courseId);
        scheduledIds.remove(rawId);

        AppSettings settings = new SettingsRepository(context).getSettings();
        LocalDate semesterStart = WeekCalculator.resolveSemesterStartDate(
                settings.semesterStartDate, settings.academicYear, settings.semester);
        CourseEntity entity = AppDatabase.getInstance(context).courseDao().getById(courseId);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (settings.courseReminderEnabled && semesterStart != null && entity != null && alarmManager != null
                && entity.academicYear.equals(settings.academicYear)
                && entity.semester.equals(settings.semester)
                && scheduleNext(alarmManager, context, entity.toDomainModel(), semesterStart,
                LocalDateTime.now())) {
            scheduledIds.add(rawId);
        }
        alarmPrefs.edit().putStringSet(KEY_SCHEDULED_IDS, scheduledIds).commit();
    }

    @SuppressLint("ScheduleExactAlarm")
    private static boolean scheduleNext(AlarmManager alarmManager, Context context, Course course,
                                        LocalDate semesterStart, LocalDateTime after) {
        LocalDateTime reminderAt = CourseReminderTimeCalculator.findNextReminder(
                course, semesterStart, after);
        if (reminderAt == null) return false;

        Intent intent = reminderIntent(context, course);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, requestCode(course.id), intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        long triggerAtMillis = reminderAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
            }
        } catch (SecurityException exactAlarmDenied) {
            // The permission can be revoked between the capability check and the call.
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
        }
        return true;
    }

    private static void cancel(AlarmManager alarmManager, Context context, long courseId) {
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, requestCode(courseId),
                reminderIntent(context, courseId), PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
        }
    }

    private static Intent reminderIntent(Context context, Course course) {
        return reminderIntent(context, course.id)
                .putExtra(EXTRA_COURSE_NAME, course.name)
                .putExtra(EXTRA_TEACHER, course.teacher)
                .putExtra(EXTRA_CLASSROOM, course.classroom)
                .putExtra(EXTRA_START_TIME, CourseDisplayUtils.startTimeLabel(course.startPeriod));
    }

    private static Intent reminderIntent(Context context, long courseId) {
        return new Intent(context, CourseReminderReceiver.class)
                .setAction(ACTION_SHOW_REMINDER)
                .setData(Uri.parse("kebiao://course-reminder/" + courseId))
                .putExtra(EXTRA_COURSE_ID, courseId);
    }

    private static int requestCode(long courseId) {
        return (int) (courseId ^ (courseId >>> 32));
    }
}
