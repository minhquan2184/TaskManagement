import nodemailer from 'nodemailer';
import dotenv from 'dotenv';

dotenv.config();

const transporter = nodemailer.createTransport({
  host: process.env.SMTP_HOST || 'smtp.gmail.com',
  port: parseInt(process.env.SMTP_PORT || '587'),
  secure: process.env.SMTP_SECURE === 'true', // true for 465, false for other ports
  auth: {
    user: process.env.SMTP_USER,
    pass: process.env.SMTP_PASS,
  },
});

export const sendInvitationEmail = async (toEmail: string, workspaceTitle: string, inviterName: string, acceptLink: string) => {
  // If SMTP is not fully configured, just log the link and return
  if (!process.env.SMTP_USER || !process.env.SMTP_PASS) {
    console.log('\n[MOCK EMAIL SERVICE] SMTP_USER is not configured.');
    console.log(`Sending invitation to: ${toEmail}`);
    console.log(`Workspace: ${workspaceTitle}`);
    console.log(`Link: ${acceptLink}\n`);
    return;
  }

  const mailOptions = {
    from: `"Task Management" <${process.env.SMTP_USER}>`,
    to: toEmail,
    subject: `Invitation to join workspace: ${workspaceTitle}`,
    html: `
      <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #ddd; border-radius: 8px;">
        <h2 style="color: #333;">Hello!</h2>
        <p>You have been invited by <strong>${inviterName}</strong> to join the workspace <strong>${workspaceTitle}</strong> on Task Management.</p>
        <p>To accept the invitation and join the group, please click the button below:</p>
        <div style="text-align: center; margin: 30px 0;">
          <a href="${acceptLink}" style="background-color: #007bff; color: white; padding: 12px 24px; text-decoration: none; border-radius: 4px; font-weight: bold; font-size: 16px;">Join Workspace</a>
        </div>
        <p style="color: #666; font-size: 14px;">If the button doesn't work, you can copy and paste the following link into your browser:</p>
        <p style="color: #666; font-size: 14px; word-break: break-all;">${acceptLink}</p>
        <hr style="border: none; border-top: 1px solid #eee; margin-top: 30px;" />
        <p style="color: #999; font-size: 12px; text-align: center;">Task Management App &copy; ${new Date().getFullYear()}</p>
      </div>
    `,
  };

  try {
    const info = await transporter.sendMail(mailOptions);
    console.log('Message sent: %s', info.messageId);
  } catch (error) {
    console.error('Error sending email:', error);
    throw error;
  }
};
