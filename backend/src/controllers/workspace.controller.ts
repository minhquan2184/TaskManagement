import { Request, Response } from 'express';
import { prisma } from '../index';
import crypto from 'crypto';
import { sendInvitationEmail } from '../services/email.service';

// GET /api/workspaces — Lấy tất cả workspace (kèm stats)
export const getWorkspaces = async (req: Request, res: Response) => {
  try {
    const userId = req.user!.userId;
    const workspaces = await prisma.workspace.findMany({
      where: {
        members: {
          some: { userId }
        }
      },
      include: {
        _count: {
          select: {
            members: true,
            tasks: true,
          }
        },
        tasks: {
          select: { status: true },
        },
      },
      orderBy: [
        { isPersonal: 'desc' },  // Personal workspace hiển thị trước
        { createdAt: 'asc' },
      ],
    });

    // Map thêm stats vào response
    const result = workspaces.map(ws => {
      const totalTasks = ws._count.tasks;
      const completedTasks = ws.tasks.filter((t: any) => t.status === 'DONE').length;
      const progressPercent = totalTasks > 0 ? Math.round((completedTasks / totalTasks) * 100) : 0;

      return {
        id: ws.id,
        title: ws.title,
        description: ws.description,
        isPersonal: ws.isPersonal,
        ownerId: ws.ownerId,
        memberCount: ws._count.members,
        taskCount: totalTasks,
        completedTaskCount: completedTasks,
        progressPercent,
        createdAt: ws.createdAt,
        updatedAt: ws.updatedAt,
      };
    });

    res.json(result);
  } catch (error) {
    console.error(error);
    res.status(500).json({ error: 'Failed to fetch workspaces' });
  }
};

// GET /api/workspaces/:id — Lấy chi tiết 1 workspace
export const getWorkspaceById = async (req: Request, res: Response) => {
  try {
    const id = req.params.id as string;
    const userId = req.user!.userId;

    const workspace = await prisma.workspace.findUnique({
      where: { id },
      include: {
        members: {
          include: {
            user: { select: { id: true, email: true, fullName: true, avatarUrl: true } }
          }
        },
        _count: {
          select: { tasks: true, members: true }
        },
        tasks: {
          select: { status: true },
        },
      },
    });

    if (!workspace) {
      return res.status(404).json({ error: 'Workspace not found' });
    }

    const isMember = workspace.members.some((m: any) => m.userId === userId);
    if (!isMember) {
      return res.status(403).json({ error: 'Not authorized to access this workspace' });
    }

    const totalTasks = workspace._count.tasks;
    const completedTasks = workspace.tasks.filter((t: any) => t.status === 'DONE').length;

    res.json({
      id: workspace.id,
      title: workspace.title,
      description: workspace.description,
      isPersonal: workspace.isPersonal,
      ownerId: workspace.ownerId,
      memberCount: workspace._count.members,
      taskCount: totalTasks,
      completedTaskCount: completedTasks,
      progressPercent: totalTasks > 0 ? Math.round((completedTasks / totalTasks) * 100) : 0,
      members: workspace.members.map((m: any) => ({
        userId: m.user.id,
        email: m.user.email,
        fullName: m.user.fullName,
        avatarUrl: m.user.avatarUrl,
        role: m.role,
      })),
      createdAt: workspace.createdAt,
      updatedAt: workspace.updatedAt,
    });
  } catch (error) {
    console.error(error);
    res.status(500).json({ error: 'Failed to fetch workspace' });
  }
};

// POST /api/workspaces — Tạo workspace mới
export const createWorkspace = async (req: Request, res: Response) => {
  try {
    const { title, description, isPersonal } = req.body;
    const resolvedOwnerId = req.user!.userId;

    const workspace = await prisma.workspace.create({
      data: {
        title,
        description,
        isPersonal: isPersonal || false,
        ownerId: resolvedOwnerId,
        members: {
          create: {
            userId: resolvedOwnerId,
            role: 'ADMIN',
          },
        },
      },
    });

    res.status(201).json({
      ...workspace,
      memberCount: 1,
      taskCount: 0,
      completedTaskCount: 0,
      progressPercent: 0,
    });
  } catch (error) {
    console.error(error);
    res.status(500).json({ error: 'Failed to create workspace' });
  }
};

// PUT /api/workspaces/:id — Cập nhật workspace
export const updateWorkspace = async (req: Request, res: Response) => {
  try {
    const id = req.params.id as string;
    const userId = req.user!.userId;
    const { title, description } = req.body;

    // Verify workspace exists and user is a member
    const workspace = await prisma.workspace.findUnique({
      where: { id },
      include: { members: true },
    });
    if (!workspace) {
      return res.status(404).json({ error: 'Workspace not found' });
    }
    const member = workspace.members.find((m: any) => m.userId === userId);
    if (!member) {
      return res.status(403).json({ error: 'Not authorized to update this workspace' });
    }
    // Only ADMIN or owner can update
    if (member.role !== 'ADMIN' && workspace.ownerId !== userId) {
      return res.status(403).json({ error: 'Only admin can update workspace' });
    }

    const data: any = {};
    if (title !== undefined) data.title = title;
    if (description !== undefined) data.description = description;

    const updated = await prisma.workspace.update({
      where: { id },
      data,
      include: {
        _count: { select: { tasks: true, members: true } },
        tasks: { select: { status: true } },
      },
    });

    const totalTasks = updated._count.tasks;
    const completedTasks = updated.tasks.filter((t: any) => t.status === 'DONE').length;

    res.json({
      id: updated.id,
      title: updated.title,
      description: updated.description,
      isPersonal: updated.isPersonal,
      ownerId: updated.ownerId,
      memberCount: updated._count.members,
      taskCount: totalTasks,
      completedTaskCount: completedTasks,
      progressPercent: totalTasks > 0 ? Math.round((completedTasks / totalTasks) * 100) : 0,
      createdAt: updated.createdAt,
      updatedAt: updated.updatedAt,
    });
  } catch (error) {
    console.error(error);
    res.status(500).json({ error: 'Failed to update workspace' });
  }
};

// DELETE /api/workspaces/:id — Xóa workspace (cascade deletes tasks & members)
export const deleteWorkspace = async (req: Request, res: Response) => {
  try {
    const id = req.params.id as string;
    const userId = req.user!.userId;

    const workspace = await prisma.workspace.findUnique({
      where: { id },
      include: { members: true },
    });
    if (!workspace) {
      return res.status(404).json({ error: 'Workspace not found' });
    }
    if (workspace.isPersonal) {
      return res.status(400).json({ error: 'Cannot delete personal workspace' });
    }
    // Only the owner can delete
    if (workspace.ownerId !== userId) {
      return res.status(403).json({ error: 'Only the owner can delete this workspace' });
    }

    await prisma.workspace.delete({ where: { id } });
    res.json({ message: 'Workspace deleted successfully' });
  } catch (error) {
    console.error(error);
    res.status(500).json({ error: 'Failed to delete workspace' });
  }
};

// POST /api/workspaces/:id/invite — Mời thành viên vào workspace
export const inviteMember = async (req: Request, res: Response) => {
  try {
    const id = req.params.id as string;
    const currentUserId = req.user!.userId;
    const { email } = req.body;

    if (!email) {
      return res.status(400).json({ error: 'Email is required' });
    }

    const workspace = await prisma.workspace.findUnique({
      where: { id },
      include: { members: true },
    });

    if (!workspace) {
      return res.status(404).json({ error: 'Workspace not found' });
    }

    const currentUserMember = workspace.members.find((m: any) => m.userId === currentUserId);
    if (!currentUserMember) {
      return res.status(403).json({ error: 'Not authorized to invite members' });
    }

    if (currentUserMember.role !== 'ADMIN' && workspace.ownerId !== currentUserId) {
      return res.status(403).json({ error: 'Only admins can invite members' });
    }

    const invitedUser = await prisma.user.findUnique({
      where: { email },
    });

    if (!invitedUser) {
      return res.status(404).json({ error: 'User not found. Please ask them to register first.' });
    }

    const isAlreadyMember = workspace.members.some((m: any) => m.userId === invitedUser.id);
    if (isAlreadyMember) {
      return res.status(400).json({ error: 'User is already a member of this workspace' });
    }

    // Check for pending invitation
    const existingInvite = await prisma.workspaceInvitation.findFirst({
      where: { workspaceId: id, email, status: 'PENDING' }
    });

    if (existingInvite) {
      return res.status(400).json({ error: 'An invitation has already been sent to this user' });
    }

    const token = crypto.randomBytes(32).toString('hex');

    await prisma.workspaceInvitation.create({
      data: {
        workspaceId: id,
        inviterId: currentUserId,
        email,
        token,
      }
    });

    // Assume current user profile is needed for name
    const inviter = await prisma.user.findUnique({ where: { id: currentUserId }});
    const inviterName = inviter?.fullName || inviter?.email || 'A member';
    
    // Construct the backend URL (you may want to put this in env variables, but using req.headers.host for now)
    const protocol = req.protocol || 'http';
    const host = req.get('host') || 'localhost:3000';
    const acceptLink = `${protocol}://${host}/api/workspaces/invite/accept?token=${token}`;

    await sendInvitationEmail(email, workspace.title, inviterName, acceptLink);

    res.status(200).json({
      message: 'Invitation email sent successfully',
    });
  } catch (error) {
    console.error(error);
    res.status(500).json({ error: 'Failed to send invitation email' });
  }
};

// GET /api/workspaces/invite/accept — Xử lý khi người dùng ấn vào link trong email
export const acceptInvitation = async (req: Request, res: Response) => {
  try {
    const token = req.query.token as string;

    if (!token) {
      return res.status(400).send('<h1>Invalid Link</h1><p>Token is missing.</p>');
    }

    const invitation = await prisma.workspaceInvitation.findUnique({
      where: { token },
      include: { workspace: true }
    });

    if (!invitation) {
      return res.status(404).send('<h1>Link Expired or Invalid</h1><p>This invitation does not exist or has already been used.</p>');
    }

    if (invitation.status !== 'PENDING') {
      return res.status(400).send('<h1>Invitation Already Accepted</h1><p>You have already joined this group.</p>');
    }

    // Find user by email
    const user = await prisma.user.findUnique({
      where: { email: invitation.email }
    });

    if (!user) {
      // Should not happen if we checked before inviting, but just in case
      return res.status(404).send('<h1>Account Not Found</h1><p>Please register an account first with this email.</p>');
    }

    // Add to workspace
    await prisma.workspaceMember.upsert({
      where: {
        workspaceId_userId: {
          workspaceId: invitation.workspaceId,
          userId: user.id
        }
      },
      update: {},
      create: {
        workspaceId: invitation.workspaceId,
        userId: user.id,
        role: 'MEMBER'
      }
    });

    // Update invitation status
    await prisma.workspaceInvitation.update({
      where: { id: invitation.id },
      data: { status: 'ACCEPTED' }
    });

    res.status(200).send(`
      <div style="font-family: sans-serif; text-align: center; margin-top: 50px;">
        <h1 style="color: #4CAF50;">Success!</h1>
        <p>You have successfully joined the workspace <strong>${invitation.workspace.title}</strong>.</p>
        <p>Please open the Task Management app to see the new workspace.</p>
      </div>
    `);
  } catch (error) {
    console.error(error);
    res.status(500).send('<h1>System Error</h1><p>Please try again later.</p>');
  }
};

// POST /api/workspaces/:id/leave — Rời khỏi workspace
export const leaveWorkspace = async (req: Request, res: Response) => {
  try {
    const id = req.params.id as string;
    const currentUserId = req.user!.userId;

    const workspace = await prisma.workspace.findUnique({
      where: { id },
      include: { members: true },
    });

    if (!workspace) {
      return res.status(404).json({ error: 'Workspace not found' });
    }

    const currentUserMember = workspace.members.find((m: any) => m.userId === currentUserId);
    if (!currentUserMember) {
      return res.status(400).json({ error: 'You are not a member of this workspace' });
    }

    if (workspace.ownerId === currentUserId) {
      return res.status(400).json({ error: 'Owner cannot leave their own workspace. Please delete the workspace instead.' });
    }

    await prisma.workspaceMember.delete({
      where: {
        workspaceId_userId: {
          workspaceId: id,
          userId: currentUserId,
        }
      }
    });

    res.status(200).json({ message: 'Left workspace successfully' });
  } catch (error) {
    console.error(error);
    res.status(500).json({ error: 'Failed to leave workspace' });
  }
};
