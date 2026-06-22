package vn.edu.usth.taskmanagement.presentation.shared

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import vn.edu.usth.taskmanagement.databinding.ItemTaskBinding
import vn.edu.usth.taskmanagement.domain.model.TaskModel

class TaskAdapter(
    private val onTaskCheckedChange: (TaskModel, Boolean) -> Unit,
    private val onTaskClick: (TaskModel) -> Unit
) : ListAdapter<TaskModel, TaskAdapter.TaskViewHolder>(TaskDiffCallback()) {

    private var onItemLongClick: ((TaskModel, View) -> Unit)? = null

    fun setOnItemLongClickListener(listener: (TaskModel, View) -> Unit) {
        onItemLongClick = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemTaskBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TaskViewHolder(private val binding: ItemTaskBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onTaskClick(getItem(position))
                }
            }
            binding.root.setOnLongClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemLongClick?.invoke(getItem(position), it)
                }
                true
            }
            binding.checkboxTask.setOnCheckedChangeListener { _, isChecked ->
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val task = getItem(position)
                    // Tránh gọi callback nếu view vừa tái sử dụng (recycler view rác)
                    if (task.isDone != isChecked) {
                        onTaskCheckedChange(task, isChecked)
                    }
                }
            }
        }

        fun bind(task: TaskModel) {
            binding.tvTaskTitle.text = task.title
            binding.tvTaskDescription.text = task.description ?: ""
            binding.checkboxTask.isChecked = task.isDone
            binding.tvPriority.text = task.priority
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
}

