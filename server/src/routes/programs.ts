import { Router } from 'express';
import { z } from 'zod';
import { Program } from '../models/Program.js';
import { auth } from '../middleware/auth.js';

const router = Router();

// Public list with filters
router.get('/', async (req, res) => {
  const q = String(req.query.q || '').trim();
  const language = String(req.query.language || '').trim();
  const level = String(req.query.level || '').trim();
  const university = String(req.query.university || '').trim();
  const filter: any = { active: true };
  if (q) {
    filter.$or = [
      { title: { $regex: q, $options: 'i' } },
      { description: { $regex: q, $options: 'i' } },
      { university: { $regex: q, $options: 'i' } }
    ];
  }
  if (language) filter.language = language;
  if (level) filter.level = level;
  if (university) filter.university = { $regex: `^${university}$`, $options: 'i' };
  const items = await Program.find(filter).sort({ order: 1, createdAt: -1 }).lean();
  res.json(items.map(p => ({
    id: p._id,
    title: p.title,
    slug: p.slug,
    description: p.description,
    language: p.language,
    level: p.level,
    university: p.university,
    tuition: p.tuition,
    durationMonths: p.durationMonths,
    imageUrl: p.imageUrl
  })));
});

router.get('/:id', async (req, res) => {
  const p = await Program.findOne({ _id: req.params.id, active: true }).lean();
  if (!p) return res.status(404).json({ error: 'not_found' });
  res.json({
    id: p._id,
    title: p.title,
    slug: p.slug,
    description: p.description,
    language: p.language,
    level: p.level,
    university: p.university,
    tuition: p.tuition,
    durationMonths: p.durationMonths,
    imageUrl: p.imageUrl
  });
});

// Admin CRUD
router.post('/', auth('admin'), async (req, res) => {
  const schema = z.object({
    title: z.string().min(1),
    slug: z.string().min(1),
    description: z.string().optional().default(''),
    language: z.enum(['ru','en','zh']).optional().default('en'),
    level: z.enum(['bachelor','master','phd','foundation','other']).optional().default('other'),
    university: z.string().optional().default(''),
    tuition: z.coerce.number().optional().default(0),
    durationMonths: z.coerce.number().optional().default(0),
    imageUrl: z.string().optional().default(''),
    active: z.coerce.boolean().optional().default(true),
    order: z.coerce.number().optional().default(0)
  });
  const data = schema.parse(req.body);
  const created = await Program.create(data);
  res.status(201).json({ id: created._id });
});

router.put('/:id', auth('admin'), async (req, res) => {
  const schema = z.object({
    title: z.string().optional(),
    slug: z.string().optional(),
    description: z.string().optional(),
    language: z.enum(['ru','en','zh']).optional(),
    level: z.enum(['bachelor','master','phd','foundation','other']).optional(),
    university: z.string().optional(),
    tuition: z.coerce.number().optional(),
    durationMonths: z.coerce.number().optional(),
    imageUrl: z.string().optional(),
    active: z.coerce.boolean().optional(),
    order: z.coerce.number().optional()
  });
  const data = schema.parse(req.body);
  await Program.updateOne({ _id: req.params.id }, data);
  res.json({ ok: true });
});

router.delete('/:id', auth('admin'), async (req, res) => {
  await Program.deleteOne({ _id: req.params.id });
  res.json({ ok: true });
});

export default router;



