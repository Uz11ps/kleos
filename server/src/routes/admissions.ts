import { Router } from 'express';
import { z } from 'zod';
import { Admission } from '../models/Admission.js';
import { auth } from '../middleware/auth.js';

const router = Router();

const createSchema = z.object({
  fullName: z.string().min(1),
  phone: z.string().min(3),
  email: z.string().email(),
  program: z.string().min(1),
  comment: z.string().optional()
});

router.post('/', auth(), async (req, res) => {
  const data = createSchema.parse(req.body);
  const userId = (req as any).auth?.uid;
  await Admission.create({ ...data, userId });
  res.status(201).json({ ok: true });
});

router.get('/', auth('admin'), async (_req, res) => {
  const items = await Admission.find().sort({ createdAt: -1 });
  res.json(items.map(a => ({
    id: a._id,
    fullName: a.fullName,
    phone: a.phone,
    email: a.email,
    program: a.program,
    comment: a.comment,
    status: a.status,
    createdAt: a.createdAt
  })));
});

const updateSchema = z.object({
  status: z.enum(['new','processing','done']).optional(),
  studentId: z.string().optional()
});

router.put('/:id', auth('admin'), async (req, res) => {
  const data = updateSchema.parse(req.body);
  await Admission.updateOne({ _id: req.params.id }, data);
  res.json({ ok: true });
});

export default router;


