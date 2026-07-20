const express = require('express');
const router = express.Router();
const adController = require('../controllers/adController');
const { requireAdminAuth } = require('../middleware/authMiddleware');
const multer = require('multer');
const path = require('path');

// Storage configuration - similar to others
let storage;
if (process.env.FIREBASE_PROJECT_ID) {
  const FirebaseStorage = require('multer-firebase-storage');
  storage = FirebaseStorage({
    bucketName: process.env.FIREBASE_STORAGE_BUCKET,
    credentials: {
      projectId: process.env.FIREBASE_PROJECT_ID,
      clientEmail: process.env.FIREBASE_CLIENT_EMAIL,
      privateKey: process.env.FIREBASE_PRIVATE_KEY?.replace(/\\n/g, '\n'),
    },
    public: true,
    namePrefix: 'music_app_ads/',
    hooks: {
      beforeUpload: (req, file) => {
        file.originalname = Date.now() + '-' + Math.round(Math.random() * 1e9) + path.extname(file.originalname);
      }
    }
  });
} else {
  storage = multer.diskStorage({
    destination: (req, file, cb) => cb(null, 'uploads/'),
    filename: (req, file, cb) => cb(null, Date.now() + path.extname(file.originalname))
  });
}

const upload = multer({ storage });

// Public Route
router.get('/random', adController.getRandomAd);

// Admin Routes
router.get('/admin', requireAdminAuth, adController.getAds);
router.post('/admin', requireAdminAuth, upload.fields([{ name: 'media' }, { name: 'image' }]), adController.createAd);
router.delete('/admin/:id', requireAdminAuth, adController.deleteAd);

module.exports = router;
