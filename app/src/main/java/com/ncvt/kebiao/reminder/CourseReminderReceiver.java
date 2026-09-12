package com.ncvt.kebiao.reminder;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import com.ncvt.kebiao.MainActivity;
import com.ncvt.kebiao.R;
import com.ncvt.kebiao.data.repository.SettingsRepository;

public class CourseReminderReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        PendingResult pendingResult = goAsync();
        if (CourseReminderScheduler.ACTION_SHOW_REMINDER.equals(intent.getAction())) {
            if (new SettingsRepository(context).getSettings().courseReminderEnabled) {
                showNotification(context, intent);
            }
            long courseId = intent.getLongExtra(CourseReminderScheduler.EXTRA_COURSE_ID, -1L);
            CourseReminderScheduler.scheduleFollowingAsync(context, courseId, pendingResult::finish);
        } else {
            CourseReminderScheduler.syncAsync(context, pendingResult::finish);
        }
    }

    private void showNotification(Context context, Intent source) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) return;

        CourseReminderScheduler.ensureNotificationChannel(context);
        String courseName = source.getStringExtra(CourseReminderScheduler.EXTRA_COURSE_NAME);
        String teacher = source.getStringExtra(CourseReminderScheduler.EXTRA_TEACHER);
        String classroom = source.getStringExtra(CourseReminderScheduler.EXTRA_CLASSROOM);
        String startTime = source.getStringExtra(CourseReminderScheduler.EXTRA_START_TIME);
        if (courseName == null || courseName.trim().isEmpty()) courseName = context.getString(R.string.app_name);

        StringBuilder details = new StringBuilder(startTime == null ? "" : startTime);
        appendDetail(details, classroom);
        appendDetail(details, teacher);

        Intent openApp = new Intent(context, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        long courseId = source.getLongExtra(CourseReminderScheduler.EXTRA_COURSE_ID, -1L);
        PendingIntent contentIntent = PendingIntent.getActivity(context, (int) courseId, openApp,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder notification = new NotificationCompat.Builder(
                context, CourseReminderScheduler.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_course_notification)
                .setContentTitle(context.getString(R.string.course_reminder_title, courseName))
                .setContentText(details.toString())
                .setStyle(new NotificationCompat.BigTextStyle().bigText(details.toString()))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_EVENT)
                .setAutoCancel(true)
                .setContentIntent(contentIntent);
        try {
            NotificationManagerCompat.from(context).notify(notificationId(courseId), notification.build());
        } catch (SecurityException ignored) {
            // Notification permission can be revoked while this receiver is running.
        }
    }

    private void appendDetail(StringBuilder details, String value) {
        if (value == null || value.trim().isEmpty()) return;
        if (details.length() > 0) details.append(" · ");
        details.append(value.trim());
    }

    private int notificationId(long courseId) {
        return 0x4B420000 ^ (int) (courseId ^ (courseId >>> 32));
    }
}
