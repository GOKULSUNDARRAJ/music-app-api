const { Playlist, PlaylistItem, Category, Song, Like } = require('../models');
const { Op } = require('sequelize');

const formatEntityId = (prefix, id) => `${prefix}_${String(id).padStart(3, '0')}`;

// ─── Create a Named Playlist ──────────────────────────────────────────────────
exports.createPlaylist = async (req, res, next) => {
  try {
    const { name } = req.body;
    const userId = req.userId;
    const image = req.file ? `/uploads/${req.file.filename}` : null;

    if (!name) {
      return res.status(400).json({ status: false, message: 'Playlist name is required' });
    }

    const playlist = await Playlist.create({
      userId,
      name,
      image
    });

    return res.json({
      status: true,
      success: true,
      message: 'Playlist created successfully',
      playlistId: playlist.id,
      categoryId: `cat_playlist_${playlist.id}`,
      playlist: {
        id: playlist.id,
        name: playlist.name,
        image: playlist.image
      }
    });
  } catch (err) {
    return next(err);
  }
};

// ─── Add Item to a Specific Playlist ──────────────────────────────────────────
exports.addItemToPlaylist = async (req, res, next) => {
  try {
    let { playlistId, categoryId, songId } = req.body;
    const userId = req.userId;

    if (!playlistId) {
      return res.status(400).json({ status: false, message: 'playlistId is required' });
    }

    if (!categoryId && !songId) {
      return res.status(400).json({ status: false, message: 'categoryId or songId is required' });
    }

    // Handle formatted IDs
    if (typeof categoryId === 'string' && categoryId.startsWith('cat_')) {
      categoryId = parseInt(categoryId.split('_')[1], 10);
    }
    if (typeof songId === 'string' && songId.startsWith('song_')) {
      songId = parseInt(songId.split('_')[1], 10);
    }

    // Verify playlist belongs to user
    const playlist = await Playlist.findOne({ where: { id: playlistId, userId } });
    if (!playlist) {
      return res.status(404).json({ status: false, message: 'Playlist not found' });
    }

    // Add item
    await PlaylistItem.create({
      playlistId,
      categoryId,
      songId
    });

    return res.json({
      status: true,
      success: true,
      message: 'Added to playlist successfully',
      isAdded: true
    });
  } catch (err) {
    return next(err);
  }
};

// ─── Get All User Playlists (Simple List) ─────────────────────────────────────
exports.getPlaylistSimple = async (req, res, next) => {
  try {
    const userId = req.userId;
    const playlists = await Playlist.findAll({
      where: { userId },
      order: [['createdAt', 'DESC']]
    });

    return res.json({
      status: true,
      success: true,
      playlists: playlists.map(p => ({
        id: p.id,
        name: p.name,
        image: p.image || 'https://via.placeholder.com/150'
      }))
    });
  } catch (err) {
    return next(err);
  }
};

// ─── Get All User Playlists (Nested Sections format for Android) ──────────────
exports.getPlaylistList = async (req, res, next) => {
  try {
    const userId = req.userId;

    // Fetch all playlists for the user
    const playlists = await Playlist.findAll({
      where: { userId },
      include: [
        {
          model: PlaylistItem,
          as: 'items',
          include: [
            { 
              model: Category, 
              as: 'category', 
              include: [{ model: Song, as: 'songs' }] 
            },
            { 
              model: Song, 
              as: 'song' 
            }
          ]
        }
      ],
      order: [['createdAt', 'DESC']]
    });

    // Fetch user likes to populate isLiked field
    const userLikes = await Like.findAll({ where: { userId } });
    const likedCategoryIds = new Set(userLikes.filter(l => l.categoryId).map(l => formatEntityId('cat', l.categoryId)));
    const likedSongIds = new Set(userLikes.filter(l => l.songId).map(l => formatEntityId('song', l.songId)));

    const protocol = req.protocol;
    const host = req.get('host');
    const baseUrl = `${protocol}://${host}`;

    // Filter and Map each playlist to a Category object inside a single section
    const allCategories = [];
    const emptyPlaylistIds = [];

    for (const playlist of playlists) {
      const categoriesInside = [];

      playlist.items.forEach(item => {
        if (item.category) {
          const catId = formatEntityId('cat', item.category.id);
          categoriesInside.push({
            categoryId: catId,
            categoryName: item.category.categoryName,
            categoryImage: item.category.categoryImage ? (item.category.categoryImage.startsWith('http') ? item.category.categoryImage : `${baseUrl}${item.category.categoryImage}`) : '',
            adapterType: 1,
            isLiked: likedCategoryIds.has(catId),
            songs: (item.category.songs || []).map(s => {
              const sId = formatEntityId('song', s.id);
              return {
                songId: sId,
                audioName: s.audioName,
                audioUrl: s.audioUrl ? (s.audioUrl.startsWith('http') ? s.audioUrl : `${baseUrl}${s.audioUrl}`) : '',
                category: item.category.categoryName,
                imageUrl: s.imageUrl ? (s.imageUrl.startsWith('http') ? s.imageUrl : `${baseUrl}${s.imageUrl}`) : '',
                categoryId: catId,
                isLiked: likedSongIds.has(sId)
              };
            })
          });
        } else if (item.song) {
          const catId = formatEntityId('cat', item.song.categoryId);
          const sId = formatEntityId('song', item.song.id);
          categoriesInside.push({
            categoryId: catId,
            categoryName: item.song.audioName,
            categoryImage: item.song.imageUrl ? (item.song.imageUrl.startsWith('http') ? item.song.imageUrl : `${baseUrl}${item.song.imageUrl}`) : '',
            adapterType: 1,
            isLiked: likedSongIds.has(sId),
            songs: [{
              songId: sId,
              audioName: item.song.audioName,
              audioUrl: item.song.audioUrl ? (item.song.audioUrl.startsWith('http') ? item.song.audioUrl : `${baseUrl}${item.song.audioUrl}`) : '',
              category: "Playlist Song",
              imageUrl: item.song.imageUrl ? (item.song.imageUrl.startsWith('http') ? item.song.imageUrl : `${baseUrl}${item.song.imageUrl}`) : '',
              categoryId: catId,
              isLiked: likedSongIds.has(sId)
            }]
          });
        }
      });

      const allSongs = categoriesInside.flatMap(c => c.songs);

      // Add playlist even if it is empty
      allCategories.push({
        categoryId: `cat_playlist_${playlist.id}`,
        categoryName: playlist.name,
        categoryImage: playlist.image ? (playlist.image.startsWith('http') ? playlist.image : `${baseUrl}${playlist.image}`) : 'https://via.placeholder.com/150',
        adapterType: 6,
        isPlaylist: true,
        songs: allSongs,
        songs_count: allSongs.length
      });
    }

    return res.json({
      status: true,
      success: true,
      result: "true",
      sections: [
        {
          sectionId: "sec_library",
          sectionTitle: "My Library",
          layoutType: 2,
          spanCount: 2,
          categories: allCategories
        }
      ]
    });
  } catch (err) {
    return next(err);
  }
};

// ─── Get User Playlists as Categories (Special Layout for Android) ─────────────
exports.getPlaylistCategories = async (req, res, next) => {
  try {
    const userId = req.userId;

    // Fetch all playlists for the user
    const playlists = await Playlist.findAll({
      where: { userId },
      include: [
        {
          model: PlaylistItem,
          as: 'items',
          include: [
            { 
              model: Category, 
              as: 'category', 
              include: [{ model: Song, as: 'songs' }] 
            },
            { 
              model: Song, 
              as: 'song' 
            }
          ]
        }
      ],
      order: [['createdAt', 'DESC']]
    });

    // Fetch user likes to populate isLiked field
    const userLikes = await Like.findAll({ where: { userId } });
    const likedCategoryIds = new Set(userLikes.filter(l => l.categoryId).map(l => formatEntityId('cat', l.categoryId)));
    const likedSongIds = new Set(userLikes.filter(l => l.songId).map(l => formatEntityId('song', l.songId)));

    const protocol = req.protocol;
    const host = req.get('host');
    const baseUrl = `${protocol}://${host}`;

    const allCategories = [];

    for (const playlist of playlists) {
      const categoriesInside = [];

      playlist.items.forEach(item => {
        if (item.category) {
          const catId = formatEntityId('cat', item.category.id);
          categoriesInside.push({
            categoryId: catId,
            categoryName: item.category.categoryName,
            categoryImage: item.category.categoryImage ? (item.category.categoryImage.startsWith('http') ? item.category.categoryImage : `${baseUrl}${item.category.categoryImage}`) : '',
            adapterType: 6, // Requested
            isLiked: likedCategoryIds.has(catId),
            songs: (item.category.songs || []).map(s => {
              const sId = formatEntityId('song', s.id);
              return {
                songId: sId,
                audioName: s.audioName,
                audioUrl: s.audioUrl ? (s.audioUrl.startsWith('http') ? s.audioUrl : `${baseUrl}${s.audioUrl}`) : '',
                category: item.category.categoryName,
                imageUrl: s.imageUrl ? (s.imageUrl.startsWith('http') ? s.imageUrl : `${baseUrl}${s.imageUrl}`) : '',
                categoryId: catId,
                isLiked: likedSongIds.has(sId)
              };
            })
          });
        } else if (item.song) {
          const catId = formatEntityId('cat', item.song.categoryId);
          const sId = formatEntityId('song', item.song.id);
          categoriesInside.push({
            categoryId: catId,
            categoryName: item.song.audioName,
            categoryImage: item.song.imageUrl ? (item.song.imageUrl.startsWith('http') ? item.song.imageUrl : `${baseUrl}${item.song.imageUrl}`) : '',
            adapterType: 6, // Requested
            isLiked: likedSongIds.has(sId),
            songs: [{
              songId: sId,
              audioName: item.song.audioName,
              audioUrl: item.song.audioUrl ? (item.song.audioUrl.startsWith('http') ? item.song.audioUrl : `${baseUrl}${item.song.audioUrl}`) : '',
              category: "Playlist Song",
              imageUrl: item.song.imageUrl ? (item.song.imageUrl.startsWith('http') ? item.song.imageUrl : `${baseUrl}${item.song.imageUrl}`) : '',
              categoryId: catId,
              isLiked: likedSongIds.has(sId)
            }]
          });
        }
      });

      const allSongs = categoriesInside.flatMap(c => c.songs);

      const playlistCategoryId = `cat_playlist_${playlist.id}`;
      allCategories.push({
        categoryId: playlistCategoryId,
        categoryName: playlist.name,
        categoryImage: playlist.image ? (playlist.image.startsWith('http') ? playlist.image : `${baseUrl}${playlist.image}`) : 'https://via.placeholder.com/150',
        url: playlist.name === 'My Favorites' ? `${baseUrl}/api/playlist/my` : `${baseUrl}/api/playlist/items/${playlist.id}`,
        adapterType: 6,
        isPlaylist: true,
        songs: allSongs,
        songs_count: allSongs.length
      });
    }

    return res.json({
      status: true,
      success: true,
      result: "true",
      sections: [
        {
          sectionId: "sec_user_categories",
          sectionTitle: "User Categories",
          layoutType: 4, // Requested
          spanCount: 4,   // Requested
          categories: allCategories
        }
      ]
    });
  } catch (err) {
    return next(err);
  }
};

// ─── Get Items in a Specific Playlist ─────────────────────────────────────────
exports.getPlaylistItems = async (req, res, next) => {
  try {
    const { playlistId } = req.params;
    let { songId, categoryId } = req.query;
    const userId = req.userId;

    // Handle formatted IDs from query if present
    if (typeof songId === 'string' && songId.startsWith('song_')) {
      songId = parseInt(songId.split('_')[1], 10);
    }
    if (typeof categoryId === 'string' && categoryId.startsWith('cat_')) {
      categoryId = parseInt(categoryId.split('_')[1], 10);
    }

    // If songId or categoryId is provided, perform a status check instead of returning all items
    if (songId || categoryId) {
      const where = { playlistId };
      if (songId) where.songId = songId;
      if (categoryId) where.categoryId = categoryId;

      const existing = await PlaylistItem.findOne({ where });
      const exists = !!existing;
      return res.json({
        status: true,
        success: true,
        result: exists ? "true" : "false",
        isInPlaylist: exists,
        isAdded: exists
      });
    }

    const playlist = await Playlist.findOne({ where: { id: playlistId, userId } });
    if (!playlist) {
      return res.status(404).json({ status: false, message: 'Playlist not found' });
    }

    const items = await PlaylistItem.findAll({
      where: { playlistId },
      include: [
        { model: Category, as: 'category', include: [{ model: Song, as: 'songs' }] },
        { model: Song, as: 'song' }
      ]
    });

    // Fetch user likes to populate isLiked field
    const userLikes = await Like.findAll({ where: { userId } });
    const likedCategoryIds = new Set(userLikes.filter(l => l.categoryId).map(l => formatEntityId('cat', l.categoryId)));
    const likedSongIds = new Set(userLikes.filter(l => l.songId).map(l => formatEntityId('song', l.songId)));

    const categories = [];
    const protocol = req.protocol;
    const host = req.get('host');
    const baseUrl = `${protocol}://${host}`;

    const adapterType = req.query.adapterType ? parseInt(req.query.adapterType, 10) : 1;
    const layoutType = req.query.layoutType ? parseInt(req.query.layoutType, 10) : 1;
    const spanCount = req.query.spanCount ? parseInt(req.query.spanCount, 10) : 1;

    items.forEach(item => {
      if (item.category) {
        const catId = formatEntityId('cat', item.category.id);
        categories.push({
          categoryId: catId,
          categoryName: item.category.categoryName,
          categoryImage: item.category.categoryImage ? (item.category.categoryImage.startsWith('http') ? item.category.categoryImage : `${baseUrl}${item.category.categoryImage}`) : '',
          adapterType: adapterType, // Use dynamic adapterType
          isLiked: likedCategoryIds.has(catId),
          songs: (item.category.songs || []).map(s => {
            const sId = formatEntityId('song', s.id);
            return {
              songId: sId,
              audioName: s.audioName,
              audioUrl: s.audioUrl ? (s.audioUrl.startsWith('http') ? s.audioUrl : `${baseUrl}${s.audioUrl}`) : '',
              category: item.category.categoryName,
              imageUrl: s.imageUrl ? (s.imageUrl.startsWith('http') ? s.imageUrl : `${baseUrl}${s.imageUrl}`) : '',
              categoryId: catId,
              isLiked: likedSongIds.has(sId)
            };
          })
        });
      } else if (item.song) {
        const catId = formatEntityId('cat', item.song.categoryId);
        const sId = formatEntityId('song', item.song.id);
        
        // Check if this song's category is already added to avoid duplicates if needed, 
        // but for playlists, we usually show exactly what was added.
        categories.push({
          categoryId: catId,
          categoryName: item.song.audioName, // For single songs, use song name as category name
          categoryImage: item.song.imageUrl ? (item.song.imageUrl.startsWith('http') ? item.song.imageUrl : `${baseUrl}${item.song.imageUrl}`) : '',
          adapterType: adapterType, // Use dynamic adapterType
          isLiked: likedSongIds.has(sId), // For single song category, use song's like status
          songs: [{
            songId: sId,
            audioName: item.song.audioName,
            audioUrl: item.song.audioUrl ? (item.song.audioUrl.startsWith('http') ? item.song.audioUrl : `${baseUrl}${item.song.audioUrl}`) : '',
            category: "Playlist Song",
            imageUrl: item.song.imageUrl ? (item.song.imageUrl.startsWith('http') ? item.song.imageUrl : `${baseUrl}${item.song.imageUrl}`) : '',
            categoryId: catId,
            isLiked: likedSongIds.has(sId)
          }]
        });
      }
    });

    const flatItems = categories.map(c => {
      // For songs directly in playlists, we use the first song's details if available
      const song = c.songs && c.songs.length > 0 ? c.songs[0] : null;
      return {
        id: c.categoryId,
        songId: song ? song.songId : c.categoryId,
        title: c.categoryName,
        artist: song ? song.category : "Various Artists",
        imageUrl: c.categoryImage,
        songUrl: song ? song.audioUrl : ""
      };
    });

    return res.json({
      status: true,
      success: true,
      result: "true",
      items: flatItems, // Support for Android PlaylistDetailsResponse
      sections: [
        {
          sectionId: "sec_playlist_items",
          sectionTitle: playlist.name,
          layoutType: layoutType, // Use dynamic layoutType
          spanCount: spanCount,   // Use dynamic spanCount
          url: playlist.name === 'My Favorites' ? `${baseUrl}/api/playlist/my` : `${baseUrl}/api/playlist/items/${playlist.id}`,
          categories: categories
        }
      ]
    });
  } catch (err) {
    return next(err);
  }
};

// ─── Legacy Toggle (Backward Compatibility) ───────────────────────────────────
exports.togglePlaylistLegacy = async (req, res, next) => {
  try {
    let { categoryId, songId } = req.body;
    const userId = req.userId;

    // Find or create "My Favorites" playlist
    const [defaultPlaylist] = await Playlist.findOrCreate({
      where: { userId, name: 'My Favorites' },
      defaults: { image: 'https://via.placeholder.com/150' }
    });

    if (typeof categoryId === 'string' && categoryId.startsWith('cat_')) {
      categoryId = parseInt(categoryId.split('_')[1], 10);
    }
    if (typeof songId === 'string' && songId.startsWith('song_')) {
      songId = parseInt(songId.split('_')[1], 10);
    }

    const where = { playlistId: defaultPlaylist.id };
    if (categoryId) where.categoryId = categoryId;
    if (songId) where.songId = songId;

    const existing = await PlaylistItem.findOne({ where });

    if (existing) {
      await existing.destroy();
      return res.json({ status: true, success: true, result: "true", message: 'Removed from playlist', isInPlaylist: false, isLiked: false });
    } else {
      await PlaylistItem.create({
        playlistId: defaultPlaylist.id,
        categoryId,
        songId
      });
      return res.json({ status: true, success: true, result: "true", message: 'Added to playlist', isInPlaylist: true, isLiked: true });
    }
  } catch (err) {
    return next(err);
  }
};

// ─── Toggle item in "My Playlist" (Quick Toggle) ───────────────────────────────
exports.toggleMyPlaylist = exports.togglePlaylistLegacy;

// ─── Delete a Specific Playlist ───────────────────────────────────────────────
exports.deletePlaylist = async (req, res, next) => {
  try {
    const { playlistId } = req.params;
    const userId = req.userId;

    const playlist = await Playlist.findOne({ where: { id: playlistId, userId } });
    if (!playlist) {
      return res.status(404).json({ status: false, message: 'Playlist not found or access denied' });
    }

    await playlist.destroy();

    return res.json({
      status: true,
      success: true,
      message: 'Playlist deleted successfully'
    });
  } catch (err) {
    return next(err);
  }
};

// ─── Delete All Playlists for User ───────────────────────────────────────────
exports.deleteAllPlaylists = async (req, res, next) => {
  try {
    const userId = req.userId;

    await Playlist.destroy({ where: { userId } });

    return res.json({
      status: true,
      success: true,
      message: 'All playlists deleted successfully',
      sections: [] // Return empty sections for immediate UI update
    });
  } catch (err) {
    return next(err);
  }
};
exports.getMyPlaylist = async (req, res, next) => {
  try {
    const userId = req.userId;
    const playlist = await Playlist.findOne({ where: { userId, name: 'My Favorites' } });
    
    if (!playlist) {
      return res.json({ status: true, success: true, result: "true", sections: [] });
    }

    req.params.playlistId = playlist.id;
    // Set requested layout settings for My Favorites
    req.query.layoutType = "4";
    req.query.spanCount = "4";
    req.query.adapterType = "6";
    
    return exports.getPlaylistItems(req, res, next);
  } catch (err) {
    return next(err);
  }
};

// ─── Check if item is in a specific playlist (Handles both specific and legacy My Favorites) ─
exports.checkPlaylistStatus = async (req, res, next) => {
  try {
    let { playlistId } = req.params;
    let { songId, categoryId } = req.query;
    const userId = req.userId;

    // Handle legacy case: if playlistId is actually a categoryId string (e.g., 'cat_018')
    // it means check if that category is in "My Favorites"
    if (typeof playlistId === 'string' && playlistId.startsWith('cat_')) {
      const targetCategoryId = parseInt(playlistId.split('_')[1], 10);
      const myFavPlaylist = await Playlist.findOne({ where: { userId, name: 'My Favorites' } });
      if (!myFavPlaylist) {
        return res.json({ status: true, success: true, result: "false", isInPlaylist: false, isAdded: false });
      }
      const existing = await PlaylistItem.findOne({ 
        where: { playlistId: myFavPlaylist.id, categoryId: targetCategoryId } 
      });
      const exists = !!existing;
      return res.json({ 
        status: true, 
        success: true, 
        result: exists ? "true" : "false", 
        isInPlaylist: exists,
        isAdded: exists 
      });
    }

    // Normal case: check if item is in specific numeric playlistId
    if (typeof songId === 'string' && songId.startsWith('song_')) {
      songId = parseInt(songId.split('_')[1], 10);
    }
    if (typeof categoryId === 'string' && categoryId.startsWith('cat_')) {
      categoryId = parseInt(categoryId.split('_')[1], 10);
    }

    const playlist = await Playlist.findOne({ where: { id: playlistId, userId } });
    if (!playlist) {
      return res.status(404).json({ status: false, message: 'Playlist not found' });
    }

    const where = { playlistId: playlist.id };
    if (songId) where.songId = songId;
    if (categoryId) where.categoryId = categoryId;

    const existing = await PlaylistItem.findOne({ where });
    const exists = !!existing;
    return res.json({ 
      status: true, 
      success: true, 
      result: exists ? "true" : "false", 
      isInPlaylist: exists,
      isAdded: exists 
    });
  } catch (err) {
    return next(err);
  }
};