const express = require('express');
const router = express.Router();
const artistController = require('../controllers/artistController');
const { requireUserAuth } = require('../middleware/authMiddleware');

router.use(requireUserAuth);

// Follow status
router.get('/follow/status/:artistId', artistController.getFollowStatus);

// Toggle follow
router.post('/follow/toggle', artistController.toggleFollowArtist);

// Get followed artists list
router.get('/followed', artistController.getFollowedArtists);

module.exports = router;
