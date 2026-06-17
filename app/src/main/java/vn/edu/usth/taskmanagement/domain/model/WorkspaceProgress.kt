package vn.edu.usth.taskmanagement.domain.model

data class WorkspaceProgress(
    val totalTasks: Int = 0,
    val completedTasks: Int = 0,
    val progressPercent: Int = 0,
    val updatedAt: Long = 0L
)
