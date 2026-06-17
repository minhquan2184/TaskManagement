import { PrismaClient, TaskStatus, ActivityType, Priority, WorkspaceRole } from '@prisma/client';
import { Pool } from 'pg';
import { PrismaPg } from '@prisma/adapter-pg';
import dotenv from 'dotenv';

dotenv.config();

const pool = new Pool({ connectionString: process.env.DATABASE_URL });
const adapter = new PrismaPg(pool);
const prisma = new PrismaClient({ adapter });

async function main() {
  console.log('Start seeding...');

  // Xóa dữ liệu cũ (Tùy chọn, để đảm bảo chạy lại script không bị duplicate nếu không dùng upsert)
  await prisma.activityEvent.deleteMany();
  await prisma.subtask.deleteMany();
  await prisma.task.deleteMany();
  await prisma.tag.deleteMany();
  await prisma.workspaceMember.deleteMany();
  await prisma.workspace.deleteMany();
  await prisma.user.deleteMany();

  // 1. Tạo 2 Users
  const user1 = await prisma.user.upsert({
    where: { email: 'alice@example.com' },
    update: {},
    create: {
      email: 'alice@example.com',
      fullName: 'Alice Smith',
      fcmToken: 'token_alice_123',
    },
  });

  const user2 = await prisma.user.upsert({
    where: { email: 'bob@example.com' },
    update: {},
    create: {
      email: 'bob@example.com',
      fullName: 'Bob Johnson',
      fcmToken: 'token_bob_456',
    },
  });

  // 2. Tạo 2 Workspaces (1 Personal, 1 Group)
  const personalWorkspace = await prisma.workspace.create({
    data: {
      title: 'My Personal List',
      isPersonal: true,
      ownerId: user1.id,
      members: {
        create: {
          userId: user1.id,
          role: WorkspaceRole.ADMIN,
        },
      },
    },
  });

  const groupWorkspace = await prisma.workspace.create({
    data: {
      title: 'Marketing Campaign Q3',
      description: 'Tasks for the upcoming Q3 campaign',
      isPersonal: false,
      ownerId: user1.id,
      members: {
        create: [
          { userId: user1.id, role: WorkspaceRole.ADMIN },
          { userId: user2.id, role: WorkspaceRole.MEMBER },
        ],
      },
    },
  });

  // 3. Tạo 2 Tags
  const tagUrgent = await prisma.tag.create({
    data: { name: 'Urgent', color: '#FF0000' },
  });

  const tagDesign = await prisma.tag.create({
    data: { name: 'Design', color: '#0000FF' },
  });

  // 4. Tạo 2 Tasks (1 in Personal, 1 in Group)
  const task1 = await prisma.task.create({
    data: {
      title: 'Buy Groceries',
      description: 'Milk, Eggs, Bread',
      workspaceId: personalWorkspace.id,
      createdBy: user1.id,
      status: TaskStatus.TODO,
      priority: Priority.MEDIUM,
      tags: { connect: [{ id: tagUrgent.id }] },
    },
  });

  const task2 = await prisma.task.create({
    data: {
      title: 'Design new landing page',
      description: 'Create Figma mockups for the Q3 campaign',
      workspaceId: groupWorkspace.id,
      createdBy: user1.id,
      assigneeId: user2.id,
      status: TaskStatus.IN_PROGRESS,
      priority: Priority.HIGH,
      tags: { connect: [{ id: tagDesign.id }] },
    },
  });

  // 5. Tạo 2 Subtasks
  await prisma.subtask.create({
    data: {
      title: 'Get milk from the local store',
      taskId: task1.id,
    },
  });

  await prisma.subtask.create({
    data: {
      title: 'Hero section mockup',
      taskId: task2.id,
    },
  });

  // 6. Tạo 2 Activity Events (1 chat, 1 status change)
  await prisma.activityEvent.create({
    data: {
      taskId: task2.id,
      actorId: user1.id,
      eventType: ActivityType.CHAT_MESSAGE,
      content: 'Hey Bob, how is the mockup going?',
    },
  });

  await prisma.activityEvent.create({
    data: {
      taskId: task2.id,
      actorId: user2.id,
      eventType: ActivityType.STATE_TRANSITION,
      content: JSON.stringify({ from: 'TODO', to: 'IN_PROGRESS' }),
    },
  });

  console.log('Seeding finished.');
}

main()
  .then(async () => {
    await prisma.$disconnect();
    await pool.end();
  })
  .catch(async (e) => {
    console.error(e);
    await prisma.$disconnect();
    await pool.end();
    process.exit(1);
  });
