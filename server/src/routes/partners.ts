import { Router } from 'express';
import { z } from 'zod';
import { Partner } from '../models/Partner.js';
import { auth } from '../middleware/auth.js';

const router = Router();

router.get('/', async (_req, res) => {
  const items = await Partner.find({ active: true }).sort({ order: 1, createdAt: -1 });
  res.json(items.map(p => ({
    id: p._id,
    name: p.name,
    description: p.description,
    logoUrl: p.logoUrl,
    url: p.url
  })));
});

const upsertSchema = z.object({
  name: z.string().min(1),
  description: z.string().optional(),
  logoUrl: z.string().url().optional(),
  url: z.string().url().optional(),
  active: z.boolean().optional(),
  order: z.number().optional()
});

router.post('/', auth('admin'), async (req, res) => {
  const data = upsertSchema.parse(req.body);
  const created = await Partner.create(data);
  res.status(201).json({ id: created._id });
});

router.put('/:id', auth('admin'), async (req, res) => {
  const data = upsertSchema.parse(req.body);
  await Partner.updateOne({ _id: req.params.id }, data);
  res.json({ ok: true });
});

router.delete('/:id', auth('admin'), async (req, res) => {
  await Partner.deleteOne({ _id: req.params.id });
  res.json({ ok: true });
});

export default router;


