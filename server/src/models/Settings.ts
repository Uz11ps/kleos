import { Schema, model } from 'mongoose';

const settingsSchema = new Schema({
  key: { type: String, required: true, unique: true, index: true },
  value: Schema.Types.Mixed, // Can be string, array, object, etc.
  description: String
}, { timestamps: true });

export const Settings = model('Settings', settingsSchema);

