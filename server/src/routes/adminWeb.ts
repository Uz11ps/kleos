import { Router } from 'express';
import cookieParser from 'cookie-parser';
import jwt from 'jsonwebtoken';
import { User } from '../models/User.js';
import { z, ZodError } from 'zod';
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

// File uploads configuration
const uploadLogos = multer({
  storage: multer.diskStorage({
    destination: (_req, _file, cb) => {
      const dir = path.join(process.cwd(), 'uploads', 'logos');
      fs.mkdirSync(dir, { recursive: true });
      cb(null, dir);
    },
    filename: (_req, file, cb) => {
      const name = `${Date.now()}-${Math.random().toString(36).slice(2)}${path.extname(file.originalname || '')}`;
      cb(null, name);
    }
  }),
  limits: { fileSize: 5 * 1024 * 1024 } // 5MB
});

const uploadMedia = multer({
  storage: multer.diskStorage({
    destination: (_req, _file, cb) => {
      const dir = path.join(process.cwd(), 'uploads', 'media');
      fs.mkdirSync(dir, { recursive: true });
      cb(null, dir);
    },
    filename: (_req, file, cb) => {
      const name = `${Date.now()}-${Math.random().toString(36).slice(2)}${path.extname(file.originalname || '')}`;
      cb(null, name);
    }
  }),
  limits: { fileSize: 50 * 1024 * 1024 } // 50MB for videos
});

const uploadImages = multer({
  storage: multer.diskStorage({
    destination: (_req, _file, cb) => {
      const dir = path.join(process.cwd(), 'uploads', 'images');
      fs.mkdirSync(dir, { recursive: true });
      cb(null, dir);
    },
    filename: (_req, file, cb) => {
      const name = `${Date.now()}-${Math.random().toString(36).slice(2)}${path.extname(file.originalname || '')}`;
      cb(null, name);
    }
  }),
  limits: { fileSize: 10 * 1024 * 1024 } // 10MB for images
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
  text: String,
  isReadByAdmin: { type: Boolean, default: false, index: true }
}, { timestamps: true });
const Chat = (mongoose.models.Chat as any) || model('Chat', ChatSchema);
const Message = (mongoose.models.Message as any) || model('Message', MessageSchema);

async function adminLayout(opts: {
  title: string;
  active?: 'users' | 'partners' | 'admissions' | 'programs' | 'chats' | 'i18n' | 'news' | 'gallery' | 'universities' | '';
  body: string;
}) {
  const { title, active = '', body } = opts;
  // Проверяем наличие непрочитанных сообщений от студентов
  let unreadCount = 0;
  try {
    unreadCount = await Message.countDocuments({ senderRole: 'student', isReadByAdmin: false });
  } catch (e) {
    // Игнорируем ошибки при подсчете непрочитанных сообщений
  }
  const navLink = (href: string, label: string, key: typeof active, badge?: number, icon?: string) => {
    const badgeHtml = badge && badge > 0 ? `<span style="background:#ef4444;color:#fff;border-radius:10px;padding:2px 6px;font-size:11px;margin-left:auto;font-weight:600">${badge}</span>` : '';
    const iconHtml = icon ? `<span style="font-size:18px">${icon}</span>` : '';
    return `<a class="nav-link ${active === key ? 'active' : ''}" href="${href}">${iconHtml}<span>${label}</span>${badgeHtml}</a>`;
  };
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
        --sidebar-width:280px;
      }
      *{box-sizing:border-box}
      html,body{margin:0;padding:0;background:var(--bg);color:var(--text);font-family:system-ui,-apple-system,Segoe UI,Roboto,Ubuntu,Helvetica,Arial,sans-serif;height:100%}
      a{color:var(--accent);text-decoration:none}
      a:hover{text-decoration:underline}
      
      .layout-wrapper{display:flex;min-height:100vh}
      
      .sidebar{
        width:var(--sidebar-width);
        background:linear-gradient(180deg,#0f1631 0%,#0b1021 100%);
        border-right:1px solid var(--border);
        position:fixed;left:0;top:0;bottom:0;
        display:flex;flex-direction:column;
        z-index:100;
        overflow-y:auto;
      }
      
      .brand{
        display:flex;align-items:center;gap:12px;
        font-weight:700;font-size:20px;
        padding:24px 20px;border-bottom:1px solid var(--border);
        background:rgba(37,99,235,0.08);
      }
      .brand .dot{
        width:12px;height:12px;border-radius:50%;
        background:linear-gradient(135deg,var(--accent),var(--accent-2));
        box-shadow:0 0 8px rgba(37,99,235,0.5);
      }
      
      .nav{
        display:flex;flex-direction:column;gap:4px;padding:16px 12px;flex:1;
      }
      .nav-link{
        padding:12px 16px;border-radius:10px;color:var(--text);
        display:flex;align-items:center;gap:12px;
        transition:all 0.2s ease;
        border:1px solid transparent;
        font-size:14px;
        position:relative;
      }
      .nav-link > span:first-of-type{flex-shrink:0;width:24px;text-align:center}
      .nav-link > span:nth-of-type(2){flex:1}
      .nav-link > span:last-of-type{margin-left:auto}
      .nav-link:hover{
        background:rgba(37,99,235,0.1);
        border-color:var(--border);
        transform:translateX(4px);
      }
      .nav-link.active{
        border-color:var(--accent);
        background:linear-gradient(90deg,rgba(37,99,235,0.2),rgba(37,99,235,0.1));
        color:#60a5fa;
        font-weight:500;
        box-shadow:0 2px 8px rgba(37,99,235,0.15);
      }
      
      .logout-section{
        padding:16px 12px;border-top:1px solid var(--border);
        margin-top:auto;
      }
      .logout-link{
        display:flex;align-items:center;gap:10px;
        padding:12px 16px;border-radius:10px;
        color:#fecaca;border:1px solid rgba(239,68,68,0.3);
        background:rgba(239,68,68,0.1);
        transition:all 0.2s ease;
        font-size:14px;
      }
      .logout-link:hover{
        background:rgba(239,68,68,0.2);
        border-color:var(--danger);
        transform:translateX(4px);
      }

      .main-content{
        margin-left:var(--sidebar-width);
        flex:1;padding:24px;max-width:calc(100vw - var(--sidebar-width));
      }
      .container{max-width:1400px;margin:0 auto}
      
      .page{padding:0}
      .card{
        background:var(--card);border:1px solid var(--border);
        border-radius:16px;padding:24px;margin-bottom:20px;
        box-shadow:0 4px 12px rgba(0,0,0,0.2);
      }
      h1,h2,h3{margin:0 0 16px 0;font-weight:600}
      h1{font-size:28px}
      h2{font-size:24px}
      h3{font-size:20px}
      .muted{color:var(--muted);font-size:14px}

      .toolbar{display:flex;flex-wrap:wrap;gap:10px;align-items:center;margin:16px 0}
      .btn{
        display:inline-block;padding:12px 20px;border-radius:10px;
        border:1px solid var(--border);background:#11183a;
        color:var(--text);cursor:pointer;transition:all 0.2s ease;
        font-size:14px;font-weight:500;
      }
      .btn:hover{background:#152044;transform:translateY(-1px);box-shadow:0 4px 8px rgba(0,0,0,0.2)}
      .btn.primary{
        background:linear-gradient(135deg,var(--accent),var(--accent-2));
        border:none;color:#fff;box-shadow:0 4px 12px rgba(37,99,235,0.3);
      }
      .btn.primary:hover{box-shadow:0 6px 16px rgba(37,99,235,0.4);transform:translateY(-2px)}
      .btn.danger{
        background:rgba(239,68,68,0.15);
        border:1px solid rgba(239,68,68,0.5);color:#fecaca;
      }
      .btn.danger:hover{background:rgba(239,68,68,0.25)}

      .grid{display:grid;gap:16px}
      @media (min-width: 900px){
        .grid.cols-2{grid-template-columns: 1fr 1fr}
      }

      .form-row{display:flex;flex-wrap:wrap;gap:10px;align-items:flex-end}
      
      .input-group{
        display:flex;flex-direction:column;gap:6px;margin-bottom:16px;
      }
      
      .input-group label{
        display:block;
        font-size:13px;
        font-weight:500;
        color:var(--text);
        margin-bottom:4px;
      }
      
      .input-group label.required::after{
        content:" *";
        color:var(--danger);
      }
      
      input,select,textarea{
        background:#0c1330;
        border:2px solid var(--border);
        color:var(--text);
        padding:14px 18px;
        border-radius:12px;
        outline:none;
        min-width:0;
        font-size:15px;
        transition:all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
        box-sizing:border-box;
        vertical-align:top;
        width:100%;
      }
      
      input:focus,select:focus,textarea:focus{
        border-color:var(--accent);
        box-shadow:0 0 0 4px rgba(37,99,235,0.15),
                   0 4px 12px rgba(37,99,235,0.2);
        outline:none;
        background:#0d1432;
        transform:translateY(-1px);
      }
      
      input:hover:not(:focus),select:hover:not(:focus),textarea:hover:not(:focus){
        border-color:rgba(37,99,235,0.5);
        background:#0d1431;
      }
      
      input::placeholder,textarea::placeholder{
        color:var(--muted);
        opacity:0.7;
        transition:opacity 0.3s ease;
      }
      
      input:focus::placeholder,textarea:focus::placeholder{
        opacity:0.5;
      }
      
      input:disabled{
        opacity:0.6;
        cursor:not-allowed;
        background:#0a1028;
      }
      
      textarea{resize:vertical;min-height:120px;font-family:inherit}
      
      select{
        cursor:pointer;
        background-image:url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 12 12'%3E%3Cpath fill='%2394a3b8' d='M6 9L1 4h10z'/%3E%3C/svg%3E");
        background-repeat:no-repeat;
        background-position:right 14px center;
        padding-right:40px;
        appearance:none;
      }
      
      .form-row .input-group{
        flex:1;
        min-width:200px;
        margin-bottom:0;
      }
      
      form:has(input:focus) .input-group label,
      form:has(select:focus) .input-group label,
      form:has(textarea:focus) .input-group label{
        color:var(--accent-2);
      }

      .table-wrap{
        overflow:auto;border:1px solid var(--border);
        border-radius:12px;margin-top:16px;
      }
      table{
        border-collapse:collapse;min-width:800px;width:100%;
      }
      th,td{
        border-bottom:1px solid var(--border);
        text-align:left;padding:14px 16px;vertical-align:top;
        font-size:14px;
      }
      thead th{
        position:sticky;top:0;background:#0c1330;
        font-weight:600;color:var(--accent-2);
        border-bottom:2px solid var(--accent);
      }
      tbody tr:hover{background:rgba(37,99,235,0.05)}

      .stack{display:none}
      
      @media (max-width: 1024px){
        .sidebar{width:240px}
        .main-content{margin-left:240px;max-width:calc(100vw - 240px)}
      }
      
      @media (max-width: 768px){
        .sidebar{transform:translateX(-100%);transition:transform 0.3s ease}
        .sidebar.open{transform:translateX(0)}
        .main-content{margin-left:0;max-width:100vw;padding:16px}
        .mobile-menu-btn{
          position:fixed;top:16px;left:16px;z-index:101;
          background:var(--card);border:1px solid var(--border);
          padding:10px;border-radius:8px;cursor:pointer;
          display:block;
        }
      }
      
      @media (min-width: 769px){
        .mobile-menu-btn{display:none}
      }
    </style>
  </head>
  <body>
    <div class="layout-wrapper">
      <button class="mobile-menu-btn" onclick="document.querySelector('.sidebar').classList.toggle('open')">☰</button>
      <aside class="sidebar">
        <div class="brand">
          <span class="dot"></span>
          <span>Kleos Admin</span>
        </div>
        <nav class="nav">
          ${navLink('/admin/users','Users','users', undefined, '👥')}
          ${navLink('/admin/partners','Partners','partners', undefined, '🤝')}
          ${navLink('/admin/admissions','Admissions','admissions', undefined, '📝')}
          ${navLink('/admin/programs','Programs','programs', undefined, '🎓')}
          ${navLink('/admin/chats','Chats','chats', unreadCount, '💬')}
          ${navLink('/admin/i18n','I18n','i18n', undefined, '🌐')}
          ${navLink('/admin/news','News','news', undefined, '📰')}
          ${navLink('/admin/gallery','Gallery','gallery', undefined, '🖼️')}
          ${navLink('/admin/universities','Universities','universities', undefined, '🏛️')}
        </nav>
        <div class="logout-section">
          <a class="logout-link" href="/admin/logout">
            <span>🚪</span>
            <span>Logout</span>
          </a>
        </div>
      </aside>
      <main class="main-content">
        <div class="container page">
          ${body}
        </div>
                  </main>
                </div>
                <script>
                  function handleUniversitySelect(select) {
                    const universities = JSON.parse(select.getAttribute('data-universities') || '[]');
                    const selectedId = select.value;
                    const universityInfo = document.getElementById('universityInfo');
                    const universityDetails = document.getElementById('universityDetails');
                    const programFields = document.getElementById('programFields');
                    
                    if (selectedId) {
                      const university = universities.find(u => u.id === selectedId);
                      if (university) {
                        universityDetails.innerHTML = '<div style="font-weight:600;font-size:18px;color:var(--accent);margin-bottom:8px;">' + university.name + '</div><div style="color:var(--muted);font-size:14px;">📍 ' + (university.city ? university.city + ', ' : '') + university.country + '</div>';
                        universityInfo.style.display = 'block';
                        programFields.style.display = 'block';
                        programFields.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
                      }
                    } else {
                      universityInfo.style.display = 'none';
                      programFields.style.display = 'none';
                    }
                  }
                </script>
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

// Helper для отправки ответов админ-панели с отключенным кэшированием
function sendAdminResponse(res: any, html: string) {
  res.removeHeader('ETag');
  res.set({
    'Cache-Control': 'no-store, no-cache, must-revalidate, private',
    'Pragma': 'no-cache',
    'Expires': '0'
  });
  res.send(html);
}

router.use(cookieParser());

// Redirect root to admin
router.get('/', (_req, res) => {
  res.redirect('/admin');
});

// Login form
router.get('/admin', async (req, res) => {
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
  sendAdminResponse(res, await adminLayout({ title: 'Kleos Admin - Login', active: '', body }));
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
  const rows = users.map(u => {
    const studentId = (u as any).studentId || '';
    const displayId = studentId || u._id.toString().slice(-6);
    return `
    <tr>
      <td style="vertical-align:top;padding-top:20px;">
        <div style="font-weight:600;color:var(--accent);font-size:18px;">ID: ${displayId}</div>
        <div style="font-size:12px;color:var(--muted);margin-top:4px;">${u._id}</div>
      </td>
      <td>
        <form method="post" action="/admin/users/${u._id}" style="display:grid;grid-template-columns:repeat(auto-fit,minmax(250px,1fr));gap:16px;">
          <div class="input-group">
            <label class="required">Полное имя</label>
            <input name="fullName" value="${(u.fullName || '').toString().replace(/"/g, '&quot;')}" required />
          </div>
          <div class="input-group">
            <label>Email</label>
            <input name="email" value="${(u.email || '').toString().replace(/"/g, '&quot;')}" disabled />
          </div>
          <div class="input-group">
            <label class="required">Роль</label>
            <select name="role">
              <option value="user"${u.role === 'user' ? ' selected' : ''}>Пользователь</option>
              <option value="student"${u.role === 'student' ? ' selected' : ''}>Студент</option>
              <option value="admin"${u.role === 'admin' ? ' selected' : ''}>Администратор</option>
            </select>
          </div>
          <div class="input-group">
            <label>ID студента</label>
            <input name="studentId" placeholder="Введите ID студента" value="${(u as any).studentId || ''}" />
          </div>
          <div class="input-group">
            <label>Телефон</label>
            <input name="phone" placeholder="+7 (999) 999-99-99" value="${(u as any).phone || ''}" />
          </div>
          <div class="input-group">
            <label>Курс</label>
            <input name="course" placeholder="Введите курс" value="${(u as any).course || ''}" />
          </div>
          <div class="input-group">
            <label>Специальность</label>
            <input name="speciality" placeholder="Введите специальность" value="${(u as any).speciality || ''}" />
          </div>
          <div class="input-group">
            <label>Статус</label>
            <input name="status" placeholder="Введите статус" value="${(u as any).status || ''}" />
          </div>
          <div class="input-group">
            <label>Университет</label>
            <input name="university" placeholder="Введите университет" value="${(u as any).university || ''}" />
          </div>
          <div class="input-group">
            <label>Оплата</label>
            <input name="payment" placeholder="Информация об оплате" value="${(u as any).payment || ''}" />
          </div>
          <div class="input-group">
            <label>Штрафы</label>
            <input name="penalties" placeholder="Информация о штрафах" value="${(u as any).penalties || ''}" />
          </div>
          <div class="input-group" style="grid-column:1/-1;">
            <label>Заметки</label>
            <textarea name="notes" placeholder="Дополнительные заметки о пользователе">${(u as any).notes || ''}</textarea>
          </div>
          <div class="input-group" style="grid-column:1/-1;">
            <label style="display:flex;align-items:center;gap:8px;cursor:pointer;">
              <input type="checkbox" name="emailVerified" ${u.emailVerified ? 'checked' : ''} style="width:auto;margin:0;" />
              <span>Email подтвержден</span>
            </label>
          </div>
          <div style="grid-column:1/-1;display:flex;gap:10px;margin-top:8px;">
            <button class="btn primary" type="submit">💾 Сохранить изменения</button>
          </div>
        </form>
        <form method="post" action="/admin/users/${u._id}/delete" onsubmit="return confirm(&quot;Вы уверены, что хотите удалить этого пользователя?&quot;);" style="margin-top:8px;">
          <button class="btn danger" type="submit">🗑️ Удалить</button>
        </form>
      </td>
    </tr>
  `;
  }).join('');

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
  sendAdminResponse(res, await adminLayout({ title: 'Kleos Admin - Users', active: 'users', body }));
});

// Update user
router.post('/admin/users/:id', adminAuthMiddleware, async (req: any, res: any) => {
  const schema = z.object({
    fullName: z.string().optional(),
    role: z.enum(['user','student','admin']).optional(),
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
      <td style="vertical-align:top;padding-top:20px;">
        <div style="font-weight:600;color:var(--accent);font-size:16px;">${p._id}</div>
      </td>
      <td>
        <form method="post" action="/admin/partners/${p._id}" enctype="multipart/form-data" style="display:grid;grid-template-columns:repeat(auto-fit,minmax(250px,1fr));gap:16px;">
          <div class="input-group">
            <label class="required">Название партнера</label>
            <input name="name" value="${(p.name || '').toString().replace(/"/g, '&quot;')}" required />
          </div>
          <div class="input-group">
            <label>Описание</label>
            <textarea name="description" placeholder="Описание партнера">${(p.description || '').toString().replace(/</g,'&lt;')}</textarea>
          </div>
          <div class="input-group">
            <label>URL логотипа</label>
            <input name="logoUrl" placeholder="https://example.com/logo.png" value="${(p.logoUrl || '').toString().replace(/"/g, '&quot;')}" />
          </div>
          <div class="input-group">
            <label>Загрузить логотип</label>
            <input type="file" name="logoFile" accept="image/*" style="padding:10px;" />
          </div>
          <div class="input-group">
            <label>Сайт партнера</label>
            <input name="url" placeholder="https://example.com" value="${(p.url || '').toString().replace(/"/g, '&quot;')}" />
          </div>
          <div class="input-group">
            <label>Порядок сортировки</label>
            <input name="order" type="number" value="${p.order || 0}" />
          </div>
          <div class="input-group" style="grid-column:1/-1;">
            <label style="display:flex;align-items:center;gap:8px;cursor:pointer;">
              <input type="checkbox" name="active" ${p.active ? 'checked' : ''} style="width:auto;margin:0;" />
              <span>Активен</span>
            </label>
          </div>
          <div style="grid-column:1/-1;display:flex;gap:10px;margin-top:8px;">
            <button class="btn primary" type="submit">💾 Сохранить</button>
            <button class="btn danger" type="button" onclick="if(confirm('Удалить партнера?')){const f=document.createElement('form');f.method='post';f.action='/admin/partners/${p._id}/delete';document.body.appendChild(f);f.submit();}">🗑️ Удалить</button>
          </div>
        </form>
      </td>
    </tr>`).join('');
  const body = `
    <div class="grid cols-2">
      <div class="card">
        <h2>➕ Добавить партнера</h2>
        <form method="post" action="/admin/partners" enctype="multipart/form-data" style="margin-top:16px;">
          <div class="input-group">
            <label class="required">Название партнера</label>
            <input name="name" placeholder="Введите название" required />
          </div>
          <div class="input-group">
            <label>Описание</label>
            <textarea name="description" placeholder="Описание партнера"></textarea>
          </div>
          <div class="input-group">
            <label>URL логотипа</label>
            <input name="logoUrl" placeholder="https://example.com/logo.png" />
          </div>
          <div class="input-group">
            <label>Загрузить логотип</label>
            <input type="file" name="logoFile" accept="image/*" style="padding:10px;" />
          </div>
          <div class="input-group">
            <label>Сайт партнера</label>
            <input name="url" placeholder="https://example.com" />
          </div>
          <div class="input-group">
            <label>Порядок сортировки</label>
            <input name="order" type="number" value="0" />
          </div>
          <div class="input-group">
            <label style="display:flex;align-items:center;gap:8px;cursor:pointer;">
              <input type="checkbox" name="active" checked style="width:auto;margin:0;" />
              <span>Активен</span>
            </label>
          </div>
          <button class="btn primary" type="submit" style="margin-top:8px;">✨ Создать партнера</button>
        </form>
      </div>
      <div class="card">
        <h2>🤝 Все партнеры</h2>
        <div class="table-wrap" style="margin-top:16px">
          <table>
            <thead><tr><th style="width:200px">ID</th><th>Данные</th></tr></thead>
            <tbody>${items || '<tr><td colspan="2" class="muted" style="text-align:center;padding:40px;">Нет партнеров</td></tr>'}</tbody>
          </table>
        </div>
      </div>
    </div>
  `;
  sendAdminResponse(res, await adminLayout({ title: 'Kleos Admin - Partners', active: 'partners', body }));
});

router.post('/admin/partners', adminAuthMiddleware, uploadLogos.single('logoFile'), async (req: any, res) => {
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

router.post('/admin/partners/:id', adminAuthMiddleware, uploadLogos.single('logoFile'), async (req: any, res) => {
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
  // По умолчанию показываем все заявки
  const filter = showAll ? {} : {};
  const list = await Admission.find(filter).sort({ createdAt: -1 }).lean();
  const rows = list.map(a => {
    const status = a.status || 'new';
    const isProcessed = status === 'done' || status === 'rejected';
    const statusColor = status === 'done' ? '#10b981' : status === 'rejected' ? '#ef4444' : '#3b82f6';
    const statusText = status === 'done' ? 'Принята' : status === 'rejected' ? 'Отклонена' : status === 'processing' ? 'В обработке' : 'Новая';
    return `
    <tr>
      <td style="vertical-align:top;padding-top:20px;">
        <div style="font-weight:600;color:var(--accent);font-size:16px;">${a._id}</div>
        <div style="font-size:12px;color:var(--muted);margin-top:4px;">${new Date((a as any).createdAt).toLocaleString('ru-RU')}</div>
      </td>
      <td>
        <div style="padding:16px;background:var(--card);border:1px solid var(--border);border-radius:12px;margin-bottom:12px;">
          <div style="display:grid;grid-template-columns:repeat(auto-fit,minmax(200px,1fr));gap:12px;margin-bottom:12px;">
            <div>
              <div style="font-size:12px;color:var(--muted);margin-bottom:4px;">ФИО</div>
              <div style="font-weight:600;font-size:16px;">${((a as any).lastName || '')} ${((a as any).firstName || '')} ${((a as any).patronymic || '')}</div>
            </div>
            <div>
              <div style="font-size:12px;color:var(--muted);margin-bottom:4px;">Email</div>
              <div>${a.email || '-'}</div>
            </div>
            <div>
              <div style="font-size:12px;color:var(--muted);margin-bottom:4px;">Телефон</div>
              <div>${a.phone || '-'}</div>
            </div>
            <div>
              <div style="font-size:12px;color:var(--muted);margin-bottom:4px;">Программа</div>
              <div style="font-weight:500;">${a.program || '-'}</div>
            </div>
          </div>
          ${a.comment ? `<div style="margin-top:12px;padding:12px;background:var(--bg);border-radius:8px;">
            <div style="font-size:12px;color:var(--muted);margin-bottom:4px;">Комментарий</div>
            <div>${(a.comment as string).toString().replace(/</g,'&lt;').replace(/\n/g,'<br>')}</div>
          </div>` : ''}
          <div style="margin-top:12px;display:flex;gap:12px;align-items:center;flex-wrap:wrap;">
            <div style="padding:6px 12px;background:rgba(${status === 'done' ? '16,185,129' : status === 'rejected' ? '239,68,68' : '59,130,246'},0.15);border-radius:8px;border:1px solid ${statusColor};">
              <span style="color:${statusColor};font-weight:600;">${statusText}</span>
            </div>
            ${(a as any).studentId ? `<div style="font-size:12px;color:var(--muted);">ID студента: <span style="color:var(--text);font-weight:500;">${(a as any).studentId}</span></div>` : ''}
          </div>
        </div>
        <div style="display:flex;gap:10px;flex-wrap:wrap;">
          <a class="btn" href="/admin/admissions/${a._id}/view">📄 Подробнее</a>
          ${!isProcessed ? `
          <form method="post" action="/admin/admissions/${a._id}/accept" style="display:flex;gap:8px;flex:1;min-width:300px;">
            <input name="studentId" placeholder="ID студента (опционально)" value="${(a as any).studentId || ''}" style="flex:1;min-width:150px;" />
            <button class="btn primary" type="submit">✅ Принять</button>
          </form>
          <form method="post" action="/admin/admissions/${a._id}/reject" onsubmit="return confirm('Отклонить заявку?')" style="margin-left:0;">
            <button class="btn danger" type="submit">❌ Отклонить</button>
          </form>
          ` : ''}
        </div>
      </td>
    </tr>`;
  }).join('');
  const body = `
    <div class="card">
      <div style="display:flex;align-items:center;justify-content:space-between;gap:10px;margin-bottom:12px">
        <h2 style="margin:0">Admissions</h2>
        <div>
          <a class="btn ${showAll ? 'primary' : ''}" href="/admin/admissions?all=true">Все</a>
          <a class="btn ${showAll ? '' : 'primary'}" href="/admin/admissions" style="margin-left:8px">Новые</a>
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
  sendAdminResponse(res, await adminLayout({ title: 'Kleos Admin - Admissions', active: 'admissions', body }));
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
      
      // Отправка push-уведомления пользователю о принятии заявки
      try {
        const { sendPushToUser } = await import('../utils/pushNotifications.js');
        await sendPushToUser(
          uid.toString(),
          'Заявка принята',
          'Ваша заявка на поступление была принята. Теперь вы студент!',
          { type: 'admission_accepted', admissionId: id }
        );
      } catch (e: any) {
        console.error('Error sending push notification for admission:', e);
        // Не прерываем процесс из-за ошибки отправки уведомления
      }
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
  sendAdminResponse(res, await adminLayout({ title: `Kleos Admin - Admission ${id}`, active: 'admissions', body }));
});

router.post('/admin/admissions/:id', adminAuthMiddleware, async (req, res) => {
  const schema = z.object({ status: z.string().optional(), studentId: z.string().optional() });
  const data = schema.parse(req.body);
  await Admission.updateOne({ _id: req.params.id }, data);
  res.redirect('/admin/admissions');
});

// Chats simple UI (uses public chats endpoints)
router.get('/admin/chats', adminAuthMiddleware, async (_req, res) => {
  const chats = await Chat.find().populate('userId', 'studentId fullName email').sort({ lastMessageAt: -1 }).limit(200).lean();
  // Получаем все непрочитанные сообщения одним запросом
  const chatIds = chats.map(c => (c as any)._id);
  let unreadCounts: Record<string, number> = {};
  try {
    const unreadMessages = await Message.find({ 
      chatId: { $in: chatIds }, 
      senderRole: 'student', 
      isReadByAdmin: false 
    }).lean();
    for (const msg of unreadMessages) {
      const chatId = (msg as any).chatId?.toString();
      if (chatId) {
        unreadCounts[chatId] = (unreadCounts[chatId] || 0) + 1;
      }
    }
  } catch (e) {
    // Игнорируем ошибки
  }
  const items = chats.map((c) => {
    const chatIdStr = (c as any)._id.toString();
    const user = (c as any).userId;
    const studentId = user?.studentId || '';
    const displayId = studentId || (user?._id ? user._id.toString().slice(-6) : chatIdStr.slice(-6));
    const userName = user?.fullName || 'Гость';
    const userEmail = user?.email || '';
    const unreadCount = unreadCounts[chatIdStr] || 0;
    const badgeHtml = unreadCount > 0 ? `<span style="background:#ef4444;color:#fff;border-radius:12px;padding:4px 8px;font-size:12px;font-weight:600;margin-left:8px;">${unreadCount}</span>` : '';
    return `<div class="chat-item" style="padding:12px;border:1px solid var(--border);border-radius:10px;margin-bottom:8px;background:var(--card);transition:all 0.2s ease;">
      <div style="display:flex;justify-content:space-between;align-items:center;">
        <div style="flex:1;">
          <div style="display:flex;align-items:center;gap:8px;">
            <div style="font-weight:600;color:var(--accent);font-size:16px;">ID: ${displayId}</div>
            ${badgeHtml}
          </div>
          <div style="color:var(--text);margin-top:4px;">${userName}${userEmail ? ` (${userEmail})` : ''}</div>
        </div>
        <a href="/admin/chats/${chatIdStr}" class="btn primary" style="text-decoration:none;">Открыть</a>
      </div>
    </div>`;
  });
  const body = `
    <div class="card">
      <h2>💬 Чаты поддержки</h2>
      <div style="margin-top:16px">${items.length > 0 ? items.join('') : '<div class="muted" style="padding:20px;text-align:center;">Нет активных чатов</div>'}</div>
    </div>
  `;
  sendAdminResponse(res, await adminLayout({ title: 'Kleos Admin - Chats', active: 'chats', body }));
});

router.get('/admin/chats/:id', adminAuthMiddleware, async (req, res) => {
  const chatId = req.params.id;
  const chat = await Chat.findById(chatId).populate('userId', 'studentId fullName email').lean();
  if (!chat) return res.status(404).send('Chat not found');
  const user = (chat as any).userId;
  const studentId = user?.studentId || '';
  const displayId = studentId || (user?._id ? user._id.toString().slice(-6) : chatId.slice(-6));
  const userName = user?.fullName || 'Гость';
  const userEmail = user?.email || '';
  
  // Помечаем все сообщения от студентов в этом чате как прочитанные
  await Message.updateMany({ chatId, senderRole: 'student' }, { isReadByAdmin: true });
  const msgs = await Message.find({ chatId }).sort({ createdAt: 1 }).lean();
  const list = msgs.map(m => {
    const senderName = m.senderRole === 'admin' ? 'Администратор' : m.senderRole === 'student' ? userName : 'Система';
    const senderColor = m.senderRole === 'admin' ? 'var(--accent)' : m.senderRole === 'student' ? 'var(--text)' : 'var(--muted)';
    return `<div style="padding:12px;margin-bottom:8px;border-left:3px solid ${senderColor};background:var(--card);border-radius:6px;">
      <div style="font-weight:600;color:${senderColor};margin-bottom:4px;">${senderName}</div>
      <div style="color:var(--text);white-space:pre-wrap;">${String(m.text || '').replace(/</g,'&lt;').replace(/\n/g,'<br>')}</div>
      <div style="font-size:12px;color:var(--muted);margin-top:6px;">${new Date((m as any).createdAt).toLocaleString('ru-RU')}</div>
    </div>`;
  }).join('');
  const body = `
    <div class="card">
      <div style="margin-bottom:16px;"><a href="/admin/chats" class="btn">&larr; Назад к списку</a></div>
      <div style="padding:16px;background:var(--card);border:1px solid var(--border);border-radius:12px;margin-bottom:20px;">
        <div style="font-size:20px;font-weight:600;color:var(--accent);margin-bottom:8px;">ID пользователя: ${displayId}</div>
        <div style="color:var(--text);margin-bottom:4px;">Имя: ${userName}</div>
        ${userEmail ? `<div style="color:var(--muted);">Email: ${userEmail}</div>` : ''}
      </div>
      <h3 style="margin-bottom:16px;">Сообщения</h3>
      <div class="card" style="height:500px;overflow-y:auto;padding:16px;background:var(--bg);">${list || '<div class="muted" style="text-align:center;padding:40px;">Пока нет сообщений</div>'}</div>
      <form method="post" action="/admin/chats/${chatId}/send" style="margin-top:16px;">
        <div style="display:flex;gap:10px;">
          <input name="text" placeholder="Введите сообщение..." style="flex:1;min-width:200px;" required/>
          <button class="btn primary" type="submit">Отправить</button>
        </div>
      </form>
    </div>
    <script>
      // Автопрокрутка вниз при загрузке
      window.addEventListener('load', function() {
        const messagesDiv = document.querySelector('.card[style*="height:500px"]');
        if (messagesDiv) messagesDiv.scrollTop = messagesDiv.scrollHeight;
      });
    </script>
  `;
  sendAdminResponse(res, await adminLayout({ title: `Kleos Admin - Chat (ID: ${displayId})`, active: 'chats', body }));
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
  sendAdminResponse(res, await adminLayout({ title: 'Kleos Admin - I18n', active: 'i18n', body }));
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
        <form method="post" action="/admin/news/${n._id}" enctype="multipart/form-data">
          <input name="title" value="${(n.title || '').toString().replace(/"/g,'&quot;')}" />
          ${n.imageUrl ? `<div style="margin:8px 0;"><img src="${n.imageUrl}" style="max-width:200px;max-height:150px;border-radius:8px;" alt="Current image"/></div>` : ''}
          <input type="file" name="imageFile" accept="image/*" />
          ${n.imageUrl ? `<div style="font-size:12px;color:var(--muted);margin-top:4px;">Текущее изображение: ${n.imageUrl}</div>` : ''}
          <input name="publishedAt" type="datetime-local" value="${n.publishedAt ? new Date(n.publishedAt).toISOString().slice(0,16) : ''}" />
          <input name="order" type="number" value="${n.order || 0}" />
          <label><input type="checkbox" name="active" ${n.active ? 'checked' : ''}/> active</label>
          <textarea name="content" rows="3" placeholder="Content" style="width:100%">${(n.content || '').toString().replace(/</g,'&lt;')}</textarea>
          <div style="margin-top:6px;display:flex;gap:8px;flex-wrap:wrap;">
            <button class="btn primary" type="submit">Save</button>
            <button class="btn danger" formaction="/admin/news/${n._id}/delete" formmethod="post" onclick="return confirm('Delete?')">Delete</button>
            ${n.active ? `<button class="btn" type="button" onclick="sendNotification('${n._id}', '${(n.title || '').toString().replace(/'/g, "\\'")}')" style="background:var(--accent-2);color:#fff;border:none;">📢 Отправить уведомление</button>` : ''}
          </div>
        </form>
      </td>
    </tr>
  `).join('');
  const body = `
    <div class="card">
      <h2>News</h2>
      <form method="post" action="/admin/news/create" enctype="multipart/form-data" class="form-row">
        <input name="title" placeholder="Title" style="min-width:260px" required/>
        <input type="file" name="imageFile" accept="image/*"/>
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
    <script>
      async function sendNotification(newsId, title) {
        if (!confirm('Отправить push-уведомление о новости "' + title + '" всем пользователям?')) {
          return;
        }
        try {
          const response = await fetch('/admin/news/' + newsId + '/send-notification', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' }
          });
          const result = await response.json();
          if (result.ok) {
            const message = 'Уведомление отправлено успешно!\\n' +
              'Отправлено: ' + result.count + '\\n' +
              'Всего пользователей с токенами: ' + (result.totalUsersWithTokens || 0);
            alert(message);
            if (result.count === 0 && result.totalUsersWithTokens === 0) {
              alert('Внимание: У пользователей нет сохраненных FCM токенов.\\n' +
                'Пользователи должны войти в приложение, чтобы токен был отправлен на сервер.');
            }
          } else {
            alert('Ошибка отправки: ' + (result.error || 'unknown'));
          }
        } catch (e) {
          alert('Ошибка: ' + e.message);
        }
      }
    </script>
  `;
  sendAdminResponse(res, await adminLayout({ title: 'Kleos Admin - News', active: 'news', body }));
});

// Programs UI
router.get('/admin/programs', adminAuthMiddleware, async (req, res) => {
  const { Program } = await import('../models/Program.js');
  const { University } = await import('../models/University.js');
  const q = String(req.query.q || '').trim();
  const filter: any = {};
  if (q) filter.$or = [{ title: { $regex: q, $options: 'i' } }, { university: { $regex: q, $options: 'i' } }];
  const list = await Program.find(filter).sort({ order: 1, createdAt: -1 }).lean();
  const universities = await University.find({ active: true }).sort({ name: 1 }).lean();
  const universityOptions = universities.map(u => `<option value="${u._id}">${u.name}</option>`).join('');
  const rows = list.map(p => {
    const currentUnivId = (p as any).universityId?.toString() || '';
    const levelText = p.level === 'bachelor' ? 'Бакалавриат' : p.level === 'master' ? 'Магистратура' : p.level === 'phd' ? 'Аспирантура' : p.level === 'foundation' ? 'Подготовительный' : 'Другое';
    const langText = p.language === 'ru' ? 'Русский' : p.language === 'en' ? 'Английский' : 'Китайский';
    return `
    <tr>
      <td style="vertical-align:top;padding-top:20px;">
        <div style="font-weight:600;color:var(--accent);font-size:16px;">${p._id}</div>
        <div style="font-size:12px;color:var(--muted);margin-top:4px;">${p.active ? '<span style="color:#10b981;">✓ Активна</span>' : '<span style="color:var(--muted);">✗ Неактивна</span>'}</div>
      </td>
      <td>
        <form method="post" action="/admin/programs/${p._id}" enctype="multipart/form-data" style="display:grid;grid-template-columns:repeat(auto-fit,minmax(250px,1fr));gap:16px;">
          <div class="input-group">
            <label class="required">Название программы</label>
            <input name="title" value="${(p.title || '').toString().replace(/"/g,'&quot;')}" placeholder="Введите название" required />
          </div>
          <div class="input-group">
            <label class="required">Slug (URL)</label>
            <input name="slug" value="${(p.slug || '').toString().replace(/"/g,'&quot;')}" placeholder="program-slug" required />
          </div>
          <div class="input-group">
            <label class="required">Язык</label>
            <select name="language">
              ${['ru','en','zh'].map(l=>`<option value="${l}" ${p.language===l?'selected':''}>${l === 'ru' ? 'Русский' : l === 'en' ? 'Английский' : 'Китайский'}</option>`).join('')}
            </select>
          </div>
          <div class="input-group">
            <label class="required">Уровень</label>
            <select name="level">
              ${['bachelor','master','phd','foundation','other'].map(l=>`<option value="${l}" ${p.level===l?'selected':''}>${l === 'bachelor' ? 'Бакалавриат' : l === 'master' ? 'Магистратура' : l === 'phd' ? 'Аспирантура' : l === 'foundation' ? 'Подготовительный' : 'Другое'}</option>`).join('')}
            </select>
          </div>
          <div class="input-group">
            <label class="required">Университет</label>
            <select name="universityId" required>
              <option value="">-- Выберите университет --</option>
              ${universities.map(u => `<option value="${u._id}" ${currentUnivId === u._id.toString() ? 'selected' : ''}>${u.name}</option>`).join('')}
            </select>
          </div>
          <div class="input-group">
            <label>Университет (legacy)</label>
            <input name="university" value="${(p.university||'').toString().replace(/"/g,'&quot;')}" placeholder="Название университета" />
          </div>
          <div class="input-group">
            <label>Стоимость обучения</label>
            <input type="number" name="tuition" value="${p.tuition || 0}" placeholder="0" min="0" />
          </div>
          <div class="input-group">
            <label>Длительность (месяцы)</label>
            <input type="number" name="durationMonths" value="${p.durationMonths || 0}" placeholder="0" min="0" />
          </div>
          <div class="input-group">
            <label>Изображение программы</label>
            ${p.imageUrl ? `<div style="margin:8px 0;"><img src="${p.imageUrl}" style="max-width:200px;max-height:150px;border-radius:8px;" alt="Current image"/></div>` : ''}
            <input type="file" name="imageFile" accept="image/*" />
            ${p.imageUrl ? `<div style="font-size:12px;color:var(--muted);margin-top:4px;">Текущее изображение: ${p.imageUrl}</div>` : ''}
          </div>
          <div class="input-group">
            <label>Порядок сортировки</label>
            <input type="number" name="order" value="${p.order || 0}" placeholder="0" />
          </div>
          <div class="input-group" style="grid-column:1/-1;">
            <label>Описание программы</label>
            <textarea name="description" rows="4" placeholder="Подробное описание программы">${(p.description || '').toString().replace(/</g,'&lt;')}</textarea>
          </div>
          <div class="input-group" style="grid-column:1/-1;">
            <label style="display:flex;align-items:center;gap:8px;cursor:pointer;">
              <input type="checkbox" name="active" ${p.active ? 'checked' : ''} style="width:auto;margin:0;" />
              <span>Программа активна</span>
            </label>
          </div>
          <div style="grid-column:1/-1;display:flex;gap:10px;margin-top:8px;">
            <button class="btn primary" type="submit">💾 Сохранить</button>
            <button class="btn danger" type="button" onclick="if(confirm('Удалить программу?')){const f=document.createElement('form');f.method='post';f.action='/admin/programs/${p._id}/delete';document.body.appendChild(f);f.submit();}">🗑️ Удалить</button>
          </div>
        </form>
      </td>
    </tr>
  `;
  }).join('');
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
      <form method="post" action="/admin/programs/create" enctype="multipart/form-data" id="programCreateForm">
        <div class="card" style="margin-bottom:20px;background:linear-gradient(135deg,rgba(37,99,235,0.1),rgba(14,165,233,0.1));border:2px solid var(--accent);">
          <h3 style="margin-top:0;color:var(--accent);">🏛️ Шаг 1: Выберите университет</h3>
          <div class="input-group">
            <label class="required" style="font-size:16px;font-weight:600;">Университет</label>
            <select name="universityId" id="universitySelect" required style="min-width:300px;font-size:16px;padding:14px;" onchange="handleUniversitySelect(this)" data-universities='${JSON.stringify(universities.map(u => ({ id: u._id.toString(), name: u.name, city: u.city || '', country: u.country || 'Russia' })))}'>
              <option value="">-- Выберите университет --</option>
              ${universityOptions}
            </select>
          </div>
          <div id="universityInfo" style="margin-top:16px;padding:16px;background:var(--card);border:1px solid var(--border);border-radius:12px;display:none;">
            <div id="universityDetails" style="font-size:15px;"></div>
          </div>
        </div>
        <div class="card" id="programFields" style="display:none;">
          <h3 style="margin-top:0;">📝 Шаг 2: Заполните данные программы</h3>
          <div style="display:grid;grid-template-columns:repeat(auto-fit,minmax(250px,1fr));gap:16px;">
            <div class="input-group">
              <label class="required">Название программы</label>
              <input name="title" placeholder="Введите название" required />
            </div>
            <div class="input-group">
              <label class="required">Slug (URL)</label>
              <input name="slug" placeholder="program-slug" required />
            </div>
            <div class="input-group">
              <label class="required">Язык</label>
              <select name="language">
                ${['ru','en','zh'].map(l=>`<option value="${l}">${l === 'ru' ? 'Русский' : l === 'en' ? 'Английский' : 'Китайский'}</option>`).join('')}
              </select>
            </div>
            <div class="input-group">
              <label class="required">Уровень</label>
              <select name="level">
                ${['bachelor','master','phd','foundation','other'].map(l=>`<option value="${l}">${l === 'bachelor' ? 'Бакалавриат' : l === 'master' ? 'Магистратура' : l === 'phd' ? 'Аспирантура' : l === 'foundation' ? 'Подготовительный' : 'Другое'}</option>`).join('')}
              </select>
            </div>
            <div class="input-group">
              <label>Университет (legacy)</label>
              <input name="university" placeholder="Название университета" />
            </div>
            <div class="input-group">
              <label>Стоимость обучения</label>
              <input type="number" name="tuition" placeholder="0" min="0" value="0" />
            </div>
            <div class="input-group">
              <label>Длительность (месяцы)</label>
              <input type="number" name="durationMonths" placeholder="0" min="0" value="0" />
            </div>
            <div class="input-group">
              <label>Изображение программы</label>
              <input type="file" name="imageFile" accept="image/*" />
            </div>
            <div class="input-group">
              <label>Порядок сортировки</label>
              <input type="number" name="order" value="0" placeholder="0" />
            </div>
            <div class="input-group" style="grid-column:1/-1;">
              <label>Описание программы</label>
              <textarea name="description" rows="4" placeholder="Подробное описание программы"></textarea>
            </div>
            <div class="input-group" style="grid-column:1/-1;">
              <label style="display:flex;align-items:center;gap:8px;cursor:pointer;">
                <input type="checkbox" name="active" checked style="width:auto;margin:0;" />
                <span>Программа активна</span>
              </label>
            </div>
            <div style="grid-column:1/-1;">
              <button class="btn primary" type="submit">✨ Создать программу</button>
            </div>
          </div>
        </div>
      </form>
      <div class="table-wrap" style="margin-top:12px">
        <table>
          <thead><tr><th style="width:240px">ID</th><th>Data</th></tr></thead>
          <tbody>${rows}</tbody>
        </table>
      </div>
    </div>
  `;
  sendAdminResponse(res, await adminLayout({ title: 'Kleos Admin - Programs', active: 'programs', body }));
});

router.post('/admin/programs/create', adminAuthMiddleware, uploadImages.single('imageFile'), async (req, res) => {
  try {
    const { Program } = await import('../models/Program.js');
    const { University } = await import('../models/University.js');
    const schema = z.object({
      title: z.string().min(1),
      slug: z.string().min(1),
      language: z.enum(['ru','en','zh']).optional().default('en'),
      level: z.enum(['bachelor','master','phd','foundation','other']).optional().default('other'),
      university: z.string().optional().default(''),
      universityId: z.string().min(1, 'Университет обязателен для выбора'),
      tuition: z.coerce.number().optional().default(0),
      durationMonths: z.coerce.number().optional().default(0),
      active: z.string().optional(),
      order: z.coerce.number().optional().default(0),
      description: z.string().optional().default('')
    });
    const d = schema.parse(req.body);
    
    // Проверяем, что университет существует
    const university = await University.findById(d.universityId);
    if (!university) {
      return res.status(400).send('Выбранный университет не найден. <a href="/admin/programs">Вернуться назад</a>');
    }
    
    // Проверяем, что slug уникален
    const existingProgram = await Program.findOne({ slug: d.slug });
    if (existingProgram) {
      return res.status(400).send(`Программа с slug "${d.slug}" уже существует. Пожалуйста, используйте другой slug. <a href="/admin/programs">Вернуться назад</a>`);
    }
    
    const base = process.env.PUBLIC_BASE_URL || '';
    const imageUrl = req.file ? `${base}/uploads/images/${req.file.filename}` : '';
    await Program.create({
      title: d.title, slug: d.slug, language: d.language, level: d.level,
      university: d.university || university.name, universityId: d.universityId,
      tuition: d.tuition, durationMonths: d.durationMonths,
      imageUrl: imageUrl, active: d.active === 'on', order: d.order, description: d.description || ''
    });
    res.redirect('/admin/programs');
  } catch (e: any) {
    if (e instanceof ZodError) {
      const errors = e.errors.map(err => `${err.path.join('.')}: ${err.message}`).join(', ');
      return res.status(400).send(`Ошибка валидации: ${errors}. <a href="/admin/programs">Вернуться назад</a>`);
    }
    // Обработка ошибок дублирования MongoDB
    if (e.code === 11000) {
      const field = Object.keys(e.keyPattern || {})[0] || 'slug';
      return res.status(400).send(`Программа с таким ${field === 'slug' ? 'slug' : field} уже существует. Пожалуйста, используйте другое значение. <a href="/admin/programs">Вернуться назад</a>`);
    }
    throw e;
  }
});

router.post('/admin/programs/:id', adminAuthMiddleware, uploadImages.single('imageFile'), async (req, res) => {
  try {
    const { Program } = await import('../models/Program.js');
    const { University } = await import('../models/University.js');
    const schema = z.object({
      title: z.string().optional(),
      slug: z.string().optional(),
      description: z.string().optional(),
      language: z.enum(['ru','en','zh']).optional(),
      level: z.enum(['bachelor','master','phd','foundation','other']).optional(),
      university: z.string().optional(),
      universityId: z.string().min(1, 'Университет обязателен').optional(),
      tuition: z.coerce.number().optional(),
      durationMonths: z.coerce.number().optional(),
      active: z.string().optional(),
      order: z.coerce.number().optional()
    });
    const d = schema.parse(req.body);
    const update: any = { ...d };
    if ('active' in d) update.active = d.active === 'on';
    
    // Если universityId указан, проверяем его существование и не позволяем удалить привязку
    if (d.universityId !== undefined) {
      if (d.universityId === '') {
        return res.status(400).send('Нельзя удалить привязку программы к университету. Программа должна быть привязана к университету. <a href="/admin/programs">Вернуться назад</a>');
      }
      const university = await University.findById(d.universityId);
      if (!university) {
        return res.status(400).send('Выбранный университет не найден. <a href="/admin/programs">Вернуться назад</a>');
      }
      update.universityId = d.universityId;
      if (!update.university) {
        update.university = university.name;
      }
    }
    
    if (req.file) {
      const base = process.env.PUBLIC_BASE_URL || '';
      update.imageUrl = `${base}/uploads/images/${req.file.filename}`;
    }
    await Program.updateOne({ _id: req.params.id }, update);
    res.redirect('/admin/programs');
  } catch (e: any) {
    if (e instanceof ZodError) {
      const errors = e.errors.map(err => `${err.path.join('.')}: ${err.message}`).join(', ');
      return res.status(400).send(`Ошибка валидации: ${errors}. <a href="/admin/programs">Вернуться назад</a>`);
    }
    throw e;
  }
});

router.post('/admin/programs/:id/delete', adminAuthMiddleware, async (req, res) => {
  const { Program } = await import('../models/Program.js');
  await Program.deleteOne({ _id: req.params.id });
  res.redirect('/admin/programs');
});
router.post('/admin/news/create', adminAuthMiddleware, uploadImages.single('imageFile'), async (req, res) => {
  try {
    const { News } = await import('../models/News.js');
    const schema = z.object({
      title: z.string().min(1),
      publishedAt: z.string().optional().default(''),
      order: z.coerce.number().optional().default(0),
      active: z.string().optional(),
      content: z.string().optional().default('')
    });
    const data = schema.parse(req.body);
    const base = process.env.PUBLIC_BASE_URL || '';
    const imageUrl = req.file ? `${base}/uploads/images/${req.file.filename}` : '';
    
    // Обработка publishedAt: если пустая строка или невалидная дата, используем текущую дату
    let publishedAt = new Date();
    if (data.publishedAt && data.publishedAt.trim() !== '') {
      const parsedDate = new Date(data.publishedAt);
      if (!isNaN(parsedDate.getTime())) {
        publishedAt = parsedDate;
      }
    }
    
    const newsItem = await News.create({
      title: data.title,
      imageUrl: imageUrl,
      publishedAt: publishedAt,
      order: data.order,
      active: data.active === 'on',
      content: data.content || ''
    });
    
    // Отправка push-уведомления о новой новости всем пользователям
    if (data.active === 'on') {
      try {
        const { sendPushToAll } = await import('../utils/pushNotifications.js');
        await sendPushToAll(
          'Новая новость',
          data.title,
          { newsId: newsItem._id.toString(), type: 'news' }
        );
      } catch (e: any) {
        console.error('Error sending push notification for news:', e);
        // Не прерываем создание новости из-за ошибки отправки уведомления
      }
    }
    
    res.redirect('/admin/news');
  } catch (e: any) {
    if (e instanceof ZodError) {
      const errors = e.errors.map(err => `${err.path.join('.')}: ${err.message}`).join(', ');
      return res.status(400).send(`Ошибка валидации: ${errors}. <a href="/admin/news">Вернуться назад</a>`);
    }
    console.error('Error creating news:', e);
    return res.status(500).send(`Ошибка при создании новости: ${e?.message || 'unknown'}. <a href="/admin/news">Вернуться назад</a>`);
  }
});

router.post('/admin/news/:id', adminAuthMiddleware, uploadImages.single('imageFile'), async (req, res) => {
  const { News } = await import('../models/News.js');
  const schema = z.object({
    title: z.string().optional(),
    content: z.string().optional(),
    publishedAt: z.string().optional(),
    order: z.coerce.number().optional(),
    active: z.string().optional()
  });
  const data = schema.parse(req.body);
  const update: any = { ...data };
  if ('active' in data) update.active = data.active === 'on';
  if (data.publishedAt) update.publishedAt = new Date(data.publishedAt);
  if (req.file) {
    const base = process.env.PUBLIC_BASE_URL || '';
    update.imageUrl = `${base}/uploads/images/${req.file.filename}`;
  }
  await News.updateOne({ _id: req.params.id }, update);
  res.redirect('/admin/news');
});

router.post('/admin/news/:id/delete', adminAuthMiddleware, async (req, res) => {
  const { News } = await import('../models/News.js');
  await News.deleteOne({ _id: req.params.id });
  res.redirect('/admin/news');
});

router.post('/admin/news/:id/send-notification', adminAuthMiddleware, async (req, res) => {
  try {
    const { News } = await import('../models/News.js');
    const { sendPushToAll } = await import('../utils/pushNotifications.js');
    const { User } = await import('../models/User.js');
    const news = await News.findById(req.params.id);
    if (!news) {
      return res.status(404).json({ ok: false, error: 'News not found' });
    }
    if (!news.active) {
      return res.status(400).json({ ok: false, error: 'News is not active' });
    }
    
    // Проверяем количество пользователей с токенами перед отправкой
    const usersWithTokens = await User.countDocuments({ fcmToken: { $exists: true, $ne: null, $ne: '' } });
    console.log(`Sending notification to ${usersWithTokens} users with FCM tokens`);
    
    const count = await sendPushToAll(
      'Новая новость',
      news.title,
      { newsId: news._id.toString(), type: 'news' }
    );
    
    res.json({ ok: true, count, totalUsersWithTokens: usersWithTokens });
  } catch (e: any) {
    console.error('Error sending notification:', e);
    res.status(500).json({ ok: false, error: e?.message || 'unknown' });
  }
});

// Gallery UI
router.get('/admin/gallery', adminAuthMiddleware, async (_req, res) => {
  const { GalleryItem } = await import('../models/GalleryItem.js');
  const list = await GalleryItem.find().sort({ order: 1, createdAt: -1 }).lean();
  const rows = list.map(g => `
    <tr>
      <td>${g._id}</td>
      <td>
        <form method="post" action="/admin/gallery/${g._id}" enctype="multipart/form-data">
          <input name="title" value="${(g.title || '').toString().replace(/"/g,'&quot;')}" placeholder="Title"/>
          ${g.mediaUrl ? `<div style="margin:8px 0;"><img src="${g.mediaUrl}" style="max-width:200px;max-height:150px;border-radius:8px;" alt="Current media"/></div>` : ''}
          <input type="file" name="mediaFile" accept="image/*,video/*" />
          ${g.mediaUrl ? `<div style="font-size:12px;color:var(--muted);margin-top:4px;">Текущий файл: ${g.mediaUrl}</div>` : ''}
          <select name="mediaType">
            <option value="photo" ${g.mediaType === 'photo' ? 'selected' : ''}>Photo</option>
            <option value="video" ${g.mediaType === 'video' ? 'selected' : ''}>Video</option>
          </select>
          <input type="number" name="order" value="${g.order || 0}" placeholder="Order"/>
          <textarea name="description" rows="2" placeholder="Description" style="width:100%">${(g.description || '').toString().replace(/</g,'&lt;')}</textarea>
          <div style="margin-top:6px">
            <button class="btn primary" type="submit">Save</button>
            <button class="btn danger" formaction="/admin/gallery/${g._id}/delete" formmethod="post" onclick="return confirm('Delete?')">Delete</button>
          </div>
        </form>
      </td>
    </tr>
  `).join('');
  const body = `
    <div class="card">
      <h2>Gallery</h2>
      <form method="post" action="/admin/gallery/create" enctype="multipart/form-data" class="form-row">
        <input name="title" placeholder="Title" style="min-width:200px" required/>
        <input type="file" name="mediaFile" accept="image/*,video/*" required/>
        <select name="mediaType">
          <option value="photo">Photo</option>
          <option value="video">Video</option>
        </select>
        <input type="number" name="order" placeholder="Order" value="0"/>
        <textarea name="description" rows="2" placeholder="Description" style="width:100%"></textarea>
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
  sendAdminResponse(res, await adminLayout({ title: 'Kleos Admin - Gallery', active: 'gallery', body }));
});

router.post('/admin/gallery/create', adminAuthMiddleware, uploadMedia.single('mediaFile'), async (req: any, res: any) => {
  try {
    const { GalleryItem } = await import('../models/GalleryItem.js');
    if (!req.file) {
      return res.status(400).send('Media file is required. <a href="/admin/gallery">Go back</a>');
    }
    const schema = z.object({
      title: z.string().min(1),
      description: z.string().optional().default(''),
      mediaType: z.enum(['photo', 'video']).optional().default('photo'),
      order: z.coerce.number().optional().default(0)
    });
    const data = schema.parse(req.body);
    const base = process.env.PUBLIC_BASE_URL || '';
    const mediaUrl = `${base}/uploads/media/${req.file.filename}`;
    await GalleryItem.create({
      title: data.title,
      description: data.description || '',
      mediaUrl: mediaUrl,
      mediaType: data.mediaType || 'photo',
      order: data.order || 0
    });
    res.redirect('/admin/gallery');
  } catch (e: any) {
    if (e instanceof ZodError) {
      const errors = e.errors.map(err => `${err.path.join('.')}: ${err.message}`).join(', ');
      return res.status(400).send(`Validation error: ${errors}. <a href="/admin/gallery">Go back</a>`);
    }
    throw e;
  }
});

router.post('/admin/gallery/:id', adminAuthMiddleware, uploadMedia.single('mediaFile'), async (req: any, res: any) => {
  const { GalleryItem } = await import('../models/GalleryItem.js');
  const schema = z.object({
    title: z.string().min(1).optional(),
    description: z.string().optional(),
    mediaType: z.enum(['photo', 'video']).optional(),
    order: z.coerce.number().optional()
  });
  const parsed = schema.parse(req.body);
  const update: any = {};
  if (parsed.title !== undefined) update.title = parsed.title;
  if (parsed.description !== undefined) update.description = parsed.description;
  if (parsed.mediaType !== undefined) update.mediaType = parsed.mediaType;
  if (parsed.order !== undefined) update.order = parsed.order;
  if (req.file) {
    const base = process.env.PUBLIC_BASE_URL || '';
    update.mediaUrl = `${base}/uploads/media/${req.file.filename}`;
  }
  await GalleryItem.updateOne({ _id: req.params.id }, update);
  res.redirect('/admin/gallery');
});

router.post('/admin/gallery/:id/delete', adminAuthMiddleware, async (req, res) => {
  const { GalleryItem } = await import('../models/GalleryItem.js');
  await GalleryItem.deleteOne({ _id: req.params.id });
  res.redirect('/admin/gallery');
});

// Universities UI
router.get('/admin/universities', adminAuthMiddleware, async (_req, res) => {
  const { University } = await import('../models/University.js');
  const list = await University.find().sort({ order: 1, name: 1 }).lean();
  const rows = list.map(u => `
    <tr>
      <td>${u._id}</td>
      <td>
        <form method="post" action="/admin/universities/${u._id}" enctype="multipart/form-data">
          <input name="name" value="${(u.name || '').toString().replace(/"/g,'&quot;')}" placeholder="Name" style="min-width:200px"/>
          <input name="city" value="${(u.city || '').toString().replace(/"/g,'&quot;')}" placeholder="City"/>
          <input name="country" value="${(u.country || 'Russia').toString().replace(/"/g,'&quot;')}" placeholder="Country"/>
          <input name="website" value="${(u.website || '').toString().replace(/"/g,'&quot;')}" placeholder="Website URL"/>
          ${u.logoUrl ? `<div style="margin:8px 0;"><img src="${u.logoUrl}" style="max-width:100px;max-height:100px;border-radius:8px;" alt="Current logo"/></div>` : ''}
          <input type="file" name="logoFile" accept="image/*" />
          ${u.logoUrl ? `<div style="font-size:12px;color:var(--muted);margin-top:4px;">Текущий логотип: ${u.logoUrl}</div>` : ''}
          <input type="number" name="order" value="${u.order || 0}" placeholder="Order"/>
          <label style="display:inline-flex;align-items:center;gap:6px"><input type="checkbox" name="active" ${u.active ? 'checked' : ''}/> active</label>
          <textarea name="description" rows="2" placeholder="Description" style="width:100%">${(u.description || '').toString().replace(/</g,'&lt;')}</textarea>
          <div style="margin-top:6px">
            <button class="btn primary" type="submit">Save</button>
            <button class="btn danger" formaction="/admin/universities/${u._id}/delete" formmethod="post" onclick="return confirm('Delete?')">Delete</button>
          </div>
        </form>
      </td>
    </tr>
  `).join('');
  const body = `
    <div class="card">
      <h2>Universities</h2>
      <form method="post" action="/admin/universities/create" enctype="multipart/form-data" class="form-row">
        <input name="name" placeholder="Name" style="min-width:200px"/>
        <input name="city" placeholder="City"/>
        <input name="country" placeholder="Country" value="Russia"/>
        <input name="website" placeholder="Website URL"/>
        <input type="file" name="logoFile" accept="image/*" />
        <input type="number" name="order" placeholder="Order" value="0"/>
        <label style="display:inline-flex;align-items:center;gap:6px"><input type="checkbox" name="active" checked/> active</label>
        <textarea name="description" rows="2" placeholder="Description" style="width:100%"></textarea>
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
  sendAdminResponse(res, await adminLayout({ title: 'Kleos Admin - Universities', active: 'universities', body }));
});

router.post('/admin/universities/create', adminAuthMiddleware, uploadLogos.single('logoFile'), async (req: any, res: any) => {
  const { University } = await import('../models/University.js');
  const schema = z.object({
    name: z.string().min(1),
    city: z.string().optional().default(''),
    country: z.string().optional().default('Russia'),
    description: z.string().optional().default(''),
    website: z.string().optional().refine((val) => {
      if (!val || val.trim() === '') return true;
      try {
        new URL(val.trim());
        return true;
      } catch {
        return false;
      }
    }, { message: 'Invalid URL' }),
    active: z.string().optional(),
    order: z.coerce.number().optional().default(0)
  });
  const data = schema.parse(req.body);
  const base = process.env.PUBLIC_BASE_URL || '';
  const logoUrl = req.file ? `${base}/uploads/logos/${req.file.filename}` : undefined;
  await University.create({
    name: data.name,
    city: data.city || '',
    country: data.country || 'Russia',
    description: data.description || '',
    website: data.website && data.website.trim() ? data.website.trim() : undefined,
    logoUrl: logoUrl,
    active: data.active === 'on',
    order: data.order || 0
  });
  res.redirect('/admin/universities');
});

router.post('/admin/universities/:id', adminAuthMiddleware, uploadLogos.single('logoFile'), async (req: any, res: any) => {
  const { University } = await import('../models/University.js');
  const schema = z.object({
    name: z.string().min(1).optional(),
    city: z.string().optional(),
    country: z.string().optional(),
    description: z.string().optional(),
    website: z.string().optional().refine((val) => {
      if (!val || val.trim() === '') return true;
      try {
        new URL(val.trim());
        return true;
      } catch {
        return false;
      }
    }, { message: 'Invalid URL' }),
    active: z.string().optional(),
    order: z.coerce.number().optional()
  });
  const parsed = schema.parse(req.body);
  const update: any = {};
  if (parsed.name !== undefined) update.name = parsed.name;
  if (parsed.city !== undefined) update.city = parsed.city;
  if (parsed.country !== undefined) update.country = parsed.country;
  if (parsed.description !== undefined) update.description = parsed.description;
  if (parsed.website !== undefined) update.website = parsed.website && parsed.website.trim() ? parsed.website.trim() : undefined;
  if (req.file) {
    const base = process.env.PUBLIC_BASE_URL || '';
    update.logoUrl = `${base}/uploads/logos/${req.file.filename}`;
  }
  if (parsed.active !== undefined) update.active = parsed.active === 'on';
  if (parsed.order !== undefined) update.order = parsed.order;
  await University.updateOne({ _id: req.params.id }, update);
  res.redirect('/admin/universities');
});

router.post('/admin/universities/:id/delete', adminAuthMiddleware, async (req, res) => {
  const { University } = await import('../models/University.js');
  await University.deleteOne({ _id: req.params.id });
  res.redirect('/admin/universities');
});

export default router;


