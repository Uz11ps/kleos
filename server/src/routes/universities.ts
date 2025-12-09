import { Router } from 'express';
import { z } from 'zod';
import { University } from '../models/University.js';
import { auth } from '../middleware/auth.js';

const router = Router();

// Public endpoint - get all active universities
router.get('/', async (_req, res) => {
  const items = await University.find({ active: true }).sort({ order: 1, name: 1 });
  res.json(items.map(u => ({
    id: u._id,
    name: u.name,
    city: u.city,
    country: u.country,
    description: u.description,
    website: u.website,
    logoUrl: u.logoUrl
  })));
});

// Admin endpoints
router.post('/', auth('admin'), async (req, res) => {
  try {
    const schema = z.object({
      name: z.string().min(1),
      city: z.string().optional(),
      country: z.string().optional().default('Russia'),
      description: z.string().optional(),
      website: z.string().url().optional(),
      logoUrl: z.string().url().optional(),
      active: z.coerce.boolean().optional().default(true),
      order: z.coerce.number().optional().default(0)
    });
    const data = schema.parse(req.body);
    const university = await University.create(data);
    res.status(201).json({ ok: true, id: university._id });
  } catch (e: any) {
    res.status(400).json({ ok: false, error: e?.message || 'bad_request' });
  }
});

router.put('/:id', auth('admin'), async (req, res) => {
  try {
    const schema = z.object({
      name: z.string().min(1).optional(),
      city: z.string().optional(),
      country: z.string().optional(),
      description: z.string().optional(),
      website: z.string().url().optional(),
      logoUrl: z.string().url().optional(),
      active: z.coerce.boolean().optional(),
      order: z.coerce.number().optional()
    });
    const data = schema.parse(req.body);
    await University.updateOne({ _id: req.params.id }, data);
    res.json({ ok: true });
  } catch (e: any) {
    res.status(400).json({ ok: false, error: e?.message || 'bad_request' });
  }
});

router.delete('/:id', auth('admin'), async (req, res) => {
  await University.deleteOne({ _id: req.params.id });
  res.json({ ok: true });
});

export default router;

