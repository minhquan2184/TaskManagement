import { Request, Response } from 'express';
import { OAuth2Client } from 'google-auth-library';
import { prisma } from '../index';
import jwt from 'jsonwebtoken';

// Nhập ID Client từ Google Cloud Console/Firebase vào biến môi trường
const client = new OAuth2Client(process.env.GOOGLE_WEB_CLIENT_ID);

export const loginWithGoogle = async (req: Request, res: Response) => {
  try {
    const { idToken } = req.body;

    if (!idToken) {
      return res.status(400).json({ error: 'ID Token not found' });
    }

    // Xác minh chữ ký Token với hệ thống Google
    const ticket = await client.verifyIdToken({
      idToken: idToken,
      audience: process.env.GOOGLE_WEB_CLIENT_ID,
    });
    
    const payload = ticket.getPayload();
    if (!payload) return res.status(401).json({ error: 'Invalid Token' });

    const email = payload.email!;
    const fullName = payload.name;
    const avatarUrl = payload.picture;

    // Tìm trong Neon DB xem User có tồn tại theo Email không
    let user = await prisma.user.findUnique({ where: { email } });

    if (!user) {
      // Nếu chưa thì tự động tạo tài khoản mới (Sign Up)
      user = await prisma.user.create({
        data: {
          email,
          fullName,
          avatarUrl,
        }
      });

      // Tự động tạo Personal Workspace cho user mới
      await prisma.workspace.create({
        data: {
          title: 'My List',
          isPersonal: true,
          ownerId: user.id,
          members: {
            create: {
              userId: user.id,
              role: 'ADMIN',
            },
          },
        },
      });
    }

    // Sinh Session Token tự chế (JWT) dùng cho các yêu cầu hệ thống sau này
    const sessionToken = jwt.sign(
      { userId: user.id }, 
      process.env.JWT_SECRET || 'secret_key_123', 
      { expiresIn: '7d' }
    );

    res.status(200).json({
      message: 'Login successful',
      user: user,
      token: sessionToken
    });

  } catch (error) {
    console.error('Google login error:', error);
    res.status(500).json({ error: 'Login failed due to server error' });
  }
};

