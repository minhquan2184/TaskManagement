import express from 'express';
import cors from 'cors';
import { PrismaClient } from '@prisma/client';
import { Pool } from 'pg';
import { PrismaPg } from '@prisma/adapter-pg';
import dotenv from 'dotenv';

dotenv.config();

const app = express();

const pool = new Pool({ connectionString: process.env.DATABASE_URL });
const adapter = new PrismaPg(pool);
export const prisma = new PrismaClient({ adapter });

app.use(cors());
app.use(express.json());

import taskRoutes from './routes/task.routes';
import authRoutes from './routes/auth.routes';
import workspaceRoutes from './routes/workspace.routes';
import userRoutes from './routes/user.routes';

app.use('/api/tasks', taskRoutes);
app.use('/api/auth', authRoutes);
app.use('/api/workspaces', workspaceRoutes);
app.use('/api/users', userRoutes);

// API kiểm tra trạng thái
app.get('/api/health', (req, res) => {
  res.json({ status: 'ok', message: 'Task Management API is running' });
});

const PORT = process.env.PORT || 3000;

app.listen(Number(PORT), '0.0.0.0', () => {
  console.log(`Server is running at: http://0.0.0.0:${PORT}`);
});
