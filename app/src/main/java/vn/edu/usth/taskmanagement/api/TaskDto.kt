package vn.edu.usth.taskmanagement.api

import kotlinx.serialization.Serializable

@Serializable
data class TaskDto(
    val id: String,
    val workspaceId: String,
    val createdBy: String,
    val assigneeId: String? = null,
    val title: String,
    val description: String? = null,
    val status: String = "TODO",
    val priority: String = "MEDIUM",
    val dueDate: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

