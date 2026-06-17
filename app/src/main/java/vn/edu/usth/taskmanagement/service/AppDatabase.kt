package vn.edu.usth.taskmanagement.service

import androidx.room.Database
import androidx.room.RoomDatabase
import vn.edu.usth.taskmanagement.service.TaskDao
import vn.edu.usth.taskmanagement.service.WorkspaceDao
import vn.edu.usth.taskmanagement.service.TaskEntity
import vn.edu.usth.taskmanagement.service.WorkspaceEntity

@Database(
    entities = [WorkspaceEntity::class, TaskEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun workspaceDao(): WorkspaceDao
    abstract fun taskDao(): TaskDao
}

