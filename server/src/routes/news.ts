import { Router } from 'express';
import { z } from 'zod';
import { News } from '../models/News.js';
import { auth } from '../middleware/auth.js';

const router = Router();

// Public list
router.get('/', async (_req, res) => {
  const items = await News.find({ active: true }).sort({ order: 1, publishedAt: -1, createdAt: -1 }).lean();
  res.json(items.map(n => ({
    id: n._id,
    title: n.title,
    content: n.content,
    imageUrl: n.imageUrl,
    publishedAt: n.publishedAt,
  })));
});

// Admin CRUD (can also be managed in adminWeb, but keep API endpoints)
router.post('/', auth('admin'), async (req, res) => {
  const schema = z.object({
    title: z.string().min(1),
    content: z.string().optional().default(''),
    imageUrl: z.string().optional().default(''),
    publishedAt: z.coerce.date().optional(),
    active: z.coerce.boolean().optional(),
    order: z.coerce.number().optional()
  });
  const data = schema.parse(req.body);
  const created = await News.create(data);
  res.status(201).json({ id: created._id });
});

router.put('/:id', auth('admin'), async (req, res) => {
  const schema = z.object({
    title: z.string().optional(),
    content: z.string().optional(),
    imageUrl: z.string().optional(),
    publishedAt: z.coerce.date().optional(),
    active: z.coerce.boolean().optional(),
    order: z.coerce.number().optional()
  });
  const data = schema.parse(req.body);
  await News.updateOne({ _id: req.params.id }, data);
  res.json({ ok: true });
});

router.delete('/:id', auth('admin'), async (req, res) => {
  await News.deleteOne({ _id: req.params.id });
  res.json({ ok: true });
});

export default router;



