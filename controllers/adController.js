const { Advertisement } = require('../models');
const { Op } = require('sequelize');

exports.getRandomAd = async (req, res, next) => {
  try {
    const activeAds = await Advertisement.findAll({
      where: { active: true }
    });

    if (!activeAds || activeAds.length === 0) {
      return res.status(200).json({
        status: true,
        hasAd: false,
        ad: null
      });
    }

    // Pick a random ad
    const randomIndex = Math.floor(Math.random() * activeAds.length);
    const randomAd = activeAds[randomIndex];

    return res.status(200).json({
      status: true,
      hasAd: true,
      response: {
        id: randomAd.id,
        adTitle: randomAd.adTitle,
        adType: randomAd.adType,
        mediaUrl: randomAd.mediaUrl,
        imageUrl: randomAd.imageUrl,
        redirectUrl: randomAd.redirectUrl
      }
    });
  } catch (error) {
    return next(error);
  }
};

exports.getAds = async (req, res, next) => {
  try {
    const ads = await Advertisement.findAll({
      order: [['createdAt', 'DESC']]
    });
    return res.status(200).json(ads);
  } catch (error) {
    return next(error);
  }
};

exports.createAd = async (req, res, next) => {
  try {
    const { adTitle, adType, redirectUrl } = req.body;
    let mediaUrl = '';
    let imageUrl = '';

    if (req.files) {
      if (req.files.media && req.files.media.length > 0) {
        mediaUrl = req.files.media[0].publicUrl || `/uploads/${req.files.media[0].filename}`;
      }
      if (req.files.image && req.files.image.length > 0) {
        imageUrl = req.files.image[0].publicUrl || `/uploads/${req.files.image[0].filename}`;
      }
    }

    if (!mediaUrl) {
      return res.status(400).json({ status: false, message: 'Media file is required' });
    }

    const newAd = await Advertisement.create({
      adTitle,
      adType: adType || 'audio',
      mediaUrl,
      imageUrl,
      redirectUrl,
      active: true
    });

    return res.status(201).json(newAd);
  } catch (error) {
    return next(error);
  }
};

exports.deleteAd = async (req, res, next) => {
  try {
    const { id } = req.params;
    const ad = await Advertisement.findByPk(id);
    if (!ad) {
      return res.status(404).json({ status: false, message: 'Ad not found' });
    }

    await ad.destroy();
    return res.status(200).json({ status: true, message: 'Ad deleted successfully' });
  } catch (error) {
    return next(error);
  }
};
