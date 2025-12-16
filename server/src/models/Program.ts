import { Schema, model } from 'mongoose';

import { Types } from 'mongoose';

const programSchema = new Schema({
  title: { type: String, required: true, index: 'text' },
  description: { type: String, default: '' },
  language: { type: String, enum: ['ru','en','zh'], default: 'en', index: true },
  level: { type: String, enum: ["Bachelor's degree", "Master's degree", "Research degree", "Speciality degree"], default: "Bachelor's degree", index: true },
  university: { type: String, index: true }, // Legacy field, kept for backward compatibility
  universityId: { type: Types.ObjectId, ref: 'University', required: true, index: true },
  tuition: { type: Number, default: 0 },
  durationYears: { type: Number, default: 4 }, // Changed from durationMonths to durationYears
  active: { type: Boolean, default: true, index: true },
  order: { type: Number, default: 0 }
}, { timestamps: true });

export const Program = model('Program', programSchema);



