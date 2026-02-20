import 'dotenv/config';
import 'express-async-errors';
import express from 'express';
import mongoose from 'mongoose';
import cors from 'cors';
import helmet from 'helmet';
import morgan from 'morgan';
import { ZodError } from 'zod';
import authRoutes from './routes/auth.js';
import partnersRoutes from './routes/partners.js';
import admissionsRoutes from './routes/admissions.js';
import adminWeb from './routes/adminWeb.js';
import usersRoutes from './routes/users.js';
import chatsRoutes from './routes/chats.js';
import i18nRoutes from './routes/i18n.js';
import newsRoutes from './routes/news.js';
import programsRoutes from './routes/programs.js';
import galleryRoutes from './routes/gallery.js';
import universitiesRoutes from './routes/universities.js';
import path from 'path';
import fs from 'fs';

const app = express();
app.use(cors({
  origin: process.env.CORS_ORIGIN?.split(',').map(s => s.trim()) ?? '*',
}));
app.use(helmet({
  contentSecurityPolicy: {
    directives: {
      defaultSrc: ["'self'"],
      scriptSrc: ["'self'", "'unsafe-inline'", "'unsafe-eval'", "'unsafe-hashes'"],
      scriptSrcAttr: ["'unsafe-inline'"],
      styleSrc: ["'self'", "'unsafe-inline'"],
      imgSrc: ["'self'", "data:", "https:"],
      connectSrc: ["'self'"],
      fontSrc: ["'self'"],
      objectSrc: ["'none'"],
      mediaSrc: ["'self'"],
      frameSrc: ["'none'"],
    },
  },
}));
app.use(express.json());
app.use(express.urlencoded({ extended: false }));
app.use(morgan('dev'));

// Отключаем ETag только для админ-роутов
app.use((req, res, next) => {
  if (req.path.startsWith('/admin')) {
    // Устанавливаем заголовки для отключения кэширования
    res.set({
      'Cache-Control': 'no-store, no-cache, must-revalidate, private',
      'Pragma': 'no-cache',
      'Expires': '0'
    });
  }
  next();
});

// Static uploads
const uploadsDir = path.join(process.cwd(), 'uploads');
const logosDir = path.join(uploadsDir, 'logos');
try { fs.mkdirSync(logosDir, { recursive: true }); } catch {}
app.use('/uploads', express.static(uploadsDir));

const mongoUri = process.env.MONGODB_URI;
if (!mongoUri) {
  // eslint-disable-next-line no-console
  console.error('Missing MONGODB_URI');
  process.exit(1);
}

const jwtSecret = process.env.JWT_SECRET;
if (!jwtSecret) {
  // eslint-disable-next-line no-console
  console.error('Missing JWT_SECRET');
  process.exit(1);
}

mongoose.connect(mongoUri).then(() => {
  // eslint-disable-next-line no-console
  console.log('MongoDB connected');
}).catch((err) => {
  console.error('Mongo connection error', err);
  process.exit(1);
});

app.get('/health', (_, res) => res.json({ ok: true }));
app.get('/', (_req, res) => {
  res.redirect('/admin');
});
// API routes - поддерживаем оба варианта для совместимости
app.use('/api/auth', authRoutes);
app.use('/api/partners', partnersRoutes);
app.use('/api/admissions', admissionsRoutes);
app.use('/api/users', usersRoutes);
app.use('/api/chats', chatsRoutes);
app.use('/api/i18n', i18nRoutes);
app.use('/api/news', newsRoutes);
app.use('/api/programs', programsRoutes);
app.use('/api/gallery', galleryRoutes);
app.use('/api/universities', universitiesRoutes);
// Старые маршруты без префикса /api для обратной совместимости
app.use('/auth', authRoutes);
app.use('/partners', partnersRoutes);
app.use('/admissions', admissionsRoutes);
app.use('/users', usersRoutes);
app.use('/chats', chatsRoutes);
app.use('/i18n', i18nRoutes);
app.use('/news', newsRoutes);
app.use('/programs', programsRoutes);
app.use('/gallery', galleryRoutes);
app.use('/universities', universitiesRoutes);
app.use('/', adminWeb);

// Error handler (keeps server stable on invalid input / async throws)
app.use((err: unknown, _req: express.Request, res: express.Response, _next: express.NextFunction) => {
  if (err instanceof ZodError) {
    return res.status(400).json({ error: 'validation_error', details: err.issues });
  }
  // eslint-disable-next-line no-console
  console.error('Unhandled error:', err);
  return res.status(500).json({ error: 'internal_error' });
});

const port = Number(process.env.PORT) || 8080;
app.listen(port, () => {
  // eslint-disable-next-line no-console
  console.log(`API listening on :${port}`);
});


