package vn.edu.usth.taskmanagement.presentation.adapter

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import vn.edu.usth.taskmanagement.R
import vn.edu.usth.taskmanagement.domain.model.TaskModel

class HomeTaskAdapter(
    private val onTaskCheckedChange: (TaskModel, Boolean) -> Unit,
    private val onTaskClick: (TaskModel) -> Unit
) : ListAdapter<TaskModel, HomeTaskAdapter.ViewHolder>(DiffCallback()) {

    private var onItemLongClick: ((TaskModel, View) -> Unit)? = null

    fun setOnItemLongClickListener(listener: (TaskModel, View) -> Unit) {
        onItemLongClick = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_home_task, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val checkbox: View = itemView.findViewById(R.id.checkboxCircle)
        private val checkboxDone: View = itemView.findViewById(R.id.checkboxCircleDone)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvTaskTitle)
        private val tvDueDate: TextView = itemView.findViewById(R.id.tvDueDate)
        private val priorityDot: View = itemView.findViewById(R.id.priorityDot)

        fun bind(task: TaskModel) {
            tvTitle.text = task.title

            // Hiển thị due date nếu có
            if (task.dueDate != null) {
                tvDueDate.visibility = View.VISIBLE
                tvDueDate.text = formatDueDate(task.dueDate)
            } else {
                tvDueDate.visibility = View.GONE
            }

            // Toggle checkbox visual
            if (task.isDone) {
                checkbox.visibility = View.GONE
                checkboxDone.visibility = View.VISIBLE
                tvTitle.paintFlags = tvTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                tvTitle.alpha = 0.5f
            } else {
                checkbox.visibility = View.VISIBLE
                checkboxDone.visibility = View.GONE
                tvTitle.paintFlags = tvTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                tvTitle.alpha = 1.0f
            }

            // Priority dot color
            val dotColorRes = when (task.priority) {
                "URGENT" -> R.color.priority_high
                "HIGH" -> R.color.priority_high
                "MEDIUM" -> R.color.primary
                else -> R.color.outline_variant
            }
            priorityDot.backgroundTintList = itemView.context.getColorStateList(dotColorRes)

            // Click handlers
            checkbox.setOnClickListener { onTaskCheckedChange(task, true) }
            checkboxDone.setOnClickListener { onTaskCheckedChange(task, false) }
            itemView.setOnClickListener { onTaskClick(task) }
            itemView.setOnLongClickListener {
                onItemLongClick?.invoke(task, it)
                true
            }
        }
    }

    private fun formatDueDate(dateStr: String): String {
        try {
            val formats = listOf(
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd'T'HH:mm"
            )
            for (format in formats) {
                try {
                    val sdf = java.text.SimpleDateFormat(format, java.util.Locale.US)
                    if (format.contains("'Z'")) {
                        sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
                    } else {
                        sdf.timeZone = java.util.TimeZone.getDefault()
                    }
                    val date = sdf.parse(dateStr)
                    if (date != null) {
                        val outFormat = java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.US)
                        outFormat.timeZone = java.util.TimeZone.getDefault()
                        return outFormat.format(date)
                    }
                } catch (e: Exception) {}
            }
        } catch (e: Exception) {}
        return dateStr
    }

    class DiffCallback : DiffUtil.ItemCallback<TaskModel>() {
        override fun areItemsTheSame(oldItem: TaskModel, newItem: TaskModel) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: TaskModel, newItem: TaskModel) = oldItem == newItem
    }
}

