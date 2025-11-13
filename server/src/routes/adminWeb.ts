import { Router } from 'express';
import cookieParser from 'cookie-parser';
import jwt from 'jsonwebtoken';
import { User } from '../models/User.js';
import { z } from 'zod';
import { Partner } from '../models/Partner.js';
import { Admission } from '../models/Admission.js';
import chatsRoutes from './chats.js';

const router = Router();

// Helpers
function getAdminCreds() {
  return {
    username: process.env.ADMIN_USERNAME || '123',
    password: process.env.ADMIN_PASSWORD || '123'
  };
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
  res.send(`
<!doctype html>
<html><head><meta charset="utf-8"><title>Kleos Admin</title>
<style>
body{font-family:Arial;max-width:560px;margin:40px auto;padding:0 16px;}
label{display:block;margin:8px 0 4px;}
input{width:100%;padding:8px;}
button{margin-top:12px;padding:10px 16px;}
.err{color:#c00;margin:8px 0;}
</style></head><body>
  <h2>Kleos Admin Login</h2>
  ${req.query.err ? `<div class="err">${req.query.err}</div>` : ''}
  <form method="post" action="/admin/login">
    <label>Username</label>
    <input name="username" />
    <label>Password</label>
    <input name="password" type="password" />
    <button type="submit">Sign In</button>
  </form>
</body></html>
  `);
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
          <button type="submit">Save</button>
        </form>
        <form method="post" action="/admin/users/${u._id}/delete" onsubmit="return confirm('Delete this user?')">
          <button type="submit" style="margin-top:6px;color:#a00">Delete</button>
        </form>
      </td>
    </tr>
  `).join('');

  res.send(`
<!doctype html>
<html><head><meta charset="utf-8"><title>Kleos Admin - Users</title>
<style>
body{font-family:Arial;max-width:1000px;margin:40px auto;padding:0 16px;}
table{border-collapse:collapse;width:100%;}
td,th{border:1px solid #ddd;padding:8px;vertical-align:top;}
form {display:flex; gap:8px; align-items:center;}
input,select{padding:6px;}
a{margin-right:12px;}
</style></head><body>
  <h2>Users</h2>
  <div><a href="/admin/logout">Logout</a></div>
  <div style="margin:8px 0;"><a href="/admin/partners">Partners</a> | <a href="/admin/admissions">Admissions</a> | <a href="/admin/chats">Chats</a></div>
  <div style="margin:8px 0;"><a href="/admin/i18n">I18n</a></div>
  <table>
    <thead><tr><th>ID</th><th>Data</th></tr></thead>
    <tbody>${rows}</tbody>
  </table>
</body></html>
  `);
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
        <form method="post" action="/admin/partners/${p._id}">
          <input name="name" value="${(p.name || '').toString().replace(/"/g, '&quot;')}" />
          <input name="description" value="${(p.description || '').toString().replace(/"/g, '&quot;')}" />
          <input name="logoUrl" placeholder="Logo URL" value="${(p.logoUrl || '').toString().replace(/"/g, '&quot;')}" />
          <input name="url" placeholder="Site URL" value="${(p.url || '').toString().replace(/"/g, '&quot;')}" />
          <input name="order" type="number" value="${p.order || 0}" />
          <label><input type="checkbox" name="active" ${p.active ? 'checked' : ''}/> active</label>
          <button type="submit">Save</button>
        </form>
        <form method="post" action="/admin/partners/${p._id}/delete" onsubmit="return confirm('Delete partner?')">
          <button type="submit" style="margin-top:6px;color:#a00">Delete</button>
        </form>
      </td>
    </tr>`).join('');
  res.send(`<!doctype html><html><body style="font-family:Arial;max-width:1000px;margin:40px auto;padding:0 16px;">
  <h2>Partners</h2>
  <div><a href="/admin/users">Users</a> | <a href="/admin/admissions">Admissions</a> | <a href="/admin/chats">Chats</a></div>
  <h3>Add partner</h3>
  <form method="post" action="/admin/partners">
    <input name="name" placeholder="Name"/>
    <input name="description" placeholder="Description"/>
    <input name="logoUrl" placeholder="Logo URL"/>
    <input name="url" placeholder="Site URL"/>
    <input name="order" type="number" value="0"/>
    <label><input type="checkbox" name="active" checked/> active</label>
    <button type="submit">Create</button>
  </form>
  <h3>All</h3>
  <table border="1" cellpadding="6" style="width:100%;border-collapse:collapse;">
    <thead><tr><th>ID</th><th>Data</th></tr></thead>
    <tbody>${items}</tbody>
  </table>
  </body></html>`);
});

router.post('/admin/partners', adminAuthMiddleware, async (req, res) => {
  const schema = z.object({
    name: z.string(),
    description: z.string().optional(),
    logoUrl: z.string().optional(),
    url: z.string().optional(),
    order: z.coerce.number().optional(),
    active: z.string().optional()
  });
  const data = schema.parse(req.body);
  await Partner.create({ ...data, active: !!data.active });
  res.redirect('/admin/partners');
});

router.post('/admin/partners/:id', adminAuthMiddleware, async (req, res) => {
  const schema = z.object({
    name: z.string().optional(),
    description: z.string().optional(),
    logoUrl: z.string().optional(),
    url: z.string().optional(),
    order: z.coerce.number().optional(),
    active: z.string().optional()
  });
  const data = schema.parse(req.body);
  await Partner.updateOne({ _id: req.params.id }, { ...data, active: !!data.active });
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
        <div><b>${a.fullName}</b> (${a.email}, ${a.phone}) — ${a.program}</div>
        <form method="post" action="/admin/admissions/${a._id}">
          <select name="status">
            <option value="new"${a.status==='new'?' selected':''}>new</option>
            <option value="processing"${a.status==='processing'?' selected':''}>processing</option>
            <option value="done"${a.status==='done'?' selected':''}>done</option>
          </select>
          <input name="studentId" placeholder="Student ID" value="${(a as any).studentId || ''}" />
          <button type="submit">Save</button>
        </form>
      </td>
    </tr>`).join('');
  res.send(`<!doctype html><html><body style="font-family:Arial;max-width:1000px;margin:40px auto;padding:0 16px;">
  <h2>Admissions</h2>
  <div><a href="/admin/users">Users</a> | <a href="/admin/partners">Partners</a> | <a href="/admin/chats">Chats</a></div>
  <table border="1" cellpadding="6" style="width:100%;border-collapse:collapse;">
    <thead><tr><th>ID</th><th>Data</th></tr></thead>
    <tbody>${rows}</tbody>
  </table>
  </body></html>`);
});

router.post('/admin/admissions/:id', adminAuthMiddleware, async (req, res) => {
  const schema = z.object({ status: z.string().optional(), studentId: z.string().optional() });
  const data = schema.parse(req.body);
  await Admission.updateOne({ _id: req.params.id }, data);
  res.redirect('/admin/admissions');
});

// Chats simple UI (uses public chats endpoints)
router.get('/admin/chats', adminAuthMiddleware, async (_req, res) => {
  res.send(`<!doctype html><html><body style="font-family:Arial;max-width:1000px;margin:40px auto;padding:0 16px;">
  <h2>Chats</h2>
  <div><a href="/admin/users">Users</a> | <a href="/admin/partners">Partners</a> | <a href="/admin/admissions">Admissions</a></div>
  <script>
    async function loadChats(){
      const r=await fetch('/chats/all',{headers:{'Accept':'application/json'}});
      const data=await r.json();
      const list=document.getElementById('list');
      list.innerHTML=data.map(c=>'<li><a href="/admin/chats/'+c.id+'">'+c.id+' (user '+c.userId+')</a></li>').join('');
    }
    window.addEventListener('load',loadChats);
  </script>
  <ul id="list">Loading...</ul>
  </body></html>`);
});

router.get('/admin/chats/:id', adminAuthMiddleware, async (req, res) => {
  const chatId = req.params.id;
  res.send(`<!doctype html><html><body style="font-family:Arial;max-width:800px;margin:40px auto;padding:0 16px;">
  <div><a href="/admin/chats">&larr; Back</a></div>
  <h3>Chat ${chatId}</h3>
  <div id="msgs" style="border:1px solid #ccc;padding:8px;height:400px;overflow:auto;">Loading...</div>
  <form id="sendForm">
    <input name="text" placeholder="Message" style="width:70%"/>
    <button type="submit">Send</button>
  </form>
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
  </body></html>`);
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
            <button type="submit">Save</button>
            <button formaction="/admin/i18n/delete" formmethod="post" style="color:#a00" onclick="return confirm('Delete?')">Delete</button>
          </div>
        </form>
      </td>
    </tr>
  `).join('');
  res.send(`<!doctype html><html><body style="font-family:Arial;max-width:1000px;margin:40px auto;padding:0 16px;">
    <h2>I18n (Translations)</h2>
    <div>
      ${langs.map(l => `<a href="/admin/i18n?lang=${l}" ${l===lang?'style="font-weight:bold"':''}>${l}</a>`).join(' | ')}
    </div>
    <h3 style="margin-top:12px;">Add</h3>
    <form method="post" action="/admin/i18n/save" style="display:flex; gap:8px; align-items:center;">
      <select name="lang">${langs.map(l=>`<option ${l===lang?'selected':''} value="${l}">${l}</option>`).join('')}</select>
      <input name="key" placeholder="key (e.g., no_suitable_question)" style="width:300px"/>
      <input name="value" placeholder="value" style="width:420px"/>
      <button type="submit">Create</button>
    </form>
    <h3 style="margin-top:12px;">All (${lang})</h3>
    <table border="1" cellpadding="6" style="width:100%;border-collapse:collapse;">
      <thead><tr><th style="width:30%">Key</th><th>Value</th></tr></thead>
      <tbody>${rows}</tbody>
    </table>
  </body></html>`);
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


