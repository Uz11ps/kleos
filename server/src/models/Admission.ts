import { Schema, model, Types } from 'mongoose';

const admissionSchema = new Schema({
  userId: { type: Types.ObjectId, ref: 'User' },
  fullName: String,
  phone: String,
  email: String,
  program: String,
  comment: String,
  status: { type: String, enum: ['new', 'processing', 'done'], default: 'new', index: true }
}, { timestamps: true });

export const Admission = model('Admission', admissionSchema);


