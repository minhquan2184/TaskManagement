package vn.edu.usth.taskmanagement.presentation.calendar

import vn.edu.usth.taskmanagement.presentation.task.TaskDetailBottomSheetFragment

import android.Manifest
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.applandeo.materialcalendarview.EventDay
import com.applandeo.materialcalendarview.listeners.OnDayClickListener
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import vn.edu.usth.taskmanagement.R
import vn.edu.usth.taskmanagement.databinding.FragmentCalendarBinding
import vn.edu.usth.taskmanagement.service.CalendarSyncManager
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class CalendarFragment : Fragment() {
    private var _binding: FragmentCalendarBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CalendarViewModel by viewModel()
    private val calendarSyncManager: CalendarSyncManager by inject()
    private lateinit var taskAdapter: CalendarTaskAdapter
    private lateinit var eventAdapter: CalendarEventAdapter

    // Permission launcher for calendar access
    private val calendarPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            syncCurrentTasksToCalendar()
            viewModel.refreshGoogleCalendarEvents()
            viewModel.fetchGoogleCalendarDatesForDots()
        } else {
            Toast.makeText(requireContext(), "Calendar permission is required to sync", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCalendarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerViews()
        setupCalendar()
        observeViewModel()

        binding.btnHome.setOnClickListener {
            findNavController().navigate(R.id.action_calendar_to_home)
        }

        binding.btnSync.setOnClickListener {
            requestCalendarPermissionAndSync()
        }
    }

    private fun setupRecyclerViews() {
        taskAdapter = CalendarTaskAdapter { _, _ ->
            // TODO: Toggle task status
        }
        binding.rvTasks.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = taskAdapter
        }

        eventAdapter = CalendarEventAdapter()
        binding.rvGoogleEvents.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = eventAdapter
        }
    }

    private fun setupCalendar() {
        binding.calendarView.setOnDayClickListener(object : OnDayClickListener {
            override fun onDayClick(eventDay: EventDay) {
                val cal = eventDay.calendar
                viewModel.onDateSelected(
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
                )
            }
        })
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.selectedDateTasks.combine(viewModel.selectedDateStr) { tasks, dateStr ->
                Pair(tasks, dateStr)
            }.collectLatest { (tasks, dateStr) ->
                taskAdapter.submitList(tasks)
                binding.tvSelectedDate.text = formatDateHeader(dateStr)

                val count = tasks.size
                binding.tvTaskCount.text = "$count task${if (count != 1) "s" else ""}"

                binding.tvEmptyState.visibility = if (tasks.isEmpty()) View.VISIBLE else View.GONE
                binding.rvTasks.visibility = if (tasks.isEmpty()) View.GONE else View.VISIBLE
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.googleCalendarEvents.collectLatest { events ->
                eventAdapter.submitList(events)
                val count = events.size
                binding.tvGoogleEventCount.text = "$count event${if (count != 1) "s" else ""}"
                binding.tvGoogleEmpty.visibility = if (events.isEmpty()) View.VISIBLE else View.GONE
                binding.rvGoogleEvents.visibility = if (events.isEmpty()) View.GONE else View.VISIBLE
            }
        }

        // Observe dates that have tasks to plot red dots
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.allEventDates.collectLatest { calendars ->
                val events = calendars.map { cal ->
                    EventDay(cal, R.drawable.ic_dot_red)
                }
                binding.calendarView.setEvents(events)
            }
        }
    }

    private fun formatDateHeader(dateStr: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val outputFormat = SimpleDateFormat("EEEE, MMMM d", Locale.US)
            val date = inputFormat.parse(dateStr)

            val today = Calendar.getInstance()
            val selected = Calendar.getInstance().apply { time = date ?: Date() }

            if (today.get(Calendar.YEAR) == selected.get(Calendar.YEAR) &&
                today.get(Calendar.DAY_OF_YEAR) == selected.get(Calendar.DAY_OF_YEAR)) {
                "Today"
            } else {
                outputFormat.format(date ?: Date())
            }
        } catch (e: Exception) {
            dateStr
        }
    }

    private fun requestCalendarPermissionAndSync() {
        if (calendarSyncManager.hasCalendarPermissions()) {
            syncCurrentTasksToCalendar()
        } else {
            calendarPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_CALENDAR,
                    Manifest.permission.WRITE_CALENDAR
                )
            )
        }
    }

    private fun syncCurrentTasksToCalendar() {
        val tasks = viewModel.selectedDateTasks.value
        if (tasks.isEmpty()) {
            Toast.makeText(requireContext(), "No tasks to sync for this day", Toast.LENGTH_SHORT).show()
            viewModel.refreshGoogleCalendarEvents()
            return
        }

        var syncedCount = 0
        tasks.forEach { task ->
            if (task.dueDate != null) {
                val eventId = calendarSyncManager.syncTaskToCalendar(task)
                if (eventId != null && task.calendarEventId == null) {
                    viewModel.updateCalendarEventId(task.id, eventId)
                    syncedCount++
                }
            }
        }

        if (syncedCount > 0) {
            Toast.makeText(requireContext(), "Synced $syncedCount task(s) to Google Calendar", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "All tasks already synced", Toast.LENGTH_SHORT).show()
        }
        binding.tvSyncStatus.text = "Last synced: just now • Google Calendar"
        
        viewLifecycleOwner.lifecycleScope.launch {
            if (syncedCount > 0) {
                kotlinx.coroutines.delay(500) // Give Calendar Provider time to update Instances view
            }
            viewModel.refreshGoogleCalendarEvents()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

