package vn.edu.usth.taskmanagement.data

import vn.edu.usth.taskmanagement.service.TaskEntity
import vn.edu.usth.taskmanagement.service.WorkspaceEntity
import vn.edu.usth.taskmanagement.api.TaskDto
import vn.edu.usth.taskmanagement.api.WorkspaceDto
import vn.edu.usth.taskmanagement.domain.model.TaskModel
import vn.edu.usth.taskmanagement.domain.model.Workspace

// ==========================================
// Workspace Mappers
// ==========================================

fun WorkspaceDto.toDomain(): Workspace = Workspace(
    id = id,
    title = title,
    description = description,
    isPersonal = isPersonal,
    memberCount = memberCount,
    taskCount = taskCount,
    completedTaskCount = completedTaskCount,
    progressPercent = progressPercent
)

fun WorkspaceDto.toEntity(): WorkspaceEntity = WorkspaceEntity(
    id = id,
    title = title,
    description = description,
    isPersonal = isPersonal,
    ownerId = ownerId,
    memberCount = memberCount,
    taskCount = taskCount,
    completedTaskCount = completedTaskCount,
    progressPercent = progressPercent
)

fun WorkspaceEntity.toDomain(): Workspace = Workspace(
    id = id,
    title = title,
    description = description,
    isPersonal = isPersonal,
    memberCount = memberCount,
    taskCount = taskCount,
    completedTaskCount = completedTaskCount,
    progressPercent = progressPercent
)

// ==========================================
// Task Mappers
// ==========================================

fun TaskDto.toDomain(): TaskModel = TaskModel(
    id = id,
    workspaceId = workspaceId,
    createdBy = createdBy,
    assigneeId = assigneeId,
    title = title,
    description = description,
    status = status,
    priority = priority,
    dueDate = dueDate,
    calendarEventId = null
)

fun TaskDto.toEntity(): TaskEntity = TaskEntity(
    id = id,
    workspaceId = workspaceId,
    createdBy = createdBy,
    assigneeId = assigneeId,
    title = title,
    description = description,
    status = status,
    priority = priority,
    dueDate = dueDate,
    calendarEventId = null
)

fun TaskEntity.toDomain(): TaskModel = TaskModel(
    id = id,
    workspaceId = workspaceId,
    createdBy = createdBy,
    assigneeId = assigneeId,
    title = title,
    description = description,
    status = status,
    priority = priority,
    dueDate = dueDate,
    calendarEventId = calendarEventId
)

