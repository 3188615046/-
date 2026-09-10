package com.ncvt.kebiao.ui.common;

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import com.ncvt.kebiao.R;

public class CaptchaDialog extends DialogFragment {
    public static final String RESULT_KEY = "captcha_result";
    public static final String ACTION = "action";
    public static final String VALUE = "value";
    private static final String IMAGE = "image";
    private ImageView imageView;
    private EditText editText;
    private boolean refreshing;

    public static CaptchaDialog newInstance(byte[] bytes) {
        CaptchaDialog dialog = new CaptchaDialog();
        Bundle args = new Bundle();
        args.putByteArray(IMAGE, bytes);
        dialog.setArguments(args);
        return dialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_captcha, null, false);
        imageView = view.findViewById(R.id.iv_captcha);
        editText = view.findViewById(R.id.et_captcha);
        byte[] image = requireArguments().getByteArray(IMAGE);
        if (image != null) updateCaptchaImage(image);
        imageView.setOnClickListener(v -> {
            if (refreshing) return;
            refreshing = true;
            setRefreshing(true);
            sendResult("refresh", "");
        });
        return new AlertDialog.Builder(requireContext()).setTitle(R.string.captcha_title).setView(view)
                .setPositiveButton(R.string.captcha_confirm, null)
                .setNegativeButton(R.string.captcha_cancel, (dialog, which) -> sendResult("cancel", ""))
                .create();
    }

    @Override
    public void onStart() {
        super.onStart();
        AlertDialog dialog = (AlertDialog) requireDialog();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
            String input = editText.getText().toString().trim();
            if (input.isEmpty()) {
                editText.setError("请输入验证码");
                return;
            }
            sendResult("confirm", input);
            dismiss();
        });
        setRefreshing(refreshing);
    }

    public void updateCaptchaImage(byte[] bytes) {
        requireArguments().putByteArray(IMAGE, bytes);
        if (imageView != null) imageView.setImageBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.length));
        if (editText != null) editText.setText("");
        refreshing = false;
        setRefreshing(false);
    }

    public void setRefreshing(boolean value) {
        refreshing = value;
        if (imageView != null) {
            imageView.setEnabled(!value);
            imageView.setAlpha(value ? 0.5f : 1f);
        }
        if (getDialog() instanceof AlertDialog) {
            android.widget.Button button = ((AlertDialog) getDialog()).getButton(AlertDialog.BUTTON_POSITIVE);
            if (button != null) button.setEnabled(!value);
        }
    }

    private void sendResult(String action, String value) {
        Bundle result = new Bundle();
        result.putString(ACTION, action);
        result.putString(VALUE, value);
        getParentFragmentManager().setFragmentResult(RESULT_KEY, result);
    }

    @Override
    public void onCancel(@NonNull DialogInterface dialog) {
        sendResult("cancel", "");
        super.onCancel(dialog);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        imageView = null;
        editText = null;
    }
}
