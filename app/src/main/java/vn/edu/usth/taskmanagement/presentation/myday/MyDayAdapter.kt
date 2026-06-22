package vn.edu.usth.taskmanagement.presentation.myday

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import vn.edu.usth.taskmanagement.R
import vn.edu.usth.taskmanagement.databinding.ItemMyDayHeaderBinding
import vn.edu.usth.taskmanagement.databinding.ItemTaskBinding
import vn.edu.usth.taskmanagement.domain.model.TaskModel

class MyDayAdapter(
    private val onTaskCheckedChange: (TaskModel, Boolean) -> Unit,
    private val onTaskClick: (TaskModel) -> Unit
) : ListAdapter<MyDayItem, RecyclerView.ViewHolder>(MyDayDiffCallback()) {

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_TASK = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is MyDayItem.Header -> VIEW_TYPE_HEADER
            is MyDayItem.TaskNode -> VIEW_TYPE_TASK
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_HEADER -> {
                val binding = ItemMyDayHeaderBinding.inflate(inflater, parent, false)
                HeaderViewHolder(binding)
            }
            VIEW_TYPE_TASK -> {
                val binding = ItemTaskBinding.inflate(inflater, parent, false)
                TaskViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (holder) {
            is HeaderViewHolder -> holder.bind(item as MyDayItem.Header)
            is TaskViewHolder -> holder.bind((item as MyDayItem.TaskNode).task)
        }
    }

    inner class HeaderViewHolder(private val binding: ItemMyDayHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(header: MyDayItem.Header) {
            binding.tvTitle.text = if (header.isPersonal) "PERSONAL" else header.title.uppercase()
            binding.tvCount.text = header.taskCount.toString()
            
            val context = binding.root.context
            if (header.isPersonal) {
                binding.tvTitle.setTextColor(context.getColor(R.color.primary))
                binding.ivIcon.visibility = android.view.View.GONE
                binding.tvCount.background = context.getDrawable(R.drawable.bg_tag_personal)
                binding.tvCount.setTextColor(context.getColor(R.color.tag_personal_text))
            } else {
                binding.tvTitle.setTextColor(context.getColor(R.color.tag_group_a_text)) // generic group color
                binding.ivIcon.visibility = android.view.View.VISIBLE
                binding.tvCount.background = context.getDrawable(R.drawable.bg_section_header)
                binding.tvCount.setTextColor(context.getColor(R.color.on_surface_variant))
            }
        }
    }

    inner class TaskViewHolder(private val binding: ItemTaskBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val item = getItem(position) as? MyDayItem.TaskNode
                    if (item != null) {
                        onTaskClick(item.task)
                    }
                }
            }
            binding.checkboxTask.setOnCheckedChangeListener { _, isChecked ->
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val item = getItem(position) as? MyDayItem.TaskNode
                    if (item != null && item.task.isDone != isChecked) {
                        onTaskCheckedChange(item.task, isChecked)
                    }
                }
            }
        }

        fun bind(task: TaskModel) {
            binding.tvTaskTitle.text = task.title
            binding.tvTaskDescription.text = task.description ?: ""
            binding.tvTaskDescription.visibility = if (task.description.isNullOrEmpty()) android.view.View.GONE else android.view.View.VISIBLE
            
            // Temporary remove the listener to prevent triggering it during binding
            binding.checkboxTask.setOnCheckedChangeListener(null)
            binding.checkboxTask.isChecked = task.isDone
            binding.checkboxTask.setOnCheckedChangeListener { _, isChecked ->
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val item = getItem(position) as? MyDayItem.TaskNode
                    if (item != null && item.task.isDone != isChecked) {
                        onTaskCheckedChange(item.task, isChecked)
                    }
                }
            }
            
            binding.tvPriority.text = task.priority
        }
    }

    class MyDayDiffCallback : DiffUtil.ItemCallback<MyDayItem>() {
        override fun areItemsTheSame(oldItem: MyDayItem, newItem: MyDayItem): Boolean {
            if (oldItem is MyDayItem.Header && newItem is MyDayItem.Header) {
                return oldItem.workspaceId == newItem.workspaceId
            }
            if (oldItem is MyDayItem.TaskNode && newItem is MyDayItem.TaskNode) {
                return oldItem.task.id == newItem.task.id
            }
            return false
        }

        override fun areContentsTheSame(oldItem: MyDayItem, newItem: MyDayItem): Boolean {
            return oldItem == newItem
        }
    }
}
