import 'dotenv/config';
import express from 'express';
import mongoose from 'mongoose';
import cors from 'cors';
import helmet from 'helmet';
import morgan from 'morgan';
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
app.use(helmet());
app.use(express.json());
app.use(express.urlencoded({ extended: false }));
app.use(morgan('dev'));

// Отключаем ETag только для админ-роутов
app.use((req, res, next) => {
  if (req.path.startsWith('/admin')) {
    // Отключаем автоматическое добавление ETag для админ-роутов
    res.setHeader('ETag', '');
    res.set({
      'Cache-Control': 'no-store, no-cache, must-revalidate, private',
      'Pragma': 'no-cache',
      'Expires': '0'
    });
    // Переопределяем send для гарантированного удаления ETag
    const originalSend = res.send.bind(res);
    res.send = function(body: any) {
      res.removeHeader('ETag');
      res.set({
        'Cache-Control': 'no-store, no-cache, must-revalidate, private',
        'Pragma': 'no-cache',
        'Expires': '0'
      });
      return originalSend(body);
    };
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

const port = Number(process.env.PORT) || 8080;
app.listen(port, () => {
  // eslint-disable-next-line no-console
  console.log(`API listening on :${port}`);
});


