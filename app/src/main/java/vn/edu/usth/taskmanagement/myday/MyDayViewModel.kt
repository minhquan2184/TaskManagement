package vn.edu.usth.taskmanagement.myday

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import vn.edu.usth.taskmanagement.domain.model.TaskModel
import vn.edu.usth.taskmanagement.domain.repository.TaskRepository
import vn.edu.usth.taskmanagement.domain.repository.WorkspaceRepository
import vn.edu.usth.taskmanagement.myday.MyDayItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class MyDayUiState(
    val isLoading: Boolean = true,
    val items: List<MyDayItem> = emptyList(),
    val error: String? = null
)

class MyDayViewModel(
    private val taskRepository: TaskRepository,
    private val workspaceRepository: WorkspaceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyDayUiState())
    val uiState: StateFlow<MyDayUiState> = _uiState.asStateFlow()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    init {
        loadMyDayData()
    }

    private fun loadMyDayData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                // Collect tasks and workspaces
                taskRepository.getAllTasks()
                    .catch { e ->
                        Log.e("MyDayViewModel", "Error fetching tasks: ${e.message}")
                        _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
                    }
                    .collect { tasks ->
                        processTasks(tasks)
                    }
            } catch (e: Exception) {
                Log.e("MyDayViewModel", "Failed to load My Day data: ${e.message}")
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    private suspend fun processTasks(tasks: List<TaskModel>) {
        val todayStr = dateFormat.format(Date())
        val todayTasks = tasks.filter { it.dueDate?.take(10) == todayStr && it.status != "ARCHIVED" }

        val items = mutableListOf<MyDayItem>()

        // Get all workspaces to resolve names
        val personalWs = workspaceRepository.getPersonalWorkspace()
        
        // We will collect group workspaces but for simplicity we can just wait for the first emission
        var groupWorkspaces = emptyList<vn.edu.usth.taskmanagement.domain.model.Workspace>()
        try {
            workspaceRepository.getGroupWorkspaces().collect { groups ->
                groupWorkspaces = groups
                
                val groupedTasks = todayTasks.groupBy { it.workspaceId }
                
                // Add Personal
                if (personalWs != null) {
                    val pTasks = groupedTasks[personalWs.id] ?: emptyList()
                    if (pTasks.isNotEmpty()) {
                        items.add(MyDayItem.Header(personalWs.id, personalWs.title, true, pTasks.size))
                        pTasks.forEach { items.add(MyDayItem.TaskNode(it)) }
                    }
                }

                // Add Groups
                for (group in groupWorkspaces) {
                    val gTasks = groupedTasks[group.id] ?: emptyList()
                    if (gTasks.isNotEmpty()) {
                        items.add(MyDayItem.Header(group.id, group.title, false, gTasks.size))
                        gTasks.forEach { items.add(MyDayItem.TaskNode(it)) }
                    }
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    items = items
                )
                
                // Breaking out of collect since we just need the latest state
                throw kotlinx.coroutines.CancellationException("Data loaded")
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            // Expected
        } catch (e: Exception) {
            Log.e("MyDayViewModel", "Error processing tasks: ${e.message}")
            _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
        }
    }

    fun toggleTaskStatus(task: TaskModel, isDone: Boolean) {
        viewModelScope.launch {
            try {
                taskRepository.toggleTaskStatus(task.id, isDone)
            } catch (e: Exception) {
                Log.e("MyDayViewModel", "Failed to toggle task: ${e.message}")
            }
        }
    }

    fun addTask(title: String, description: String?, dueDate: String? = null) {
        viewModelScope.launch {
            try {
                val personalWs = workspaceRepository.getPersonalWorkspace()
                taskRepository.addTask(title, description, personalWs?.id, dueDate)
            } catch (e: Exception) {
                Log.e("MyDayViewModel", "Failed to add task: ${e.message}")
            }
        }
    }
}
