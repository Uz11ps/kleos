import { Schema, model } from 'mongoose';

const universitySchema = new Schema({
  name: { type: String, required: true, index: true },
  city: String,
  country: { type: String, default: 'Russia' },
  description: String,
  website: String,
  logoUrl: String,
  active: { type: Boolean, default: true, index: true },
  order: { type: Number, default: 0 }
}, { timestamps: true });

export const University = model('University', universitySchema);

