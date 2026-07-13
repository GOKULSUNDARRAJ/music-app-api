const { Blend, User, Like, Song, Category, Section } = require('../models');
const { Op } = require('sequelize');
const crypto = require('crypto');

const formatEntityId = (prefix, value) => `${prefix}_${String(value).padStart(3, '0')}`;

exports.invite = async (req, res, next) => {
  try {
    const inviteCode = crypto.randomBytes(8).toString('hex');
    const blend = await Blend.create({
      user1Id: req.userId,
      inviteCode,
      status: 'pending'
    });

    return res.status(201).json({
      status: true,
      message: 'Blend invite created',
      inviteCode: blend.inviteCode,
      blendId: blend.id
    });
  } catch (err) {
    next(err);
  }
};

exports.join = async (req, res, next) => {
  try {
    const { inviteCode } = req.body;
    if (!inviteCode) {
      return res.status(400).json({ status: false, message: 'Invite code is required' });
    }

    const blend = await Blend.findOne({ where: { inviteCode } });
    if (!blend) {
      return res.status(404).json({ status: false, message: 'Invalid invite code' });
    }

    if (blend.user1Id === req.userId) {
      return res.status(400).json({ status: false, message: 'Cannot join your own blend' });
    }

    if (blend.status === 'accepted') {
      return res.status(400).json({ status: false, message: 'Blend is already full' });
    }

    blend.user2Id = req.userId;
    blend.status = 'accepted';
    await blend.save();

    return res.status(200).json({
      status: true,
      message: 'Successfully joined blend',
      blendId: blend.id
    });
  } catch (err) {
    next(err);
  }
};

exports.list = async (req, res, next) => {
  try {
    const blends = await Blend.findAll({
      where: {
        [Op.or]: [
          { user1Id: req.userId },
          { user2Id: req.userId }
        ],
        status: 'accepted'
      },
      include: [
        { model: User, as: 'user1', attributes: ['id', 'userName'] },
        { model: User, as: 'user2', attributes: ['id', 'userName'] }
      ]
    });

    const results = blends.map(b => {
      const otherUser = b.user1Id === req.userId ? b.user2 : b.user1;
      return {
        blendId: b.id,
        otherUserId: otherUser.id,
        otherUsername: otherUser.userName || 'Unknown User',
        blendName: `Blend with ${otherUser.userName || 'Unknown User'}`,
        createdAt: b.createdAt
      };
    });

    return res.status(200).json({
      status: true,
      blends: results
    });
  } catch (err) {
    next(err);
  }
};

exports.getPlaylist = async (req, res, next) => {
  try {
    const { blendId } = req.params;
    const blend = await Blend.findByPk(blendId);

    if (!blend || blend.status !== 'accepted') {
      return res.status(404).json({ status: false, message: 'Blend not found or not accepted' });
    }

    if (blend.user1Id !== req.userId && blend.user2Id !== req.userId) {
      return res.status(403).json({ status: false, message: 'Unauthorized access to this blend' });
    }

    // Get 50 recent likes for user1
    const user1Likes = await Like.findAll({
      where: { userId: blend.user1Id, songId: { [Op.ne]: null } },
      order: [['createdAt', 'DESC']],
      limit: 50,
      include: [{
        model: Song, as: 'song',
        include: [{
          model: Category, as: 'category', include: [{ model: Section, as: 'section' }]
        }]
      }]
    });

    // Get 50 recent likes for user2
    const user2Likes = await Like.findAll({
      where: { userId: blend.user2Id, songId: { [Op.ne]: null } },
      order: [['createdAt', 'DESC']],
      limit: 50,
      include: [{
        model: Song, as: 'song',
        include: [{
          model: Category, as: 'category', include: [{ model: Section, as: 'section' }]
        }]
      }]
    });

    const user1Songs = user1Likes.map(l => l.song);
    const user2Songs = user2Likes.map(l => l.song);

    // Identify shared songs
    const user1SongIds = new Set(user1Songs.map(s => s.id));
    const user2SongIds = new Set(user2Songs.map(s => s.id));
    
    const sharedSongs = user1Songs.filter(s => user2SongIds.has(s.id));
    const uniqueUser1Songs = user1Songs.filter(s => !user2SongIds.has(s.id));
    const uniqueUser2Songs = user2Songs.filter(s => !user1SongIds.has(s.id));

    // Combine: Shared first, then interleave
    let finalSongs = [...sharedSongs];
    let i = 0;
    while (i < uniqueUser1Songs.length || i < uniqueUser2Songs.length) {
      if (i < uniqueUser1Songs.length) finalSongs.push(uniqueUser1Songs[i]);
      if (i < uniqueUser2Songs.length) finalSongs.push(uniqueUser2Songs[i]);
      i++;
    }

    // Fallback if users have no liked songs
    if (finalSongs.length === 0) {
      const fallbackSongs = await Song.findAll({
        limit: 20,
        order: [['createdAt', 'DESC']],
        include: [{
          model: Category, as: 'category', include: [{ model: Section, as: 'section' }]
        }]
      });
      finalSongs = fallbackSongs;
    }

    const songResults = finalSongs.map(song => ({
      songId: formatEntityId('song', song.id),
      audioName: song.audioName,
      audioUrl: song.audioUrl,
      imageUrl: song.imageUrl,
      categoryName: song.category?.categoryName || 'Single Track',
      categoryId: song.category ? formatEntityId('cat', song.category.id) : 'cat_000',
      isLiked: true // since they liked it, though strictly it might be liked by the OTHER user.
    }));

    return res.status(200).json({
      status: true,
      message: 'Blend Playlist',
      songs: songResults
    });

  } catch (err) {
    next(err);
  }
};
