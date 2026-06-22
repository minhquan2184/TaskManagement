package vn.edu.usth.taskmanagement.presentation.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import vn.edu.usth.taskmanagement.domain.model.TaskModel
import vn.edu.usth.taskmanagement.domain.model.Workspace
import vn.edu.usth.taskmanagement.domain.repository.TaskRepository
import vn.edu.usth.taskmanagement.domain.repository.WorkspaceRepository

data class HomeUiState(
    val isLoading: Boolean = true,
    val personalTasks: List<TaskModel> = emptyList(),
    val groupWorkspaces: List<Workspace> = emptyList(),
    val personalWorkspace: Workspace? = null,
    val error: String? = null
)

class HomeViewModel(
    private val taskRepository: TaskRepository,
    private val workspaceRepository: WorkspaceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHomeData()
    }

    fun loadHomeData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                // 1. Refresh data from remote
                workspaceRepository.refreshWorkspaces()
                taskRepository.refreshTasks()
            } catch (e: Exception) {
                Log.w("HomeViewModel", "Remote refresh failed, using cache: ${e.message}")
            }

            // 2. Observe group workspaces from Room
            launch {
                workspaceRepository.getGroupWorkspaces()
                    .catch { e -> Log.e("HomeViewModel", "Error observing groups: ${e.message}") }
                    .collect { groups ->
                        _uiState.value = _uiState.value.copy(
                            groupWorkspaces = groups,
                            isLoading = false
                        )
                    }
            }

            // 3. Load personal workspace + its tasks
            launch {
                try {
                    val personal = workspaceRepository.getPersonalWorkspace()
                    _uiState.value = _uiState.value.copy(personalWorkspace = personal)

                    if (personal != null) {
                        taskRepository.getTasksByWorkspace(personal.id)
                            .catch { e -> Log.e("HomeViewModel", "Error observing tasks: ${e.message}") }
                            .collect { tasks ->
                                val activeTasks = tasks.filter { it.status != "ARCHIVED" }
                                _uiState.value = _uiState.value.copy(
                                    personalTasks = activeTasks,
                                    isLoading = false
                                )
                            }
                    } else {
                        _uiState.value = _uiState.value.copy(isLoading = false)
                    }
                } catch (e: Exception) {
                    Log.e("HomeViewModel", "Error loading personal: ${e.message}")
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            }
        }
    }

    fun addTask(title: String, description: String?, dueDate: String? = null) {
        viewModelScope.launch {
            try {
                val personalWs = _uiState.value.personalWorkspace
                taskRepository.addTask(title, description, personalWs?.id, dueDate)
                // Refresh to update counts
                workspaceRepository.refreshWorkspaces()
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Failed to add task: ${e.message}")
            }
        }
    }

    fun toggleTaskStatus(task: TaskModel, isDone: Boolean) {
        viewModelScope.launch {
            try {
                taskRepository.toggleTaskStatus(task.id, isDone)
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Failed to toggle task: ${e.message}")
            }
        }
    }

    fun createGroup(title: String, description: String?) {
        viewModelScope.launch {
            try {
                workspaceRepository.createWorkspace(title, description)
                // Refresh group list after creating
                workspaceRepository.refreshWorkspaces()
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Failed to create group: ${e.message}")
                _uiState.value = _uiState.value.copy(error = "Failed to create group: ${e.message}")
            }
        }
    }

    fun updateTask(taskId: String, title: String?, description: String?, priority: String?, dueDate: String?) {
        viewModelScope.launch {
            try {
                taskRepository.updateTask(taskId, title, description, priority, dueDate)
                workspaceRepository.refreshWorkspaces()
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Failed to update task: ${e.message}")
            }
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            try {
                taskRepository.deleteTask(taskId)
                workspaceRepository.refreshWorkspaces()
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Failed to delete task: ${e.message}")
            }
        }
    }

    fun updateGroup(workspaceId: String, title: String?, description: String?) {
        viewModelScope.launch {
            try {
                workspaceRepository.updateWorkspace(workspaceId, title, description)
                workspaceRepository.refreshWorkspaces()
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Failed to update group: ${e.message}")
            }
        }
    }

    fun deleteGroup(workspaceId: String) {
        viewModelScope.launch {
            try {
                workspaceRepository.deleteWorkspace(workspaceId)
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Failed to delete group: ${e.message}")
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
