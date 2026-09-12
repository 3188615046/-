package com.ncvt.kebiao.ui.settings;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.snackbar.Snackbar;
import com.ncvt.kebiao.R;
import com.ncvt.kebiao.data.remote.ScrapeResult;
import com.ncvt.kebiao.databinding.FragmentSettingsBinding;
import com.ncvt.kebiao.model.AppSettings;
import com.ncvt.kebiao.ui.common.FragmentEventKeys;
import com.ncvt.kebiao.viewmodel.SettingsViewModel;

public class SettingsFragment extends Fragment {
    private FragmentSettingsBinding binding;
    private SettingsViewModel viewModel;
    private boolean advancedVisible;
    private AppSettings currentSettings = new AppSettings();
    private final ActivityResultLauncher<String[]> jsonPicker = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(), this::importJson);
    private final ActivityResultLauncher<String> notificationPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), granted -> {});

    private void importJson(Uri uri) {
        if (uri != null) {
            if (viewModel == null) viewModel = new ViewModelProvider(this).get(SettingsViewModel.class);
            viewModel.importPyqtJson(uri);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle state) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle state) {
        super.onViewCreated(view, state);
        viewModel = new ViewModelProvider(this).get(SettingsViewModel.class);
        viewModel.getSettings().observe(getViewLifecycleOwner(), this::fillForm);
        viewModel.getSaveResult().observe(getViewLifecycleOwner(), event -> {
            if (Boolean.TRUE.equals(event.getContentIfNotHandled())) {
                getParentFragmentManager().setFragmentResult(FragmentEventKeys.SETTINGS_CHANGED, new Bundle());
                Snackbar.make(binding.getRoot(), R.string.settings_saved, Snackbar.LENGTH_SHORT).show();
            }
        });
        viewModel.getImportResult().observe(getViewLifecycleOwner(), event -> {
            ScrapeResult result = event.getContentIfNotHandled();
            if (result == null) return;
            String message;
            if (result instanceof ScrapeResult.CoursesFetched) {
                getParentFragmentManager().setFragmentResult(FragmentEventKeys.COURSES_UPDATED, new Bundle());
                message = "JSON 导入成功，共 " + ((ScrapeResult.CoursesFetched) result).courses.size() + " 门课程";
            } else {
                message = result instanceof ScrapeResult.Error ? ((ScrapeResult.Error) result).message : "JSON 导入失败";
            }
            Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG).show();
        });
        viewModel.getImporting().observe(getViewLifecycleOwner(), importing ->
                binding.btnImportJson.setEnabled(!Boolean.TRUE.equals(importing)));
        binding.btnAdvancedToggle.setOnClickListener(v -> setAdvancedVisible(!advancedVisible));
        binding.btnImportJson.setOnClickListener(v -> jsonPicker.launch(
                new String[]{"application/json", "text/plain", "*/*"}));
        binding.btnSave.setOnClickListener(v -> viewModel.saveSettings(collectFormData()));
        binding.switchCourseReminder.setOnCheckedChangeListener((button, checked) -> {
            if (checked && button.isPressed()) requestNotificationPermission();
        });
    }

    public void refreshData() {
        if (viewModel != null) viewModel.loadSettings();
    }

    private void fillForm(AppSettings settings) {
        currentSettings = settings;
        binding.etEduUrl.setText(settings.eduUrl);
        binding.etAccount.setText(settings.account);
        binding.etPassword.setText(settings.password);
        binding.etAcademicYear.setText(settings.academicYear);
        binding.etSemester.setText(settings.semester);
        binding.etMenuCode.setText(settings.menuCode);
        binding.etSemesterStart.setText(settings.semesterStartDate);
        binding.switchCourseReminder.setChecked(settings.courseReminderEnabled);
        setAdvancedVisible(!settings.eduUrl.equals(AppSettings.DEFAULT_EDU_URL)
                || !settings.academicYear.isEmpty() || !settings.semester.isEmpty()
                || !settings.menuCode.isEmpty() || !settings.semesterStartDate.isEmpty());
    }

    private AppSettings collectFormData() {
        AppSettings.Builder builder = currentSettings.toBuilder();
        String url = binding.etEduUrl.getText().toString().trim();
        builder.eduUrl = url.isEmpty() ? AppSettings.DEFAULT_EDU_URL : url;
        builder.account = binding.etAccount.getText().toString().trim();
        builder.password = binding.etPassword.getText().toString();
        builder.academicYear = binding.etAcademicYear.getText().toString().trim();
        builder.semester = binding.etSemester.getText().toString().trim();
        builder.menuCode = binding.etMenuCode.getText().toString().trim();
        builder.semesterStartDate = binding.etSemesterStart.getText().toString().trim();
        builder.courseReminderEnabled = binding.switchCourseReminder.isChecked();
        return builder.build();
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    private void setAdvancedVisible(boolean visible) {
        advancedVisible = visible;
        binding.layoutAdvanced.setVisibility(visible ? View.VISIBLE : View.GONE);
        binding.btnAdvancedToggle.setText(visible ? R.string.hide_advanced_settings : R.string.show_advanced_settings);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
