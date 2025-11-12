import { Router } from 'express';
import { z } from 'zod';
import bcrypt from 'bcrypt';
import jwt from 'jsonwebtoken';
import { User } from '../models/User.js';
import crypto from 'crypto';
import nodemailer from 'nodemailer';

const router = Router();

const registerSchema = z.object({
  fullName: z.string().min(1),
  email: z.string().email(),
  password: z.string().min(6)
});

const loginSchema = z.object({
  email: z.string().email(),
  password: z.string().min(6)
});

router.get('/verify', async (req, res) => {
  const token = (req.query.token as string) || '';
  if (!token) return res.status(400).send('Missing token');
  const user = await User.findOne({ emailVerifyToken: token, emailVerifyExpires: { $gt: new Date() } });
  if (!user) return res.status(400).send('Invalid or expired token');
  user.emailVerified = true;
  user.emailVerifyToken = undefined as any;
  user.emailVerifyExpires = undefined as any;
  await user.save();
  res.send('<html><body style="font-family:Arial;padding:24px;"><h2>Email verified</h2><p>You can close this page and return to the app.</p></body></html>');
});

router.post('/verify/consume', async (req, res) => {
  const token = (req.body?.token as string) || '';
  if (!token) return res.status(400).json({ error: 'missing_token' });
  const user = await User.findOne({ emailVerifyToken: token, emailVerifyExpires: { $gt: new Date() } });
  if (!user) return res.status(400).json({ error: 'invalid_or_expired' });
  user.emailVerified = true;
  user.emailVerifyToken = undefined as any;
  user.emailVerifyExpires = undefined as any;
  await user.save();
  const jwtToken = jwt.sign({ uid: user._id.toString(), role: user.role }, process.env.JWT_SECRET!, { expiresIn: '30d' });
  return res.json({ token: jwtToken, user: { id: user._id, fullName: user.fullName, email: user.email, role: user.role } });
});

router.post('/register', async (req, res) => {
  try {
    const { fullName, email, password } = registerSchema.parse(req.body);
    const existing = await User.findOne({ email });
    if (existing) return res.status(400).json({ error: 'email_taken' });
    const passwordHash = await bcrypt.hash(password, 10);
    const verifyToken = crypto.randomBytes(32).toString('hex');
    const verifyExpires = new Date(Date.now() + 24 * 60 * 60 * 1000);
    const user = await User.create({ fullName, email, passwordHash, emailVerifyToken: verifyToken, emailVerifyExpires: verifyExpires, emailVerified: false });
    const base = process.env.PUBLIC_BASE_URL || `https://${req.get('host')}`;
    const webLink = `${base}/auth/verify?token=${verifyToken}`;
    const appScheme = process.env.APP_DEEP_LINK_SCHEME || 'kleos';
    const appHost = process.env.APP_DEEP_LINK_HOST || 'verify';
    const appLink = `${appScheme}://${appHost}?token=${verifyToken}`;
    await sendVerificationEmail(email, fullName, webLink, appLink);
    return res.json({ requiresVerification: true });
  } catch (e: any) {
    return res.status(400).json({ error: e?.message || 'bad_request' });
  }
});

router.post('/login', async (req, res) => {
  try {
    const { email, password } = loginSchema.parse(req.body);
    const user = await User.findOne({ email });
    if (!user) return res.status(400).json({ error: 'invalid_credentials' });
    const ok = await bcrypt.compare(password, user.passwordHash);
    if (!ok) return res.status(400).json({ error: 'invalid_credentials' });
    if (!user.emailVerified) return res.status(403).json({ error: 'email_not_verified' });
    const token = jwt.sign({ uid: user._id.toString(), role: user.role }, process.env.JWT_SECRET!, { expiresIn: '30d' });
    return res.json({ token, user: { id: user._id, fullName: user.fullName, email, role: user.role } });
  } catch (e: any) {
    return res.status(400).json({ error: e?.message || 'bad_request' });
  }
});

export default router;

async function sendVerificationEmail(to: string, name: string, webLink: string, appLink: string) {
  const host = process.env.SMTP_HOST;
  const from = process.env.SMTP_FROM || 'no-reply@kleos';
  if (!host) {
    console.log(`Verify email for ${to}: ${webLink} (app: ${appLink})`);
    return;
  }
  const transporter = nodemailer.createTransport({
    host,
    port: parseInt(process.env.SMTP_PORT || '587', 10),
    secure: false,
    auth: process.env.SMTP_USER ? { user: process.env.SMTP_USER, pass: process.env.SMTP_PASS } : undefined
  });
  const html = `<div style="font-family:Arial;">
    <p>Здравствуйте, ${name}!</p>
    <p>Пожалуйста, подтвердите ваш email:</p>
    <p><a href="${webLink}">Подтвердить email</a></p>
    <p>На Android можно открыть приложение напрямую: <a href="${appLink}">${appLink}</a></p>
    <p>Ссылка действует 24 часа.</p>
  </div>`;
  await transporter.sendMail({ from, to, subject: 'Kleos — подтверждение email', html });
}

