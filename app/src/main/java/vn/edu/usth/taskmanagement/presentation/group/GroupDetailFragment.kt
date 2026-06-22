package vn.edu.usth.taskmanagement.presentation.group

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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.widget.Toast
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import vn.edu.usth.taskmanagement.R
import vn.edu.usth.taskmanagement.databinding.FragmentGroupDetailBinding
import vn.edu.usth.taskmanagement.domain.model.TaskModel
import vn.edu.usth.taskmanagement.presentation.shared.TaskAdapter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GroupDetailFragment : Fragment() {

    private var _binding: FragmentGroupDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: GroupDetailViewModel by viewModel()
    private lateinit var taskAdapter: TaskAdapter

    private var workspaceId: String = ""
    private var workspaceTitle: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGroupDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        workspaceId = arguments?.getString("workspaceId") ?: ""
        workspaceTitle = arguments?.getString("workspaceTitle") ?: "Group"

        binding.tvGroupTitle.text = workspaceTitle

        setupRecyclerView()
        setupClickListeners()
        observeViewModel()

        if (workspaceId.isNotEmpty()) {
            viewModel.loadTasks(workspaceId)
        }
    }

    private fun setupRecyclerView() {
        taskAdapter = TaskAdapter(
            { task, isDone -> viewModel.toggleTaskStatus(task, isDone) },
            { task ->
                val bottomSheet = TaskDetailBottomSheetFragment().apply {
                    arguments = android.os.Bundle().apply {
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
        // Long press to show edit/delete popup
        taskAdapter.setOnItemLongClickListener { task, anchorView ->
            showTaskPopupMenu(task, anchorView)
        }
        binding.rvGroupTasks.adapter = taskAdapter
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.fabAddTask.setOnClickListener {
            showAddTaskDialog()
        }

        // Group options menu (edit/delete group)
        binding.btnOptions.setOnClickListener {
            showGroupOptionsMenu(it)
        }
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

    private fun showGroupOptionsMenu(anchorView: View) {
        val popup = PopupMenu(requireContext(), anchorView)
        popup.menu.add(0, 1, 0, "Invite Member")
        popup.menu.add(0, 2, 1, "Edit Group")
        popup.menu.add(0, 3, 2, "Leave Group")
        popup.menu.add(0, 4, 3, "Delete Group")

        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                1 -> {
                    showInviteMemberDialog()
                    true
                }
                2 -> {
                    showEditGroupDialog()
                    true
                }
                3 -> {
                    showLeaveGroupConfirmation()
                    true
                }
                4 -> {
                    showDeleteGroupConfirmation()
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun showInviteMemberDialog() {
        val safeContext = context ?: return

        val container = android.widget.LinearLayout(safeContext).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(64, 32, 64, 0)
        }

        val emailInput = android.widget.EditText(safeContext).apply {
            hint = "Member's email"
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        }
        container.addView(emailInput)

        MaterialAlertDialogBuilder(safeContext)
            .setTitle("Invite Member")
            .setView(container)
            .setPositiveButton("Invite") { _, _ ->
                val email = emailInput.text.toString().trim()
                if (email.isNotEmpty()) {
                    viewModel.inviteMember(workspaceId, email)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showLeaveGroupConfirmation() {
        val safeContext = context ?: return
        MaterialAlertDialogBuilder(safeContext)
            .setTitle("Leave Group")
            .setMessage("Are you sure you want to leave \"$workspaceTitle\"?")
            .setPositiveButton("Leave") { _, _ ->
                viewModel.leaveWorkspace(workspaceId)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditGroupDialog() {
        val safeContext = context ?: return

        val container = android.widget.LinearLayout(safeContext).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(64, 32, 64, 0)
        }

        val titleInput = android.widget.EditText(safeContext).apply {
            hint = "Group name"
            setText(workspaceTitle)
        }
        container.addView(titleInput)

        val descInput = android.widget.EditText(safeContext).apply {
            hint = "Description (optional)"
        }
        container.addView(descInput)

        MaterialAlertDialogBuilder(safeContext)
            .setTitle("Edit Group")
            .setView(container)
            .setPositiveButton("Save") { _, _ ->
                val newTitle = titleInput.text.toString().trim()
                if (newTitle.isNotEmpty()) {
                    val newDesc = descInput.text.toString().trim().ifEmpty { null }
                    viewModel.updateWorkspace(workspaceId, newTitle, newDesc)
                    workspaceTitle = newTitle
                    binding.tvGroupTitle.text = newTitle
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteGroupConfirmation() {
        val safeContext = context ?: return
        MaterialAlertDialogBuilder(safeContext)
            .setTitle("Delete Group")
            .setMessage("Are you sure you want to delete \"$workspaceTitle\"? All tasks in this group will also be deleted.")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteWorkspace(workspaceId)
            }
            .setNegativeButton("Cancel", null)
            .show()
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

        val dateBtn = android.widget.Button(safeContext).apply {
            text = "Set due date (optional)"
            isAllCaps = false
        }
        var selectedDate: String? = null
        dateBtn.setOnClickListener {
            val cal = java.util.Calendar.getInstance()
            android.app.DatePickerDialog(safeContext, { _, year, month, day ->
                selectedDate = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day)
                dateBtn.text = selectedDate
            }, cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH), cal.get(java.util.Calendar.DAY_OF_MONTH)).show()
        }
        container.addView(dateBtn)

        MaterialAlertDialogBuilder(safeContext)
            .setTitle("New Task in $workspaceTitle")
            .setView(container)
            .setPositiveButton("Add") { _, _ ->
                val title = titleInput.text.toString().trim()
                if (title.isNotEmpty()) {
                    val desc = descInput.text.toString().trim().ifEmpty { null }
                    viewModel.addTask(title, desc, workspaceId, selectedDate)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    // Navigate back if workspace was deleted
                    if (state.workspaceDeleted) {
                        findNavController().popBackStack()
                        return@collect
                    }

                    binding.progressLoading.visibility =
                        if (state.isLoading) View.VISIBLE else View.GONE

                    taskAdapter.submitList(state.tasks)

                    val hasTasks = state.tasks.isNotEmpty()
                    binding.rvGroupTasks.visibility = if (hasTasks) View.VISIBLE else View.GONE
                    binding.tvEmpty.visibility =
                        if (!hasTasks && !state.isLoading) View.VISIBLE else View.GONE

                    val total = state.progress?.totalTasks ?: state.tasks.size
                    val done = state.progress?.completedTasks ?: state.tasks.count { it.isDone }
                    binding.tvGroupMeta.text = "$total tasks • $done done"
                    binding.progressGroup.max = if (total > 0) total else 1
                    binding.progressGroup.progress = done
                    
                    if (state.error != null) {
                        Toast.makeText(context, state.error, Toast.LENGTH_LONG).show()
                        viewModel.clearMessages()
                    }
                    
                    if (state.actionSuccessMessage != null) {
                        Toast.makeText(context, state.actionSuccessMessage, Toast.LENGTH_SHORT).show()
                        viewModel.clearMessages()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
