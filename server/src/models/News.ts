import { Schema, model } from 'mongoose';

const newsSchema = new Schema({
  title: { type: String, required: true },
  content: { type: String, default: '' },
  imageUrl: { type: String, default: '' },
  publishedAt: { type: Date, default: () => new Date(), index: true },
  active: { type: Boolean, default: true, index: true },
  order: { type: Number, default: 0 }
}, { timestamps: true });

export const News = model('News', newsSchema);



