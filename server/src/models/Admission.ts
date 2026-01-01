import { Schema, model, Types } from 'mongoose';

const admissionSchema = new Schema({
  userId: { type: Types.ObjectId, ref: 'User' },
  firstName: String,
  lastName: String,
  patronymic: String,
  phone: String,
  email: String,
  dateOfBirth: String,
  placeOfBirth: String,
  nationality: String,
  passportNumber: String,
  passportIssue: String,
  passportExpiry: String,
  visaCity: String,
  program: String,
  comment: String,
  studentId: String,
  status: { type: String, enum: ['new', 'processing', 'done', 'rejected'], default: 'new', index: true }
}, { timestamps: true });

export const Admission = model('Admission', admissionSchema);


