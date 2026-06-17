package vn.edu.usth.taskmanagement.domain.model

data class TaskModel(
    val id: String,
    val workspaceId: String,
    val createdBy: String,
    val assigneeId: String? = null,
    val title: String,
    val description: String? = null,
    val status: String = "TODO",
    val priority: String = "MEDIUM",
    val dueDate: String? = null,
    val calendarEventId: String? = null
) {
    val isDone: Boolean get() = status == "DONE"
    val isInProgress: Boolean get() = status == "IN_PROGRESS"
}

