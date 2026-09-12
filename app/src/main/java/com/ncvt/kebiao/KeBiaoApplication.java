package com.ncvt.kebiao;

import android.app.Application;
import com.ncvt.kebiao.data.local.AppDatabase;
import com.ncvt.kebiao.reminder.CourseReminderScheduler;

public class KeBiaoApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        CourseReminderScheduler.ensureNotificationChannel(this);
        CourseReminderScheduler.syncAsync(this);
    }

    public AppDatabase getDatabase() {
        return AppDatabase.getInstance(this);
    }
}
