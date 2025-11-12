import 'dotenv/config';
import express from 'express';
import mongoose from 'mongoose';
import cors from 'cors';
import helmet from 'helmet';
import morgan from 'morgan';
import authRoutes from './routes/auth.js';
import partnersRoutes from './routes/partners.js';
import admissionsRoutes from './routes/admissions.js';
import usersRoutes from './routes/users.js';

const app = express();
app.use(cors({
  origin: process.env.CORS_ORIGIN?.split(',').map(s => s.trim()) ?? '*',
}));
app.use(helmet());
app.use(express.json());
app.use(morgan('dev'));

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
  res.json({
    ok: true,
    service: 'Kleos API',
    endpoints: [
      '/health',
      '/auth/login',
      '/auth/register',
      '/partners',
      '/admissions'
    ]
  });
});
app.use('/auth', authRoutes);
app.use('/partners', partnersRoutes);
app.use('/admissions', admissionsRoutes);
app.use('/users', usersRoutes);

const port = Number(process.env.PORT) || 8080;
app.listen(port, () => {
  // eslint-disable-next-line no-console
  console.log(`API listening on :${port}`);
});


