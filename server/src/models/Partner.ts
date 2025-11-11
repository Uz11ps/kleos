import { Schema, model } from 'mongoose';

const partnerSchema = new Schema({
  name: { type: String, required: true },
  description: String,
  logoUrl: String,
  url: String,
  active: { type: Boolean, default: true, index: true },
  order: { type: Number, default: 0, index: true }
}, { timestamps: true });

export const Partner = model('Partner', partnerSchema);


