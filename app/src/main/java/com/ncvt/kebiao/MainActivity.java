package com.ncvt.kebiao;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import com.ncvt.kebiao.data.repository.SettingsRepository;
import com.ncvt.kebiao.databinding.ActivityMainBinding;
import com.ncvt.kebiao.model.AppSettings;
import com.ncvt.kebiao.reminder.CourseReminderScheduler;
import com.ncvt.kebiao.ui.common.FragmentEventKeys;
import com.ncvt.kebiao.ui.settings.SettingsFragment;
import com.ncvt.kebiao.ui.timetable.WeekScheduleFragment;
import com.ncvt.kebiao.ui.today.TodayCoursesFragment;

public class MainActivity extends AppCompatActivity {
    private static final String TAG_WEEK = "week_schedule";
    private static final String TAG_TODAY = "today_courses";
    private static final String TAG_SETTINGS = "settings";
    private static final String ACTIVE_TAB = "active_tab";
    private static final String PERMISSION_PREFS = "notification_permission_state";
    private static final String KEY_PERMISSION_REQUESTED = "requested";
    private ActivityMainBinding binding;
    private WeekScheduleFragment weekFragment;
    private TodayCoursesFragment todayFragment;
    private SettingsFragment settingsFragment;
    private Fragment activeFragment;
    private ActivityResultLauncher<String> notificationPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(new SettingsRepository(this).getSettings().nightModeEnabled
                ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        notificationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(), granted -> {});
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applySystemBarInsets();

        FragmentManager manager = getSupportFragmentManager();
        weekFragment = (WeekScheduleFragment) manager.findFragmentByTag(TAG_WEEK);
        todayFragment = (TodayCoursesFragment) manager.findFragmentByTag(TAG_TODAY);
        settingsFragment = (SettingsFragment) manager.findFragmentByTag(TAG_SETTINGS);
        if (weekFragment == null) weekFragment = new WeekScheduleFragment();
        if (todayFragment == null) todayFragment = new TodayCoursesFragment();
        if (settingsFragment == null) settingsFragment = new SettingsFragment();
        androidx.fragment.app.FragmentTransaction transaction = manager.beginTransaction();
        if (!weekFragment.isAdded()) transaction.add(R.id.fragment_container, weekFragment, TAG_WEEK);
        if (!todayFragment.isAdded()) transaction.add(R.id.fragment_container, todayFragment, TAG_TODAY);
        if (!settingsFragment.isAdded()) transaction.add(R.id.fragment_container, settingsFragment, TAG_SETTINGS);
        int selected = savedInstanceState == null ? R.id.nav_week_schedule
                : savedInstanceState.getInt(ACTIVE_TAB, R.id.nav_week_schedule);
        activeFragment = fragmentForItem(selected);
        for (Fragment fragment : new Fragment[]{weekFragment, todayFragment, settingsFragment}) {
            if (fragment == activeFragment) transaction.show(fragment);
            else transaction.hide(fragment);
        }
        transaction.commitNow();
        binding.bottomNav.setSelectedItemId(selected);
        binding.bottomNav.setOnItemSelectedListener(item -> {
            switchFragment(fragmentForItem(item.getItemId()));
            return true;
        });

        // Fragment results have one listener per key; broadcast refreshes from the activity.
        manager.setFragmentResultListener(FragmentEventKeys.SETTINGS_CHANGED, this,
                (key, result) -> refreshPagesAndReminders());
        manager.setFragmentResultListener(FragmentEventKeys.COURSES_UPDATED, this,
                (key, result) -> refreshPagesAndReminders());

        maybeRequestNotificationPermission(new SettingsRepository(this).getSettings());
    }

    private Fragment fragmentForItem(int itemId) {
        if (itemId == R.id.nav_today) return todayFragment;
        if (itemId == R.id.nav_settings) return settingsFragment;
        return weekFragment;
    }

    private void switchFragment(Fragment target) {
        if (target == activeFragment) return;
        getSupportFragmentManager().beginTransaction().hide(activeFragment).show(target).commit();
        activeFragment = target;
    }

    private void refreshPages() {
        weekFragment.refreshData();
        todayFragment.refreshData();
        settingsFragment.refreshData();
    }

    private void refreshPagesAndReminders() {
        refreshPages();
        CourseReminderScheduler.syncAsync(this);
    }

    private void maybeRequestNotificationPermission(AppSettings settings) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || !settings.courseReminderEnabled
                || ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) return;
        if (getSharedPreferences(PERMISSION_PREFS, Context.MODE_PRIVATE)
                .getBoolean(KEY_PERMISSION_REQUESTED, false)) return;
        getSharedPreferences(PERMISSION_PREFS, Context.MODE_PRIVATE).edit()
                .putBoolean(KEY_PERMISSION_REQUESTED, true).apply();
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
    }

    private void applySystemBarInsets() {
        int bottomPadding = binding.bottomNav.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(binding.mainRoot, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            binding.fragmentContainer.setPadding(bars.left, bars.top, bars.right, 0);
            binding.bottomNav.setPadding(bars.left, binding.bottomNav.getPaddingTop(),
                    bars.right, bottomPadding + bars.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(binding.mainRoot);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(ACTIVE_TAB, binding.bottomNav.getSelectedItemId());
        super.onSaveInstanceState(outState);
    }
}
