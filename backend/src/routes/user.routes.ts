import { Router } from 'express';
import { updateFcmToken } from '../controllers/user.controller';
import { authMiddleware } from '../middleware/auth.middleware';

const router = Router();

// Apply auth middleware to all user routes
router.use(authMiddleware);

// /api/users
router.put('/fcm-token', updateFcmToken);

export default router;
