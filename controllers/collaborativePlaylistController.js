const { CollaborativePlaylist, CollaborativePlaylistUser, CollaborativePlaylistSong, Song, User, Category, Section } = require('../models');
const crypto = require('crypto');
const { Op } = require('sequelize');

const formatEntityId = (prefix, value) => `${prefix}_${String(value).padStart(3, '0')}`;

// Create a new collaborative playlist
exports.create = async (req, res, next) => {
  try {
    const { name } = req.body;
    if (!name) return res.status(400).json({ status: false, message: 'Playlist name is required' });

    const inviteCode = crypto.randomBytes(8).toString('hex');
    
    const playlist = await CollaborativePlaylist.create({
      name,
      inviteCode,
      ownerId: req.userId
    });

    // Add owner as a member
    await CollaborativePlaylistUser.create({
      playlistId: playlist.id,
      userId: req.userId
    });

    return res.status(201).json({
      status: true,
      message: 'Collaborative playlist created',
      playlist: {
        id: formatEntityId('cpl', playlist.id),
        name: playlist.name,
        inviteCode: playlist.inviteCode,
        ownerId: playlist.ownerId
      }
    });
  } catch (err) {
    next(err);
  }
};

// Join a collaborative playlist
exports.join = async (req, res, next) => {
  try {
    const { inviteCode } = req.body;
    if (!inviteCode) return res.status(400).json({ status: false, message: 'Invite code is required' });

    const playlist = await CollaborativePlaylist.findOne({ where: { inviteCode } });
    if (!playlist) return res.status(404).json({ status: false, message: 'Invalid invite code' });

    const existingMembership = await CollaborativePlaylistUser.findOne({
      where: { playlistId: playlist.id, userId: req.userId }
    });

    if (existingMembership) {
      return res.status(400).json({ status: false, message: 'Already a member of this playlist' });
    }

    await CollaborativePlaylistUser.create({
      playlistId: playlist.id,
      userId: req.userId
    });

    return res.status(200).json({
      status: true,
      message: 'Successfully joined collaborative playlist',
      playlist: {
        id: formatEntityId('cpl', playlist.id),
        name: playlist.name
      }
    });
  } catch (err) {
    next(err);
  }
};

// Get all collaborative playlists for user
exports.list = async (req, res, next) => {
  try {
    const memberships = await CollaborativePlaylistUser.findAll({
      where: { userId: req.userId },
      include: [{
        model: CollaborativePlaylist,
        as: 'playlist',
        include: [{ model: User, as: 'owner', attributes: ['id', 'userName'] }]
      }]
    });

    const checkSongId = req.query.checkSongId;

    const results = await Promise.all(memberships.map(async (m) => {
      const p = m.playlist;
      const memberCount = await CollaborativePlaylistUser.count({ where: { playlistId: p.id } });
      const songCount = await CollaborativePlaylistSong.count({ where: { playlistId: p.id } });
      
      let hasSong = false;
      if (checkSongId) {
        const existing = await CollaborativePlaylistSong.findOne({
          where: { playlistId: p.id, songId: checkSongId }
        });
        hasSong = !!existing;
      }
      
      return {
        id: formatEntityId('cpl', p.id),
        name: p.name,
        inviteCode: p.inviteCode,
        ownerId: p.ownerId,
        ownerName: p.owner ? p.owner.userName : 'Unknown',
        imageUrl: p.imageUrl,
        memberCount,
        songCount,
        isOwner: p.ownerId === req.userId,
        adminOnlyRemove: p.adminOnlyRemove,
        hasSong
      };
    }));

    return res.status(200).json({
      status: true,
      playlists: results
    });
  } catch (err) {
    next(err);
  }
};

// Get songs in a collaborative playlist
exports.getSongs = async (req, res, next) => {
  try {
    const { id } = req.params;
    const playlistId = parseInt(id.replace('cpl_', ''), 10);
    
    const membership = await CollaborativePlaylistUser.findOne({
      where: { playlistId, userId: req.userId }
    });

    if (!membership) return res.status(403).json({ status: false, message: 'Not a member of this playlist' });

    const songs = await CollaborativePlaylistSong.findAll({
      where: { playlistId },
      include: [
        { 
          model: Song, as: 'song', 
          include: [{ model: Category, as: 'category' }] 
        },
        { model: User, as: 'addedBy', attributes: ['id', 'userName'] }
      ],
      order: [['createdAt', 'DESC']]
    });

    const results = songs.map(s => {
      const song = s.song;
      if (!song) return null;
      return {
        entryId: s.id,
        songId: formatEntityId('song', song.id),
        audioName: song.audioName,
        audioUrl: song.audioUrl,
        imageUrl: song.imageUrl,
        categoryName: song.category?.categoryName || 'Single Track',
        addedById: s.addedById,
        addedByUserName: s.addedBy?.userName || 'Unknown User'
      };
    }).filter(s => s != null);

    return res.status(200).json({
      status: true,
      songs: results
    });
  } catch (err) {
    next(err);
  }
};

// Add a song to a collaborative playlist
exports.addSong = async (req, res, next) => {
  try {
    const { id } = req.params;
    const { songId } = req.body;
    const playlistId = parseInt(id.replace('cpl_', ''), 10);
    const parsedSongId = parseInt(songId.replace('song_', ''), 10);

    const membership = await CollaborativePlaylistUser.findOne({
      where: { playlistId, userId: req.userId }
    });

    if (!membership) return res.status(403).json({ status: false, message: 'Not a member of this playlist' });

    // Check if song already exists
    const existing = await CollaborativePlaylistSong.findOne({
      where: { playlistId, songId: parsedSongId }
    });

    if (existing) return res.status(400).json({ status: false, message: 'Song already in playlist' });

    await CollaborativePlaylistSong.create({
      playlistId,
      songId: parsedSongId,
      addedById: req.userId
    });

    // Update playlist imageUrl if it doesn't have one
    const playlist = await CollaborativePlaylist.findByPk(playlistId);
    if (playlist && !playlist.imageUrl) {
      const song = await Song.findByPk(parsedSongId);
      if (song && song.imageUrl) {
        playlist.imageUrl = song.imageUrl;
        await playlist.save();
      }
    }

    return res.status(201).json({ status: true, message: 'Song added to playlist' });
  } catch (err) {
    next(err);
  }
};

// Remove a song
exports.removeSong = async (req, res, next) => {
  try {
    const { id, entryId } = req.params;
    const playlistId = parseInt(id.replace('cpl_', ''), 10);
    
    const entry = await CollaborativePlaylistSong.findOne({
      where: { id: entryId, playlistId }
    });

    if (!entry) return res.status(404).json({ status: false, message: 'Song not found in playlist' });

    const playlist = await CollaborativePlaylist.findByPk(playlistId);

    if (playlist.adminOnlyRemove) {
      if (playlist.ownerId !== req.userId) {
        return res.status(403).json({ status: false, message: 'Only the admin can remove songs' });
      }
    } else {
      // Can only delete if user is the one who added it, OR user is the owner of the playlist
      if (entry.addedById !== req.userId && playlist.ownerId !== req.userId) {
        return res.status(403).json({ status: false, message: 'Cannot remove songs added by other members unless you are the admin' });
      }
    }

    await entry.destroy();
    return res.status(200).json({ status: true, message: 'Song removed' });
  } catch (err) {
    next(err);
  }
};

// Get members of a collaborative playlist
exports.getMembers = async (req, res, next) => {
  try {
    const { id } = req.params;
    const playlistId = parseInt(id.replace('cpl_', ''), 10);
    
    const membership = await CollaborativePlaylistUser.findOne({
      where: { playlistId, userId: req.userId }
    });

    if (!membership) return res.status(403).json({ status: false, message: 'Not a member of this playlist' });

    const playlist = await CollaborativePlaylist.findByPk(playlistId);
    if (!playlist) return res.status(404).json({ status: false, message: 'Playlist not found' });

    const members = await CollaborativePlaylistUser.findAll({
      where: { playlistId },
      include: [{ model: User, as: 'user', attributes: ['id', 'userName'] }]
    });

    const results = members.map(m => ({
      id: m.userId,
      userName: m.user ? m.user.userName : 'Unknown User',
      isOwner: m.userId === playlist.ownerId
    }));

    return res.status(200).json({
      status: true,
      adminOnlyRemove: playlist.adminOnlyRemove,
      members: results
    });
  } catch (err) {
    next(err);
  }
};

// Update settings
exports.updateSettings = async (req, res, next) => {
  try {
    const { id } = req.params;
    const { adminOnlyRemove } = req.body;
    const playlistId = parseInt(id.replace('cpl_', ''), 10);

    const playlist = await CollaborativePlaylist.findByPk(playlistId);
    if (!playlist) return res.status(404).json({ status: false, message: 'Playlist not found' });

    if (playlist.ownerId !== req.userId) {
      return res.status(403).json({ status: false, message: 'Only admin can change settings' });
    }

    if (typeof adminOnlyRemove !== 'undefined') {
      playlist.adminOnlyRemove = adminOnlyRemove;
    }

    await playlist.save();

    return res.status(200).json({ status: true, message: 'Settings updated successfully' });
  } catch (err) {
    next(err);
  }
};
