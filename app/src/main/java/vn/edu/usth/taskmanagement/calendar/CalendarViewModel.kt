package vn.edu.usth.taskmanagement.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import vn.edu.usth.taskmanagement.domain.model.TaskModel
import vn.edu.usth.taskmanagement.domain.repository.TaskRepository
import vn.edu.usth.taskmanagement.service.CalendarEvent
import vn.edu.usth.taskmanagement.service.CalendarSyncManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CalendarViewModel(
    private val taskRepository: TaskRepository,
    private val calendarSyncManager: CalendarSyncManager
) : ViewModel() {

    private val _selectedDateTasks = MutableStateFlow<List<TaskModel>>(emptyList())
    val selectedDateTasks: StateFlow<List<TaskModel>> = _selectedDateTasks.asStateFlow()

    private val _googleCalendarEvents = MutableStateFlow<List<CalendarEvent>>(emptyList())
    val googleCalendarEvents: StateFlow<List<CalendarEvent>> = _googleCalendarEvents.asStateFlow()

    private val _selectedDateStr = MutableStateFlow("")
    val selectedDateStr: StateFlow<String> = _selectedDateStr.asStateFlow()

    // Store currently selected year/month/day for Google Calendar read
    private var currentYear = 0
    private var currentMonth = 0
    private var currentDay = 0

    private var allTasks: List<TaskModel> = emptyList()

    private val _taskDates = MutableStateFlow<List<java.util.Calendar>>(emptyList())
    
    private val _googleCalendarDates = MutableStateFlow<List<java.util.Calendar>>(emptyList())

    // Combine both app tasks and google calendar dates
    val allEventDates = kotlinx.coroutines.flow.combine(_taskDates, _googleCalendarDates) { tasks, google ->
        (tasks + google).distinctBy { "${it.get(java.util.Calendar.YEAR)}-${it.get(java.util.Calendar.DAY_OF_YEAR)}" }
    }

    init {
        val today = java.util.Calendar.getInstance()
        currentYear = today.get(java.util.Calendar.YEAR)
        currentMonth = today.get(java.util.Calendar.MONTH)
        currentDay = today.get(java.util.Calendar.DAY_OF_MONTH)

        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        _selectedDateStr.value = dateStr

        viewModelScope.launch {
            taskRepository.getAllTasks().collectLatest { tasks ->
                allTasks = tasks
                filterTasksByDate(_selectedDateStr.value)
                extractTaskDates(tasks)
            }
        }

        // Read Google Calendar events for today
        refreshGoogleCalendarEvents()
        
        // Read Google Calendar dates for the next 1 year to show dots
        fetchGoogleCalendarDatesForDots()
    }

    fun fetchGoogleCalendarDatesForDots() {
        viewModelScope.launch {
            try {
                val startMillis = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000 // 1 month ago
                val endMillis = System.currentTimeMillis() + 365L * 24 * 60 * 60 * 1000 // 1 year later
                val dates = calendarSyncManager.readEventDatesInRange(startMillis, endMillis)
                _googleCalendarDates.value = dates
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    private fun extractTaskDates(tasks: List<TaskModel>) {
        val cals = mutableListOf<java.util.Calendar>()
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        tasks.forEach { task ->
            if (!task.dueDate.isNullOrBlank()) {
                try {
                    // Extract just the date part if it has time
                    val datePart = task.dueDate.substringBefore("T")
                    val date = format.parse(datePart)
                    if (date != null) {
                        val cal = java.util.Calendar.getInstance()
                        cal.time = date
                        cals.add(cal)
                    }
                } catch (e: Exception) {}
            }
        }
        _taskDates.value = cals
    }

    fun onDateSelected(year: Int, month: Int, dayOfMonth: Int) {
        currentYear = year
        currentMonth = month
        currentDay = dayOfMonth

        val dateStr = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth)
        _selectedDateStr.value = dateStr
        filterTasksByDate(dateStr)
        refreshGoogleCalendarEvents()
    }

    fun refreshGoogleCalendarEvents() {
        viewModelScope.launch {
            try {
                val events = calendarSyncManager.readEventsForDate(currentYear, currentMonth, currentDay)
                _googleCalendarEvents.value = events
            } catch (e: Exception) {
                _googleCalendarEvents.value = emptyList()
            }
        }
    }

    private fun filterTasksByDate(dateStr: String) {
        _selectedDateTasks.value = allTasks.filter { task ->
            task.dueDate?.startsWith(dateStr) == true
        }
    }
}

