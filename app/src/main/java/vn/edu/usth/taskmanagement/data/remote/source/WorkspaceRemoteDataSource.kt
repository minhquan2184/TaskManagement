package vn.edu.usth.taskmanagement.data.remote.source

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.delete
import io.ktor.client.request.put
import io.ktor.http.ContentType
import io.ktor.http.contentType
import vn.edu.usth.taskmanagement.data.local.SessionManager
import vn.edu.usth.taskmanagement.data.remote.dto.WorkspaceDto
import kotlinx.serialization.Serializable

@Serializable
data class WorkspaceCreateRequest(
    val title: String,
    val description: String? = null,
    val isPersonal: Boolean = false
)

@Serializable
data class WorkspaceUpdateRequest(
    val title: String? = null,
    val description: String? = null
)

class WorkspaceRemoteDataSource(
    private val client: HttpClient,
    private val sessionManager: SessionManager
) {
    private val baseUrl: String get() = sessionManager.getBaseUrl()

    suspend fun getWorkspaces(): List<WorkspaceDto> {
        return client.get("$baseUrl/workspaces").body()
    }

    suspend fun getWorkspaceById(id: String): WorkspaceDto {
        return client.get("$baseUrl/workspaces/$id").body()
    }

    suspend fun createWorkspace(title: String, description: String?): WorkspaceDto {
        return client.post("$baseUrl/workspaces") {
            contentType(ContentType.Application.Json)
            setBody(WorkspaceCreateRequest(title, description))
        }.body()
    }

    suspend fun updateWorkspace(id: String, title: String?, description: String?): WorkspaceDto {
        return client.put("$baseUrl/workspaces/$id") {
            contentType(ContentType.Application.Json)
            setBody(WorkspaceUpdateRequest(title, description))
        }.body()
    }

    suspend fun deleteWorkspace(id: String) {
        client.delete("$baseUrl/workspaces/$id")
    }

    suspend fun inviteMember(workspaceId: String, email: String) {
        client.post("$baseUrl/workspaces/$workspaceId/invite") {
            contentType(ContentType.Application.Json)
            setBody(WorkspaceInviteRequest(email))
        }
    }

    suspend fun leaveWorkspace(workspaceId: String) {
        client.post("$baseUrl/workspaces/$workspaceId/leave")
    }
}

@Serializable
data class WorkspaceInviteRequest(
    val email: String
)
