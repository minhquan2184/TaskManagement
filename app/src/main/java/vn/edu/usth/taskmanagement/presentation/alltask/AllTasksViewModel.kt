package vn.edu.usth.taskmanagement.presentation.alltask

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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class AllTasksUiState(
    val isLoading: Boolean = true,
    val todayTasks: List<TaskModel> = emptyList(),
    val tomorrowTasks: List<TaskModel> = emptyList(),
    val upcomingTasks: List<TaskModel> = emptyList(),
    val overdueTasks: List<TaskModel> = emptyList(),
    val doneTasks: List<TaskModel> = emptyList(),
    val error: String? = null
)

class AllTasksViewModel(
    private val taskRepository: TaskRepository,
    private val workspaceRepository: WorkspaceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AllTasksUiState())
    val uiState: StateFlow<AllTasksUiState> = _uiState.asStateFlow()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    init {
        loadAllTasks()
    }

    private fun loadAllTasks() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            taskRepository.getAllTasks()
                .catch { e ->
                    Log.e("AllTasksViewModel", "Error fetching tasks: ${e.message}")
                    _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
                }
                .collect { tasks ->
                    categorizeTasks(tasks)
                }
        }
    }

    private fun categorizeTasks(tasks: List<TaskModel>) {
        val todayStr = dateFormat.format(Date())
        val tomorrowCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
        val tomorrowStr = dateFormat.format(tomorrowCal.time)

        val overdueTasks = mutableListOf<TaskModel>()
        val todayTasks = mutableListOf<TaskModel>()
        val tomorrowTasks = mutableListOf<TaskModel>()
        val upcomingTasks = mutableListOf<TaskModel>()
        val doneTasks = mutableListOf<TaskModel>()

        for (task in tasks) {
            if (task.isDone) {
                doneTasks.add(task)
            } else {
                val dateStr = task.dueDate?.take(10)
                when {
                    dateStr == null -> upcomingTasks.add(task)
                    dateStr < todayStr -> overdueTasks.add(task)
                    dateStr == todayStr -> todayTasks.add(task)
                    dateStr == tomorrowStr -> tomorrowTasks.add(task)
                    else -> upcomingTasks.add(task)
                }
            }
        }

        _uiState.value = _uiState.value.copy(
            isLoading = false,
            todayTasks = todayTasks,
            tomorrowTasks = tomorrowTasks,
            upcomingTasks = upcomingTasks,
            overdueTasks = overdueTasks,
            doneTasks = doneTasks
        )
    }

    fun toggleTaskStatus(task: TaskModel, isDone: Boolean) {
        viewModelScope.launch {
            try {
                taskRepository.toggleTaskStatus(task.id, isDone)
            } catch (e: Exception) {
                Log.e("AllTasksViewModel", "Failed to toggle task: ${e.message}")
            }
        }
    }

    fun addTask(title: String, description: String?, dueDate: String? = null) {
        viewModelScope.launch {
            try {
                val personalWs = workspaceRepository.getPersonalWorkspace()
                taskRepository.addTask(title, description, personalWs?.id, dueDate)
            } catch (e: Exception) {
                Log.e("AllTasksViewModel", "Failed to add task: ${e.message}")
            }
        }
    }

    fun updateTask(taskId: String, title: String?, description: String?, priority: String?, dueDate: String?) {
        viewModelScope.launch {
            try {
                taskRepository.updateTask(taskId, title, description, priority, dueDate)
            } catch (e: Exception) {
                Log.e("AllTasksViewModel", "Failed to update task: ${e.message}")
            }
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            try {
                taskRepository.deleteTask(taskId)
            } catch (e: Exception) {
                Log.e("AllTasksViewModel", "Failed to delete task: ${e.message}")
            }
        }
    }
}
