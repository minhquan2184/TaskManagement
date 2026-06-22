package vn.edu.usth.taskmanagement.presentation.myday

import vn.edu.usth.taskmanagement.domain.model.TaskModel

sealed class MyDayItem {
    data class Header(
        val workspaceId: String,
        val title: String,
        val isPersonal: Boolean,
        val taskCount: Int
    ) : MyDayItem()

    data class TaskNode(
        val task: TaskModel
    ) : MyDayItem()
}
