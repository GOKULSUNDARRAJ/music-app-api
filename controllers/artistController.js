const { Follow, Category, Song, Section, Like } = require('../models');

const formatEntityId = (prefix, id) => `${prefix}_${String(id).padStart(3, '0')}`;

// ─── Check if user is following an artist ─────────────────────────────────────
exports.getFollowStatus = async (req, res, next) => {
  try {
    const { artistId } = req.params;
    const userId = req.userId;

    // Support both numeric IDs and 'cat_' prefixed IDs
    let targetId = artistId;
    if (typeof artistId === 'string' && artistId.startsWith('cat_')) {
      targetId = parseInt(artistId.split('_')[1], 10);
    }

    const follow = await Follow.findOne({
      where: { userId, artistId: targetId }
    });

    return res.json({
      status: true,
      success: true,
      isFollowing: !!follow
    });
  } catch (err) {
    return next(err);
  }
};

// ─── Toggle follow/unfollow an artist ──────────────────────────────────────────
exports.toggleFollowArtist = async (req, res, next) => {
  try {
    let { artistId } = req.body;
    const userId = req.userId;

    if (!artistId) {
      return res.status(400).json({ status: false, message: 'artistId is required' });
    }

    // Support 'cat_' prefix
    if (typeof artistId === 'string' && artistId.startsWith('cat_')) {
      artistId = parseInt(artistId.split('_')[1], 10);
    }

    // Verify category exists
    const category = await Category.findByPk(artistId);
    if (!category) {
      return res.status(404).json({ status: false, message: 'Artist not found' });
    }

    const existing = await Follow.findOne({
      where: { userId, artistId }
    });

    if (existing) {
      await existing.destroy();
      return res.json({
        status: true,
        success: true,
        isFollowing: false,
        message: 'Unfollowed successfully'
      });
    } else {
      await Follow.create({ userId, artistId });
      return res.json({
        status: true,
        success: true,
        isFollowing: true,
        message: 'Followed successfully'
      });
    }
  } catch (err) {
    return next(err);
  }
};

// ─── Get all followed artists ───────────────────────────────────────────────
exports.getFollowedArtists = async (req, res, next) => {
  try {
    const userId = req.userId;

    const follows = await Follow.findAll({
      where: { userId },
      include: [
        {
          model: Category,
          as: 'artist',
          include: [
            { model: Song, as: 'songs' },
            { model: Section, as: 'section' }
          ]
        }
      ]
    });

    // Fetch user likes for songs
    const userLikes = await Like.findAll({
      where: { userId, songId: { [require('sequelize').Op.ne]: null } }
    });
    const likedSongIds = new Set(userLikes.map(l => formatEntityId('song', l.songId)));

    const artists = follows.map(f => {
      const artist = f.artist;
      if (!artist) return null;

      const catId = formatEntityId('cat', artist.id);
      return {
        categoryId: catId,
        categoryName: artist.categoryName,
        categoryImage: artist.categoryImage,
        adapterType: 6, // Matching playlist/artist adapter type
        isFollowing: true,
        songs: (artist.songs || []).map(s => {
          const sId = formatEntityId('song', s.id);
          return {
            songId: sId,
            audioName: s.audioName,
            audioUrl: s.audioUrl,
            imageUrl: s.imageUrl,
            category: artist.categoryName,
            categoryId: catId,
            isLiked: likedSongIds.has(sId),
            liked: likedSongIds.has(sId)
          };
        }),
        songs_count: (artist.songs || []).length
      };
    }).filter(Boolean);

    return res.json({
      status: true,
      success: true,
      result: "true",
      sections: [
        {
          sectionId: "sec_following",
          sectionTitle: "Following",
          layoutType: 4,
          spanCount: 4,
          categories: artists,
          artists: artists,
          artistCategories: artists
        }
      ]
    });
  } catch (err) {
    return next(err);
  }
};
