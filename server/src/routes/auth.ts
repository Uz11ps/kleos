import { Router } from 'express';
import { z } from 'zod';
import bcrypt from 'bcrypt';
import jwt from 'jsonwebtoken';
import { User } from '../models/User.js';
import crypto from 'crypto';
import nodemailer from 'nodemailer';

const router = Router();

// Helper: build proxy list and SMTP (port/secure) combinations for fallback
function getRelayUrl(): string | undefined {
  const url = process.env.EMAIL_RELAY_URL;
  return url && url.trim().length > 0 ? url.trim() : undefined;
}

function getProxyList(): string[] {
  const list = (process.env.SMTP_PROXIES || process.env.SMTP_PROXY || '')
    .split(/[\s,]+/)
    .map(s => s.trim())
    .filter(Boolean);
  return list;
}

function getSmtpCombos(host: string): Array<{ port: number; secure: boolean }> {
  const combos: Array<{ port: number; secure: boolean }> = [];
  const envPort = process.env.SMTP_PORT ? parseInt(process.env.SMTP_PORT, 10) : undefined;
  const envSecure = (process.env.SMTP_SECURE || '').toLowerCase() === 'true';
  if (envPort) {
    combos.push({ port: envPort, secure: envSecure });
  }
  // Add common fallbacks if not already present
  if (!combos.find(c => c.port === 587 && c.secure === false)) {
    combos.push({ port: 587, secure: false });
  }
  if (!combos.find(c => c.port === 465 && c.secure === true)) {
    combos.push({ port: 465, secure: true });
  }
  return combos;
}

const registerSchema = z.object({
  fullName: z.string().min(1),
  email: z.string().email(),
  password: z.string().min(6)
});

const loginSchema = z.object({
  email: z.string().email(),
  password: z.string().min(6)
});

const resendSchema = z.object({
  email: z.string().email()
});

router.get('/verify', async (req, res) => {
  const token = (req.query.token as string) || '';
  if (!token) return res.status(400).send('Missing token');
  const user = await User.findOne({ emailVerifyToken: token, emailVerifyExpires: { $gt: new Date() } });
  if (!user) return res.status(400).send('Invalid or expired token');
  user.emailVerified = true;
  user.emailVerifyToken = undefined as any;
  user.emailVerifyExpires = undefined as any;
  await user.save();
  res.send('<html><body style="font-family:Arial;padding:24px;"><h2>Email verified</h2><p>You can close this page and return to the app.</p></body></html>');
});

router.post('/verify/consume', async (req, res) => {
  const token = (req.body?.token as string) || '';
  if (!token) return res.status(400).json({ error: 'missing_token' });
  const user = await User.findOne({ emailVerifyToken: token, emailVerifyExpires: { $gt: new Date() } });
  if (!user) return res.status(400).json({ error: 'invalid_or_expired' });
  user.emailVerified = true;
  user.emailVerifyToken = undefined as any;
  user.emailVerifyExpires = undefined as any;
  await user.save();
  const jwtToken = jwt.sign({ uid: user._id.toString(), role: user.role }, process.env.JWT_SECRET!, { expiresIn: '30d' });
  return res.json({ token: jwtToken, user: { id: user._id, fullName: user.fullName, email: user.email, role: user.role } });
});

router.post('/register', async (req, res) => {
  try {
    const { fullName, email, password } = registerSchema.parse(req.body);
    const existing = await User.findOne({ email });
    if (existing) return res.status(400).json({ error: 'email_taken' });
    const passwordHash = await bcrypt.hash(password, 10);
    const verifyToken = crypto.randomBytes(32).toString('hex');
    const verifyExpires = new Date(Date.now() + 24 * 60 * 60 * 1000);
    const user = await User.create({ fullName, email, passwordHash, emailVerifyToken: verifyToken, emailVerifyExpires: verifyExpires, emailVerified: false });
    const base = process.env.PUBLIC_BASE_URL || `https://${req.get('host')}`;
    const webLink = `${base}/auth/verify?token=${verifyToken}`;
    const appScheme = process.env.APP_DEEP_LINK_SCHEME || 'kleos';
    const appHost = process.env.APP_DEEP_LINK_HOST || 'verify';
    const appLink = `${appScheme}://${appHost}?token=${verifyToken}`;
    // Send email in background to avoid client timeout if SMTP is slow
    sendVerificationEmail(email, fullName, webLink, appLink)
      .then(() => console.log(`Verification email queued to ${email}`))
      .catch((e) => console.error('Send verification email failed', e));
    return res.json({ requiresVerification: true, verifyUrl: webLink, appLink });
  } catch (e: any) {
    return res.status(400).json({ error: e?.message || 'bad_request' });
  }
});

// Resend verification email for not-verified accounts
router.post('/verify/resend', async (req, res) => {
  try {
    const { email } = resendSchema.parse(req.body);
    const user = await User.findOne({ email });
    if (!user) return res.status(404).json({ error: 'not_found' });
    if (user.emailVerified) return res.status(400).json({ error: 'already_verified' });

    // issue a new token and expiry
    const verifyToken = crypto.randomBytes(32).toString('hex');
    const verifyExpires = new Date(Date.now() + 24 * 60 * 60 * 1000);
    user.emailVerifyToken = verifyToken;
    user.emailVerifyExpires = verifyExpires;
    await user.save();

    const base = process.env.PUBLIC_BASE_URL || `https://${req.get('host')}`;
    const webLink = `${base}/auth/verify?token=${verifyToken}`;
    const appScheme = process.env.APP_DEEP_LINK_SCHEME || 'kleos';
    const appHost = process.env.APP_DEEP_LINK_HOST || 'verify';
    const appLink = `${appScheme}://${appHost}?token=${verifyToken}`;

    await sendVerificationEmail(email, user.fullName, webLink, appLink);
    return res.json({ ok: true, verifyUrl: webLink, appLink });
  } catch (e: any) {
    return res.status(400).json({ error: e?.message || 'bad_request' });
  }
});

router.post('/login', async (req, res) => {
  try {
    const { email, password } = loginSchema.parse(req.body);
    const user = await User.findOne({ email });
    if (!user) return res.status(400).json({ error: 'invalid_credentials' });
    const ok = await bcrypt.compare(password, user.passwordHash);
    if (!ok) return res.status(400).json({ error: 'invalid_credentials' });
    if (!user.emailVerified) return res.status(403).json({ error: 'email_not_verified' });
    const token = jwt.sign({ uid: user._id.toString(), role: user.role }, process.env.JWT_SECRET!, { expiresIn: '30d' });
    return res.json({ token, user: { id: user._id, fullName: user.fullName, email, role: user.role } });
  } catch (e: any) {
    return res.status(400).json({ error: e?.message || 'bad_request' });
  }
});

// Diagnostics: check SMTP connectivity quickly (no email sent)
router.get('/smtp/ping', async (_req, res) => {
  try {
    const host = process.env.SMTP_HOST;
    if (!host) return res.status(400).json({ ok: false, error: 'smtp_not_configured' });

    const proxies = getProxyList();
    const combos = getSmtpCombos(host);
    const tlsInsecure = (process.env.SMTP_TLS_INSECURE || '').toLowerCase() === 'true';

    const attemptOrder: Array<{ proxy?: string; port: number; secure: boolean }> = [];
    // Try each proxy with each combo, then try direct without proxy as last resort
    for (const proxy of proxies) {
      for (const combo of combos) {
        attemptOrder.push({ proxy, port: combo.port, secure: combo.secure });
      }
    }
    for (const combo of combos) {
      attemptOrder.push({ proxy: undefined, port: combo.port, secure: combo.secure });
    }

    let lastError: any = null;
    for (const attempt of attemptOrder) {
      try {
        const options: any = {
          host,
          port: attempt.port,
          secure: attempt.secure,
          connectionTimeout: parseInt(process.env.SMTP_CONN_TIMEOUT || '7000', 10),
          greetingTimeout: parseInt(process.env.SMTP_GREET_TIMEOUT || '7000', 10),
          socketTimeout: parseInt(process.env.SMTP_SOCKET_TIMEOUT || '12000', 10),
          auth: process.env.SMTP_USER ? { user: process.env.SMTP_USER, pass: process.env.SMTP_PASS } : undefined,
          requireTLS: !attempt.secure,
          tls: { servername: host }
        };
        if (attempt.proxy) options.proxy = attempt.proxy;
        if (tlsInsecure) options.tls = { ...(options.tls || {}), rejectUnauthorized: false };
        const transporter = nodemailer.createTransport(options);
        await transporter.verify();
        return res.json({
          ok: true,
          host,
          portTried: attempt.port,
          secureTried: attempt.secure,
          proxyUsed: attempt.proxy ? 'enabled' : 'disabled'
        });
      } catch (e: any) {
        lastError = e;
        // Continue to next attempt
      }
    }
    return res.status(500).json({ ok: false, error: lastError?.message || 'smtp_error', code: lastError?.code });
  } catch (e: any) {
    return res.status(500).json({ ok: false, error: e?.message || 'smtp_error', code: e?.code });
  }
});

// Diagnostics: HTTP relay ping
router.get('/relay/ping', async (_req, res) => {
  try {
    const relayUrl = getRelayUrl();
    if (!relayUrl) return res.status(400).json({ ok: false, error: 'relay_not_configured' });
    const resp = await fetch(relayUrl, {
      method: 'POST',
      headers: { 'content-type': 'application/json' },
      body: JSON.stringify({ ping: true })
    });
    const text = await resp.text();
    return res.status(resp.status).json({ ok: resp.ok, body: text });
  } catch (e: any) {
    return res.status(500).json({ ok: false, error: e?.message || 'relay_error' });
  }
});

export default router;

async function sendVerificationEmail(to: string, name: string, webLink: string, appLink: string) {
  // 1) HTTP relay (if configured) — no SMTP needed
  const relayUrl = getRelayUrl();
  if (relayUrl) {
    try {
      const controller = new AbortController();
      const timeout = setTimeout(() => controller.abort(), parseInt(process.env.RELAY_TIMEOUT_MS || '8000', 10));
      const html = `<div style="font-family:Arial;">
    <p>Здравствуйте, ${name}!</p>
    <p>Пожалуйста, подтвердите ваш email:</p>
    <p><a href="${webLink}">Подтвердить email</a></p>
    <p>На Android можно открыть приложение напрямую: <a href="${appLink}">${appLink}</a></p>
    <p>Ссылка действует 24 часа.</p>
  </div>`;
      const resp = await fetch(relayUrl, {
        method: 'POST',
        headers: { 'content-type': 'application/json' },
        body: JSON.stringify({
          to,
          name,
          subject: 'Kleos — подтверждение email',
          html,
          fromEmail: process.env.SMTP_FROM || 'no-reply@kleos-study.ru',
          fromName: 'Kleos University'
        }),
        signal: controller.signal
      });
      clearTimeout(timeout);
      if (resp.ok) return;
      // if relay responded but not ok, fall through to SMTP
      try {
        const body = await resp.text();
        console.error(`Email relay responded with ${resp.status} to ${to}: ${body}`);
      } catch {}
    } catch (_e) {
      // ignore and fall back to SMTP
    }
  }

  const host = process.env.SMTP_HOST;
  const from = process.env.SMTP_FROM || 'no-reply@kleos';
  if (!host) {
    console.log(`Verify email for ${to}: ${webLink} (app: ${appLink})`);
    return;
  }

  const proxies = getProxyList();
  const combos = getSmtpCombos(host);
  const tlsInsecure = (process.env.SMTP_TLS_INSECURE || '').toLowerCase() === 'true';

  const attempts: Array<{ proxy?: string; port: number; secure: boolean }> = [];
  for (const proxy of proxies) {
    for (const combo of combos) {
      attempts.push({ proxy, port: combo.port, secure: combo.secure });
    }
  }
  for (const combo of combos) {
    attempts.push({ proxy: undefined, port: combo.port, secure: combo.secure });
  }

  const html = `<div style="font-family:Arial;">
    <div style="max-width:560px;margin:0 auto;padding:24px 20px;background:#ffffff;border:1px solid #eee;border-radius:8px;">
      <h2 style="margin:0 0 16px 0;color:#111;">Подтверждение email</h2>
      <p style="margin:0 0 12px 0;color:#333;">Здравствуйте, ${name}!</p>
      <p style="margin:0 0 18px 0;color:#333;">Чтобы завершить регистрацию в приложении Kleos, подтвердите ваш email.</p>

      <!-- Основная кнопка: открыть приложение по deep link -->
      <div style="text-align:center;margin:18px 0;">
        <a href="${appLink}" style="display:inline-block;background:#2563eb;color:#fff;text-decoration:none;padding:12px 18px;border-radius:6px;font-weight:600;">
          Открыть приложение и подтвердить
        </a>
      </div>

      <!-- Запасной вариант: веб‑кнопка -->
      <p style="margin:16px 0 8px 0;color:#333;">Если приложение не открылось, подтвердите по ссылке на сайте:</p>
      <div style="text-align:center;margin:10px 0 22px 0;">
        <a href="${webLink}" style="display:inline-block;background:#10b981;color:#fff;text-decoration:none;padding:10px 16px;border-radius:6px;font-weight:600;">
          Подтвердить на сайте
        </a>
      </div>

      <!-- Прямая deep‑link и веб‑ссылка на случай устаревших клиентов -->
      <div style="font-size:13px;color:#555;line-height:1.6;border-top:1px solid #f0f0f0;padding-top:12px;">
        <p style="margin:0 0 8px 0;">Если кнопки выше не работают, скопируйте и откройте одну из ссылок вручную:</p>
        <p style="margin:0 0 6px 0;word-break:break-all;"><strong>Приложение:</strong> <a href="${appLink}" style="color:#2563eb">${appLink}</a></p>
        <p style="margin:0;word-break:break-all;"><strong>Сайт:</strong> <a href="${webLink}" style="color:#2563eb">${webLink}</a></p>
        <p style="margin:12px 0 0 0;color:#777;">Ссылка действует 24 часа.</p>
      </div>
    </div>
  </div>`;

  let lastError: any = null;
  for (const attempt of attempts) {
    try {
      const options: any = {
        host,
        port: attempt.port,
        secure: attempt.secure,
        connectionTimeout: parseInt(process.env.SMTP_CONN_TIMEOUT || '7000', 10),
        greetingTimeout: parseInt(process.env.SMTP_GREET_TIMEOUT || '7000', 10),
        socketTimeout: parseInt(process.env.SMTP_SOCKET_TIMEOUT || '12000', 10),
        auth: process.env.SMTP_USER ? { user: process.env.SMTP_USER, pass: process.env.SMTP_PASS } : undefined,
        requireTLS: !attempt.secure,
        tls: { servername: host }
      };
      if (attempt.proxy) options.proxy = attempt.proxy;
      if (tlsInsecure) options.tls = { ...(options.tls || {}), rejectUnauthorized: false };
      const transporter = nodemailer.createTransport(options);
      await transporter.sendMail({ from, to, subject: 'Kleos — подтверждение email', html });
      return; // success
    } catch (e: any) {
      lastError = e;
    }
  }
  throw lastError || new Error('SMTP send failed');
}

