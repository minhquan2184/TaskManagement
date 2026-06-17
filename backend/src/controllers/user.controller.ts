import { Request, Response } from 'express';
import { prisma } from '../index';

// PUT /api/users/fcm-token — Cập nhật FCM token cho User hiện tại
export const updateFcmToken = async (req: Request, res: Response) => {
  try {
    const userId = req.user!.userId;
    const { fcmToken } = req.body;

    if (!fcmToken) {
      return res.status(400).json({ error: 'fcmToken is required' });
    }

    const updatedUser = await prisma.user.update({
      where: { id: userId },
      data: { fcmToken },
      select: { id: true, email: true, fcmToken: true }
    });

    res.json({ message: 'FCM token updated successfully', user: updatedUser });
  } catch (error) {
    console.error('Failed to update FCM token:', error);
    res.status(500).json({ error: 'Failed to update FCM token' });
  }
};
