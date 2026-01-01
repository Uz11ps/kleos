import { Router } from 'express';
import { z } from 'zod';
import { GalleryItem } from '../models/GalleryItem.js';
import { auth } from '../middleware/auth.js';

const router = Router();

const createSchema = z.object({
  title: z.string().min(1),
  description: z.string().optional(),
  mediaUrl: z.string().url(),
  mediaType: z.enum(['photo', 'video']).default('photo'),
  order: z.number().optional()
});

const updateSchema = z.object({
  title: z.string().min(1).optional(),
  description: z.string().optional(),
  mediaUrl: z.string().url().optional(),
  mediaType: z.enum(['photo', 'video']).optional(),
  order: z.number().optional()
});

// Public endpoint - get all gallery items
router.get('/', async (_req, res) => {
  const items = await GalleryItem.find().sort({ order: 1, createdAt: -1 });
  res.json(items.map(item => ({
    id: item._id,
    title: item.title,
    description: item.description,
    mediaUrl: item.mediaUrl,
    mediaType: item.mediaType,
    createdAt: item.createdAt
  })));
});

// Admin endpoints
router.post('/', auth('admin'), async (req, res) => {
  try {
    const data = createSchema.parse(req.body);
    const item = await GalleryItem.create(data);
    res.status(201).json({ ok: true, id: item._id });
  } catch (e: any) {
    res.status(400).json({ ok: false, error: e?.message || 'bad_request' });
  }
});

router.put('/:id', auth('admin'), async (req, res) => {
  try {
    const data = updateSchema.parse(req.body);
    await GalleryItem.updateOne({ _id: req.params.id }, data);
    res.json({ ok: true });
  } catch (e: any) {
    res.status(400).json({ ok: false, error: e?.message || 'bad_request' });
  }
});

router.delete('/:id', auth('admin'), async (req, res) => {
  await GalleryItem.deleteOne({ _id: req.params.id });
  res.json({ ok: true });
});

export default router;

