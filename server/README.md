# Kleos Backend (MongoDB Atlas + Express)

## Setup
1. Node.js 18+
2. Create `.env` in `server/`:
```
PORT=8080
MONGODB_URI="mongodb+srv://vvlad1001_db_user:<PASSWORD>@kleos.bias4cc.mongodb.net/kleos?retryWrites=true&w=majority&appName=kleos"
JWT_SECRET="change-me"
```
3. Install deps and run (ESM-friendly dev runner via tsx):
```
cd server
npm install
npm run dev
```
API: http://localhost:8080/health

## Endpoints
- POST /auth/register {fullName,email,password}
- POST /auth/login {email,password}
- GET /partners
- POST /partners (admin)
- PUT /partners/:id (admin)
- DELETE /partners/:id (admin)
- POST /admissions (auth)
- GET /admissions (admin)


