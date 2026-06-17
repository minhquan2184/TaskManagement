package vn.edu.usth.taskmanagement.service

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String,
    val workspaceId: String,
    val createdBy: String,
    val assigneeId: String? = null,
    val title: String,
    val description: String? = null,
    val status: String = "TODO",
    val priority: String = "MEDIUM",
    val dueDate: String? = null,
    val calendarEventId: String? = null
)

