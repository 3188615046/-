package com.ncvt.kebiao.ui.fortune;

import android.app.Dialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.ncvt.kebiao.R;
import com.ncvt.kebiao.data.repository.FortuneRepository;
import com.ncvt.kebiao.databinding.DialogDailyFortuneBinding;
import com.ncvt.kebiao.model.DailyFortune;
import com.ncvt.kebiao.ui.common.FragmentEventKeys;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class DailyFortuneDialogFragment extends DialogFragment {
    private DialogDailyFortuneBinding binding;
    private FortuneRepository repository;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle state) {
        binding = DialogDailyFortuneBinding.inflate(LayoutInflater.from(requireContext()));
        repository = new FortuneRepository(requireContext());
        bindFortuneState(repository.getTodayFortune());
        Dialog dialog = new MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_KeBiao_DetailDialog)
                .setView(binding.getRoot()).create();
        binding.getRoot().setOnClickListener(v -> dismiss());
        binding.cardContainer.setOnClickListener(v -> { /* Consume taps inside the panel. */ });
        binding.btnClose.setOnClickListener(v -> dismiss());
        binding.btnDraw.setOnClickListener(v -> drawTodayFortune());
        dialog.setCanceledOnTouchOutside(true);
        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }
        bindFortuneState(repository.getTodayFortune());
    }

    private void drawTodayFortune() {
        DailyFortune existing = repository.getTodayFortune();
        if (existing != null) {
            bindFortuneState(existing);
            return;
        }
        bindFortuneState(repository.drawTodayFortune());
        getParentFragmentManager().setFragmentResult(FragmentEventKeys.DAILY_FORTUNE_UPDATED, new Bundle());
        Snackbar.make(binding.getRoot(), "今日签文已保存，今天内可以随时回来查看", Snackbar.LENGTH_SHORT).show();
    }

    private void bindFortuneState(DailyFortune fortune) {
        binding.tvDate.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy年M月d日  EEEE", Locale.CHINESE)));
        int accent;
        if (fortune == null) {
            accent = 0xFFD96C49;
            binding.tvStatus.setText("每天只能抽一次，抽过之后今天内都可以反复查看");
            binding.tvFortuneLevel.setText("待抽签");
            binding.tvFortuneTitle.setText("把今天最想知道的一件事放在心里");
            binding.tvFortunePoem.setText("静心片刻，再抽取属于今天的那支签。");
            binding.tvFortuneSummary.setText("签文会保存到今天结束，明天会自动恢复为可重新抽签。");
            binding.tvFortuneGoodFor.setText("抽签说明：每天只会产生一支新的签文，不可重复抽取。");
            binding.tvFortuneLuckyFocus.setText("查看方式：今天再次打开时，会直接显示同一支签。");
            binding.tvFortuneReminder.setText("小提醒：把它当作一天的小仪式就好，保持轻松会更有意思。");
            binding.btnDraw.setEnabled(true);
            binding.btnDraw.setText("抽取今日签文");
        } else {
            accent = colorForLevel(fortune.level);
            binding.tvStatus.setText("今日已经抽过签了，结果会保留到明天刷新");
            binding.tvFortuneLevel.setText(fortune.level);
            binding.tvFortuneTitle.setText(fortune.title);
            binding.tvFortunePoem.setText(fortune.poem);
            binding.tvFortuneSummary.setText(fortune.summary);
            binding.tvFortuneGoodFor.setText(fortune.goodFor);
            binding.tvFortuneLuckyFocus.setText(fortune.luckyFocus);
            binding.tvFortuneReminder.setText(fortune.reminder);
            binding.btnDraw.setEnabled(false);
            binding.btnDraw.setText("今日已抽，仅可查看");
        }
        binding.viewFortuneAccent.setBackgroundColor(accent);
        binding.tvFortuneLevel.setTextColor(accent);
        binding.tvFortuneLevel.setBackgroundTintList(ColorStateList.valueOf(Color.argb(
                (int) (255 * (fortune == null ? 0.14f : 0.16f)), Color.red(accent), Color.green(accent), Color.blue(accent))));
    }

    private int colorForLevel(String level) {
        switch (level) {
            case "大吉": return 0xFFD95C40;
            case "吉": return 0xFFE08A2E;
            case "中吉": return 0xFF358B72;
            case "小吉": return 0xFF4E7DD6;
            case "末吉": return 0xFF7B6DD1;
            case "小凶": return 0xFFA85F73;
            default: return 0xFF6E7C91;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
