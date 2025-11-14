import { Router } from 'express';
import { z } from 'zod';
import { Admission } from '../models/Admission.js';
import jwt from 'jsonwebtoken';
import { auth } from '../middleware/auth.js';

const router = Router();

const createSchema = z.object({
  fullName: z.string().min(1),
  phone: z.string().min(3),
  email: z.string().email(),
  dateOfBirth: z.string().optional(),
  placeOfBirth: z.string().optional(),
  nationality: z.string().optional(),
  passportNumber: z.string().optional(),
  passportIssue: z.string().optional(),
  passportExpiry: z.string().optional(),
  visaCity: z.string().optional(),
  program: z.string().min(1),
  comment: z.string().optional()
});

router.post('/', async (req, res) => {
  try {
    const data = createSchema.parse(req.body);
    let userId: string | undefined;
    const authz = req.headers.authorization || '';
    const m = authz.match(/^Bearer\s+(.+)$/i);
    if (m) {
      try {
        const token = m[1];
        const payload: any = jwt.verify(token, process.env.JWT_SECRET!);
        userId = payload?.uid;
      } catch {
        // ignore invalid token; allow unauthenticated submission
      }
    }
    await Admission.create({ ...data, userId });
    return res.status(201).json({ ok: true });
  } catch (e: any) {
    return res.status(400).json({ ok: false, error: e?.message || 'bad_request' });
  }
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


