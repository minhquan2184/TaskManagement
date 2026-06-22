package vn.edu.usth.taskmanagement.presentation.group

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import vn.edu.usth.taskmanagement.domain.model.TaskModel
import vn.edu.usth.taskmanagement.domain.model.WorkspaceProgress
import vn.edu.usth.taskmanagement.domain.repository.FirebaseRepository
import vn.edu.usth.taskmanagement.domain.repository.TaskRepository
import vn.edu.usth.taskmanagement.domain.repository.WorkspaceRepository

data class GroupDetailUiState(
    val isLoading: Boolean = true,
    val tasks: List<TaskModel> = emptyList(),
    val error: String? = null,
    val progress: WorkspaceProgress? = null,
    val workspaceDeleted: Boolean = false,
    val actionSuccessMessage: String? = null
)

class GroupDetailViewModel(
    private val taskRepository: TaskRepository,
    private val workspaceRepository: WorkspaceRepository,
    private val firebaseRepository: FirebaseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GroupDetailUiState())
    val uiState: StateFlow<GroupDetailUiState> = _uiState.asStateFlow()

    fun loadTasks(workspaceId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                taskRepository.refreshTasks()
            } catch (e: Exception) {
                Log.e("GroupDetailVM", "Failed to refresh tasks from remote: ${e.message}")
            }

            taskRepository.getTasksByWorkspace(workspaceId)
                .catch { e ->
                    Log.e("GroupDetailVM", "Error loading tasks: ${e.message}")
                    _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
                }
                .collect { tasks ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        tasks = tasks.filter { it.status != "ARCHIVED" }
                    )
                }
        }
        
        viewModelScope.launch {
            firebaseRepository.observeWorkspaceProgress(workspaceId)
                .catch { e -> Log.e("GroupDetailVM", "Firebase error: ${e.message}") }
                .collect { progress ->
                    _uiState.value = _uiState.value.copy(progress = progress)
                }
        }
    }

    fun addTask(title: String, description: String?, workspaceId: String, dueDate: String? = null) {
        viewModelScope.launch {
            try {
                taskRepository.addTask(title, description, workspaceId, dueDate)
            } catch (e: Exception) {
                Log.e("GroupDetailVM", "Failed to add task: ${e.message}")
                _uiState.value = _uiState.value.copy(error = "Failed to add task: ${e.message}")
            }
        }
    }

    fun updateTask(taskId: String, title: String?, description: String?, priority: String?, dueDate: String?) {
        viewModelScope.launch {
            try {
                taskRepository.updateTask(taskId, title, description, priority, dueDate)
            } catch (e: Exception) {
                Log.e("GroupDetailVM", "Failed to update task: ${e.message}")
                _uiState.value = _uiState.value.copy(error = "Failed to update task: ${e.message}")
            }
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            try {
                taskRepository.deleteTask(taskId)
            } catch (e: Exception) {
                Log.e("GroupDetailVM", "Failed to delete task: ${e.message}")
                _uiState.value = _uiState.value.copy(error = "Failed to delete task: ${e.message}")
            }
        }
    }

    fun toggleTaskStatus(task: TaskModel, isDone: Boolean) {
        viewModelScope.launch {
            try {
                taskRepository.toggleTaskStatus(task.id, isDone)
            } catch (e: Exception) {
                Log.e("GroupDetailVM", "Failed to toggle task: ${e.message}")
            }
        }
    }

    fun updateWorkspace(workspaceId: String, title: String?, description: String?) {
        viewModelScope.launch {
            try {
                workspaceRepository.updateWorkspace(workspaceId, title, description)
            } catch (e: Exception) {
                Log.e("GroupDetailVM", "Failed to update workspace: ${e.message}")
                _uiState.value = _uiState.value.copy(error = "Failed to update group: ${e.message}")
            }
        }
    }

    fun deleteWorkspace(workspaceId: String) {
        viewModelScope.launch {
            try {
                workspaceRepository.deleteWorkspace(workspaceId)
                _uiState.value = _uiState.value.copy(workspaceDeleted = true)
            } catch (e: Exception) {
                Log.e("GroupDetailVM", "Failed to delete workspace: ${e.message}")
                _uiState.value = _uiState.value.copy(error = "Failed to delete group: ${e.message}")
            }
        }
    }

    fun inviteMember(workspaceId: String, email: String) {
        viewModelScope.launch {
            try {
                workspaceRepository.inviteMember(workspaceId, email)
                _uiState.value = _uiState.value.copy(actionSuccessMessage = "Invitation email sent successfully")
            } catch (e: Exception) {
                Log.e("GroupDetailVM", "Failed to invite member: ${e.message}")
                _uiState.value = _uiState.value.copy(error = "Failed to invite member: ${e.message}")
            }
        }
    }

    fun leaveWorkspace(workspaceId: String) {
        viewModelScope.launch {
            try {
                workspaceRepository.leaveWorkspace(workspaceId)
                _uiState.value = _uiState.value.copy(workspaceDeleted = true) // Redirect back
            } catch (e: Exception) {
                Log.e("GroupDetailVM", "Failed to leave workspace: ${e.message}")
                _uiState.value = _uiState.value.copy(error = "Failed to leave group: ${e.message}")
            }
        }
    }
    
    fun clearMessages() {
        _uiState.value = _uiState.value.copy(error = null, actionSuccessMessage = null)
    }
}
