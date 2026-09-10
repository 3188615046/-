package com.ncvt.kebiao.ui.timetable;

import android.app.Dialog;
import android.os.Bundle;
import android.text.InputType;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class WeekPickerDialog extends DialogFragment {
    public static final String RESULT_KEY = "week_selected";
    public static final String WEEK = "week";

    public static WeekPickerDialog newInstance(int currentWeek) {
        WeekPickerDialog dialog = new WeekPickerDialog();
        Bundle arguments = new Bundle();
        arguments.putInt(WEEK, currentWeek);
        dialog.setArguments(arguments);
        return dialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        int week = Math.max(1, Math.min(25, requireArguments().getInt(WEEK, 1)));
        int padding = (int) (20 * getResources().getDisplayMetrics().density);
        NumberPicker picker = new NumberPicker(requireContext());
        picker.setMinValue(1);
        picker.setMaxValue(25);
        picker.setValue(week);
        picker.setWrapSelectorWheel(false);
        TextInputEditText input = new TextInputEditText(requireContext());
        input.setText(Integer.toString(week));
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setSelectAllOnFocus(true);
        TextInputLayout inputLayout = new TextInputLayout(requireContext());
        inputLayout.setHint("直接输入周次");
        inputLayout.addView(input, new LinearLayout.LayoutParams(-1, -2));
        LinearLayout container = new LinearLayout(requireContext());
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(padding, padding, padding, 0);
        container.addView(picker, new LinearLayout.LayoutParams(-1, -2));
        container.addView(inputLayout, new LinearLayout.LayoutParams(-1, -2));
        picker.setOnValueChangedListener((view, oldValue, newValue) -> input.setText(Integer.toString(newValue)));
        return new AlertDialog.Builder(requireContext()).setTitle("选择周次").setView(container)
                .setPositiveButton("确定", (dialog, which) -> {
                    int selected = picker.getValue();
                    try { selected = Integer.parseInt(input.getText().toString().trim()); }
                    catch (NumberFormatException ignored) { /* Use the picker value. */ }
                    Bundle result = new Bundle();
                    result.putInt(WEEK, Math.max(1, Math.min(25, selected)));
                    getParentFragmentManager().setFragmentResult(RESULT_KEY, result);
                }).setNegativeButton("取消", null).create();
    }
}
