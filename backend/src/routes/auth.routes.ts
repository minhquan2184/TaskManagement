import { Router } from 'express';
import { loginWithGoogle } from '../controllers/auth.controller';

const router = Router();

// Endpoint dứng đợi Token của Android: POST /api/auth/google
router.post('/google', loginWithGoogle);


export default router;
