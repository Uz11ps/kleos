import { Router } from 'express';
import cookieParser from 'cookie-parser';
import jwt from 'jsonwebtoken';
import { User } from '../models/User.js';
import { z } from 'zod';

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
    role: z.enum(['student','admin']).optional()
  });
  const data = schema.parse(req.body);
  await User.updateOne({ _id: req.params.id }, data);
  res.redirect('/admin/users');
});

router.post('/admin/users/:id/delete', adminAuthMiddleware, async (req, res) => {
  await User.deleteOne({ _id: req.params.id });
  res.redirect('/admin/users');
});

export default router;


