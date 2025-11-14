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

function adminLayout(opts: {
  title: string;
  active?: 'users' | 'partners' | 'admissions' | 'chats' | 'i18n' | '';
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
        ${navLink('/admin/chats','Chats','chats')}
        ${navLink('/admin/i18n','I18n','i18n')}
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
    studentId: z.string().optional()
  });
  const data = schema.parse(req.body);
  await User.updateOne({ _id: req.params.id }, data);
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
router.get('/admin/admissions', adminAuthMiddleware, async (_req, res) => {
  const list = await Admission.find().sort({ createdAt: -1 }).lean();
  const rows = list.map(a => `
    <tr>
      <td>${a._id}</td>
      <td>
        <div style="margin-bottom:8px">
          <div><b>${a.fullName}</b></div>
          <div>Email: ${a.email} | Phone: ${a.phone}</div>
          <div>Program: ${a.program}</div>
          ${a.comment ? `<div class="muted">Comment: ${(a.comment as string).toString().replace(/</g,'&lt;')}</div>` : ''}
          <div class="muted">Status: ${a.status || 'new'}${(a as any).userId ? ` | userId: ${(a as any).userId}` : ''}</div>
        </div>
        <div class="form-row">
          <form method="post" action="/admin/admissions/${a._id}/accept" class="form-row">
            <input name="studentId" placeholder="Student ID" value="${(a as any).studentId || ''}" />
            <button class="btn primary" type="submit">Принять</button>
          </form>
          <form method="post" action="/admin/admissions/${a._id}/reject" onsubmit="return confirm('Отклонить заявку?')" style="margin-left:8px">
            <button class="btn danger" type="submit">Отклонить</button>
          </form>
        </div>
      </td>
    </tr>`).join('');
  const body = `
    <div class="card">
      <h2>Admissions</h2>
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

router.post('/admin/admissions/:id', adminAuthMiddleware, async (req, res) => {
  const schema = z.object({ status: z.string().optional(), studentId: z.string().optional() });
  const data = schema.parse(req.body);
  await Admission.updateOne({ _id: req.params.id }, data);
  res.redirect('/admin/admissions');
});

// Chats simple UI (uses public chats endpoints)
router.get('/admin/chats', adminAuthMiddleware, async (_req, res) => {
  const body = `
    <div class="card">
      <h2>Chats</h2>
      <ul id="list" style="margin-top:12px">Loading...</ul>
    </div>
    <script>
    async function loadChats(){
      const r=await fetch('/chats/all',{headers:{'Accept':'application/json'}});
      const data=await r.json();
      const list=document.getElementById('list');
      list.innerHTML=data.map(c=>'<li><a href="/admin/chats/'+c.id+'">'+c.id+' (user '+c.userId+')</a></li>').join('');
    }
    window.addEventListener('load',loadChats);
    </script>
  `;
  res.send(adminLayout({ title: 'Kleos Admin - Chats', active: 'chats', body }));
});

router.get('/admin/chats/:id', adminAuthMiddleware, async (req, res) => {
  const chatId = req.params.id;
  const body = `
    <div class="card">
      <div><a href="/admin/chats">&larr; Back</a></div>
      <h3 style="margin-top:8px">Chat ${chatId}</h3>
      <div id="msgs" class="card" style="margin-top:10px;height:400px;overflow:auto;">Loading...</div>
      <form id="sendForm" class="form-row" style="margin-top:10px">
        <input name="text" placeholder="Message" style="flex:1;min-width:180px"/>
        <button class="btn primary" type="submit">Send</button>
      </form>
    </div>
    <script>
    async function load(){
      const r=await fetch('/chats/${chatId}/messages',{headers:{'Accept':'application/json'}});
      const data=await r.json();
      document.getElementById('msgs').innerHTML=data.map(m=>'<div><b>'+m.senderRole+':</b> '+m.text+'</div>').join('');
    }
    document.getElementById('sendForm').addEventListener('submit',async (e)=>{
      e.preventDefault();
      const fd=new FormData(e.target);
      const text=fd.get('text');
      await fetch('/chats/${chatId}/messages',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({text})});
      e.target.reset();
      load();
    });
    load();
    </script>
  `;
  res.send(adminLayout({ title: `Kleos Admin - Chat ${chatId}`, active: 'chats', body }));
});

// I18n UI
router.get('/admin/i18n', adminAuthMiddleware, async (req, res) => {
  const lang = (req.query.lang as string) || 'en';
  const langs = ['en', 'ru', 'zh'];
  const items = await (await import('../models/Translation.js')).Translation.find({ lang }).sort({ key: 1 }).lean();
  const rows = items.map(t => `
    <tr>
      <td>${t.key}</td>
      <td>
        <form method="post" action="/admin/i18n/save">
          <input type="hidden" name="lang" value="${lang}" />
          <input type="hidden" name="key" value="${t.key}" />
          <textarea name="value" rows="2" style="width:100%">${(t.value || '').toString().replace(/</g,'&lt;')}</textarea>
          <div style="margin-top:6px">
            <button class="btn primary" type="submit">Save</button>
            <button class="btn danger" formaction="/admin/i18n/delete" formmethod="post" onclick="return confirm('Delete?')">Delete</button>
          </div>
        </form>
      </td>
    </tr>
  `).join('');
  const body = `
    <div class="card">
      <h2>I18n (Translations)</h2>
      <div class="toolbar">
        ${langs.map(l => `<a class="nav-link ${l===lang?'active':''}" href="/admin/i18n?lang=${l}">${l}</a>`).join('')}
      </div>
      <form method="post" action="/admin/i18n/save" class="form-row">
        <select name="lang">${langs.map(l=>`<option ${l===lang?'selected':''} value="${l}">${l}</option>`).join('')}</select>
        <input name="key" placeholder="key (e.g., no_suitable_question)" style="min-width:260px"/>
        <input name="value" placeholder="value" style="min-width:300px;flex:1"/>
        <button class="btn primary" type="submit">Create</button>
      </form>
      <div class="table-wrap" style="margin-top:12px">
        <table>
          <thead><tr><th style="width:30%">Key</th><th>Value</th></tr></thead>
          <tbody>${rows}</tbody>
        </table>
      </div>
    </div>
  `;
  res.send(adminLayout({ title: `Kleos Admin - I18n (${lang})`, active: 'i18n', body }));
});

router.post('/admin/i18n/save', adminAuthMiddleware, async (req, res) => {
  const schema = z.object({ lang: z.enum(['en','ru','zh']), key: z.string(), value: z.string().default('') });
  const data = schema.parse(req.body);
  const { Translation } = await import('../models/Translation.js');
  await Translation.updateOne({ lang: data.lang, key: data.key }, { $set: { value: data.value } }, { upsert: true });
  res.redirect(`/admin/i18n?lang=${data.lang}`);
});

router.post('/admin/i18n/delete', adminAuthMiddleware, async (req, res) => {
  const schema = z.object({ lang: z.enum(['en','ru','zh']), key: z.string() });
  const data = schema.parse(req.body);
  const { Translation } = await import('../models/Translation.js');
  await Translation.deleteOne({ lang: data.lang, key: data.key });
  res.redirect(`/admin/i18n?lang=${data.lang}`);
});

export default router;


