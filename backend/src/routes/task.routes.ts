import { Router } from 'express';
import { getTasks, getTodayTasks, createTask, updateTask, updateTaskStatus, deleteTask, addTaskChat } from '../controllers/task.controller';
import { authMiddleware } from '../middleware/auth.middleware';

const router = Router();

// Apply auth middleware to all task routes
router.use(authMiddleware);

// /api/tasks
router.get('/', getTasks);
router.get('/today', getTodayTasks);
router.post('/', createTask);
router.put('/:id', updateTask);
router.put('/:id/status', updateTaskStatus);
router.delete('/:id', deleteTask);
router.post('/:id/chat', addTaskChat);

export default router;
