package vn.edu.usth.taskmanagement.task

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import android.widget.PopupMenu
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import vn.edu.usth.taskmanagement.databinding.FragmentTaskDetailBottomSheetBinding
import vn.edu.usth.taskmanagement.adapter.ChatAdapter

class TaskDetailBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentTaskDetailBottomSheetBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TaskDetailViewModel by viewModel()
    private lateinit var chatAdapter: ChatAdapter

    private var taskId: String = ""
    private var taskTitle: String = ""
    private var workspaceId: String = ""
    private var priority: String = "MEDIUM"
    private var assigneeId: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTaskDetailBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        taskId = arguments?.getString("taskId") ?: ""
        taskTitle = arguments?.getString("taskTitle") ?: "Task Detail"
        val taskDesc = arguments?.getString("taskDesc") ?: ""
        workspaceId = arguments?.getString("workspaceId") ?: ""
        priority = arguments?.getString("priority") ?: "MEDIUM"
        assigneeId = arguments?.getString("assigneeId") ?: ""

        binding.tvTaskTitle.text = taskTitle
        if (taskDesc.isNotEmpty()) {
            binding.tvTaskDesc.text = taskDesc
        } else {
            binding.tvTaskDesc.visibility = View.GONE
        }
        
        binding.tvPriority.text = "Priority: $priority"
        binding.tvAssignee.text = if (assigneeId.isNotEmpty()) "Assignee: Loading..." else "Assignee: Unassigned"

        setupRecyclerView()
        setupClickListeners()
        observeViewModel()

        if (taskId.isNotEmpty()) {
            viewModel.loadChat(taskId)
        }
        if (workspaceId.isNotEmpty()) {
            viewModel.loadMembers(workspaceId)
        }
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter()
        binding.rvChat.adapter = chatAdapter
    }

    private fun setupClickListeners() {
        binding.btnSendChat.setOnClickListener {
            val content = binding.etChatMessage.text.toString().trim()
            if (content.isNotEmpty() && taskId.isNotEmpty()) {
                viewModel.sendMessage(taskId, content)
                binding.etChatMessage.text.clear()
            }
        }

        binding.tvPriority.setOnClickListener { view ->
            val popup = PopupMenu(requireContext(), view)
            val priorities = listOf("LOW", "MEDIUM", "HIGH", "URGENT")
            priorities.forEachIndexed { index, p -> popup.menu.add(0, index, index, p) }
            popup.setOnMenuItemClickListener { item ->
                val selected = priorities[item.itemId]
                binding.tvPriority.text = "Priority: $selected"
                viewModel.updateTaskPriority(taskId, selected)
                true
            }
            popup.show()
        }

        binding.tvAssignee.setOnClickListener { view ->
            val members = viewModel.uiState.value.members
            if (members.isEmpty()) return@setOnClickListener

            val popup = PopupMenu(requireContext(), view)
            popup.menu.add(0, -1, 0, "Unassigned")
            members.forEachIndexed { index, member -> 
                val name = member.fullName ?: member.email
                popup.menu.add(0, index, index + 1, name) 
            }
            popup.setOnMenuItemClickListener { item ->
                if (item.itemId == -1) {
                    binding.tvAssignee.text = "Assignee: Unassigned"
                    viewModel.assignTask(taskId, "")
                } else {
                    val selectedMember = members[item.itemId]
                    val name = selectedMember.fullName ?: selectedMember.email
                    binding.tvAssignee.text = "Assignee: $name"
                    viewModel.assignTask(taskId, selectedMember.userId)
                }
                true
            }
            popup.show()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    if (state.members.isNotEmpty() && assigneeId.isNotEmpty()) {
                        val assignedMember = state.members.find { it.userId == assigneeId }
                        if (assignedMember != null) {
                            val name = assignedMember.fullName ?: assignedMember.email
                            binding.tvAssignee.text = "Assignee: $name"
                        } else {
                            binding.tvAssignee.text = "Assignee: Unassigned"
                        }
                    }

                    chatAdapter.submitList(state.messages) {
                        if (state.messages.isNotEmpty()) {
                            binding.rvChat.scrollToPosition(state.messages.size - 1)
                        }
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
