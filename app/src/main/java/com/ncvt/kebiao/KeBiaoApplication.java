package com.ncvt.kebiao;

import android.app.Application;
import com.ncvt.kebiao.data.local.AppDatabase;

public class KeBiaoApplication extends Application {
    public AppDatabase getDatabase() {
        return AppDatabase.getInstance(this);
    }
}
