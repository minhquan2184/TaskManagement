package vn.edu.usth.taskmanagement.data.remote.source

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.request.delete
import io.ktor.http.ContentType
import io.ktor.http.contentType
import vn.edu.usth.taskmanagement.data.local.SessionManager
import vn.edu.usth.taskmanagement.data.remote.dto.TaskDto
import kotlinx.serialization.Serializable

@Serializable
data class TaskCreateRequest(
    val title: String,
    val description: String? = null,
    val workspaceId: String? = null,
    val priority: String? = null,
    val dueDate: String? = null,
    val assigneeId: String? = null
)

@Serializable
data class TaskUpdateRequest(
    val title: String? = null,
    val description: String? = null,
    val priority: String? = null,
    val dueDate: String? = null,
    val status: String? = null,
    val assigneeId: String? = null
)

@Serializable
data class TaskStatusUpdateRequest(
    val status: String
)

@Serializable
data class TaskChatRequest(
    val content: String,
    val actorId: String? = null
)

class TaskRemoteDataSource(
    private val client: HttpClient,
    private val sessionManager: SessionManager
) {
    private val baseUrl: String get() = sessionManager.getBaseUrl()

    suspend fun getTasks(): List<TaskDto> {
        return client.get("$baseUrl/tasks").body()
    }

    suspend fun getTasksByWorkspace(workspaceId: String): List<TaskDto> {
        return client.get("$baseUrl/tasks?workspaceId=$workspaceId").body()
    }

    suspend fun getTodayTasks(): List<TaskDto> {
        return client.get("$baseUrl/tasks/today").body()
    }

    suspend fun addTask(
        title: String,
        description: String?,
        workspaceId: String? = null,
        priority: String? = null,
        dueDate: String? = null,
        assigneeId: String? = null
    ): TaskDto {
        return client.post("$baseUrl/tasks") {
            contentType(ContentType.Application.Json)
            setBody(TaskCreateRequest(title, description, workspaceId, priority, dueDate, assigneeId))
        }.body()
    }

    suspend fun updateTask(
        taskId: String,
        title: String? = null,
        description: String? = null,
        priority: String? = null,
        dueDate: String? = null,
        assigneeId: String? = null
    ): TaskDto {
        return client.put("$baseUrl/tasks/$taskId") {
            contentType(ContentType.Application.Json)
            setBody(TaskUpdateRequest(title, description, priority, dueDate, null, assigneeId))
        }.body()
    }

    suspend fun deleteTask(taskId: String) {
        client.delete("$baseUrl/tasks/$taskId")
    }

    suspend fun updateTaskStatus(taskId: String, status: String): TaskDto {
        return client.put("$baseUrl/tasks/$taskId/status") {
            contentType(ContentType.Application.Json)
            setBody(TaskStatusUpdateRequest(status))
        }.body()
    }

    suspend fun sendTaskChat(taskId: String, content: String) {
        client.post("$baseUrl/tasks/$taskId/chat") {
            contentType(ContentType.Application.Json)
            setBody(TaskChatRequest(content))
        }
    }
}
