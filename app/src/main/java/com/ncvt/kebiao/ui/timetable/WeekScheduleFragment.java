package com.ncvt.kebiao.ui.timetable;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.snackbar.Snackbar;
import com.ncvt.kebiao.R;
import com.ncvt.kebiao.data.repository.FortuneRepository;
import com.ncvt.kebiao.databinding.FragmentWeekScheduleBinding;
import com.ncvt.kebiao.model.AppSettings;
import com.ncvt.kebiao.model.DailyFortune;
import com.ncvt.kebiao.ui.common.CaptchaDialog;
import com.ncvt.kebiao.ui.common.FragmentEventKeys;
import com.ncvt.kebiao.ui.fortune.DailyFortuneDialogFragment;
import com.ncvt.kebiao.util.CourseDisplayUtils;
import com.ncvt.kebiao.util.WeekCalculator;
import com.ncvt.kebiao.viewmodel.ScrapeState;
import com.ncvt.kebiao.viewmodel.WeekScheduleViewModel;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;

public class WeekScheduleFragment extends Fragment {
    private FragmentWeekScheduleBinding binding;
    private WeekScheduleViewModel viewModel;
    private FortuneRepository fortuneRepository;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle state) {
        binding = FragmentWeekScheduleBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle state) {
        super.onViewCreated(view, state);
        viewModel = new ViewModelProvider(this).get(WeekScheduleViewModel.class);
        fortuneRepository = new FortuneRepository(requireContext());
        binding.timetableView.setOnCourseClick(course -> {
            if (getChildFragmentManager().findFragmentByTag("CourseDetail") == null) {
                CourseDetailDialogFragment.newInstance(course).show(getChildFragmentManager(), "CourseDetail");
            }
        });
        binding.timetableView.setOnWeekSwipe(delta -> {
            if (delta > 0) viewModel.nextWeek();
            else if (delta < 0) viewModel.previousWeek();
        });
        getChildFragmentManager().setFragmentResultListener(WeekPickerDialog.RESULT_KEY, getViewLifecycleOwner(),
                (key, result) -> viewModel.switchToWeek(result.getInt(WeekPickerDialog.WEEK, 1)));
        getChildFragmentManager().setFragmentResultListener(CaptchaDialog.RESULT_KEY, getViewLifecycleOwner(),
                (key, result) -> {
                    String action = result.getString(CaptchaDialog.ACTION, "");
                    if (action.equals("confirm")) viewModel.submitCaptchaAndFetch(result.getString(CaptchaDialog.VALUE, ""));
                    else if (action.equals("refresh")) viewModel.refreshCaptcha();
                    else if (action.equals("cancel")) viewModel.cancelUpdate();
                });
        getChildFragmentManager().setFragmentResultListener(FragmentEventKeys.DAILY_FORTUNE_UPDATED,
                getViewLifecycleOwner(), (key, result) -> refreshDailyFortuneStatus());
        setupObservers();
        setupClickListeners();
        refreshDailyFortuneStatus();
    }

    private int selectedWeek() {
        Integer week = viewModel.getCurrentWeek().getValue();
        return week == null ? 1 : week;
    }

    private void setupObservers() {
        viewModel.getCurrentWeek().observe(getViewLifecycleOwner(), week -> {
            binding.tvWeekNumber.setText(getString(R.string.week_with_arrow, week));
            renderWeekDates(week);
            updateWeekStatusUi(week);
        });
        viewModel.getSettings().observe(getViewLifecycleOwner(), settings -> {
            binding.timetableView.setDeepCourseCardEnabled(settings.deepCourseCardEnabled);
            binding.switchShowNonWeek.setOnCheckedChangeListener(null);
            binding.switchShowNonWeek.setChecked(settings.showNonCurrentWeekCourses);
            binding.switchShowNonWeek.setOnCheckedChangeListener((button, checked) ->
                    viewModel.setShowNonCurrentWeekCourses(checked));
            binding.switchNightMode.setOnCheckedChangeListener(null);
            binding.switchNightMode.setChecked(settings.nightModeEnabled);
            binding.switchNightMode.setOnCheckedChangeListener((button, checked) -> viewModel.setNightModeEnabled(checked));
            binding.switchDeepCard.setOnCheckedChangeListener(null);
            binding.switchDeepCard.setChecked(settings.deepCourseCardEnabled);
            binding.switchDeepCard.setOnCheckedChangeListener((button, checked) -> viewModel.setDeepCourseCardEnabled(checked));
            binding.tvSemesterSummary.setText(settings.academicYear.isEmpty() ? "" : getString(R.string.semester_line,
                    settings.academicYear, CourseDisplayUtils.semesterDisplay(settings.semester)));
            updateWeekStatusUi(selectedWeek());
            renderWeekDates(selectedWeek());
        });
        viewModel.getWeekCourses().observe(getViewLifecycleOwner(), courses ->
                binding.timetableView.setCourses(courses, selectedWeek()));
        viewModel.getScrapeState().observe(getViewLifecycleOwner(), this::handleScrapeState);
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            boolean busy = Boolean.TRUE.equals(loading);
            binding.btnUpdate.setIconResource(busy ? R.drawable.ic_calendar_outline : R.drawable.ic_add);
            binding.btnUpdate.setEnabled(!busy);
            binding.btnUpdate.setContentDescription(getString(busy ? R.string.updating : R.string.update_courses));
        });
    }

    private void setupClickListeners() {
        binding.btnUpdate.setOnClickListener(v -> viewModel.startUpdateCourses());
        binding.fabCurrentWeek.setOnClickListener(v -> viewModel.goToCurrentWeek());
        binding.tvWeekNumber.setOnClickListener(v -> {
            if (getChildFragmentManager().findFragmentByTag("WeekPicker") == null) {
                WeekPickerDialog.newInstance(selectedWeek()).show(getChildFragmentManager(), "WeekPicker");
            }
        });
        binding.btnCalendarPlaceholder.setOnClickListener(v -> binding.tvWeekNumber.performClick());
        binding.btnMenuPlaceholder.setOnClickListener(v -> binding.drawerLayout.openDrawer(GravityCompat.START));
        binding.layoutToggleNonWeek.setOnClickListener(v -> binding.switchShowNonWeek.toggle());
        binding.layoutToggleNightMode.setOnClickListener(v -> binding.switchNightMode.toggle());
        binding.layoutToggleDeepCard.setOnClickListener(v -> binding.switchDeepCard.toggle());
        binding.layoutDailyFortune.setOnClickListener(v -> {
            binding.drawerLayout.closeDrawer(GravityCompat.START);
            binding.drawerLayout.post(() -> {
                if (!isAdded() || getChildFragmentManager().isStateSaved()
                        || getChildFragmentManager().findFragmentByTag("DailyFortune") != null) return;
                new DailyFortuneDialogFragment().show(getChildFragmentManager(), "DailyFortune");
            });
        });
        binding.btnSharePlaceholder.setOnClickListener(v ->
                Snackbar.make(binding.getRoot(), "分享/导出入口后续再补上", Snackbar.LENGTH_SHORT).show());
    }

    private void updateWeekStatusUi(int selectedWeek) {
        AppSettings settings = viewModel.getSettings().getValue();
        if (settings == null) return;
        LocalDate start = WeekCalculator.resolveSemesterStartDate(
                settings.semesterStartDate, settings.academicYear, settings.semester);
        int current = start == null ? 1 : WeekCalculator.getCurrentWeek(start);
        boolean isCurrent = selectedWeek == current;
        binding.tvWeekNumber.setTextColor(ContextCompat.getColor(requireContext(),
                isCurrent ? R.color.text_primary : R.color.accent));
        binding.fabCurrentWeek.setVisibility(isCurrent ? View.GONE : View.VISIBLE);
    }

    private void renderWeekDates(int week) {
        AppSettings settings = viewModel.getSettings().getValue();
        if (settings == null) return;
        LocalDate today = LocalDate.now();
        LocalDate start = WeekCalculator.resolveSemesterStartDate(
                settings.semesterStartDate, settings.academicYear, settings.semester);
        if (start == null) start = today.minusDays(today.getDayOfWeek().getValue() - 1L);
        LocalDate weekStart = WeekCalculator.getWeekStartDate(start, week);
        binding.layoutWeekDates.removeAllViews();
        TextView month = new TextView(requireContext());
        LinearLayout.LayoutParams monthParams = new LinearLayout.LayoutParams(dp(32), -1);
        monthParams.setMarginEnd(dp(2));
        month.setLayoutParams(monthParams);
        month.setGravity(Gravity.CENTER);
        month.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        month.setTextSize(13);
        month.setText(getString(R.string.month_label, weekStart.getMonthValue()));
        binding.layoutWeekDates.addView(month);
        for (int day = 0; day < 7; day++) {
            LocalDate date = weekStart.plusDays(day);
            binding.layoutWeekDates.addView(createDateView(date, today.equals(date)));
        }
    }

    private View createDateView(LocalDate date, boolean today) {
        LinearLayout column = new LinearLayout(requireContext());
        column.setOrientation(LinearLayout.VERTICAL);
        column.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(48), 1);
        params.setMarginEnd(dp(3));
        column.setLayoutParams(params);
        column.setBackgroundResource(today ? R.drawable.bg_week_day_selected : R.drawable.bg_week_day_outline);
        TextView dayLabel = new TextView(requireContext());
        dayLabel.setText(date.getDayOfWeek().getDisplayName(TextStyle.NARROW, Locale.CHINESE));
        dayLabel.setTextSize(15);
        dayLabel.setGravity(Gravity.CENTER);
        dayLabel.setTextColor(ContextCompat.getColor(requireContext(), today ? R.color.text_inverse : R.color.text_primary));
        column.addView(dayLabel);
        TextView dateLabel = new TextView(requireContext());
        dateLabel.setText(getString(R.string.date_number, date.getDayOfMonth()));
        dateLabel.setTextSize(13);
        dateLabel.setGravity(Gravity.CENTER);
        dateLabel.setTextColor(ContextCompat.getColor(requireContext(), today ? R.color.text_inverse : R.color.text_secondary));
        column.addView(dateLabel);
        return column;
    }

    private void refreshDailyFortuneStatus() {
        if (binding == null || fortuneRepository == null) return;
        DailyFortune fortune = fortuneRepository.getTodayFortune();
        binding.tvDailyFortuneSummary.setText(fortune == null
                ? "今天还没抽签，点这里抽取属于你的今日运势"
                : "今日已抽到「" + fortune.level + "」，点这里查看完整签文");
        binding.tvDailyFortuneBadge.setText(fortune == null ? "去抽签" : fortune.level);
        binding.tvDailyFortuneBadge.setBackgroundResource(fortune == null
                ? R.drawable.bg_fortune_badge_pending : R.drawable.bg_fortune_badge_done);
        binding.tvDailyFortuneBadge.setTextColor(ContextCompat.getColor(requireContext(),
                fortune == null ? R.color.accent : R.color.primary));
    }

    private void handleScrapeState(ScrapeState state) {
        CaptchaDialog captcha = (CaptchaDialog) getChildFragmentManager().findFragmentByTag("Captcha");
        if (state instanceof ScrapeState.CaptchaRequired) {
            byte[] bytes = ((ScrapeState.CaptchaRequired) state).captchaBytes;
            if (captcha != null) captcha.updateCaptchaImage(bytes);
            else if (!getChildFragmentManager().isStateSaved()) {
                CaptchaDialog.newInstance(bytes).show(getChildFragmentManager(), "Captcha");
            }
        } else if (state instanceof ScrapeState.Loading) {
            if (captcha != null) captcha.setRefreshing(true);
        } else if (state instanceof ScrapeState.Success) {
            if (captcha != null) captcha.dismiss();
            getParentFragmentManager().setFragmentResult(FragmentEventKeys.COURSES_UPDATED, new Bundle());
            Snackbar.make(binding.getRoot(), ((ScrapeState.Success) state).message, Snackbar.LENGTH_LONG).show();
            viewModel.consumeMessage();
        } else if (state instanceof ScrapeState.Error) {
            if (captcha != null) captcha.dismiss();
            String message = ((ScrapeState.Error) state).message;
            if (message.toLowerCase(Locale.ROOT).contains("failed to connect to jw.sub.ncvt.net")
                    || message.toUpperCase(Locale.ROOT).contains("ECONNREFUSED")) {
                message = "当前网络无法连接教务服务器，请先切换到能打开教务网页的网络后再导入";
            }
            Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG)
                    .setAction("重试", v -> viewModel.startUpdateCourses()).show();
            viewModel.consumeMessage();
        }
    }

    public void refreshData() {
        if (viewModel != null) viewModel.loadSettingsAndCourses();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshDailyFortuneStatus();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) refreshDailyFortuneStatus();
    }

    private int dp(int value) { return (int) (value * getResources().getDisplayMetrics().density); }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding.timetableView.setOnCourseClick(null);
        binding.timetableView.setOnWeekSwipe(null);
        binding = null;
    }
}
