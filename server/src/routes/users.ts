import { Router } from 'express';
import { z } from 'zod';
import { auth } from '../middleware/auth.js';
import { User } from '../models/User.js';
import { Admission } from '../models/Admission.js';
import mongoose, { Schema, model, Types } from 'mongoose';

const router = Router();

// Keep models local to avoid extra files and allow cleanup on account deletion.
const ChatSchema = new Schema({
  userId: { type: Types.ObjectId, ref: 'User', index: true },
  status: { type: String, enum: ['open', 'closed'], default: 'open', index: true },
  lastMessageAt: { type: Date, index: true }
}, { timestamps: true });
const MessageSchema = new Schema({
  chatId: { type: Types.ObjectId, ref: 'Chat', index: true },
  senderRole: { type: String, enum: ['student', 'admin', 'system'] },
  text: String,
  isReadByAdmin: { type: Boolean, default: false, index: true }
}, { timestamps: true });
const Chat = (mongoose.models.Chat as any) || model('Chat', ChatSchema);
const Message = (mongoose.models.Message as any) || model('Message', MessageSchema);

router.get('/', auth('admin'), async (req, res) => {
  const page = Number((req.query.page as string) ?? 1);
  const limit = Math.min(Number((req.query.limit as string) ?? 20), 100);
  const skip = (page - 1) * limit;
  const [items, total] = await Promise.all([
    User.find().sort({ createdAt: -1 }).skip(skip).limit(limit),
    User.countDocuments()
  ]);
  res.json({
    page, limit, total,
    items: items.map(u => ({
      id: u._id,
      email: u.email,
      fullName: u.fullName,
      role: u.role,
      createdAt: u.createdAt
    }))
  });
});

const updateSchema = z.object({
  fullName: z.string().min(1).optional(),
  role: z.enum(['user', 'student', 'admin']).optional(),
  phone: z.string().optional(),
  course: z.string().optional(),
  speciality: z.string().optional(),
  status: z.string().optional(),
  university: z.string().optional(),
  payment: z.string().optional(),
  penalties: z.string().optional(),
  notes: z.string().optional(),
  studentId: z.string().optional(),
  emailVerified: z.boolean().optional(),
  fcmToken: z.string().optional()
});

// Эндпоинт для сохранения FCM токена
router.post('/fcm-token', auth(), async (req, res) => {
  const userId = (req as any).auth?.uid;
  if (!userId) return res.status(401).json({ error: 'unauthorized' });
  
  const schema = z.object({ token: z.string().min(1) });
  const { token } = schema.parse(req.body);
  
  await User.updateOne({ _id: userId }, { fcmToken: token });
  res.json({ ok: true });
});

// Эндпоинт для получения данных текущего пользователя
router.get('/me', auth(), async (req, res) => {
  const userId = (req as any).auth?.uid;
  if (!userId) return res.status(401).json({ error: 'unauthorized' });
  const user = await User.findById(userId);
  if (!user) return res.status(404).json({ error: 'user_not_found' });
  res.json({
    id: user._id,
    email: user.email,
    fullName: user.fullName,
    role: user.role,
    phone: user.phone || '',
    course: user.course || '',
    speciality: user.speciality || '',
    status: user.status || '',
    university: user.university || '',
    payment: user.payment || '',
    penalties: user.penalties || '',
    notes: user.notes || '',
    studentId: user.studentId || '',
    emailVerified: user.emailVerified,
    avatarUrl: (user as any).avatarUrl || null
  });
});

// Эндпоинт для обновления данных текущего пользователя
const updateMeSchema = z.object({
  fullName: z.string().min(1).optional(),
  phone: z.string().optional(),
  course: z.string().optional(),
  speciality: z.string().optional(),
  status: z.string().optional(),
  university: z.string().optional(),
  payment: z.string().optional(),
  penalties: z.string().optional(),
  notes: z.string().optional()
});

router.put('/me', auth(), async (req, res) => {
  try {
    const userId = (req as any).auth?.uid;
    if (!userId) return res.status(401).json({ error: 'unauthorized' });
    const data = updateMeSchema.parse(req.body);
    await User.updateOne({ _id: userId }, data);
    res.json({ ok: true });
  } catch (e: any) {
    return res.status(400).json({ error: e?.message || 'bad_request' });
  }
});

router.delete('/me', auth(), async (req, res) => {
  try {
    const userId = (req as any).auth?.uid;
    if (!userId) return res.status(401).json({ error: 'unauthorized' });

    const chats = await Chat.find({ userId }).select('_id').lean();
    const chatIds = chats.map((chat: any) => chat._id);

    if (chatIds.length > 0) {
      await Message.deleteMany({ chatId: { $in: chatIds } });
      await Chat.deleteMany({ _id: { $in: chatIds } });
    }

    await Admission.deleteMany({ userId });
    await User.deleteOne({ _id: userId });

    return res.json({ ok: true });
  } catch (e: any) {
    return res.status(500).json({ error: e?.message || 'delete_failed' });
  }
});

router.put('/:id', auth('admin'), async (req, res) => {
  const data = updateSchema.parse(req.body);
  await User.updateOne({ _id: req.params.id }, data);
  res.json({ ok: true });
});

router.delete('/:id', auth('admin'), async (req, res) => {
  await User.deleteOne({ _id: req.params.id });
  res.json({ ok: true });
});

export default router;


