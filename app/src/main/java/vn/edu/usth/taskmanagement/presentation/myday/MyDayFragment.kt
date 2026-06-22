package vn.edu.usth.taskmanagement.presentation.myday

import vn.edu.usth.taskmanagement.presentation.task.TaskDetailBottomSheetFragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import vn.edu.usth.taskmanagement.R
import vn.edu.usth.taskmanagement.databinding.FragmentMyDayBinding
import vn.edu.usth.taskmanagement.presentation.myday.MyDayAdapter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MyDayFragment : Fragment() {
    private var _binding: FragmentMyDayBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MyDayViewModel by viewModel()
    private lateinit var myDayAdapter: MyDayAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyDayBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupDateHeaders()
        setupRecyclerView()
        observeViewModel()

        binding.btnHome.setOnClickListener {
            findNavController().navigate(R.id.action_myDay_to_home)
        }

        binding.fabAddTask.setOnClickListener {
            showAddTaskDialog()
        }
    }

    private fun setupDateHeaders() {
        val today = Date()
        binding.tvDateSub.text = SimpleDateFormat("MMM d", Locale.US).format(today)
        binding.tvDateHeader.text = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.US).format(today)
    }

    private fun setupRecyclerView() {
        myDayAdapter = MyDayAdapter(
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
        binding.rvMyDay.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = myDayAdapter
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    myDayAdapter.submitList(state.items)
                    binding.tvEmpty.visibility = if (state.items.isEmpty() && !state.isLoading) View.VISIBLE else View.GONE
                    binding.rvMyDay.visibility = if (state.items.isEmpty()) View.GONE else View.VISIBLE
                }
            }
        }
    }

    private fun showAddTaskDialog() {
        val safeContext = context ?: return

        val container = LinearLayout(safeContext).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(64, 32, 64, 0)
        }

        val titleInput = EditText(safeContext).apply {
            hint = "Task title"
            requestFocus()
        }
        container.addView(titleInput)

        val descInput = EditText(safeContext).apply {
            hint = "Description (optional)"
        }
        container.addView(descInput)

        MaterialAlertDialogBuilder(safeContext)
            .setTitle("New Task for Today")
            .setView(container)
            .setPositiveButton("Add") { _, _ ->
                val title = titleInput.text.toString().trim()
                if (title.isNotEmpty()) {
                    val desc = descInput.text.toString().trim().ifEmpty { null }
                    
                    // Auto-assign dueDate to TODAY for tasks added from "My Day"
                    val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
                    
                    viewModel.addTask(title, desc, todayStr)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

