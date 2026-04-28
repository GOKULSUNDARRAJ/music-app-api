const express = require('express');
const adminController = require('../controllers/adminController');
const menuController = require('../controllers/menuController');
const { requireAdminAuth } = require('../middleware/authMiddleware');
const multer = require('multer');
const path = require('path');
const fs = require('fs');

const uploadDir = 'uploads';
if (!fs.existsSync(uploadDir)) {
  fs.mkdirSync(uploadDir);
}

const storage = multer.diskStorage({
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
  const url = `${process.env.BASE_URL}/uploads/${req.file.filename}`;
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

router.get('/fetch-metadata', requireAdminAuth, adminController.fetchUrlMetadata);

router.post('/menu', requireAdminAuth, menuController.createMenuItem);
router.get('/menu', requireAdminAuth, menuController.getMenuItems);
router.put('/menu/:id', requireAdminAuth, menuController.updateMenuItem);
router.delete('/menu/:id', requireAdminAuth, menuController.deleteMenuItem);


router.get('/users', requireAdminAuth, adminController.getUsers);

router.delete('/user/:id', requireAdminAuth, adminController.deleteUser);

module.exports = router;
