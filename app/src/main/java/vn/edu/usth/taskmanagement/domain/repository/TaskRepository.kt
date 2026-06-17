package vn.edu.usth.taskmanagement.domain.repository

import kotlinx.coroutines.flow.Flow
import vn.edu.usth.taskmanagement.domain.model.TaskModel

interface TaskRepository {
    fun getAllTasks(): Flow<List<TaskModel>>
    fun getTasksByWorkspace(workspaceId: String): Flow<List<TaskModel>>
    suspend fun addTask(title: String, description: String?, workspaceId: String?, dueDate: String? = null, priority: String? = null, assigneeId: String? = null): TaskModel
    suspend fun updateTask(taskId: String, title: String?, description: String?, priority: String?, dueDate: String?, assigneeId: String? = null): TaskModel
    suspend fun deleteTask(taskId: String)
    suspend fun toggleTaskStatus(taskId: String, isDone: Boolean): TaskModel
    suspend fun refreshTasks()
    suspend fun getTotalTaskCount(): Int
    suspend fun sendTaskChat(taskId: String, content: String)
}

