import * as admin from 'firebase-admin';
import * as fs from 'fs';
import * as path from 'path';
import * as dotenv from 'dotenv';

dotenv.config();

let db: admin.database.Database | null = null;

const initializeFirebase = () => {
  try {
    const serviceAccountPath = path.resolve(__dirname, '../../firebaseServiceAccountKey.json');
    if (fs.existsSync(serviceAccountPath)) {
      const serviceAccount = JSON.parse(fs.readFileSync(serviceAccountPath, 'utf8'));
      admin.initializeApp({
        credential: admin.credential.cert(serviceAccount),
        databaseURL: process.env.FIREBASE_DATABASE_URL || `https://${serviceAccount.project_id}-default-rtdb.firebaseio.com`
      });
      db = admin.database();
      console.log('Firebase Admin initialized successfully.');
    } else {
      console.warn('Firebase Admin NOT initialized: firebaseServiceAccountKey.json not found in backend root.');
    }
  } catch (error) {
    console.warn('Firebase Admin initialization error:', error);
  }
};

initializeFirebase();

export const broadcastWorkspaceProgress = async (workspaceId: string, totalTasks: number, completedTasks: number) => {
  if (!db) return;
  try {
    const progressPercent = totalTasks > 0 ? Math.round((completedTasks / totalTasks) * 100) : 0;
    const ref = db.ref(`workspaces/${workspaceId}/progress`);
    await ref.set({
      totalTasks,
      completedTasks,
      progressPercent,
      updatedAt: admin.database.ServerValue.TIMESTAMP
    });
  } catch (error) {
    console.error(`Failed to broadcast workspace progress for ${workspaceId}:`, error);
  }
};

export const broadcastTaskChatMessage = async (taskId: string, message: any) => {
  if (!db) return;
  try {
    const ref = db.ref(`tasks/${taskId}/chat`);
    await ref.push({
        ...message,
        timestamp: admin.database.ServerValue.TIMESTAMP
    });
  } catch (error) {
    console.error(`Failed to broadcast chat message for task ${taskId}:`, error);
  }
};
export const sendPushNotification = async (fcmToken: string, title: string, body: string, data?: any) => {
  if (!admin.apps.length) return;
  try {
    const message = {
      notification: {
        title,
        body,
      },
      data: data || {},
      token: fcmToken,
    };
    const response = await admin.messaging().send(message);
    console.log('Successfully sent push notification:', response);
  } catch (error) {
    console.error('Error sending push notification:', error);
  }
};
