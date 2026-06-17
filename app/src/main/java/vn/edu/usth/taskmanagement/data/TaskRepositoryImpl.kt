package vn.edu.usth.taskmanagement.data

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import vn.edu.usth.taskmanagement.service.TaskDao
import vn.edu.usth.taskmanagement.data.toDomain
import vn.edu.usth.taskmanagement.data.toEntity
import vn.edu.usth.taskmanagement.api.TaskRemoteDataSource
import vn.edu.usth.taskmanagement.domain.model.TaskModel
import vn.edu.usth.taskmanagement.domain.repository.TaskRepository

class TaskRepositoryImpl(
    private val remoteDataSource: TaskRemoteDataSource,
    private val taskDao: TaskDao
) : TaskRepository {

    override fun getAllTasks(): Flow<List<TaskModel>> {
        return taskDao.getAllTasks().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getTasksByWorkspace(workspaceId: String): Flow<List<TaskModel>> {
        return taskDao.getTasksByWorkspace(workspaceId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun addTask(title: String, description: String?, workspaceId: String?, dueDate: String?, priority: String?, assigneeId: String?): TaskModel {
        val dto = remoteDataSource.addTask(title, description, workspaceId, priority, dueDate, assigneeId)
        taskDao.insert(dto.toEntity())
        return dto.toDomain()
    }

    override suspend fun updateTask(
        taskId: String,
        title: String?,
        description: String?,
        priority: String?,
        dueDate: String?,
        assigneeId: String?
    ): TaskModel {
        val dto = remoteDataSource.updateTask(taskId, title, description, priority, dueDate, assigneeId)
        taskDao.insert(dto.toEntity())
        return dto.toDomain()
    }

    override suspend fun deleteTask(taskId: String) {
        remoteDataSource.deleteTask(taskId)
        taskDao.deleteById(taskId)
    }

    override suspend fun toggleTaskStatus(taskId: String, isDone: Boolean): TaskModel {
        val newStatus = if (isDone) "DONE" else "TODO"
        val dto = remoteDataSource.updateTaskStatus(taskId, newStatus)
        taskDao.insert(dto.toEntity())
        return dto.toDomain()
    }

    override suspend fun refreshTasks() {
        try {
            val remoteDtos = remoteDataSource.getTasks()
            val entities = remoteDtos.map { it.toEntity() }
            taskDao.deleteAll()
            taskDao.insertAll(entities)
        } catch (e: Exception) {
            Log.e("TaskRepo", "Failed to refresh tasks: ${e.message}")
        }
    }

    override suspend fun getTotalTaskCount(): Int {
        return taskDao.getTotalTaskCount()
    }

    override suspend fun sendTaskChat(taskId: String, content: String) {
        try {
            remoteDataSource.sendTaskChat(taskId, content)
        } catch (e: Exception) {
            Log.e("TaskRepo", "Failed to send chat: ${e.message}")
        }
    }
}

