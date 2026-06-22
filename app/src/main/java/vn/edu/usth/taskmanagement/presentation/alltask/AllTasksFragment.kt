package vn.edu.usth.taskmanagement.presentation.alltask

import vn.edu.usth.taskmanagement.presentation.task.TaskDetailBottomSheetFragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import vn.edu.usth.taskmanagement.R
import vn.edu.usth.taskmanagement.databinding.FragmentAllTasksBinding
import vn.edu.usth.taskmanagement.domain.model.TaskModel
import vn.edu.usth.taskmanagement.presentation.shared.TaskAdapter
import java.util.Locale

class AllTasksFragment : Fragment() {
    private var _binding: FragmentAllTasksBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AllTasksViewModel by viewModel()

    private lateinit var adapterOverdue: TaskAdapter
    private lateinit var adapterToday: TaskAdapter
    private lateinit var adapterTomorrow: TaskAdapter
    private lateinit var adapterUpcoming: TaskAdapter
    private lateinit var adapterDone: TaskAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAllTasksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerViews()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupRecyclerViews() {
        val onTaskChecked: (TaskModel, Boolean) -> Unit = { task, isDone ->
            viewModel.toggleTaskStatus(task, isDone)
        }
        val onTaskClicked: (TaskModel) -> Unit = { task ->
            val bottomSheet = TaskDetailBottomSheetFragment().apply {
                arguments = android.os.Bundle().apply {
                    putString("taskId", task.id)
                    putString("priority", task.priority)
                    putString("taskTitle", task.title)
                    putString("taskDesc", task.description ?: "")
                    putString("workspaceId", task.workspaceId)
                    putString("assigneeId", task.assigneeId ?: "")
                }
            }
            bottomSheet.show(childFragmentManager, "TaskDetail")
        }
        val onTaskLongPress: (TaskModel, View) -> Unit = { task, anchor ->
            showTaskPopupMenu(task, anchor)
        }

        adapterOverdue = TaskAdapter(onTaskChecked, onTaskClicked).also { it.setOnItemLongClickListener(onTaskLongPress) }
        adapterToday = TaskAdapter(onTaskChecked, onTaskClicked).also { it.setOnItemLongClickListener(onTaskLongPress) }
        adapterTomorrow = TaskAdapter(onTaskChecked, onTaskClicked).also { it.setOnItemLongClickListener(onTaskLongPress) }
        adapterUpcoming = TaskAdapter(onTaskChecked, onTaskClicked).also { it.setOnItemLongClickListener(onTaskLongPress) }
        adapterDone = TaskAdapter(onTaskChecked, onTaskClicked).also { it.setOnItemLongClickListener(onTaskLongPress) }

        binding.rvOverdue.adapter = adapterOverdue
        binding.rvToday.adapter = adapterToday
        binding.rvTomorrow.adapter = adapterTomorrow
        binding.rvUpcoming.adapter = adapterUpcoming
        binding.rvDone.adapter = adapterDone
    }

    private fun showTaskPopupMenu(task: TaskModel, anchorView: View) {
        val popup = PopupMenu(requireContext(), anchorView)
        popup.menu.add(0, 1, 0, "Edit")
        popup.menu.add(0, 2, 1, "Delete")

        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                1 -> {
                    showEditTaskDialog(task)
                    true
                }
                2 -> {
                    showDeleteTaskConfirmation(task)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun showEditTaskDialog(task: TaskModel) {
        val safeContext = context ?: return

        val container = android.widget.LinearLayout(safeContext).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(64, 32, 64, 0)
        }

        val titleInput = android.widget.EditText(safeContext).apply {
            hint = "Task title"
            setText(task.title)
        }
        container.addView(titleInput)

        val descInput = android.widget.EditText(safeContext).apply {
            hint = "Description (optional)"
            setText(task.description ?: "")
        }
        container.addView(descInput)

        val prioritySpinner = android.widget.Spinner(safeContext)
        val priorities = arrayOf("LOW", "MEDIUM", "HIGH", "URGENT")
        prioritySpinner.adapter = android.widget.ArrayAdapter(safeContext, android.R.layout.simple_spinner_dropdown_item, priorities)
        prioritySpinner.setSelection(priorities.indexOf(task.priority).coerceAtLeast(0))
        container.addView(prioritySpinner)

        val dateBtn = android.widget.Button(safeContext).apply {
            text = task.dueDate?.take(10) ?: "Set due date (optional)"
            isAllCaps = false
        }
        var selectedDate: String? = task.dueDate?.take(10)
        dateBtn.setOnClickListener {
            val cal = java.util.Calendar.getInstance()
            android.app.DatePickerDialog(safeContext, { _, year, month, day ->
                selectedDate = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day)
                dateBtn.text = selectedDate
            }, cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH), cal.get(java.util.Calendar.DAY_OF_MONTH)).show()
        }
        container.addView(dateBtn)

        MaterialAlertDialogBuilder(safeContext)
            .setTitle("Edit Task")
            .setView(container)
            .setPositiveButton("Save") { _, _ ->
                val newTitle = titleInput.text.toString().trim()
                if (newTitle.isNotEmpty()) {
                    val newDesc = descInput.text.toString().trim().ifEmpty { null }
                    val newPriority = prioritySpinner.selectedItem.toString()
                    viewModel.updateTask(task.id, newTitle, newDesc, newPriority, selectedDate)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteTaskConfirmation(task: TaskModel) {
        val safeContext = context ?: return
        MaterialAlertDialogBuilder(safeContext)
            .setTitle("Delete Task")
            .setMessage("Are you sure you want to delete \"${task.title}\"?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteTask(task.id)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupClickListeners() {
        binding.btnHome.setOnClickListener {
            findNavController().navigate(R.id.action_allTasks_to_home)
        }

        binding.headerOverdue.setOnClickListener {
            toggleVisibility(binding.rvOverdue, binding.ivOverdueToggle)
        }
        binding.headerToday.setOnClickListener {
            toggleVisibility(binding.rvToday, binding.ivTodayToggle)
        }
        binding.headerTomorrow.setOnClickListener {
            toggleVisibility(binding.rvTomorrow, binding.ivTomorrowToggle)
        }
        binding.headerUpcoming.setOnClickListener {
            toggleVisibility(binding.rvUpcoming, binding.ivUpcomingToggle)
        }
        binding.headerDone.setOnClickListener {
            toggleVisibility(binding.rvDone, binding.ivDoneToggle)
        }
        
        binding.fabAddTask.setOnClickListener {
            showAddTaskDialog()
        }
    }

    private fun showAddTaskDialog() {
        val safeContext = context ?: return

        val container = android.widget.LinearLayout(safeContext).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(64, 32, 64, 0)
        }

        val titleInput = android.widget.EditText(safeContext).apply {
            hint = "Task title"
            requestFocus()
        }
        container.addView(titleInput)

        val descInput = android.widget.EditText(safeContext).apply {
            hint = "Description (optional)"
        }
        container.addView(descInput)

        MaterialAlertDialogBuilder(safeContext)
            .setTitle("New Task")
            .setView(container)
            .setPositiveButton("Add") { _, _ ->
                val title = titleInput.text.toString().trim()
                if (title.isNotEmpty()) {
                    val desc = descInput.text.toString().trim().ifEmpty { null }
                    // Assign a default dueDate (e.g. today) so it shows up in "Today" list
                    val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
                    viewModel.addTask(title, desc, todayStr)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun toggleVisibility(recyclerView: RecyclerView, icon: View) {
        val isVisible = recyclerView.visibility == View.VISIBLE
        if (isVisible) {
            recyclerView.visibility = View.GONE
            icon.rotation = -90f // Convert down chevron to right chevron
        } else {
            recyclerView.visibility = View.VISIBLE
            icon.rotation = 0f // Down chevron
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    adapterOverdue.submitList(state.overdueTasks)
                    adapterToday.submitList(state.todayTasks)
                    adapterTomorrow.submitList(state.tomorrowTasks)
                    adapterUpcoming.submitList(state.upcomingTasks)
                    adapterDone.submitList(state.doneTasks)

                    binding.tvOverdueCount.text = state.overdueTasks.size.toString()
                    binding.tvTodayCount.text = state.todayTasks.size.toString()
                    binding.tvTomorrowCount.text = state.tomorrowTasks.size.toString()
                    binding.tvUpcomingCount.text = state.upcomingTasks.size.toString()
                    binding.tvDoneCount.text = state.doneTasks.size.toString()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
