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
  role: z.enum(['student', 'admin']).optional()
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


