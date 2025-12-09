import { Schema, model } from 'mongoose';

const galleryItemSchema = new Schema({
  title: String,
  description: String,
  mediaUrl: String,
  mediaType: { type: String, enum: ['photo', 'video'], default: 'photo' },
  order: { type: Number, default: 0 }
}, { timestamps: true });

export const GalleryItem = model('GalleryItem', galleryItemSchema);

