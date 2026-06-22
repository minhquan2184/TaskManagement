package vn.edu.usth.taskmanagement.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workspaces")
data class WorkspaceEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String? = null,
    val isPersonal: Boolean = false,
    val ownerId: String,
    val memberCount: Int = 0,
    val taskCount: Int = 0,
    val completedTaskCount: Int = 0,
    val progressPercent: Int = 0
)

