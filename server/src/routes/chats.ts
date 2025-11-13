import { Router } from 'express';
import { z } from 'zod';
import { auth } from '../middleware/auth.js';
import { Schema, model, Types } from 'mongoose';

// Define models here to avoid extra files if not present
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
const Chat = model('Chat', ChatSchema);
const Message = model('Message', MessageSchema);

const router = Router();

router.post('/', auth(), async (req, res) => {
  const userId = (req as any).auth?.uid;
  let chat = await Chat.findOne({ userId, status: 'open' });
  if (!chat) {
    chat = await Chat.create({ userId, status: 'open', lastMessageAt: new Date() });
  }
  res.status(201).json({ id: chat._id });
});

router.get('/', auth(), async (req, res) => {
  const userId = (req as any).auth?.uid;
  const chats = await Chat.find({ userId }).sort({ updatedAt: -1 });
  res.json(chats.map(c => ({ id: c._id, status: c.status, lastMessageAt: c.lastMessageAt })));
});

router.get('/all', auth('admin'), async (_req, res) => {
  const chats = await Chat.find().sort({ lastMessageAt: -1 }).limit(200);
  res.json(chats.map(c => ({ id: c._id, userId: c.userId, status: c.status, lastMessageAt: c.lastMessageAt })));
});

router.get('/:id/messages', auth(), async (req, res) => {
  const chatId = req.params.id;
  const msgs = await Message.find({ chatId }).sort({ createdAt: 1 });
  res.json(msgs.map(m => ({ id: m._id, senderRole: m.senderRole, text: m.text, createdAt: m.createdAt })));
});

const sendSchema = z.object({ text: z.string().min(1) });

router.post('/:id/messages', auth(), async (req, res) => {
  const { text } = sendSchema.parse(req.body);
  const isAdmin = ((req as any).auth?.role) === 'admin';
  const msg = await Message.create({ chatId: req.params.id, senderRole: isAdmin ? 'admin' : 'student', text });
  await Chat.updateOne({ _id: req.params.id }, { lastMessageAt: new Date() });
  res.status(201).json({ id: msg._id });
});

export default router;


