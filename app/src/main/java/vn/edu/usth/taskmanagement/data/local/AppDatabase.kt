package vn.edu.usth.taskmanagement.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import vn.edu.usth.taskmanagement.data.local.TaskDao
import vn.edu.usth.taskmanagement.data.local.WorkspaceDao
import vn.edu.usth.taskmanagement.data.local.TaskEntity
import vn.edu.usth.taskmanagement.data.local.WorkspaceEntity

@Database(
    entities = [WorkspaceEntity::class, TaskEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun workspaceDao(): WorkspaceDao
    abstract fun taskDao(): TaskDao
}

