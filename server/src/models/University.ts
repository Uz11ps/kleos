import { Schema, model } from 'mongoose';

// Content block schema for university pages
const contentBlockSchema = new Schema({
  type: { type: String, enum: ['text', 'image', 'heading'], required: true },
  content: String, // Text content or image URL
  order: { type: Number, default: 0 }
}, { _id: true });

const universitySchema = new Schema({
  name: { type: String, required: true, index: true },
  city: String,
  country: { type: String, default: 'Russia' },
  description: String,
  website: String,
  logoUrl: String,
  active: { type: Boolean, default: true, index: true },
  order: { type: Number, default: 0 },
  // Social media and contact links
  socialLinks: {
    facebook: String,
    twitter: String,
    instagram: String,
    youtube: String,
    whatsapp: String, // Phone number for WhatsApp
    phone: String, // Phone number
    email: String
  },
  // Degree programs offered
  degreePrograms: [{
    type: { type: String, enum: ["Bachelor's degree", "Master's degree", "Research degree", "Speciality degree"], required: true },
    description: String
  }],
  // Content blocks for custom page content
  contentBlocks: [contentBlockSchema]
}, { timestamps: true });

export const University = model('University', universitySchema);

