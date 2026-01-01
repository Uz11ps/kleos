import { Request, Response, NextFunction } from 'express';
import jwt from 'jsonwebtoken';

export type AuthPayload = { uid: string; role: 'student' | 'admin' };

export function auth(requiredRole?: 'admin', optional?: boolean) {
  return (req: Request, res: Response, next: NextFunction) => {
    const token = (req.headers.authorization || '').replace('Bearer ', '');
    if (!token && optional) {
      // Для опциональной аутентификации, если токена нет - продолжаем без auth
      (req as any).auth = null;
      return next();
    }
    try {
      const payload = jwt.verify(token, process.env.JWT_SECRET!) as AuthPayload;
      (req as any).auth = payload;
      if (requiredRole && payload.role !== requiredRole) return res.status(403).json({ error: 'forbidden' });
      next();
    } catch {
      if (optional) {
        // Для опциональной аутентификации, если токен невалиден - продолжаем без auth
        (req as any).auth = null;
        return next();
      }
      return res.status(401).json({ error: 'unauthorized' });
    }
  };
}


