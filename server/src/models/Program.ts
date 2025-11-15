import { Schema, model } from 'mongoose';

const programSchema = new Schema({
  title: { type: String, required: true, index: 'text' },
  slug: { type: String, required: true, unique: true, index: true },
  description: { type: String, default: '' },
  language: { type: String, enum: ['ru','en','zh'], default: 'en', index: true },
  level: { type: String, enum: ['bachelor','master','phd','foundation','other'], default: 'other', index: true },
  university: { type: String, index: true },
  tuition: { type: Number, default: 0 },
  durationMonths: { type: Number, default: 0 },
  imageUrl: { type: String, default: '' },
  active: { type: Boolean, default: true, index: true },
  order: { type: Number, default: 0 }
}, { timestamps: true });

export const Program = model('Program', programSchema);



