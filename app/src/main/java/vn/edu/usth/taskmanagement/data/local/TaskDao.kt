package vn.edu.usth.taskmanagement.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import vn.edu.usth.taskmanagement.data.local.TaskEntity

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY status ASC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE workspaceId = :workspaceId ORDER BY status ASC")
    fun getTasksByWorkspace(workspaceId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: String): TaskEntity?

    @Query("SELECT COUNT(*) FROM tasks WHERE status != 'ARCHIVED'")
    suspend fun getTotalTaskCount(): Int

    @Query("SELECT COUNT(*) FROM tasks WHERE status = 'DONE'")
    suspend fun getDoneTaskCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<TaskEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: TaskEntity)

    @Query("DELETE FROM tasks")
    suspend fun deleteAll()

    @Query("DELETE FROM tasks WHERE workspaceId = :workspaceId")
    suspend fun deleteByWorkspace(workspaceId: String)

    @Query("UPDATE tasks SET calendarEventId = :eventId WHERE id = :taskId")
    suspend fun updateCalendarEventId(taskId: String, eventId: String)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteById(id: String)
}

