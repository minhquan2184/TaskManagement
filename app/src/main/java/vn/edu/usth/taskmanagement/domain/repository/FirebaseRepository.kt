package vn.edu.usth.taskmanagement.domain.repository

import kotlinx.coroutines.flow.Flow
import vn.edu.usth.taskmanagement.domain.model.ChatMessage
import vn.edu.usth.taskmanagement.domain.model.WorkspaceProgress

interface FirebaseRepository {
    fun observeWorkspaceProgress(workspaceId: String): Flow<WorkspaceProgress>
    fun observeTaskChat(taskId: String): Flow<List<ChatMessage>>
    suspend fun sendChatMessage(taskId: String, actorId: String, actorName: String, content: String)
}
