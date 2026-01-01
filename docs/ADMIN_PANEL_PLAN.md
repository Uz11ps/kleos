# Kleos Admin Panel and Backend Plan

## Goals
- Admins manage student data, partners, and handle support chat/admission applications.
- Mobile app consumes the same backend for auth, profile, chat, partners, and admissions.

## Recommended Stack
- Backend: Supabase (Postgres + Auth + Realtime + Storage) or Firebase (Auth + Firestore).
  - Pick Supabase if you prefer SQL and server-side policies; pick Firebase for simpler client SDKs.
- Admin Panel (Web): Next.js (React) + Material UI + Supabase/Firebase SDK.

## Data Model (initial)
- users: id, email, full_name, role ("student" | "admin"), created_at
- profiles: user_id (fk users), phone, photo_url, extra fields
- partners: id, name, description, logo_url, url, order_index, is_active
- admissions: id, user_id (nullable for anonymous), full_name, phone, email, program, comment, status, created_at
- messages: id, chat_id, sender_id (nullable for support-bot), sender_role, text, created_at
- chats: id, user_id, status (open/closed), last_message_at

## Permissions
- Students: read self profile, read partners, create/read own chat, create admissions.
- Admins: CRUD partners, view all admissions, reply in any chat, update student data.

## Web Admin Features
1. Auth (admin role check).
2. Partners CRUD (table + form).
3. Admissions inbox (filters, status updates).
4. Support chat console (real-time).
5. Student profiles and notes (optional).

## Mobile Integration (next steps)
- Replace local Auth/Session with backend SDK.
- Partners list from backend.
- Chat via realtime channel.
- Admission form sends to backend.
- Profile fetch/update from backend.

## Deployment
- Supabase: hosted project.
- Next.js Admin: Vercel.

## Security
- Use RLS (Supabase) / Security Rules (Firebase).
- Separate admin role and policies.


