import { Router } from 'express';
import { z } from 'zod';
import { auth } from '../middleware/auth.js';
import { User } from '../models/User.js';

const router = Router();

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
  role: z.enum(['student', 'admin']).optional(),
  phone: z.string().optional(),
  course: z.string().optional(),
  speciality: z.string().optional(),
  status: z.string().optional(),
  university: z.string().optional(),
  payment: z.string().optional(),
  penalties: z.string().optional(),
  notes: z.string().optional(),
  studentId: z.string().optional(),
  emailVerified: z.boolean().optional()
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
    emailVerified: user.emailVerified
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


