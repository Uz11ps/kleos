import { Schema, model } from 'mongoose';

const userSchema = new Schema({
  email: { type: String, index: true, unique: true, required: true },
  fullName: { type: String, required: true },
  passwordHash: { type: String, required: true },
  role: { type: String, enum: ['student', 'admin'], default: 'student', index: true }
}, { timestamps: true });

export const User = model('User', userSchema);


