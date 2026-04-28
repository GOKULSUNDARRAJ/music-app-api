const { Song, Category, Section, Like } = require('../models');
const { Op } = require('sequelize');

const formatEntityId = (prefix, value) => `${prefix}_${String(value).padStart(3, '0')}`;

exports.search = async (req, res, next) => {
  try {
    const { q } = req.query;

    if (!q) {
      return res.status(200).json({
        status: true,
        songs: []
      });
    }

    const songs = await Song.findAll({
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
      limit: 20
    });

    let userLikedSongs = new Set();
    if (req.userId) {
      const likes = await Like.findAll({ where: { userId: req.userId, songId: { [Op.ne]: null } } });
      likes.forEach(l => userLikedSongs.add(l.songId));
    }

    const results = songs.map((song) => ({
      songId: formatEntityId('song', song.id),
      audioName: song.audioName,
      audioUrl: song.audioUrl,
      imageUrl: song.imageUrl,
      categoryName: song.category?.categoryName || '',
      categoryId: song.category ? formatEntityId('cat', song.category.id) : '',
      sectionTitle: song.category?.section?.sectionTitle || '',
      isLiked: userLikedSongs.has(song.id)
    }));


    return res.status(200).json({
      status: true,
      error_type: '200',
      message: 'Search Results',
      songs: results
    });
  } catch (error) {
    return next(error);
  }
};
