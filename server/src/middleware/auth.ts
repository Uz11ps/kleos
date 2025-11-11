import { Request, Response, NextFunction } from 'express';
import jwt from 'jsonwebtoken';

export type AuthPayload = { uid: string; role: 'student' | 'admin' };

export function auth(requiredRole?: 'admin') {
  return (req: Request, res: Response, next: NextFunction) => {
    const token = (req.headers.authorization || '').replace('Bearer ', '');
    try {
      const payload = jwt.verify(token, process.env.JWT_SECRET!) as AuthPayload;
      (req as any).auth = payload;
      if (requiredRole && payload.role !== requiredRole) return res.status(403).json({ error: 'forbidden' });
      next();
    } catch {
      return res.status(401).json({ error: 'unauthorized' });
    }
  };
}


