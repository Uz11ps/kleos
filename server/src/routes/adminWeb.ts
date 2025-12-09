import { Router } from 'express';
import cookieParser from 'cookie-parser';
import jwt from 'jsonwebtoken';
import { User } from '../models/User.js';
import { z } from 'zod';
import { Partner } from '../models/Partner.js';
import { Admission } from '../models/Admission.js';
import chatsRoutes from './chats.js';
import multer from 'multer';
import path from 'path';
import fs from 'fs';
import mongoose, { Schema, model, Types } from 'mongoose';

const router = Router();

// Helpers
function getAdminCreds() {
  return {
    username: process.env.ADMIN_USERNAME || '123',
    password: process.env.ADMIN_PASSWORD || '123'
  };
}

// File uploads (logos)
const upload = multer({
  storage: multer.diskStorage({
    destination: (_req, _file, cb) => cb(null, path.join(process.cwd(), 'uploads', 'logos')),
    filename: (_req, file, cb) => {
      const name = `${Date.now()}-${Math.random().toString(36).slice(2)}${path.extname(file.originalname || '')}`;
      cb(null, name);
    }
  }),
  limits: { fileSize: 5 * 1024 * 1024 } // 5MB
});

// Minimal chat models for server-side admin rendering
const ChatSchema = new Schema({
  userId: { type: Types.ObjectId, ref: 'User', index: true },
  status: { type: String, enum: ['open', 'closed'], default: 'open', index: true },
  lastMessageAt: { type: Date, index: true }
}, { timestamps: true });
const MessageSchema = new Schema({
  chatId: { type: Types.ObjectId, ref: 'Chat', index: true },
  senderRole: { type: String, enum: ['student', 'admin', 'system'] },
  text: String
}, { timestamps: true });
const Chat = (mongoose.models.Chat as any) || model('Chat', ChatSchema);
const Message = (mongoose.models.Message as any) || model('Message', MessageSchema);

function adminLayout(opts: {
  title: string;
  active?: 'users' | 'partners' | 'admissions' | 'programs' | 'chats' | 'i18n' | 'news' | '';
  body: string;
}) {
  const { title, active = '', body } = opts;
  const navLink = (href: string, label: string, key: typeof active) =>
    `<a class="nav-link ${active === key ? 'active' : ''}" href="${href}">${label}</a>`;
  return `<!doctype html>
<html lang="en">
  <head>
    <meta charset="utf-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1" />
    <title>${title}</title>
    <style>
      :root{
        --bg:#0b1021; --card:#0f1631; --muted:#94a3b8; --text:#e2e8f0;
        --accent:#2563eb; --accent-2:#0ea5e9; --danger:#ef4444; --border:#1e293b;
      }
      *{box-sizing:border-box}
      html,body{margin:0;padding:0;background:var(--bg);color:var(--text);font-family:system-ui,-apple-system,Segoe UI,Roboto,Ubuntu,Helvetica,Arial,sans-serif}
      a{color:var(--accent);text-decoration:none}
      a:hover{text-decoration:underline}
      .container{max-width:1200px;margin:0 auto;padding:16px}
      .topbar{
        display:flex;align-items:center;justify-content:space-between;gap:12px;
        position:sticky;top:0;background:linear-gradient(180deg,#0b1021 0%, rgba(11,16,33,0.85) 100%);
        backdrop-filter: blur(6px); border-bottom:1px solid var(--border); padding:10px 16px; z-index: 10;
      }
      .brand{display:flex;align-items:center;gap:10px;font-weight:700}
      .brand .dot{width:10px;height:10px;border-radius:50%;background:linear-gradient(135deg,var(--accent),var(--accent-2))}
      .nav{display:flex;gap:10px;flex-wrap:wrap}
      .nav-link{padding:8px 12px;border:1px solid var(--border);border-radius:8px;color:var(--text);display:inline-block}
      .nav-link.active{border-color:var(--accent);background:rgba(37,99,235,0.12)}
      .logout{margin-left:auto}

      .page{padding:16px}
      .card{background:var(--card);border:1px solid var(--border);border-radius:12px;padding:16px}
      h1,h2,h3{margin:0 0 12px 0}
      .muted{color:var(--muted)}

      .toolbar{display:flex;flex-wrap:wrap;gap:8px;align-items:center;margin:12px 0}
      .btn{display:inline-block;padding:10px 14px;border-radius:8px;border:1px solid var(--border);background:#11183a;color:var(--text)}
      .btn.primary{background:linear-gradient(135deg,var(--accent),var(--accent-2));border:none;color:#fff}
      .btn.danger{background:rgba(239,68,68,0.15);border:1px solid rgba(239,68,68,0.5);color:#fecaca}

      .grid{display:grid;gap:12px}
      @media (min-width: 900px){
        .grid.cols-2{grid-template-columns: 1fr 1fr}
      }

      .form-row{display:flex;flex-wrap:wrap;gap:8px}
      input,select,textarea{background:#0c1330;border:1px solid var(--border);color:var(--text);padding:10px;border-radius:8px;outline:none;min-width:0}
      textarea{resize:vertical}
      label{display:block;margin:6px 0}

      .table-wrap{overflow:auto;border:1px solid var(--border);border-radius:10px}
      table{border-collapse:collapse;min-width:800px;width:100%;}
      th,td{border-bottom:1px solid var(--border);text-align:left;padding:10px;vertical-align:top}
      thead th{position:sticky;top:0;background:#0c1330}

      .stack{display:none}
      @media (max-width: 720px){
        .nav{display:grid;grid-template-columns: 1fr 1fr;gap:8px}
        .table-wrap{border-radius:10px;overflow:auto}
        table{min-width:680px}
        .stack{display:block}
      }
    </style>
  </head>
  <body>
    <div class="topbar">
      <div class="brand"><span class="dot"></span> Kleos Admin</div>
      <nav class="nav">
        ${navLink('/admin/users','Users','users')}
        ${navLink('/admin/partners','Partners','partners')}
        ${navLink('/admin/admissions','Admissions','admissions')}
        ${navLink('/admin/programs','Programs','programs')}
        ${navLink('/admin/chats','Chats','chats')}
        ${navLink('/admin/i18n','I18n','i18n')}
        ${navLink('/admin/news','News','news')}
      </nav>
      <div class="logout"><a class="nav-link" href="/admin/logout">Logout</a></div>
    </div>
    <div class="container page">
      ${body}
    </div>
  </body>
</html>`;
}

function adminAuthMiddleware(req: any, res: any, next: any) {
  const token = req.cookies?.admin_token;
  if (!token) {
    return res.redirect('/admin/login');
  }
  try {
    jwt.verify(token, process.env.JWT_SECRET!);
    next();
  } catch {
    res.clearCookie('admin_token', { httpOnly: true, sameSite: 'lax' });
    return res.redirect('/admin/login');
  }
}

router.use(cookieParser());

// Redirect root to admin
router.get('/', (_req, res) => {
  res.redirect('/admin');
});

// Login form
router.get('/admin', (req, res) => {
  const body = `
    <div class="grid cols-2">
      <div class="card">
        <h2>Sign in</h2>
        <p class="muted">Use your admin credentials</p>
        ${req.query.err ? `<div style="color:#fecaca;margin:8px 0;">${req.query.err}</div>` : ''}
        <form method="post" action="/admin/login" class="form" style="margin-top:10px;display:grid;gap:10px">
          <div>
            <label>Username</label>
            <input name="username" placeholder="admin" />
          </div>
          <div>
            <label>Password</label>
            <input name="password" type="password" placeholder="••••••••" />
          </div>
          <div><button class="btn primary" type="submit">Sign in</button></div>
        </form>
      </div>
      <div class="card stack">
        <h3>Welcome to Kleos Admin</h3>
        <p class="muted">Manage users, partners, admissions and support chats</p>
      </div>
    </div>
  `;
  res.send(adminLayout({ title: 'Kleos Admin - Login', active: '', body }));
});

// Handle login
router.post('/admin/login', (req: any, res: any) => {
  const schema = z.object({ username: z.string(), password: z.string() });
  const { username, password } = schema.parse(req.body);
  const expected = getAdminCreds();
  if (username === expected.username && password === expected.password) {
    const token = jwt.sign({ role: 'admin' }, process.env.JWT_SECRET!, { expiresIn: '7d' });
    res.cookie('admin_token', token, { httpOnly: true, sameSite: 'lax' });
    return res.redirect('/admin/users');
  }
  return res.redirect('/admin?err=Invalid credentials');
});

router.get('/admin/logout', (req, res) => {
  res.clearCookie('admin_token', { httpOnly: true, sameSite: 'lax' });
  res.redirect('/admin');
});

// Users list
router.get('/admin/users', adminAuthMiddleware, async (_req, res) => {
  const users = await User.find().sort({ createdAt: -1 }).lean();
  const rows = users.map(u => `
    <tr>
      <td>${u._id}</td>
      <td>
        <form method="post" action="/admin/users/${u._id}">
          <input name="fullName" value="${(u.fullName || '').toString().replace(/"/g, '&quot;')}" />
          <input name="email" value="${(u.email || '').toString().replace(/"/g, '&quot;')}" disabled />
          <select name="role">
            <option value="student"${u.role === 'student' ? ' selected' : ''}>student</option>
            <option value="admin"${u.role === 'admin' ? ' selected' : ''}>admin</option>
          </select>
          <input name="studentId" placeholder="Student ID" value="${(u as any).studentId || ''}" />
          <input name="phone" placeholder="Phone" value="${(u as any).phone || ''}" />
          <input name="course" placeholder="Course" value="${(u as any).course || ''}" />
          <input name="speciality" placeholder="Speciality" value="${(u as any).speciality || ''}" />
          <input name="status" placeholder="Status" value="${(u as any).status || ''}" />
          <input name="university" placeholder="University" value="${(u as any).university || ''}" />
          <input name="payment" placeholder="Payment" value="${(u as any).payment || ''}" />
          <input name="penalties" placeholder="Penalties" value="${(u as any).penalties || ''}" />
          <input name="notes" placeholder="Notes" value="${(u as any).notes || ''}" />
          <label style="display:inline-flex;align-items:center;gap:6px">
            <input type="checkbox" name="emailVerified" ${u.emailVerified ? 'checked' : ''} />
            Verified
          </label>
          <button class="btn primary" type="submit">Save</button>
        </form>
        <form method="post" action="/admin/users/${u._id}/delete" onsubmit="return confirm('Delete this user?')">
          <button class="btn danger" type="submit" style="margin-top:6px;">Delete</button>
        </form>
      </td>
    </tr>
  `).join('');

  const body = `
    <div class="card">
      <h2>Users</h2>
      <div class="table-wrap" style="margin-top:12px">
        <table>
          <thead><tr><th style="width:240px">ID</th><th>Data</th></tr></thead>
          <tbody>${rows}</tbody>
        </table>
      </div>
    </div>
    <script>
      // Автоматическое обновление данных каждые 3 секунды
      let refreshInterval;
      function refreshUserData() {
        fetch('/admin/users')
          .then(r => r.text())
          .then(html => {
            const parser = new DOMParser();
            const doc = parser.parseFromString(html, 'text/html');
            const newRows = doc.querySelectorAll('tbody tr');
            const currentRows = document.querySelectorAll('tbody tr');
            
            newRows.forEach((newRow, idx) => {
              if (idx >= currentRows.length) return;
              const currentRow = currentRows[idx];
              const newInputs = newRow.querySelectorAll('input, select');
              const currentInputs = currentRow.querySelectorAll('input, select');
              
              newInputs.forEach((newInput, inputIdx) => {
                if (inputIdx >= currentInputs.length) return;
                const currentInput = currentInputs[inputIdx];
                // Обновляем только если поле не в фокусе
                if (document.activeElement !== currentInput && currentInput.type !== 'checkbox') {
                  currentInput.value = newInput.value || '';
                }
                if (currentInput.type === 'checkbox' && document.activeElement !== currentInput) {
                  currentInput.checked = newInput.checked;
                }
                if (currentInput.tagName === 'SELECT' && document.activeElement !== currentInput) {
                  currentInput.value = newInput.value;
                }
              });
            });
          })
          .catch(err => console.error('Refresh error:', err));
      }
      
      refreshInterval = setInterval(refreshUserData, 3000);
      
      // Останавливаем обновление при уходе со страницы
      window.addEventListener('beforeunload', () => {
        if (refreshInterval) clearInterval(refreshInterval);
      });
    </script>
  `;
  res.send(adminLayout({ title: 'Kleos Admin - Users', active: 'users', body }));
});

// Update user
router.post('/admin/users/:id', adminAuthMiddleware, async (req: any, res: any) => {
  const schema = z.object({
    fullName: z.string().optional(),
    role: z.enum(['student','admin']).optional(),
    phone: z.string().optional(),
    course: z.string().optional(),
    speciality: z.string().optional(),
    status: z.string().optional(),
    university: z.string().optional(),
    payment: z.string().optional(),
    penalties: z.string().optional(),
    notes: z.string().optional(),
    studentId: z.string().optional(),
    emailVerified: z.union([z.literal('on'), z.string()]).optional()
  });
  const parsed = schema.parse(req.body);
  const update: any = { ...parsed };
  if ('emailVerified' in parsed) {
    update.emailVerified = parsed.emailVerified === 'on';
  }
  await User.updateOne({ _id: req.params.id }, update);
  res.redirect('/admin/users');
});

router.post('/admin/users/:id/delete', adminAuthMiddleware, async (req, res) => {
  await User.deleteOne({ _id: req.params.id });
  res.redirect('/admin/users');
});

// Partners UI
router.get('/admin/partners', adminAuthMiddleware, async (_req, res) => {
  const partners = await Partner.find().sort({ order: 1, createdAt: -1 }).lean();
  const items = partners.map(p => `
    <tr>
      <td>${p._id}</td>
      <td>
        <form method="post" action="/admin/partners/${p._id}" enctype="multipart/form-data">
          <input name="name" value="${(p.name || '').toString().replace(/"/g, '&quot;')}" />
          <input name="description" value="${(p.description || '').toString().replace(/"/g, '&quot;')}" />
          <input name="logoUrl" placeholder="Logo URL (optional)" value="${(p.logoUrl || '').toString().replace(/"/g, '&quot;')}" />
          <input type="file" name="logoFile" accept="image/*" />
          <input name="url" placeholder="Site URL" value="${(p.url || '').toString().replace(/"/g, '&quot;')}" />
          <input name="order" type="number" value="${p.order || 0}" />
          <label><input type="checkbox" name="active" ${p.active ? 'checked' : ''}/> active</label>
          <button class="btn primary" type="submit">Save</button>
        </form>
        <form method="post" action="/admin/partners/${p._id}/delete" onsubmit="return confirm('Delete partner?')">
          <button class="btn danger" type="submit" style="margin-top:6px;">Delete</button>
        </form>
      </td>
    </tr>`).join('');
  const body = `
    <div class="grid cols-2">
      <div class="card">
        <h2>Add partner</h2>
        <form method="post" action="/admin/partners" class="form-row" style="margin-top:10px" enctype="multipart/form-data">
          <input name="name" placeholder="Name"/>
          <input name="description" placeholder="Description"/>
          <input name="logoUrl" placeholder="Logo URL (optional)"/>
          <input type="file" name="logoFile" accept="image/*" />
          <input name="url" placeholder="Site URL"/>
          <input name="order" type="number" value="0" style="max-width:120px"/>
          <label style="display:flex;align-items:center;gap:8px"><input type="checkbox" name="active" checked/> active</label>
          <button class="btn primary" type="submit">Create</button>
        </form>
      </div>
      <div class="card">
        <h2>All partners</h2>
        <div class="table-wrap" style="margin-top:12px">
          <table>
            <thead><tr><th style="width:240px">ID</th><th>Data</th></tr></thead>
            <tbody>${items}</tbody>
          </table>
        </div>
      </div>
    </div>
  `;
  res.send(adminLayout({ title: 'Kleos Admin - Partners', active: 'partners', body }));
});

router.post('/admin/partners', adminAuthMiddleware, upload.single('logoFile'), async (req: any, res) => {
  const schema = z.object({
    name: z.string(),
    description: z.string().optional(),
    logoUrl: z.string().optional(),
    url: z.string().optional(),
    order: z.coerce.number().optional(),
    active: z.string().optional()
  });
  const data = schema.parse(req.body);
  let finalLogo = data.logoUrl;
  if (req.file) {
    const base = process.env.PUBLIC_BASE_URL || '';
    finalLogo = `${base}/uploads/logos/${req.file.filename}`;
  }
  await Partner.create({ ...data, logoUrl: finalLogo, active: !!data.active });
  res.redirect('/admin/partners');
});

router.post('/admin/partners/:id', adminAuthMiddleware, upload.single('logoFile'), async (req: any, res) => {
  const schema = z.object({
    name: z.string().optional(),
    description: z.string().optional(),
    logoUrl: z.string().optional(),
    url: z.string().optional(),
    order: z.coerce.number().optional(),
    active: z.string().optional()
  });
  const data = schema.parse(req.body);
  let finalLogo = data.logoUrl;
  if (req.file) {
    const base = process.env.PUBLIC_BASE_URL || '';
    finalLogo = `${base}/uploads/logos/${req.file.filename}`;
  }
  await Partner.updateOne({ _id: req.params.id }, { ...data, logoUrl: finalLogo, active: !!data.active });
  res.redirect('/admin/partners');
});

router.post('/admin/partners/:id/delete', adminAuthMiddleware, async (req, res) => {
  await Partner.deleteOne({ _id: req.params.id });
  res.redirect('/admin/partners');
});

// Admissions UI
router.get('/admin/admissions', adminAuthMiddleware, async (req: any, res) => {
  const showAll = req.query.all === 'true';
  const filter = showAll ? {} : { $or: [{ status: { $in: ['new', 'processing', null] } }, { status: { $exists: false } }] };
  const list = await Admission.find(filter).sort({ createdAt: -1 }).lean();
  const rows = list.map(a => {
    const status = a.status || 'new';
    const isProcessed = status === 'done' || status === 'rejected';
    const statusColor = status === 'done' ? '#10b981' : status === 'rejected' ? '#ef4444' : '#3b82f6';
    return `
    <tr>
      <td>${a._id}</td>
      <td>
        <div style="margin-bottom:8px">
          <div><b>${a.fullName}</b></div>
          <div>Email: ${a.email} | Phone: ${a.phone}</div>
          <div>Program: ${a.program}</div>
          ${a.comment ? `<div class="muted">Comment: ${(a.comment as string).toString().replace(/</g,'&lt;')}</div>` : ''}
          <div class="muted">Status: <span style="color:${statusColor};font-weight:600">${status}</span>${(a as any).studentId ? ` | Student ID: ${(a as any).studentId}` : ''}${(a as any).userId ? ` | userId: ${(a as any).userId}` : ''}</div>
        </div>
        <div style="margin:6px 0">
          <a class="btn" href="/admin/admissions/${a._id}/view">Подробнее</a>
        </div>
        ${!isProcessed ? `
        <div class="form-row">
          <form method="post" action="/admin/admissions/${a._id}/accept" class="form-row">
            <input name="studentId" placeholder="Student ID" value="${(a as any).studentId || ''}" />
            <button class="btn primary" type="submit">Принять</button>
          </form>
          <form method="post" action="/admin/admissions/${a._id}/reject" onsubmit="return confirm('Отклонить заявку?')" style="margin-left:8px">
            <button class="btn danger" type="submit">Отклонить</button>
          </form>
        </div>
        ` : ''}
      </td>
    </tr>`;
  }).join('');
  const body = `
    <div class="card">
      <div style="display:flex;align-items:center;justify-content:space-between;gap:10px;margin-bottom:12px">
        <h2 style="margin:0">Admissions</h2>
        <div>
          <a class="btn ${showAll ? '' : 'primary'}" href="/admin/admissions">Новые</a>
          <a class="btn ${showAll ? 'primary' : ''}" href="/admin/admissions?all=true" style="margin-left:8px">Все</a>
        </div>
      </div>
      <div class="table-wrap" style="margin-top:12px">
        <table>
          <thead><tr><th style="width:240px">ID</th><th>Data</th></tr></thead>
          <tbody>${rows}</tbody>
        </table>
      </div>
    </div>
  `;
  res.send(adminLayout({ title: 'Kleos Admin - Admissions', active: 'admissions', body }));
});

// Accept admission: set status=done, optionally assign studentId; if linked userId exists — make role=student
router.post('/admin/admissions/:id/accept', adminAuthMiddleware, async (req: any, res) => {
  const id = req.params.id;
  const studentId = (req.body?.studentId as string | undefined) || undefined;
  const adm = await Admission.findById(id).lean();
  if (adm) {
    await Admission.updateOne({ _id: id }, { status: 'done', ...(studentId ? { studentId } : {}) });
    const uid = (adm as any).userId;
    if (uid) {
      await User.updateOne({ _id: uid }, { role: 'student', ...(studentId ? { studentId } : {}) });
    }
  }
  res.redirect('/admin/admissions');
});

// Reject admission: mark status=rejected (не меняем пользователя)
router.post('/admin/admissions/:id/reject', adminAuthMiddleware, async (req, res) => {
  const id = req.params.id;
  await Admission.updateOne({ _id: id }, { status: 'rejected' });
  res.redirect('/admin/admissions');
});

// Admission details
router.get('/admin/admissions/:id/view', adminAuthMiddleware, async (req, res) => {
  const id = req.params.id;
  const a: any = await Admission.findById(id).lean();
  if (!a) {
    return res.redirect('/admin/admissions');
  }
  const entries = Object.entries(a).map(([k, v]) => {
    let val: string;
    try {
      if (v === null || v === undefined) val = '';
      else if (typeof v === 'object') val = JSON.stringify(v, null, 2);
      else val = String(v);
    } catch { val = String(v); }
    val = val.replace(/</g, '&lt;');
    return `<tr><th style="width:220px">${k}</th><td><pre style="margin:0;white-space:pre-wrap">${val}</pre></td></tr>`;
  }).join('');

  const body = `
    <div class="card">
      <div style="display:flex;align-items:center;justify-content:space-between;gap:10px">
        <h2 style="margin:0">Admission details</h2>
        <a class="btn" href="/admin/admissions">&larr; Назад</a>
      </div>
      <div class="table-wrap" style="margin-top:12px">
        <table>
          <tbody>
            ${entries}
          </tbody>
        </table>
      </div>
      <div class="toolbar">
        <form method="post" action="/admin/admissions/${id}/accept" class="form-row">
          <input name="studentId" placeholder="Student ID" value="${a.studentId || ''}" />
          <button class="btn primary" type="submit">Принять</button>
        </form>
        <form method="post" action="/admin/admissions/${id}/reject" onsubmit="return confirm('Отклонить заявку?')">
          <button class="btn danger" type="submit">Отклонить</button>
        </form>
      </div>
    </div>
  `;
  res.send(adminLayout({ title: `Kleos Admin - Admission ${id}`, active: 'admissions', body }));
});

router.post('/admin/admissions/:id', adminAuthMiddleware, async (req, res) => {
  const schema = z.object({ status: z.string().optional(), studentId: z.string().optional() });
  const data = schema.parse(req.body);
  await Admission.updateOne({ _id: req.params.id }, data);
  res.redirect('/admin/admissions');
});

// Chats simple UI (uses public chats endpoints)
router.get('/admin/chats', adminAuthMiddleware, async (_req, res) => {
  const chats = await Chat.find().sort({ lastMessageAt: -1 }).limit(200).lean();
  const items = chats.map(c => `<li><a href="/admin/chats/${c._id}">${c._id} (user ${c.userId})</a></li>`).join('');
  const body = `
    <div class="card">
      <h2>Chats</h2>
      <ul style="margin-top:12px">${items || '<li class="muted">Нет чатов</li>'}</ul>
    </div>
  `;
  res.send(adminLayout({ title: 'Kleos Admin - Chats', active: 'chats', body }));
});

router.get('/admin/chats/:id', adminAuthMiddleware, async (req, res) => {
  const chatId = req.params.id;
  const msgs = await Message.find({ chatId }).sort({ createdAt: 1 }).lean();
  const list = msgs.map(m => `<div><b>${m.senderRole}:</b> ${String(m.text || '').replace(/</g,'&lt;')}</div>`).join('');
  const body = `
    <div class="card">
      <div><a href="/admin/chats">&larr; Back</a></div>
      <h3 style="margin-top:8px">Chat ${chatId}</h3>
      <div class="card" style="margin-top:10px;height:400px;overflow:auto;">${list || '<div class="muted">Пока нет сообщений</div>'}</div>
      <form method="post" action="/admin/chats/${chatId}/send" class="form-row" style="margin-top:10px">
        <input name="text" placeholder="Message" style="flex:1;min-width:180px"/>
        <button class="btn primary" type="submit">Send</button>
      </form>
    </div>
  `;
  res.send(adminLayout({ title: `Kleos Admin - Chat ${chatId}`, active: 'chats', body }));
});

router.post('/admin/chats/:id/send', adminAuthMiddleware, async (req, res) => {
  const chatId = req.params.id;
  const text = (req.body?.text as string || '').trim();
  if (text.length > 0) {
    await Message.create({ chatId, senderRole: 'admin', text });
    await Chat.updateOne({ _id: chatId }, { lastMessageAt: new Date() });
  }
  res.redirect(`/admin/chats/${chatId}`);
});

// I18n UI
router.get('/admin/i18n', adminAuthMiddleware, async (_req, res) => {
  const langs = ['ru','en','zh'];
  const { Translation } = await import('../models/Translation.js');
  const all = await Translation.find({}).lean();
  const keySet = new Set<string>();
  for (const t of all) keySet.add(t.key);
  // Дополнительно подтягиваем ключи и значения из Android strings.xml проекта
  try {
    const baseDir = path.resolve(process.cwd(), '..', 'app', 'src', 'main', 'res');
    const files: Array<{ file: string; lang: 'ru'|'en'|'zh' }> = [
      { file: path.join(baseDir, 'values', 'strings.xml'), lang: 'en' }, // дефолт считаем en
      { file: path.join(baseDir, 'values-en', 'strings.xml'), lang: 'en' },
      { file: path.join(baseDir, 'values-ru', 'strings.xml'), lang: 'ru' },
      { file: path.join(baseDir, 'values-zh-rCN', 'strings.xml'), lang: 'zh' }
    ];
    const xmlVals: Record<'ru'|'en'|'zh', Record<string,string>> = { ru:{}, en:{}, zh:{} };
    const strRe = /<string\s+[^>]*name="([^"]+)"[^>]*>([\s\S]*?)<\/string>/g;
    for (const it of files) {
      if (!fs.existsSync(it.file)) continue;
      const xml = fs.readFileSync(it.file, 'utf8');
      let m: RegExpExecArray | null;
      while ((m = strRe.exec(xml)) !== null) {
        const key = m[1];
        // Пропускаем явно не переводимые
        if (/\btranslatable="false"/.test(m[0])) continue;
        keySet.add(key);
        const val = m[2].replace(/\s+/g, ' ').trim();
        xmlVals[it.lang][key] = val;
      }
    }
    // Сохраняем xml значения для автозаполнения ниже
    (globalThis as any).__i18nXmlVals = xmlVals;
  } catch {}
  const keys = Array.from(keySet).sort((a,b)=>a.localeCompare(b));
  const map: Record<string, Record<string,string>> = {};
  for (const k of keys) map[k] = { ru:'', en:'', zh:'' };
  for (const t of all) {
    if (!map[t.key]) map[t.key] = { ru:'', en:'', zh:'' };
    (map[t.key] as any)[t.lang] = t.value || '';
  }
  // Автозаполняем пустые значения из xml, если есть
  const xmlValsLoaded = (globalThis as any).__i18nXmlVals as (Record<'ru'|'en'|'zh', Record<string,string>> | undefined);
  if (xmlValsLoaded) {
    for (const k of keys) {
      for (const ln of langs as Array<'ru'|'en'|'zh'>) {
        if (!map[k][ln] && xmlValsLoaded[ln] && xmlValsLoaded[ln][k]) {
          map[k][ln] = xmlValsLoaded[ln][k];
        }
      }
    }
  }
  const rows = keys.map(k => {
    const r = map[k];
    return `
      <tr>
        <td><input name="keys[]" value="${k.replace(/"/g,'&quot;')}" /></td>
        <td><input name="ru[]" value="${(r.ru||'').toString().replace(/"/g,'&quot;')}" /></td>
        <td><input name="en[]" value="${(r.en||'').toString().replace(/"/g,'&quot;')}" /></td>
        <td><input name="zh[]" value="${(r.zh||'').toString().replace(/"/g,'&quot;')}" /></td>
      </tr>
    `;
  }).join('');
  const body = `
    <div class="card">
      <h2>I18n (Translations)</h2>
      <form method="post" action="/admin/i18n/save-bulk">
        <div class="toolbar">
          <div class="form-row">
            <input name="newKey" placeholder="new key" />
            <input name="new_ru" placeholder="ru value" />
            <input name="new_en" placeholder="en value" />
            <input name="new_zh" placeholder="zh value" />
            <button class="btn" type="submit">Add & Save</button>
            <a class="btn" href="/admin/i18n/export?lang=ru">Export RU</a>
            <a class="btn" href="/admin/i18n/export?lang=en">Export EN</a>
            <a class="btn" href="/admin/i18n/export?lang=zh">Export ZH</a>
          </div>
        </div>
        <div class="table-wrap" style="margin-top:12px">
          <table>
            <thead><tr><th style="width:28%">Key</th><th>ru</th><th>en</th><th>zh</th></tr></thead>
            <tbody>${rows}</tbody>
          </table>
        </div>
        <div class="toolbar" style="margin-top:12px">
          <button class="btn primary" type="submit">Save All</button>
        </div>
      </form>
    </div>
  `;
  res.send(adminLayout({ title: 'Kleos Admin - I18n', active: 'i18n', body }));
});

router.post('/admin/i18n/save-bulk', adminAuthMiddleware, async (req, res) => {
  const { keys = [], ru = [], en = [], zh = [], newKey = '', new_ru = '', new_en = '', new_zh = '' } = req.body as any;
  const normalizeArr = (v: any) => Array.isArray(v) ? v : (v ? [v] : []);
  const keysArr = normalizeArr(keys);
  const ruArr = normalizeArr(ru);
  const enArr = normalizeArr(en);
  const zhArr = normalizeArr(zh);
  const ops: any[] = [];
  for (let i = 0; i < keysArr.length; i++) {
    const k = String(keysArr[i] || '').trim();
    if (!k) continue;
    ops.push({ updateOne: { filter: { lang: 'ru', key: k }, update: { $set: { value: String(ruArr[i] ?? '') } }, upsert: true } });
    ops.push({ updateOne: { filter: { lang: 'en', key: k }, update: { $set: { value: String(enArr[i] ?? '') } }, upsert: true } });
    ops.push({ updateOne: { filter: { lang: 'zh', key: k }, update: { $set: { value: String(zhArr[i] ?? '') } }, upsert: true } });
  }
  const nk = String(newKey || '').trim();
  if (nk) {
    ops.push({ updateOne: { filter: { lang: 'ru', key: nk }, update: { $set: { value: String(new_ru ?? '') } }, upsert: true } });
    ops.push({ updateOne: { filter: { lang: 'en', key: nk }, update: { $set: { value: String(new_en ?? '') } }, upsert: true } });
    ops.push({ updateOne: { filter: { lang: 'zh', key: nk }, update: { $set: { value: String(new_zh ?? '') } }, upsert: true } });
  }
  const { Translation } = await import('../models/Translation.js');
  if (ops.length) await Translation.bulkWrite(ops, { ordered: false });
  res.redirect('/admin/i18n');
});

router.get('/admin/i18n/export', adminAuthMiddleware, async (req, res) => {
  const lang = (req.query.lang as string) || 'en';
  const { Translation } = await import('../models/Translation.js');
  const items = await Translation.find({ lang }).lean();
  const map: Record<string, string> = {};
  for (const it of items) map[it.key] = it.value;
  res.setHeader('Content-Disposition', `attachment; filename=translations_${lang}.json`);
  res.json(map);
});

router.post('/admin/i18n/import', adminAuthMiddleware, async (req, res) => {
  const schema = z.object({ lang: z.enum(['en','ru','zh']), json: z.string().min(2) });
  const data = schema.parse(req.body);
  const parsed = JSON.parse(data.json);
  if (typeof parsed !== 'object' || Array.isArray(parsed)) return res.status(400).send('Bad JSON');
  const { Translation } = await import('../models/Translation.js');
  const ops: any[] = [];
  for (const [key, value] of Object.entries(parsed as Record<string, string>)) {
    ops.push({ updateOne: { filter: { lang: data.lang, key }, update: { $set: { value: String(value ?? '') } }, upsert: true } });
  }
  if (ops.length) await Translation.bulkWrite(ops, { ordered: false });
  res.redirect(`/admin/i18n?lang=${data.lang}`);
});

// News UI
router.get('/admin/news', adminAuthMiddleware, async (_req, res) => {
  const { News } = await import('../models/News.js');
  const list = await News.find().sort({ order: 1, publishedAt: -1, createdAt: -1 }).lean();
  const rows = list.map(n => `
    <tr>
      <td>${n._id}</td>
      <td>
        <form method="post" action="/admin/news/${n._id}">
          <input name="title" value="${(n.title || '').toString().replace(/"/g,'&quot;')}" />
          <input name="imageUrl" placeholder="Image URL" value="${(n.imageUrl || '').toString().replace(/"/g,'&quot;')}" />
          <input name="publishedAt" type="datetime-local" value="${n.publishedAt ? new Date(n.publishedAt).toISOString().slice(0,16) : ''}" />
          <input name="order" type="number" value="${n.order || 0}" />
          <label><input type="checkbox" name="active" ${n.active ? 'checked' : ''}/> active</label>
          <textarea name="content" rows="3" placeholder="Content" style="width:100%">${(n.content || '').toString().replace(/</g,'&lt;')}</textarea>
          <div style="margin-top:6px">
            <button class="btn primary" type="submit">Save</button>
            <button class="btn danger" formaction="/admin/news/${n._id}/delete" formmethod="post" onclick="return confirm('Delete?')">Delete</button>
          </div>
        </form>
      </td>
    </tr>
  `).join('');
  const body = `
    <div class="card">
      <h2>News</h2>
      <form method="post" action="/admin/news/create" class="form-row">
        <input name="title" placeholder="Title" style="min-width:260px"/>
        <input name="imageUrl" placeholder="Image URL"/>
        <input name="publishedAt" type="datetime-local"/>
        <input name="order" type="number" placeholder="Order" value="0"/>
        <label style="display:inline-flex;align-items:center;gap:6px"><input type="checkbox" name="active" checked/> active</label>
        <button class="btn primary" type="submit">Create</button>
      </form>
      <div class="table-wrap" style="margin-top:12px">
        <table>
          <thead><tr><th style="width:240px">ID</th><th>Data</th></tr></thead>
          <tbody>${rows}</tbody>
        </table>
      </div>
    </div>
  `;
  res.send(adminLayout({ title: 'Kleos Admin - News', active: 'news', body }));
});

// Programs UI
router.get('/admin/programs', adminAuthMiddleware, async (req, res) => {
  const { Program } = await import('../models/Program.js');
  const q = String(req.query.q || '').trim();
  const filter: any = {};
  if (q) filter.$or = [{ title: { $regex: q, $options: 'i' } }, { university: { $regex: q, $options: 'i' } }];
  const list = await Program.find(filter).sort({ order: 1, createdAt: -1 }).lean();
  const rows = list.map(p => `
    <tr>
      <td>${p._id}</td>
      <td>
        <form method="post" action="/admin/programs/${p._id}">
          <div class="form-row" style="margin-bottom:6px">
            <input name="title" value="${(p.title || '').toString().replace(/"/g,'&quot;')}" placeholder="Title"/>
            <input name="slug" value="${(p.slug || '').toString().replace(/"/g,'&quot;')}" placeholder="Slug"/>
            <select name="language">
              ${['ru','en','zh'].map(l=>`<option value="${l}" ${p.language===l?'selected':''}>${l}</option>`).join('')}
            </select>
            <select name="level">
              ${['bachelor','master','phd','foundation','other'].map(l=>`<option value="${l}" ${p.level===l?'selected':''}>${l}</option>`).join('')}
            </select>
            <input name="university" value="${(p.university||'').toString().replace(/"/g,'&quot;')}" placeholder="University"/>
            <input type="number" name="tuition" value="${p.tuition || 0}" placeholder="Tuition"/>
            <input type="number" name="durationMonths" value="${p.durationMonths || 0}" placeholder="Duration, months"/>
            <input name="imageUrl" value="${(p.imageUrl||'').toString().replace(/"/g,'&quot;')}" placeholder="Image URL"/>
            <input type="number" name="order" value="${p.order || 0}" placeholder="Order"/>
            <label><input type="checkbox" name="active" ${p.active ? 'checked' : ''}/> active</label>
          </div>
          <textarea name="description" rows="3" style="width:100%" placeholder="Description">${(p.description || '').toString().replace(/</g,'&lt;')}</textarea>
          <div style="margin-top:6px">
            <button class="btn primary" type="submit">Save</button>
            <button class="btn danger" formaction="/admin/programs/${p._id}/delete" formmethod="post" onclick="return confirm('Delete?')">Delete</button>
          </div>
        </form>
      </td>
    </tr>
  `).join('');
  const body = `
    <div class="card">
      <h2>Programs</h2>
      <div class="toolbar">
        <form method="get" action="/admin/programs" class="form-row" style="gap:8px">
          <input name="q" placeholder="Search..." value="${q.replace(/"/g,'&quot;')}"/>
          <button class="btn" type="submit">Search</button>
          <a class="btn" href="/admin/programs">Reset</a>
        </form>
      </div>
      <form method="post" action="/admin/programs/create" class="form-row">
        <input name="title" placeholder="Title" style="min-width:220px"/>
        <input name="slug" placeholder="Slug"/>
        <select name="language">${['ru','en','zh'].map(l=>`<option value="${l}">${l}</option>`).join('')}</select>
        <select name="level">${['bachelor','master','phd','foundation','other'].map(l=>`<option value="${l}">${l}</option>`).join('')}</select>
        <input name="university" placeholder="University"/>
        <input type="number" name="tuition" placeholder="Tuition"/>
        <input type="number" name="durationMonths" placeholder="Duration, months"/>
        <input name="imageUrl" placeholder="Image URL"/>
        <label style="display:inline-flex;align-items:center;gap:6px"><input type="checkbox" name="active" checked/> active</label>
        <input type="number" name="order" value="0" placeholder="Order"/>
        <button class="btn primary" type="submit">Create</button>
      </form>
      <div class="table-wrap" style="margin-top:12px">
        <table>
          <thead><tr><th style="width:240px">ID</th><th>Data</th></tr></thead>
          <tbody>${rows}</tbody>
        </table>
      </div>
    </div>
  `;
  res.send(adminLayout({ title: 'Kleos Admin - Programs', active: '', body }));
});

router.post('/admin/programs/create', adminAuthMiddleware, async (req, res) => {
  const { Program } = await import('../models/Program.js');
  const schema = z.object({
    title: z.string().min(1),
    slug: z.string().min(1),
    language: z.enum(['ru','en','zh']).optional().default('en'),
    level: z.enum(['bachelor','master','phd','foundation','other']).optional().default('other'),
    university: z.string().optional().default(''),
    tuition: z.coerce.number().optional().default(0),
    durationMonths: z.coerce.number().optional().default(0),
    imageUrl: z.string().optional().default(''),
    active: z.string().optional(),
    order: z.coerce.number().optional().default(0)
  });
  const d = schema.parse(req.body);
  await Program.create({
    title: d.title, slug: d.slug, language: d.language, level: d.level,
    university: d.university, tuition: d.tuition, durationMonths: d.durationMonths,
    imageUrl: d.imageUrl, active: d.active === 'on', order: d.order, description: ''
  });
  res.redirect('/admin/programs');
});

router.post('/admin/programs/:id', adminAuthMiddleware, async (req, res) => {
  const { Program } = await import('../models/Program.js');
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
    active: z.string().optional(),
    order: z.coerce.number().optional()
  });
  const d = schema.parse(req.body);
  const update: any = { ...d };
  if ('active' in d) update.active = d.active === 'on';
  await Program.updateOne({ _id: req.params.id }, update);
  res.redirect('/admin/programs');
});

router.post('/admin/programs/:id/delete', adminAuthMiddleware, async (req, res) => {
  const { Program } = await import('../models/Program.js');
  await Program.deleteOne({ _id: req.params.id });
  res.redirect('/admin/programs');
});
router.post('/admin/news/create', adminAuthMiddleware, async (req, res) => {
  const { News } = await import('../models/News.js');
  const schema = z.object({
    title: z.string().min(1),
    imageUrl: z.string().optional().default(''),
    publishedAt: z.string().optional().default(''),
    order: z.coerce.number().optional().default(0),
    active: z.string().optional()
  });
  const data = schema.parse(req.body);
  await News.create({
    title: data.title,
    imageUrl: data.imageUrl,
    publishedAt: data.publishedAt ? new Date(data.publishedAt) : new Date(),
    order: data.order,
    active: data.active === 'on',
    content: ''
  });
  res.redirect('/admin/news');
});

router.post('/admin/news/:id', adminAuthMiddleware, async (req, res) => {
  const { News } = await import('../models/News.js');
  const schema = z.object({
    title: z.string().optional(),
    imageUrl: z.string().optional(),
    content: z.string().optional(),
    publishedAt: z.string().optional(),
    order: z.coerce.number().optional(),
    active: z.string().optional()
  });
  const data = schema.parse(req.body);
  const update: any = { ...data };
  if ('active' in data) update.active = data.active === 'on';
  if (data.publishedAt) update.publishedAt = new Date(data.publishedAt);
  await News.updateOne({ _id: req.params.id }, update);
  res.redirect('/admin/news');
});

router.post('/admin/news/:id/delete', adminAuthMiddleware, async (req, res) => {
  const { News } = await import('../models/News.js');
  await News.deleteOne({ _id: req.params.id });
  res.redirect('/admin/news');
});
export default router;


