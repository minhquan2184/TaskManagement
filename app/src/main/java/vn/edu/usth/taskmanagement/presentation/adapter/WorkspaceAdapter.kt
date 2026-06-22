package vn.edu.usth.taskmanagement.presentation.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import vn.edu.usth.taskmanagement.R
import vn.edu.usth.taskmanagement.domain.model.Workspace

class WorkspaceAdapter(
    private val onWorkspaceClick: (Workspace) -> Unit
) : ListAdapter<Workspace, WorkspaceAdapter.ViewHolder>(DiffCallback()) {

    private var onItemLongClick: ((Workspace, View) -> Unit)? = null

    fun setOnItemLongClickListener(listener: (Workspace, View) -> Unit) {
        onItemLongClick = listener
    }

    // Alternate tint colors for group cards
    private val groupColors = listOf(
        R.color.primary,
        R.color.tertiary,
        R.color.tag_group_a_text,
        R.color.secondary
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_workspace, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle: TextView = itemView.findViewById(R.id.tvWorkspaceTitle)
        private val tvSubtitle: TextView = itemView.findViewById(R.id.tvWorkspaceSubtitle)
        private val progressBar: ProgressBar = itemView.findViewById(R.id.progressBar)
        private val chevron: ImageView = itemView.findViewById(R.id.ivChevron)

        fun bind(workspace: Workspace, position: Int) {
            tvTitle.text = workspace.title
            tvSubtitle.text = "${workspace.taskCount} tasks • ${workspace.memberCount} members"

            progressBar.max = 100
            progressBar.progress = workspace.progressPercent

            // Alternate progress bar tint
            val colorResId = groupColors[position % groupColors.size]
            progressBar.progressTintList = itemView.context.getColorStateList(colorResId)

            itemView.setOnClickListener { onWorkspaceClick(workspace) }
            itemView.setOnLongClickListener {
                onItemLongClick?.invoke(workspace, it)
                true
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Workspace>() {
        override fun areItemsTheSame(oldItem: Workspace, newItem: Workspace) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Workspace, newItem: Workspace) = oldItem == newItem
    }
}

