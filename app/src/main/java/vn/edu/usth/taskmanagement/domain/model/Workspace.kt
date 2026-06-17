package vn.edu.usth.taskmanagement.domain.model

data class Workspace(
    val id: String,
    val title: String,
    val description: String? = null,
    val isPersonal: Boolean = false,
    val memberCount: Int = 0,
    val taskCount: Int = 0,
    val completedTaskCount: Int = 0,
    val progressPercent: Int = 0
)

