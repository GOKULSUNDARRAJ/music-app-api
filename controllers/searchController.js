const { Song, Category, Section, Like } = require('../models');
const { Op } = require('sequelize');

const formatEntityId = (prefix, value) => `${prefix}_${String(value).padStart(3, '0')}`;

exports.search = async (req, res, next) => {
  try {
    const { q, page = 1, limit = 20 } = req.query;
    const pageNum = parseInt(page, 10);
    const limitNum = parseInt(limit, 10);
    const offset = (pageNum - 1) * limitNum;

    if (!q) {
      return res.status(200).json({
        status: true,
        songs: [],
        playlists: [],
        currentPage: pageNum,
        hasMore: false
      });
    }

    // 1. Search Songs
    const { count: songCount, rows: songs } = await Song.findAndCountAll({
      where: {
        audioName: {
          [Op.substring]: q
        }
      },
      include: [
        {
          model: Category,
          as: 'category',
          include: [
            {
              model: Section,
              as: 'section'
            }
          ]
        }
      ],
      limit: limitNum,
      offset: offset
    });

    let userLikedSongs = new Set();
    if (req.userId) {
      const likes = await Like.findAll({ where: { userId: req.userId, songId: { [Op.ne]: null } } });
      likes.forEach(l => userLikedSongs.add(l.songId));
    }

    const songResults = songs.map((song) => ({
      songId: formatEntityId('song', song.id),
      audioName: song.audioName,
      audioUrl: song.audioUrl,
      imageUrl: song.imageUrl,
      categoryName: song.category?.categoryName || '',
      categoryId: song.category ? formatEntityId('cat', song.category.id) : '',
      sectionTitle: song.category?.section?.sectionTitle || '',
      isLiked: userLikedSongs.has(song.id)
    }));

    // 2. Search Playlists (Categories)
    const { count: playlistCount, rows: playlists } = await Category.findAndCountAll({
      where: {
        categoryName: {
          [Op.substring]: q
        }
      },
      include: [
        {
          model: Song,
          as: 'songs'
        }
      ],
      limit: limitNum,
      offset: offset
    });

    const playlistResults = playlists.map((playlist) => ({
      categoryId: formatEntityId('cat', playlist.id),
      categoryName: playlist.categoryName,
      categoryImage: playlist.categoryImage,
      songs: (playlist.songs || []).map(song => ({
        songId: formatEntityId('song', song.id),
        audioName: song.audioName,
        audioUrl: song.audioUrl,
        category: playlist.categoryName,
        imageUrl: song.imageUrl,
        categoryId: formatEntityId('cat', song.categoryId),
        isLiked: userLikedSongs.has(song.id)
      }))
    }));

    const hasMore = (offset + limitNum) < Math.max(songCount, playlistCount);

    return res.status(200).json({
      status: true,
      error_type: '200',
      message: 'Search Results',
      songs: songResults,
      playlists: playlistResults,
      currentPage: pageNum,
      hasMore: hasMore
    });
  } catch (error) {
    return next(error);
  }
};

exports.getCategorySongs = async (req, res, next) => {
  try {
    const categoryIdStr = req.params.categoryId;
    const idParts = categoryIdStr.split('_');
    const categoryId = idParts.length > 1 ? parseInt(idParts[1], 10) : parseInt(categoryIdStr, 10);

    const category = await Category.findByPk(categoryId, {
      include: [
        {
          model: Song,
          as: 'songs'
        }
      ]
    });

    if (!category) {
      return res.status(404).json({ status: false, message: 'Category not found', songs: [] });
    }

    let userLikedSongs = new Set();
    if (req.userId) {
      const likes = await Like.findAll({ where: { userId: req.userId, songId: { [Op.ne]: null } } });
      likes.forEach(l => userLikedSongs.add(l.songId));
    }

    const songs = (category.songs || []).map(song => ({
      songId: formatEntityId('song', song.id),
      audioName: song.audioName,
      audioUrl: song.audioUrl,
      category: category.categoryName,
      imageUrl: song.imageUrl,
      categoryId: formatEntityId('cat', song.categoryId),
      isLiked: userLikedSongs.has(song.id)
    }));

    return res.status(200).json({
      status: true,
      songs: songs
    });
  } catch (err) {
    console.error('Error fetching category songs:', err);
    return res.status(500).json({ status: false, error: 'Internal Server Error' });
  }
};
