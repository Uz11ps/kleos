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
  const universityId = String(req.query.universityId || '').trim();
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
  if (universityId) {
    filter.universityId = universityId;
  } else if (university) {
    filter.university = { $regex: `^${university}$`, $options: 'i' };
  }
  const items = await Program.find(filter).populate('universityId', 'name city country').sort({ order: 1, createdAt: -1 }).lean();
  res.json(items.map((p: any) => ({
    id: p._id.toString(),
    title: p.title || '',
    description: p.description || '',
    language: p.language || 'en',
    level: p.level || "Bachelor's degree",
    university: p.university || (p.universityId?.name) || '', // Legacy field for backward compatibility
    universityId: p.universityId?._id?.toString() || p.universityId?.toString() || null,
    universityName: p.universityId?.name || p.university || '',
    tuition: p.tuition || 0,
    durationYears: p.durationYears !== undefined ? p.durationYears : (p.durationMonths ? p.durationMonths / 12 : 4), // Convert old durationMonths if exists
    active: p.active !== undefined ? p.active : true,
    order: p.order || 0
  })));
});

router.get('/:id', async (req, res) => {
  const p = await Program.findOne({ _id: req.params.id, active: true }).populate('universityId', 'name city country').lean();
  if (!p) return res.status(404).json({ error: 'not_found' });
  const pAny = p as any;
  res.json({
    id: pAny._id.toString(),
    title: pAny.title || '',
    description: pAny.description || '',
    language: pAny.language || 'en',
    level: pAny.level || "Bachelor's degree",
    university: pAny.university || (pAny.universityId?.name) || '', // Legacy field for backward compatibility
    universityId: pAny.universityId?._id?.toString() || pAny.universityId?.toString() || null,
    universityName: pAny.universityId?.name || pAny.university || '',
    tuition: pAny.tuition || 0,
    durationYears: pAny.durationYears !== undefined ? pAny.durationYears : (pAny.durationMonths ? pAny.durationMonths / 12 : 4), // Convert old durationMonths if exists
    active: pAny.active !== undefined ? pAny.active : true,
    order: pAny.order || 0
  });
});

// Admin CRUD
router.post('/', auth('admin'), async (req, res) => {
  const schema = z.object({
    title: z.string().min(1),
    description: z.string().optional().default(''),
    language: z.enum(['ru','en','zh']).optional().default('en'),
    level: z.enum(["Bachelor's degree", "Master's degree", "Research degree", "Speciality degree"]).optional().default("Bachelor's degree"),
    university: z.string().optional().default(''), // Legacy field
    universityId: z.string().min(1), // Required field
    tuition: z.coerce.number().optional().default(0),
    durationYears: z.coerce.number().optional().default(4),
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
    description: z.string().optional(),
    language: z.enum(['ru','en','zh']).optional(),
    level: z.enum(["Bachelor's degree", "Master's degree", "Research degree", "Speciality degree"]).optional(),
    university: z.string().optional(), // Legacy field
    universityId: z.string().optional(),
    tuition: z.coerce.number().optional(),
    durationYears: z.coerce.number().optional(),
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



