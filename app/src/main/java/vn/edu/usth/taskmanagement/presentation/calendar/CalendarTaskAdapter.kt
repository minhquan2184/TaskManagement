package vn.edu.usth.taskmanagement.presentation.calendar

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import vn.edu.usth.taskmanagement.databinding.ItemTaskBinding
import vn.edu.usth.taskmanagement.domain.model.TaskModel

class CalendarTaskAdapter(
    private val onTaskChecked: (TaskModel, Boolean) -> Unit
) : ListAdapter<TaskModel, CalendarTaskAdapter.TaskViewHolder>(TaskDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TaskViewHolder(private val binding: ItemTaskBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(task: TaskModel) {
            binding.tvTaskTitle.text = task.title
            
            if (task.description.isNullOrBlank()) {
                binding.tvTaskDescription.visibility = View.GONE
            } else {
                binding.tvTaskDescription.visibility = View.VISIBLE
                binding.tvTaskDescription.text = task.description
            }

            binding.tvPriority.text = task.priority
            
            // Checkbox logic
            binding.checkboxTask.setOnCheckedChangeListener(null)
            binding.checkboxTask.isChecked = task.isDone
            
            updateStrikeThrough(task.isDone)

            binding.checkboxTask.setOnCheckedChangeListener { _, isChecked ->
                updateStrikeThrough(isChecked)
                onTaskChecked(task, isChecked)
            }
        }

        private fun updateStrikeThrough(isDone: Boolean) {
            if (isDone) {
                binding.tvTaskTitle.paintFlags = binding.tvTaskTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                binding.tvTaskTitle.paintFlags = binding.tvTaskTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }
        }
    }
}

class TaskDiffCallback : DiffUtil.ItemCallback<TaskModel>() {
    override fun areItemsTheSame(oldItem: TaskModel, newItem: TaskModel): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: TaskModel, newItem: TaskModel): Boolean {
        return oldItem == newItem
    }
}

