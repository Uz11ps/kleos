import { Schema, model } from 'mongoose';

const translationSchema = new Schema({
  lang: { type: String, enum: ['en','ru','zh'], index: true, required: true },
  key: { type: String, index: true, required: true },
  value: { type: String, required: true }
}, { timestamps: true, indexes: [{ fields: { lang: 1, key: 1 }, options: { unique: true } }] as any });

export const Translation = model('Translation', translationSchema);


