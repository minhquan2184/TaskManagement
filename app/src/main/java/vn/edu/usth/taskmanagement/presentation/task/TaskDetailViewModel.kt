package vn.edu.usth.taskmanagement.presentation.task

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import vn.edu.usth.taskmanagement.data.local.SessionManager
import vn.edu.usth.taskmanagement.domain.model.ChatMessage
import vn.edu.usth.taskmanagement.domain.model.Member
import vn.edu.usth.taskmanagement.domain.repository.FirebaseRepository
import vn.edu.usth.taskmanagement.domain.repository.TaskRepository
import vn.edu.usth.taskmanagement.domain.repository.WorkspaceRepository

data class TaskDetailUiState(
    val messages: List<ChatMessage> = emptyList(),
    val members: List<Member> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class TaskDetailViewModel(
    private val firebaseRepository: FirebaseRepository,
    private val taskRepository: TaskRepository,
    private val workspaceRepository: WorkspaceRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(TaskDetailUiState())
    val uiState: StateFlow<TaskDetailUiState> = _uiState.asStateFlow()

    fun loadChat(taskId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            Log.d("TaskDetailVM", "Loading chat for taskId=$taskId")
            firebaseRepository.observeTaskChat(taskId)
                .catch { e ->
                    Log.e("TaskDetailVM", "observeTaskChat error: ${e.message}")
                    _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
                }
                .collect { messages ->
                    Log.d("TaskDetailVM", "Received ${messages.size} messages")
                    _uiState.value = _uiState.value.copy(
                        messages = messages.sortedBy { it.timestamp },
                        isLoading = false,
                        error = null
                    )
                }
        }
    }

    fun sendMessage(taskId: String, content: String) {
        viewModelScope.launch {
            try {
                val actorId = sessionManager.getUserId() ?: "anonymous"
                val actorName = sessionManager.getFullName()
                    ?: sessionManager.getEmail()?.substringBefore("@")
                    ?: "Unknown"
                Log.d("TaskDetailVM", "Sending message: '$content' as $actorName to task $taskId")
                firebaseRepository.sendChatMessage(taskId, actorId, actorName, content)
            } catch (e: Exception) {
                Log.e("TaskDetailVM", "Failed to send message: ${e.message}")
            }
        }
    }

    fun loadMembers(workspaceId: String) {
        viewModelScope.launch {
            try {
                val members = workspaceRepository.getWorkspaceMembers(workspaceId)
                _uiState.value = _uiState.value.copy(members = members)
            } catch (e: Exception) {
                Log.e("TaskDetailVM", "Failed to load members: ${e.message}")
            }
        }
    }

    fun updateTaskPriority(taskId: String, priority: String) {
        viewModelScope.launch {
            try {
                taskRepository.updateTask(taskId, null, null, priority, null, null)
            } catch (e: Exception) {
                Log.e("TaskDetailVM", "Failed to update priority: ${e.message}")
            }
        }
    }

    fun assignTask(taskId: String, assigneeId: String) {
        viewModelScope.launch {
            try {
                taskRepository.updateTask(taskId, null, null, null, null, assigneeId)
            } catch (e: Exception) {
                Log.e("TaskDetailVM", "Failed to assign task: ${e.message}")
            }
        }
    }
}
