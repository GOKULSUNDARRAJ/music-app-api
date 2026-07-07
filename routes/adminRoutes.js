const express = require('express');
const adminController = require('../controllers/adminController');
const menuController = require('../controllers/menuController');
const { requireAdminAuth } = require('../middleware/authMiddleware');
const multer = require('multer');
const path = require('path');
const fs = require('fs');
const admin = require('firebase-admin');
const FirebaseStorage = require('multer-firebase-storage');

const uploadDir = 'uploads';
if (!fs.existsSync(uploadDir)) {
  fs.mkdirSync(uploadDir);
}

// Initialize Firebase Admin if not already initialized
if (process.env.FIREBASE_PROJECT_ID && !admin.getApps().length) {
  admin.initializeApp({
    credential: admin.cert({
      projectId: process.env.FIREBASE_PROJECT_ID,
      clientEmail: process.env.FIREBASE_CLIENT_EMAIL,
      privateKey: process.env.FIREBASE_PRIVATE_KEY?.replace(/\\n/g, '\n'),
    }),
    storageBucket: process.env.FIREBASE_STORAGE_BUCKET
  });
}

// Dynamic Storage: Use Firebase if credentials exist, else fallback to local
const storage = process.env.FIREBASE_PROJECT_ID ? FirebaseStorage({
  bucketName: process.env.FIREBASE_STORAGE_BUCKET,
  credentials: admin.cert({
    projectId: process.env.FIREBASE_PROJECT_ID,
    clientEmail: process.env.FIREBASE_CLIENT_EMAIL,
    privateKey: process.env.FIREBASE_PRIVATE_KEY?.replace(/\\n/g, '\n'),
  }),
  public: true,
  namePrefix: 'music_app_uploads/',
  hooks: {
    beforeUpload: (req, file) => {
      // Create a unique filename
      file.originalname = Date.now() + '-' + Math.round(Math.random() * 1e9) + path.extname(file.originalname);
    }
  }
}) : multer.diskStorage({
  destination: (req, file, cb) => cb(null, uploadDir),
  filename: (req, file, cb) => {
    const uniqueSuffix = Date.now() + '-' + Math.round(Math.random() * 1e9);
    cb(null, uniqueSuffix + path.extname(file.originalname));
  }
});

const upload = multer({ 
  storage,
  fileFilter: (req, file, cb) => {
    const isAudio = file.mimetype.startsWith('audio/');
    const isImage = file.mimetype.startsWith('image/');
    console.log(`Filtering: ${file.originalname} | Mime: ${file.mimetype}`);
    
    if (isAudio || isImage) {
      return cb(null, true);
    }
    cb(new Error(`Only audio and image files are allowed! Rejected: ${file.originalname} (${file.mimetype})`));
  }
});

const router = express.Router();

router.post('/login', adminController.login);
router.get('/dashboard', requireAdminAuth, adminController.getDashboardCounts);
router.post('/upload', requireAdminAuth, upload.single('file'), (req, res) => {
  if (!req.file) return res.status(400).json({ message: 'No file uploaded' });
  
  let url = req.file.fileUrl || req.file.publicUrl; // Firebase returns fileUrl
  if (!process.env.FIREBASE_PROJECT_ID) {
    const baseUrl = process.env.BASE_URL || `${req.protocol}://${req.get('host')}`;
    url = `${baseUrl}/uploads/${req.file.filename}`;
  }
  
  res.json({ url });
});

router.post('/section', requireAdminAuth, adminController.createSection);
router.get('/sections', requireAdminAuth, adminController.getSections);
router.put('/section/:id', requireAdminAuth, adminController.updateSection);
router.delete('/section/:id', requireAdminAuth, adminController.deleteSection);

router.post('/category', requireAdminAuth, adminController.createCategory);
router.get('/categories', requireAdminAuth, adminController.getCategories);
router.put('/category/:id', requireAdminAuth, adminController.updateCategory);
router.delete('/category/:id', requireAdminAuth, adminController.deleteCategory);

router.post('/song', requireAdminAuth, adminController.createSong);
router.post('/song/bulk', requireAdminAuth, adminController.bulkCreateSongs);
router.post('/song/upload', requireAdminAuth, upload.array('files'), adminController.uploadSongs);
router.get('/songs', requireAdminAuth, adminController.getSongs);
router.put('/song/:id', requireAdminAuth, adminController.updateSong);
router.delete('/song/:id', requireAdminAuth, adminController.deleteSong);
router.put('/song/:id/lyrics', requireAdminAuth, adminController.updateLyrics);

router.get('/fetch-metadata', requireAdminAuth, adminController.fetchUrlMetadata);

router.post('/menu', requireAdminAuth, menuController.createMenuItem);
router.get('/menu', requireAdminAuth, menuController.getMenuItems);
router.put('/menu/:id', requireAdminAuth, menuController.updateMenuItem);
router.delete('/menu/:id', requireAdminAuth, menuController.deleteMenuItem);


router.get('/users', requireAdminAuth, adminController.getUsers);

router.delete('/user/:id', requireAdminAuth, adminController.deleteUser);

module.exports = router;
