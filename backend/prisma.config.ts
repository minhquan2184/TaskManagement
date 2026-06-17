import { defineConfig } from '@prisma/config';
import dotenv from 'dotenv';

dotenv.config();

export default defineConfig({
  datasource: {
    url: process.env.DATABASE_URL?.replace('-pooler', ''),
  },
  migrations: {
    seed: 'ts-node prisma/seed.ts',
  },
});
