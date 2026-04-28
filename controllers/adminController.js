const { Section, Category, Song } = require('../models');
const crypto = require('crypto');

const adminUser = process.env.ADMIN_USER || 'admin';
const adminPass = process.env.ADMIN_PASS || 'admin123';

const getToken = () => Buffer.from(`${adminUser}:${adminPass}`).toString('base64');
const formatEntityId = (prefix, value) => `${prefix}_${String(value).padStart(3, '0')}`;
const sectionDto = (section) => ({
  ...section.toJSON(),
  sectionId: formatEntityId('sec', section.id)
});
const categoryDto = (category) => ({
  ...category.toJSON(),
  categoryId: formatEntityId('cat', category.id),
  sectionIdFormatted: formatEntityId('sec', category.sectionId)
});
const songDto = (song) => ({
  ...song.toJSON(),
  songId: formatEntityId('song', song.id),
  categoryIdFormatted: formatEntityId('cat', song.categoryId)
});



exports.login = async (req, res) => {
  const { username, password } = req.body;

  if (!username || !password) {
    return res.status(400).json({
      status: false,
      error_type: '400',
      message: 'username and password are required'
    });
  }

  if (username !== adminUser || password !== adminPass) {
    return res.status(401).json({
      status: false,
      error_type: '401',
      message: 'Invalid credentials'
    });
  }

  return res.status(200).json({
    status: true,
    error_type: '200',
    message: 'You are successfully logged in.',
    response: {
      userId: 1,
      userName: adminUser,
      userEmail: process.env.ADMIN_EMAIL || 'admin@musicapp.local',
      userMobile: process.env.ADMIN_MOBILE || '',
      userCountry: process.env.ADMIN_COUNTRY || 'India',
      userCountryId: process.env.ADMIN_COUNTRY_ID || '91',
      userCreatedDate: new Date().toISOString().replace('T', ' ').slice(0, 19),
      userCountryCode: process.env.ADMIN_COUNTRY_CODE || '91',
      token_type: 'Bearer',
      expires_in: 31536000,
      access_token: getToken(),
      refresh_token: crypto.randomBytes(64).toString('hex')
    }
  });
};

exports.getDashboardCounts = async (req, res, next) => {
  try {
    const User = require('../models/User');
    const [sections, categories, songs, users] = await Promise.all([
      Section.count(),
      Category.count(),
      Song.count(),
      User.count()
    ]);

    return res.status(200).json({ sections, categories, songs, users });


  } catch (error) {
    return next(error);
  }
};

exports.createSection = async (req, res, next) => {
  try {
    const { sectionTitle, layoutType = 1, spanCount = 1, contentType = 'home' } = req.body;

    if (!sectionTitle) {
      return res.status(400).json({ message: 'sectionTitle is required' });
    }

    if (!['home', 'devotional', 'artist'].includes(contentType)) {
      return res.status(400).json({ message: 'contentType must be home, devotional, or artist' });
    }


    const section = await Section.create({ sectionTitle, layoutType, spanCount, contentType });
    return res.status(201).json(sectionDto(section));
  } catch (error) {
    return next(error);
  }
};

exports.getSections = async (req, res, next) => {
  try {
    const where = {};
    if (req.query.contentType) {
      where.contentType = req.query.contentType;
    }
    const sections = await Section.findAll({ where, order: [['id', 'DESC']] });
    return res.status(200).json(sections.map(sectionDto));
  } catch (error) {
    return next(error);
  }
};

exports.updateSection = async (req, res, next) => {
  try {
    const section = await Section.findByPk(req.params.id);

    if (!section) {
      return res.status(404).json({ message: 'Section not found' });
    }

    const { sectionTitle, layoutType, spanCount, contentType } = req.body;

    if (contentType && !['home', 'devotional', 'artist'].includes(contentType)) {
      return res.status(400).json({ message: 'contentType must be home, devotional, or artist' });
    }


    await section.update({
      sectionTitle: sectionTitle ?? section.sectionTitle,
      layoutType: layoutType ?? section.layoutType,
      spanCount: spanCount ?? section.spanCount,
      contentType: contentType ?? section.contentType
    });

    return res.status(200).json(sectionDto(section));
  } catch (error) {
    return next(error);
  }
};

exports.deleteSection = async (req, res, next) => {
  try {
    const section = await Section.findByPk(req.params.id);

    if (!section) {
      return res.status(404).json({ message: 'Section not found' });
    }

    await section.destroy();
    return res.status(200).json({ message: 'Section deleted' });
  } catch (error) {
    return next(error);
  }
};

exports.createCategory = async (req, res, next) => {
  try {
    const {
      categoryName,
      categoryImage = '',
      adapterType = 1,
      sectionId
    } = req.body;

    if (!categoryName || !sectionId) {
      return res.status(400).json({ message: 'categoryName and sectionId are required' });
    }

    const section = await Section.findByPk(sectionId);
    if (!section) {
      return res.status(400).json({ message: 'Invalid sectionId: section does not exist' });
    }

    const category = await Category.create({
      categoryName,
      categoryImage,
      adapterType,
      sectionId
    });

    return res.status(201).json(categoryDto(category));
  } catch (error) {
    return next(error);
  }
};

exports.getCategories = async (req, res, next) => {
  try {
    const where = {};
    const include = [];
    if (req.query.sectionId) {
      where.sectionId = req.query.sectionId;
    }
    if (req.query.contentType) {
      include.push({
        model: Section,
        as: 'section',
        where: { contentType: req.query.contentType },
        attributes: []
      });
    }

    const categories = await Category.findAll({
      where,
      include,
      order: [['id', 'DESC']]
    });
    return res.status(200).json(categories.map(categoryDto));
  } catch (error) {
    return next(error);
  }
};

exports.updateCategory = async (req, res, next) => {
  try {
    const category = await Category.findByPk(req.params.id);

    if (!category) {
      return res.status(404).json({ message: 'Category not found' });
    }

    const { categoryName, categoryImage, adapterType, sectionId } = req.body;

    if (sectionId) {
      const section = await Section.findByPk(sectionId);
      if (!section) {
        return res.status(400).json({ message: 'Invalid sectionId: section does not exist' });
      }
    }

    await category.update({
      categoryName: categoryName ?? category.categoryName,
      categoryImage: categoryImage ?? category.categoryImage,
      adapterType: adapterType ?? category.adapterType,
      sectionId: sectionId ?? category.sectionId
    });

    return res.status(200).json(categoryDto(category));
  } catch (error) {
    return next(error);
  }
};

exports.deleteCategory = async (req, res, next) => {
  try {
    const category = await Category.findByPk(req.params.id);

    if (!category) {
      return res.status(404).json({ message: 'Category not found' });
    }

    await category.destroy();
    return res.status(200).json({ message: 'Category deleted' });
  } catch (error) {
    return next(error);
  }
};

exports.createSong = async (req, res, next) => {
  try {
    const { audioName, audioUrl, imageUrl = '', categoryId } = req.body;

    if (!audioName || !audioUrl || !categoryId) {
      return res.status(400).json({ message: 'audioName, audioUrl and categoryId are required' });
    }

    const category = await Category.findByPk(categoryId);
    if (!category) {
      return res.status(400).json({ message: 'Invalid categoryId: category does not exist' });
    }

    const song = await Song.create({ audioName, audioUrl, imageUrl, categoryId });


    return res.status(201).json(songDto(song));
  } catch (error) {
    return next(error);
  }
};

exports.getSongs = async (req, res, next) => {
  try {
    const where = {};
    const include = [];

    if (req.query.categoryId) {
      where.categoryId = req.query.categoryId;
    }

    if (req.query.contentType) {
      include.push({
        model: Category,
        as: 'category',
        required: true,
        attributes: [],
        include: [
          {
            model: Section,
            as: 'section',
            required: true,
            attributes: [],
            where: { contentType: req.query.contentType }
          }
        ]
      });
    }

    const songs = await Song.findAll({
      where,
      include,
      order: [['id', 'DESC']]
    });
    return res.status(200).json(songs.map(songDto));
  } catch (error) {
    return next(error);
  }
};

exports.updateSong = async (req, res, next) => {
  try {
    const song = await Song.findByPk(req.params.id);

    if (!song) {
      return res.status(404).json({ message: 'Song not found' });
    }

    const { audioName, audioUrl, imageUrl, categoryId } = req.body;

    if (categoryId) {
      const category = await Category.findByPk(categoryId);
      if (!category) {
        return res.status(400).json({ message: 'Invalid categoryId: category does not exist' });
      }
    }

    await song.update({
      audioName: audioName ?? song.audioName,
      audioUrl: audioUrl ?? song.audioUrl,
      imageUrl: imageUrl ?? song.imageUrl,
      categoryId: categoryId ?? song.categoryId
    });



    return res.status(200).json(songDto(song));
  } catch (error) {
    return next(error);
  }
};

exports.deleteSong = async (req, res, next) => {
  try {
    const song = await Song.findByPk(req.params.id);

    if (!song) {
      return res.status(404).json({ message: 'Song not found' });
    }

    await song.destroy();
    return res.status(200).json({ message: 'Song deleted' });
  } catch (error) {
    return next(error);
  }
};

exports.uploadSongs = async (req, res, next) => {
  const transaction = await require('../models').sequelize.transaction();
  try {
    const { categoryId, imageUrl } = req.body;
    const files = req.files;

    if (!categoryId) {
      return res.status(400).json({ message: 'Category ID is required' });
    }
    if (!files || files.length === 0) {
      return res.status(400).json({ message: 'No files uploaded' });
    }

    const { Song, Category } = require('../models');
    
    // Verify category exists
    const category = await Category.findByPk(categoryId);
    if (!category) {
      await transaction.rollback();
      return res.status(404).json({ message: `Category ${categoryId} not found` });
    }

    const baseUrl = process.env.BASE_URL || `${req.protocol}://${req.get('host')}`;
    
    const songData = [];
    // If we have files, we assume the first is audio, and the rest (if any) could be image or more audio.
    // In 'Single-Single' mode, we send 1 or 2 files.
    
    const audioFile = files.find(f => f.mimetype.startsWith('audio/'));
    const imageFile = files.find(f => f.mimetype.startsWith('image/'));

    if (!audioFile) {
      await transaction.rollback();
      return res.status(400).json({ message: 'No audio file found in upload' });
    }

    const newSong = {
      audioName: req.body.audioName || audioFile.originalname.replace(/\.[^/.]+$/, ""),
      audioUrl: `${baseUrl}/uploads/${audioFile.filename}`,
      imageUrl: imageFile ? `${baseUrl}/uploads/${imageFile.filename}` : (req.body.imageUrl || ''),
      categoryId: Number(categoryId)
    };

    const song = await Song.create(newSong, { transaction });
    await transaction.commit();

    return res.status(201).json({
      message: `Song uploaded successfully`,
      song: songDto(song)
    });
  } catch (error) {
    console.error('Upload Error:', error);
    if (transaction) await transaction.rollback();
    return next(error);
  }
};

exports.bulkCreateSongs = async (req, res, next) => {
  const transaction = await require('../models').sequelize.transaction();
  try {
    const { songs } = req.body;
    
    if (!Array.isArray(songs) || songs.length === 0) {
      await transaction.rollback();
      return res.status(400).json({ message: 'Songs array is required' });
    }

    // Basic validation
    const categoryIds = [...new Set(songs.map(s => Number(s.categoryId)))];
    const categories = await Category.findAll({ where: { id: categoryIds }, transaction });
    if (categories.length !== categoryIds.length) {
      await transaction.rollback();
      return res.status(400).json({ message: 'One or more invalid category IDs' });
    }

    const createdSongs = await Song.bulkCreate(songs.map(s => ({
      audioName: s.audioName,
      audioUrl: s.audioUrl,
      imageUrl: s.imageUrl || '',
      categoryId: Number(s.categoryId)
    })), { transaction });

    await transaction.commit();
    return res.status(201).json(createdSongs.map(songDto));
  } catch (error) {
    await transaction.rollback();
    return next(error);
  }
};

exports.fetchUrlMetadata = async (req, res, next) => {
  try {
    const { url } = req.query;
    if (!url) {
      return res.status(400).json({ message: 'URL query parameter is required' });
    }

    const response = await fetch(url, { 
      headers: { 'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)' } 
    });
    
    if (!response.ok) {
      return res.status(response.status).json({ message: 'Failed to fetch' });
    }

    const html = await response.text();
    
    const titleMatch = html.match(/<title>([^<]*)<\/title>/i);
    const title = titleMatch ? titleMatch[1].trim() : '';

    const ogImageMatch1 = html.match(/<meta[^>]*property=["']og:image["'][^>]*content=["'](.*?)["']/i);
    const ogImageMatch2 = html.match(/<meta[^>]*content=["'](.*?)["'][^>]*property=["']og:image["']/i);
    let imageUrl = '';
    if (ogImageMatch1) imageUrl = ogImageMatch1[1];
    else if (ogImageMatch2) imageUrl = ogImageMatch2[1];

    return res.status(200).json({ title, imageUrl });
  } catch (error) {
    console.error('fetchUrlMetadata error', error);
    return res.status(500).json({ message: 'Error fetching metadata' });
  }
};

exports.getUsers = async (req, res, next) => {
  try {
    const User = require('../models/User');
    const users = await User.findAll({
      order: [['id', 'DESC']],
      attributes: { exclude: ['userPassword'] }
    });
    return res.status(200).json(users);
  } catch (error) {
    return next(error);
  }
};

exports.deleteUser = async (req, res, next) => {
  try {
    const User = require('../models/User');
    const user = await User.findByPk(req.params.id);

    if (!user) {
      return res.status(404).json({ message: 'User not found' });
    }

    await user.destroy();
    return res.status(200).json({ message: 'User deleted' });
  } catch (error) {
    return next(error);
  }
};


