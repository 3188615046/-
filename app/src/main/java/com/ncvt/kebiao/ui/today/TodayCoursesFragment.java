package com.ncvt.kebiao.ui.today;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.ncvt.kebiao.databinding.FragmentTodayCoursesBinding;
import com.ncvt.kebiao.viewmodel.TodayCoursesViewModel;

public class TodayCoursesFragment extends Fragment {
    private FragmentTodayCoursesBinding binding;
    private TodayCoursesViewModel viewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle state) {
        binding = FragmentTodayCoursesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle state) {
        super.onViewCreated(view, state);
        viewModel = new ViewModelProvider(this).get(TodayCoursesViewModel.class);
        TodayCourseAdapter adapter = new TodayCourseAdapter();
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerView.setAdapter(adapter);
        viewModel.getDateDisplay().observe(getViewLifecycleOwner(), binding.tvDate::setText);
        viewModel.getTodayCourses().observe(getViewLifecycleOwner(), adapter::submitList);
        viewModel.getIsEmpty().observe(getViewLifecycleOwner(), empty -> {
            binding.tvEmpty.setVisibility(Boolean.TRUE.equals(empty) ? View.VISIBLE : View.GONE);
            binding.recyclerView.setVisibility(Boolean.TRUE.equals(empty) ? View.GONE : View.VISIBLE);
        });
    }

    public void refreshData() {
        if (viewModel != null) viewModel.loadTodayCourses();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshData();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) refreshData();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding.recyclerView.setAdapter(null);
        binding = null;
    }
}
