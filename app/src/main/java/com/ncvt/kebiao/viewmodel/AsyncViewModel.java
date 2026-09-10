package com.ncvt.kebiao.viewmodel;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import androidx.lifecycle.AndroidViewModel;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

abstract class AsyncViewModel extends AndroidViewModel {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private volatile boolean cleared;

    AsyncViewModel(Application application) {
        super(application);
    }

    protected final <T> void execute(Callable<T> task, Consumer<T> success, Consumer<Exception> failure) {
        if (cleared) return;
        executor.execute(() -> {
            try {
                T result = task.call();
                dispatch(() -> success.accept(result));
            } catch (Exception e) {
                dispatch(() -> failure.accept(e));
            }
        });
    }

    private void dispatch(Runnable action) {
        if (!cleared) mainHandler.post(() -> {
            if (!cleared) action.run();
        });
    }

    @Override
    protected void onCleared() {
        cleared = true;
        executor.shutdownNow();
        mainHandler.removeCallbacksAndMessages(null);
        super.onCleared();
    }
}
