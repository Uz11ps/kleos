import { Schema, model } from 'mongoose';

const userSchema = new Schema({
  email: { type: String, index: true, unique: true, required: true },
  fullName: { type: String, required: true },
  passwordHash: { type: String, required: true },
  role: { type: String, enum: ['student', 'admin'], default: 'student', index: true },
  emailVerified: { type: Boolean, default: false, index: true },
  emailVerifyToken: { type: String, index: true, sparse: true },
  emailVerifyExpires: { type: Date },
  // Admin-editable student profile fields (optional)
  studentId: { type: String, index: true, sparse: true },
  phone: { type: String },
  course: { type: String },
  speciality: { type: String },
  status: { type: String },
  university: { type: String },
  payment: { type: String },
  penalties: { type: String },
  notes: { type: String }
}, { timestamps: true });

export const User = model('User', userSchema);


