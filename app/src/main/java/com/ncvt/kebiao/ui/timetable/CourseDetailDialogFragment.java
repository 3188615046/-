package com.ncvt.kebiao.ui.timetable;

import android.app.Dialog;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.ncvt.kebiao.R;
import com.ncvt.kebiao.databinding.DialogCourseDetailBinding;
import com.ncvt.kebiao.model.Course;
import com.ncvt.kebiao.util.CourseDisplayUtils;

public class CourseDetailDialogFragment extends DialogFragment {
    private static final String ARG_COURSE = "course";
    private DialogCourseDetailBinding binding;

    public static CourseDetailDialogFragment newInstance(Course course) {
        CourseDetailDialogFragment fragment = new CourseDetailDialogFragment();
        Bundle arguments = new Bundle();
        arguments.putSerializable(ARG_COURSE, course);
        fragment.setArguments(arguments);
        return fragment;
    }

    @NonNull
    @Override
    @SuppressWarnings("deprecation")
    public Dialog onCreateDialog(Bundle state) {
        binding = DialogCourseDetailBinding.inflate(LayoutInflater.from(requireContext()));
        Course course = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? requireArguments().getSerializable(ARG_COURSE, Course.class)
                : (Course) requireArguments().getSerializable(ARG_COURSE);
        if (course == null) throw new IllegalArgumentException("Course is required");
        binding.tvCourseName.setText(course.name);
        binding.tvWeekPattern.setText(getString(R.string.course_detail_weeks,
                course.weekPattern.trim().isEmpty() ? "全周" : course.weekPattern));
        binding.tvSchedule.setText(getString(R.string.course_detail_schedule,
                CourseDisplayUtils.dayOfWeekLabel(course.dayOfWeek), CourseDisplayUtils.periodLabel(course),
                CourseDisplayUtils.timeRangeLabel(course)));
        binding.tvClassroom.setText(getString(R.string.course_detail_classroom,
                course.classroom.trim().isEmpty() ? "未填写" : course.classroom));
        binding.tvTeacher.setText(course.teacher.trim().isEmpty() ? getString(R.string.course_detail_no_teacher)
                : getString(R.string.course_detail_teacher, course.teacher));
        Dialog dialog = new MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_KeBiao_DetailDialog)
                .setView(binding.getRoot()).create();
        binding.getRoot().setOnClickListener(v -> dismiss());
        binding.cardContainer.setOnClickListener(v -> { /* Consume taps inside the panel. */ });
        binding.btnClose.setOnClickListener(v -> dismiss());
        binding.btnEdit.setOnClickListener(v -> Snackbar.make(binding.getRoot(),
                R.string.course_detail_editing_soon, Snackbar.LENGTH_SHORT).show());
        dialog.setCanceledOnTouchOutside(true);
        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
