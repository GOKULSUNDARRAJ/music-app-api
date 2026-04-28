const express = require('express');
const router = express.Router();
const searchController = require('../controllers/searchController');
const { optionalUserAuth } = require('../middleware/authMiddleware');

router.get('/', optionalUserAuth, searchController.search);

module.exports = router;
