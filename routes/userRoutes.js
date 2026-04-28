const express = require('express');
const multer = require('multer');
const userController = require('../controllers/userController');
const menuController = require('../controllers/menuController');
const { requireUserAuth } = require('../middleware/authMiddleware');

const router = express.Router();

// Smart middleware: supports both multipart/form-data AND raw JSON
const formParser = multer().none();
const smartParser = (req, res, next) => {
  const ct = req.headers['content-type'] || '';
  if (ct.includes('multipart/form-data')) {
    return formParser(req, res, next);
  }
  // Raw JSON is already parsed by express.json() in server.js
  return next();
};

// ── Public routes ─────────────────────────────────────────────────────────────
router.post('/checkRegister', smartParser, userController.checkRegister);
router.post('/register', userController.register);
router.post('/login', userController.login);
router.all('/getTermsAndConditions', userController.getTermsAndConditions); // supports both GET and POST

// ── Protected routes ──────────────────────────────────────────────────────────
router.get('/profile', requireUserAuth, userController.getProfile);
router.put('/profile', requireUserAuth, userController.updateProfile);
router.post('/secure/numberVerification', requireUserAuth, smartParser, userController.numberVerification);
router.post('/verifyOtp', requireUserAuth, smartParser, userController.numberVerification); // Alias for Android
router.post('/resendVerificationCode', requireUserAuth, userController.resendVerificationCode);
router.get('/getNavigationMenu', requireUserAuth, menuController.getMenu);
router.post('/secure/appMenuList', requireUserAuth, menuController.getMenu);
router.post('/logout', requireUserAuth, userController.logout);

router.delete('/account', requireUserAuth, userController.deleteAccount);
router.post('/recordRecentPlay', requireUserAuth, smartParser, userController.recordRecentPlay);
router.post('/toggleLike', requireUserAuth, smartParser, require('../controllers/likeController').toggleCategoryLike);
router.post('/playlist/toggle', requireUserAuth, smartParser, require('../controllers/likeController').toggleCategoryLike); // Alias for Android Quick Toggle
router.get('/liked-items', requireUserAuth, require('../controllers/likeController').getLikedCategories);
router.get('/playlist-songs', requireUserAuth, require('../controllers/playlistController').getPlaylistList);
router.get('/following', requireUserAuth, require('../controllers/artistController').getFollowedArtists);
router.get('/playlist-status/:playlistId', requireUserAuth, require('../controllers/playlistController').checkPlaylistStatus);



module.exports = router;
