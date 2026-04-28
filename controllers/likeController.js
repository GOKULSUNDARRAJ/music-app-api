const { Like, Category, Song } = require('../models');
const { Op } = require('sequelize');

const formatEntityId = (prefix, id) => `${prefix}_${String(id).padStart(3, '0')}`;

/**
 * Toggle like for a category
 * POST /api/likes/category/toggle
 */
exports.toggleCategoryLike = async (req, res, next) => {
  try {
    let { categoryId } = req.body;
    const userId = req.userId;

    if (!categoryId) {
      return res.status(400).json({ status: false, message: 'categoryId is required' });
    }

    // Handle formatted IDs like "cat_001"
    if (typeof categoryId === 'string' && categoryId.startsWith('cat_')) {
      categoryId = parseInt(categoryId.split('_')[1], 10);
    }

    const existing = await Like.findOne({ where: { userId, categoryId } });

    if (existing) {
      await existing.destroy();
      return res.json({ 
        status: true, 
        result: "true",
        message: 'Category unliked successfully', 
        isLiked: false, 
        liked: false 
      });
    } else {
      await Like.create({ userId, categoryId });
      return res.json({ 
        status: true, 
        result: "true",
        message: 'Category liked successfully', 
        isLiked: true, 
        liked: true 
      });
    }
  } catch (err) {
    return next(err);
  }
};

/**
 * Get liked categories for the library screen
 * GET /api/likes/categories
 */
exports.getLikedCategories = async (req, res, next) => {
  try {
    const userId = req.userId;

    // 1. Fetch all likes for categories
    const likes = await Like.findAll({
      where: { userId, categoryId: { [Op.ne]: null } },
      include: [
        { 
          model: Category, 
          as: 'category', 
          include: [
            { model: Song, as: 'songs' },
            { require: false, model: require('../models').Section, as: 'section' }
          ] 
        }
      ]
    });

    // 2. Map all liked categories into a single list
    const categories = [];

    for (const like of likes) {
      const category = like.category;
      if (!category) continue;

      categories.push({
        categoryId: formatEntityId('cat', category.id),
        categoryName: category.categoryName,
        categoryImage: category.categoryImage,
        adapterType: 5,
        isLiked: true,
        songs: (category.songs || []).map(s => ({
          songId: formatEntityId('song', s.id),
          audioName: s.audioName,
          audioUrl: s.audioUrl,
          category: category.categoryName,
          imageUrl: s.imageUrl,
          categoryId: formatEntityId('cat', s.categoryId),
          isLiked: false
        }))
      });
    }

    // 3. Return as a single "My Library" section
    return res.json({
      sections: [
        {
          sectionId: "sec_library",
          sectionTitle: "My Library",
          layoutType: 4,
          spanCount: 4,
          categories: categories
        }
      ]
    });
  } catch (err) {
    return next(err);
  }
};

/**
 * Check if a specific category is liked
 * GET /api/likes/category/status/:id
 */
exports.checkCategoryStatus = async (req, res, next) => {
  try {
    let { id: categoryId } = req.params;
    const userId = req.userId;

    if (typeof categoryId === 'string' && categoryId.startsWith('cat_')) {
      categoryId = parseInt(categoryId.split('_')[1], 10);
    }

    const existing = await Like.findOne({ where: { userId, categoryId } });
    return res.json({ 
      status: true, 
      result: "true",
      liked: !!existing, 
      isLiked: !!existing 
    });
  } catch (err) {
    return next(err);
  }
};
