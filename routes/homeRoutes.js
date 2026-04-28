const express = require('express');
const router = express.Router();
const homeController = require('../controllers/homeController');
const menuController = require('../controllers/menuController');
const { optionalUserAuth, requireUserAuth } = require('../middleware/authMiddleware');

router.get('/home', optionalUserAuth, homeController.getHomeData);
router.get('/devotional', homeController.getDevotionalData);
router.get('/artist', homeController.getArtistData);
router.get('/liked-songs', requireUserAuth, homeController.getLikedSongsSection);
router.get('/menu', menuController.getMenu);




module.exports = router;
