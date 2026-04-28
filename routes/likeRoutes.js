const express = require('express');
const router = express.Router();
const likeController = require('../controllers/likeController');
const { requireUserAuth } = require('../middleware/authMiddleware');
const multer = require('multer');

// Smart middleware: supports both multipart/form-data AND raw JSON
const formParser = multer().none();
const smartParser = (req, res, next) => {
  const ct = req.headers['content-type'] || '';
  if (ct.includes('multipart/form-data')) {
    return formParser(req, res, next);
  }
  return next();
};

// All like routes require authentication
router.use(requireUserAuth);

// Category Like Routes
router.post('/category/toggle', smartParser, likeController.toggleCategoryLike);
router.get('/categories', likeController.getLikedCategories);
router.get('/category/status/:id', likeController.checkCategoryStatus);

module.exports = router;
