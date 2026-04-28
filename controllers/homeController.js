const { Section, Category, Song, RecentlyPlayed, Like } = require('../models');

const formatEntityId = (prefix, value) => `${prefix}_${String(value).padStart(3, '0')}`;

const getContentData = async (req, res, next, contentType) => {
  try {
    const sections = await Section.findAll({
      where: { contentType },
      order: [['id', 'ASC']],
      include: [
        {
          model: Category,
          as: 'categories',
          required: false,
          order: [['id', 'ASC']],
          include: [
            {
              model: Song,
              as: 'songs',
              required: false
            }
          ]
        }
      ]
    });

    let userLikedCategories = new Set();
    let userLikedSongs = new Set();
    if (req.userId) {
      const likes = await Like.findAll({ where: { userId: req.userId } });
      likes.forEach(l => {
        if (l.categoryId) userLikedCategories.add(l.categoryId);
        if (l.songId) userLikedSongs.add(l.songId);
      });
    }

    const mappedSections = sections.map((section) => {
      const sectionData = section.toJSON();

      return {
        sectionId: formatEntityId('sec', sectionData.id),
        sectionTitle: sectionData.sectionTitle,
        layoutType: sectionData.layoutType,
        spanCount: sectionData.spanCount,
        categories: (sectionData.categories || []).map((category) => ({
          categoryId: formatEntityId('cat', category.id),
          categoryName: category.categoryName,
          categoryImage: category.categoryImage,
          adapterType: category.adapterType,
          isLiked: userLikedCategories.has(category.id),
          songs: (category.songs || []).map((song) => ({
            songId: formatEntityId('song', song.id),
            audioName: song.audioName,
            audioUrl: song.audioUrl,
            category: category.categoryName,
            imageUrl: song.imageUrl,
            categoryId: formatEntityId('cat', song.categoryId),
            isLiked: userLikedSongs.has(song.id)
          }))
        }))
      };
    });

    // ── Prepend Recently Played if applicable ──
    if (contentType === 'home' && req.userId) {
      const recentPlays = await RecentlyPlayed.findAll({
        where: { userId: req.userId },
        limit: 10,
        order: [['playedAt', 'DESC']],
        include: [
          {
            model: Category,
            as: 'category',
            include: [{ model: Song, as: 'songs' }]
          }
        ]
      });

      if (recentPlays.length > 0) {
        const recentCategories = recentPlays
          .filter(rp => rp.category) // Ensure category exists
          .map(rp => ({
            categoryId: formatEntityId('cat', rp.category.id),
            categoryName: rp.category.categoryName,
            categoryImage: rp.category.categoryImage,
            adapterType: 4,
            isLiked: userLikedCategories.has(rp.category.id),
            songs: (rp.category.songs || []).map(song => ({
              songId: formatEntityId('song', song.id),
              audioName: song.audioName,
              audioUrl: song.audioUrl,
              category: rp.category.categoryName,
              imageUrl: song.imageUrl,
              categoryId: formatEntityId('cat', song.categoryId),
              isLiked: userLikedSongs.has(song.id)
            }))
          }));

        if (recentCategories.length > 0) {
          mappedSections.unshift({
            sectionId: 'sec_recent',
            sectionTitle: 'Recently Played',
            layoutType: 3,
            spanCount: 3,
            categories: recentCategories
          });
        }
      }
    }

    return res.status(200).json({ sections: mappedSections });
  } catch (error) {
    return next(error);
  }
};

exports.getHomeData = async (req, res, next) => getContentData(req, res, next, 'home');
exports.getDevotionalData = async (req, res, next) => getContentData(req, res, next, 'devotional');
exports.getArtistData = async (req, res, next) => getContentData(req, res, next, 'artist');

exports.getLikedSongsSection = async (req, res, next) => {
  try {
    const userId = req.userId;

    const likes = await Like.findAll({
      where: { userId, songId: { [require('sequelize').Op.ne]: null } },
      include: [
        {
          model: Song,
          as: 'song',
          include: [{ model: Category, as: 'category' }]
        }
      ]
    });

    const formatEntityId = (prefix, id) => `${prefix}_${String(id).padStart(3, '0')}`;

    const likedSongs = likes
      .filter(l => l.song)
      .map(l => ({
        songId: formatEntityId('song', l.song.id),
        audioName: l.song.audioName,
        audioUrl: l.song.audioUrl,
        category: l.song.category ? l.song.category.categoryName : 'Unknown',
        imageUrl: l.song.imageUrl,
        categoryId: l.song.categoryId ? formatEntityId('cat', l.song.categoryId) : null,
        isLiked: true
      }));

    const response = {
      success: true,
      section: {
        sectionId: "sec_liked",
        sectionTitle: "Your Liked Songs",
        layoutType: 1,
        spanCount: 1,
        categories: [
          {
            categoryId: "liked_playlist",
            categoryName: "Liked Songs",
            categoryImage: "https://example.com/liked_icon.png",
            adapterType: 1,
            isLiked: true,
            songs: likedSongs
          }
        ]
      }
    };

    return res.status(200).json(response);
  } catch (error) {
    return next(error);
  }
};



