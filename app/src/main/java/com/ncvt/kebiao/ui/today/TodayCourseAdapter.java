package com.ncvt.kebiao.ui.today;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.ncvt.kebiao.databinding.ItemTodayCourseBinding;
import com.ncvt.kebiao.model.Course;
import com.ncvt.kebiao.util.CourseDisplayUtils;

public class TodayCourseAdapter extends ListAdapter<Course, TodayCourseAdapter.ViewHolder> {
    public TodayCourseAdapter() {
        super(new DiffUtil.ItemCallback<Course>() {
            @Override
            public boolean areItemsTheSame(@NonNull Course oldItem, @NonNull Course newItem) {
                return oldItem.id == newItem.id;
            }
            @Override
            public boolean areContentsTheSame(@NonNull Course oldItem, @NonNull Course newItem) {
                return oldItem.equals(newItem);
            }
        });
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemTodayCourseBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemTodayCourseBinding binding;

        ViewHolder(ItemTodayCourseBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Course course) {
            binding.colorBar.setBackgroundColor(course.color);
            binding.tvCourseName.setText(course.name);
            binding.tvTimePeriod.setText(CourseDisplayUtils.periodLabel(course));
            binding.tvClassroom.setText(course.classroom);
            binding.tvTeacher.setText(course.teacher);
            binding.tvTeacher.setVisibility(course.teacher.trim().isEmpty() ? View.GONE : View.VISIBLE);
        }
    }
}
