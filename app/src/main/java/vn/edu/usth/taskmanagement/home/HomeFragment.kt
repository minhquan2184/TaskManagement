package vn.edu.usth.taskmanagement.home

import vn.edu.usth.taskmanagement.task.TaskDetailBottomSheetFragment
import vn.edu.usth.taskmanagement.auth.LoginActivity

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import vn.edu.usth.taskmanagement.R
import vn.edu.usth.taskmanagement.databinding.FragmentHomeBinding
import vn.edu.usth.taskmanagement.domain.model.TaskModel
import vn.edu.usth.taskmanagement.domain.model.Workspace
import vn.edu.usth.taskmanagement.adapter.HomeTaskAdapter
import vn.edu.usth.taskmanagement.adapter.WorkspaceAdapter
import vn.edu.usth.taskmanagement.service.SessionManager
import coil.load
import coil.transform.CircleCropTransformation
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

import org.koin.android.ext.android.inject

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModel()
    private val sessionManager: SessionManager by inject()

    private lateinit var taskAdapter: HomeTaskAdapter
    private lateinit var workspaceAdapter: WorkspaceAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupGreeting()
        setupRecyclerViews()
        setupNavigation()
        setupFab()
        observeUiState()
    }

    private fun setupGreeting() {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)

        val greeting = when {
            hour < 12 -> "Good morning"
            hour < 17 -> "Good afternoon"
            else -> "Good evening"
        }

        // Fetch user info from SessionManager
        val userName = sessionManager.getFullName() ?: sessionManager.getEmail()?.substringBefore("@") ?: ""
        binding.tvGreeting.text = if (userName.isNotEmpty()) "$greeting,\n$userName" else greeting

        // Load avatar using Coil
        val avatarUrl = sessionManager.getAvatarUrl()
        if (!avatarUrl.isNullOrEmpty()) {
            binding.avatarCircle.load(avatarUrl) {
                crossfade(true)
                placeholder(vn.edu.usth.taskmanagement.R.drawable.bg_avatar_placeholder)
                transformations(CircleCropTransformation())
            }
        }

        // Click avatar to log out
        binding.avatarCircle.setOnClickListener {
            com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("Log Out")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Log Out") { _, _ ->
                    sessionManager.logout()
                    val intent = android.content.Intent(requireActivity(), LoginActivity::class.java)
                    intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        val dateFormat = SimpleDateFormat("EEEE, MMMM d", Locale.US)
        binding.tvDate.text = dateFormat.format(calendar.time)
    }

    private fun setupRecyclerViews() {
        // My List tasks
        taskAdapter = HomeTaskAdapter(
            onTaskCheckedChange = { task, isDone ->
                viewModel.toggleTaskStatus(task, isDone)
            },
            onTaskClick = { task ->
                val bottomSheet = TaskDetailBottomSheetFragment().apply {
                    arguments = Bundle().apply {
                        putString("taskId", task.id)
                        putString("taskTitle", task.title)
                        putString("taskDesc", task.description ?: "")
                        putString("workspaceId", task.workspaceId)
                        putString("priority", task.priority)
                        putString("assigneeId", task.assigneeId ?: "")
                    }
                }
                bottomSheet.show(childFragmentManager, "TaskDetail")
            }
        )
        taskAdapter.setOnItemLongClickListener { task, anchorView ->
            showTaskPopupMenu(task, anchorView)
        }
        binding.rvMyListTasks.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = taskAdapter
        }

        // Groups
        workspaceAdapter = WorkspaceAdapter(
            onWorkspaceClick = { workspace ->
                val bundle = Bundle().apply {
                    putString("workspaceId", workspace.id)
                    putString("workspaceTitle", workspace.title)
                }
                findNavController().navigate(R.id.action_home_to_groupDetail, bundle)
            }
        )
        workspaceAdapter.setOnItemLongClickListener { workspace, anchorView ->
            showGroupPopupMenu(workspace, anchorView)
        }
        binding.rvGroups.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = workspaceAdapter
        }
    }

    private fun setupNavigation() {
        binding.cardMyDay.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_myDay)
        }
        binding.cardAllTasks.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_allTasks)
        }
        binding.cardCalendar.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_calendar)
        }
    }

    private fun setupFab() {
        binding.fabAddTask.setOnClickListener {
            showAddTaskDialog()
        }
        binding.btnAddGroup.setOnClickListener {
            showCreateGroupDialog()
        }
    }

    // ==========================================
    // Task Edit/Delete
    // ==========================================

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

        val container = LinearLayout(safeContext).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(64, 32, 64, 0)
        }

        val titleInput = EditText(safeContext).apply {
            hint = "Task title"
            setText(task.title)
        }
        container.addView(titleInput)

        val descInput = EditText(safeContext).apply {
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

        com.google.android.material.dialog.MaterialAlertDialogBuilder(safeContext)
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
        com.google.android.material.dialog.MaterialAlertDialogBuilder(safeContext)
            .setTitle("Delete Task")
            .setMessage("Are you sure you want to delete \"${task.title}\"?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteTask(task.id)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ==========================================
    // Group Edit/Delete
    // ==========================================

    private fun showGroupPopupMenu(workspace: Workspace, anchorView: View) {
        if (workspace.isPersonal) return // Can't edit/delete personal workspace

        val popup = PopupMenu(requireContext(), anchorView)
        popup.menu.add(0, 1, 0, "Edit Group")
        popup.menu.add(0, 2, 1, "Delete Group")

        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                1 -> {
                    showEditGroupDialog(workspace)
                    true
                }
                2 -> {
                    showDeleteGroupConfirmation(workspace)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun showEditGroupDialog(workspace: Workspace) {
        val safeContext = context ?: return

        val container = LinearLayout(safeContext).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(64, 32, 64, 0)
        }

        val titleInput = EditText(safeContext).apply {
            hint = "Group name"
            setText(workspace.title)
        }
        container.addView(titleInput)

        val descInput = EditText(safeContext).apply {
            hint = "Description (optional)"
            setText(workspace.description ?: "")
        }
        container.addView(descInput)

        com.google.android.material.dialog.MaterialAlertDialogBuilder(safeContext)
            .setTitle("Edit Group")
            .setView(container)
            .setPositiveButton("Save") { _, _ ->
                val newTitle = titleInput.text.toString().trim()
                if (newTitle.isNotEmpty()) {
                    val newDesc = descInput.text.toString().trim().ifEmpty { null }
                    viewModel.updateGroup(workspace.id, newTitle, newDesc)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteGroupConfirmation(workspace: Workspace) {
        val safeContext = context ?: return
        com.google.android.material.dialog.MaterialAlertDialogBuilder(safeContext)
            .setTitle("Delete Group")
            .setMessage("Are you sure you want to delete \"${workspace.title}\"? All tasks in this group will also be deleted.")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteGroup(workspace.id)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAddTaskDialog() {
        val context = context ?: return

        var selectedDate: String? = null
        var selectedTime: String? = null

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(64, 32, 64, 0)
        }

        val titleInput = EditText(context).apply {
            hint = "Task title"
            requestFocus()
        }
        container.addView(titleInput)

        val descInput = EditText(context).apply {
            hint = "Description (optional)"
        }
        container.addView(descInput)

        // Date picker button
        val dateBtn = android.widget.Button(context).apply {
            text = "Set due date (optional)"
            isAllCaps = false
            setOnClickListener {
                val cal = java.util.Calendar.getInstance()
                android.app.DatePickerDialog(context, { _, year, month, day ->
                    selectedDate = String.format(java.util.Locale.US, "%04d-%02d-%02d", year, month + 1, day)
                    this.text = selectedDate
                }, cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH), cal.get(java.util.Calendar.DAY_OF_MONTH)).show()
            }
        }
        container.addView(dateBtn)

        // Time picker button
        val timeBtn = android.widget.Button(context).apply {
            text = "Set time (optional)"
            isAllCaps = false
            setOnClickListener {
                val cal = java.util.Calendar.getInstance()
                android.app.TimePickerDialog(context, { _, hour, minute ->
                    selectedTime = String.format(java.util.Locale.US, "%02d:%02d", hour, minute)
                    this.text = selectedTime
                }, cal.get(java.util.Calendar.HOUR_OF_DAY), cal.get(java.util.Calendar.MINUTE), true).show()
            }
        }
        container.addView(timeBtn)

        AlertDialog.Builder(context)
            .setTitle("New Task")
            .setView(container)
            .setPositiveButton("Add") { _, _ ->
                val title = titleInput.text.toString().trim()
                if (title.isNotEmpty()) {
                    val desc = descInput.text.toString().trim().ifEmpty { null }
                    val dueDate = if (selectedDate != null) {
                        if (selectedTime != null) "${selectedDate}T${selectedTime}:00" else selectedDate
                    } else null
                    viewModel.addTask(title, desc, dueDate)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showCreateGroupDialog() {
        val safeContext = context ?: return

        val container = android.widget.LinearLayout(safeContext).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(64, 32, 64, 0)
        }

        val titleInput = android.widget.EditText(safeContext).apply {
            hint = "Group name"
            requestFocus()
        }
        container.addView(titleInput)

        val descInput = android.widget.EditText(safeContext).apply {
            hint = "Description (optional)"
        }
        container.addView(descInput)

        com.google.android.material.dialog.MaterialAlertDialogBuilder(safeContext)
            .setTitle("Create New Group")
            .setView(container)
            .setPositiveButton("Create") { _, _ ->
                val title = titleInput.text.toString().trim()
                if (title.isNotEmpty()) {
                    val desc = descInput.text.toString().trim().ifEmpty { null }
                    viewModel.createGroup(title, desc)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    // Loading
                    binding.progressLoading.visibility =
                        if (state.isLoading) View.VISIBLE else View.GONE

                    // My List tasks
                    taskAdapter.submitList(state.personalTasks)
                    val hasPersonalTasks = state.personalTasks.isNotEmpty()
                    binding.rvMyListTasks.visibility =
                        if (hasPersonalTasks) View.VISIBLE else View.GONE
                    binding.tvMyListEmpty.visibility =
                        if (!hasPersonalTasks && !state.isLoading) View.VISIBLE else View.GONE
                    binding.tvMyListCount.text = state.personalTasks.size.toString()

                    // Groups
                    workspaceAdapter.submitList(state.groupWorkspaces)
                    val hasGroups = state.groupWorkspaces.isNotEmpty()
                    binding.rvGroups.visibility =
                        if (hasGroups) View.VISIBLE else View.GONE
                    binding.tvGroupsEmpty.visibility =
                        if (!hasGroups && !state.isLoading) View.VISIBLE else View.GONE

                    // Card counts removed as per request
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

