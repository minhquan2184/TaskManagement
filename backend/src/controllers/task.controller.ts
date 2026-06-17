import { Request, Response } from 'express';
import { prisma } from '../index';
import { broadcastWorkspaceProgress, broadcastTaskChatMessage, sendPushNotification } from '../services/firebase.service';

// GET /api/tasks — Lấy tất cả task, hỗ trợ filter theo workspaceId
export const getTasks = async (req: Request, res: Response) => {
  try {
    const { workspaceId } = req.query;
    const userId = req.user!.userId;
    
    const where: any = {
      workspace: {
        members: {
          some: { userId }
        }
      }
    };
    if (workspaceId) {
      where.workspaceId = workspaceId as string;
    }

    const tasks = await prisma.task.findMany({
      where,
      include: {
        workspace: { select: { id: true, title: true, isPersonal: true } },
        tags: true,
      },
      orderBy: { createdAt: 'desc' },
    });
    res.json(tasks);
  } catch (error) {
    console.error(error);
    res.status(500).json({ error: 'Failed to fetch tasks' });
  }
};

// GET /api/tasks/today — Lấy task có dueDate là hôm nay
export const getTodayTasks = async (req: Request, res: Response) => {
  try {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const tomorrow = new Date(today);
    tomorrow.setDate(tomorrow.getDate() + 1);

    const userId = req.user!.userId;
    const tasks = await prisma.task.findMany({
      where: {
        dueDate: {
          gte: today,
          lt: tomorrow,
        },
        status: { not: 'ARCHIVED' },
        workspace: {
          members: {
            some: { userId }
          }
        }
      },
      include: {
        workspace: { select: { id: true, title: true, isPersonal: true } },
        tags: true,
      },
      orderBy: { createdAt: 'desc' },
    });
    res.json(tasks);
  } catch (error) {
    console.error(error);
    res.status(500).json({ error: 'Failed to fetch today tasks' });
  }
};

// POST /api/tasks — Tạo task mới
export const createTask = async (req: Request, res: Response) => {
  try {
    const { title, description, workspaceId, priority, dueDate, assigneeId } = req.body;
    const createdBy = req.user!.userId;

    if (!workspaceId) {
      return res.status(400).json({ error: 'workspaceId is required' });
    }

    // Verify user is a member of the workspace
    const isMember = await prisma.workspaceMember.findUnique({
      where: { workspaceId_userId: { workspaceId, userId: createdBy } }
    });
    if (!isMember) {
      return res.status(403).json({ error: 'Not authorized to create task in this workspace' });
    }

    const newTask = await prisma.task.create({
      data: {
        title,
        description,
        workspaceId,
        createdBy,
        assigneeId,
        priority: priority || 'MEDIUM',
        dueDate: dueDate ? new Date(dueDate) : null,
      }
    });

    if (assigneeId && assigneeId !== createdBy) {
      const assignee = await prisma.user.findUnique({ where: { id: assigneeId } });
      if (assignee?.fcmToken) {
        sendPushNotification(
          assignee.fcmToken,
          `New Task Assigned`,
          `You were assigned to: ${title}`,
          { taskId: newTask.id }
        );
      }
    }

    res.status(201).json(newTask);
  } catch (error) {
    console.error(error);
    res.status(500).json({ error: 'Failed to create task' });
  }
};

// PUT /api/tasks/:id/status — Cập nhật trạng thái task
import { TaskStatus, Priority } from '@prisma/client';

export const updateTaskStatus = async (req: Request, res: Response) => {
  try {
    const id = req.params.id as string;
    const { status } = req.body;
    const updatedTask = await prisma.task.update({
      where: { id },
      data: { status: status as TaskStatus }
    });

    // Tính toán lại tiến độ workspace và gửi qua Firebase
    const workspaceId = updatedTask.workspaceId;
    const allTasks = await prisma.task.findMany({ where: { workspaceId } });
    const total = allTasks.length;
    const done = allTasks.filter(t => t.status === 'DONE').length;
    await broadcastWorkspaceProgress(workspaceId, total, done);

    res.json(updatedTask);
  } catch (error) {
    console.error(error);
    res.status(500).json({ error: 'Failed to update task status' });
  }
};

// PUT /api/tasks/:id — Cập nhật thông tin task
export const updateTask = async (req: Request, res: Response) => {
  try {
    const id = req.params.id as string;
    const userId = req.user!.userId;
    const { title, description, priority, dueDate, status, assigneeId } = req.body;

    // Verify the task exists and user has access
    const existingTask = await prisma.task.findUnique({
      where: { id },
      include: { workspace: { include: { members: true } } }
    });
    if (!existingTask) {
      return res.status(404).json({ error: 'Task not found' });
    }
    const isMember = existingTask.workspace.members.some((m: any) => m.userId === userId);
    if (!isMember) {
      return res.status(403).json({ error: 'Not authorized to update this task' });
    }

    const data: any = {};
    if (title !== undefined) data.title = title;
    if (description !== undefined) data.description = description;
    if (priority !== undefined) data.priority = priority as Priority;
    if (dueDate !== undefined) data.dueDate = dueDate ? new Date(dueDate) : null;
    if (status !== undefined) data.status = status as TaskStatus;
    if (assigneeId !== undefined) data.assigneeId = assigneeId;

    const updatedTask = await prisma.task.update({
      where: { id },
      data,
    });
    res.json(updatedTask);
  } catch (error) {
    console.error(error);
    res.status(500).json({ error: 'Failed to update task' });
  }
};

// DELETE /api/tasks/:id — Xóa task
export const deleteTask = async (req: Request, res: Response) => {
  try {
    const id = req.params.id as string;
    const userId = req.user!.userId;

    // Verify the task exists and user has access
    const existingTask = await prisma.task.findUnique({
      where: { id },
      include: { workspace: { include: { members: true } } }
    });
    if (!existingTask) {
      return res.status(404).json({ error: 'Task not found' });
    }
    const isMember = existingTask.workspace.members.some((m: any) => m.userId === userId);
    if (!isMember) {
      return res.status(403).json({ error: 'Not authorized to delete this task' });
    }

    await prisma.task.delete({ where: { id } });

    // Recalculate workspace progress
    const workspaceId = existingTask.workspaceId;
    const allTasks = await prisma.task.findMany({ where: { workspaceId } });
    const total = allTasks.length;
    const done = allTasks.filter(t => t.status === 'DONE').length;
    await broadcastWorkspaceProgress(workspaceId, total, done);

    res.json({ message: 'Task deleted successfully' });
  } catch (error) {
    console.error(error);
    res.status(500).json({ error: 'Failed to delete task' });
  }
};

// POST /api/tasks/:id/chat — Gửi tin nhắn chat trong Task
export const addTaskChat = async (req: Request, res: Response) => {
  try {
    const taskId = req.params.id as string;
    const { content } = req.body;
    const resolvedActorId = req.user!.userId;

    // 1. Lưu vào PostgreSQL
    const event = await prisma.activityEvent.create({
      data: {
        taskId,
        actorId: resolvedActorId,
        eventType: 'CHAT_MESSAGE',
        content,
      },
      include: {
        actor: { select: { id: true, email: true, fullName: true, avatarUrl: true } }
      }
    });

    // 2. Broadcast qua Firebase RTDB
    const messagePayload = {
      id: event.id,
      actorId: event.actorId,
      actorName: event.actor.fullName || event.actor.email,
      actorAvatar: event.actor.avatarUrl,
      content: event.content,
      eventType: event.eventType,
    };
    await broadcastTaskChatMessage(taskId, messagePayload);

    // 3. Send Push Notifications
    const task = await prisma.task.findUnique({
      where: { id: taskId },
      include: {
        workspace: {
          include: {
            members: {
              include: { user: true }
            }
          }
        }
      }
    });

    if (task) {
      const senderName = event.actor.fullName || event.actor.email || 'Someone';
      task.workspace.members.forEach(member => {
        if (member.userId !== resolvedActorId && member.pushNotificationsEnabled && member.user.fcmToken) {
          sendPushNotification(
            member.user.fcmToken,
            `New message in ${task.title}`,
            `${senderName}: ${content}`,
            { taskId }
          );
        }
      });
    }

    res.status(201).json(event);
  } catch (error) {
    console.error(error);
    res.status(500).json({ error: 'Failed to send chat message' });
  }
};
