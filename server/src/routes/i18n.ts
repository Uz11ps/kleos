import { Router } from 'express';
import { z } from 'zod';
import { Translation } from '../models/Translation.js';
import { auth } from '../middleware/auth.js';

const router = Router();

// Public: get map of translations for a language
router.get('/:lang', async (req, res) => {
  const lang = String(req.params.lang || '').toLowerCase();
  if (!['en','ru','zh'].includes(lang)) return res.status(400).json({ error: 'unsupported_lang' });
  const items = await Translation.find({ lang }).lean();
  const map: Record<string,string> = {};
  for (const it of items) map[it.key] = it.value;
  res.json(map);
});

// Admin: upsert a translation
router.post('/', auth('admin'), async (req, res) => {
  const schema = z.object({ lang: z.enum(['en','ru','zh']), key: z.string().min(1), value: z.string().default('') });
  const data = schema.parse(req.body);
  await Translation.updateOne({ lang: data.lang, key: data.key }, { $set: { value: data.value } }, { upsert: true });
  res.json({ ok: true });
});

// Admin: delete a translation
router.delete('/', auth('admin'), async (req, res) => {
  const schema = z.object({ lang: z.enum(['en','ru','zh']), key: z.string().min(1) });
  const data = schema.parse(req.body);
  await Translation.deleteOne({ lang: data.lang, key: data.key });
  res.json({ ok: true });
});

export default router;


