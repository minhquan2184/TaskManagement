import { Router } from 'express';
import { getWorkspaces, getWorkspaceById, createWorkspace, updateWorkspace, deleteWorkspace, inviteMember, leaveWorkspace, acceptInvitation } from '../controllers/workspace.controller';
import { authMiddleware } from '../middleware/auth.middleware';

const router = Router();

// Public route for email link
router.get('/invite/accept', acceptInvitation);

// Apply auth middleware to all workspace routes
router.use(authMiddleware);

// /api/workspaces
router.get('/', getWorkspaces);
router.get('/:id', getWorkspaceById);
router.post('/', createWorkspace);
router.put('/:id', updateWorkspace);
router.delete('/:id', deleteWorkspace);
router.post('/:id/invite', inviteMember);
router.post('/:id/leave', leaveWorkspace);

export default router;
