const { sequelize, User, Category, Song, Like, Section, Playlist, PlaylistItem } = require('../models');
const { getPlaylistCategories } = require('../controllers/playlistController');

async function test() {
  try {
    console.log('Syncing database...');
    await sequelize.sync();
    
    // Find or create a test user
    let user = await User.findOne({ where: { userEmail: 'test@example.com' } });
    if (!user) {
      user = await User.create({ 
        userName: 'Test User', 
        userEmail: 'test@example.com', 
        userMobile: '1234567890',
        userPassword: 'password' 
      });
      console.log('Created test user.');
    }

    // Ensure we have a section
    let section = await Section.findOne();
    if (!section) {
      section = await Section.create({ sectionTitle: 'Test Section', layoutType: 1, spanCount: 1, contentType: 'home' });
      console.log('Created test section.');
    }

    // Ensure we have a category
    let cat = await Category.findOne({ where: { categoryName: 'Achcham Yenbadhu Madamaiyada songs' } });
    if (!cat) {
        cat = await Category.create({ 
          categoryName: 'Achcham Yenbadhu Madamaiyada songs', 
          categoryImage: '/uploads/test.jfif',
          sectionId: section.id 
        });
        console.log('Created test category.');
    }

    // Ensure we have some songs
    let song = await Song.findOne({ where: { audioName: 'Avalum-Naanum', categoryId: cat.id } });
    if (!song) {
        await Song.create({
            audioName: 'Avalum-Naanum',
            audioUrl: '/uploads/song1.mp3',
            imageUrl: '/uploads/img1.jfif',
            categoryId: cat.id
        });
        console.log('Created test song.');
    }

    // Create a playlist
    const [playlist] = await Playlist.findOrCreate({ 
      where: { userId: user.id, name: 'Test Playlist' } 
    });
    console.log('Playlist ensured.');

    // Add category to playlist
    await PlaylistItem.findOrCreate({ 
      where: { playlistId: playlist.id, categoryId: cat.id } 
    });
    console.log('Added category to playlist.');

    // Mock req and res
    const req = {
      userId: user.id,
      protocol: 'http',
      get: (header) => header === 'host' ? 'localhost:3000' : ''
    };
    const res = {
      json: (data) => {
          console.log('\n--- API RESPONSE ---');
          console.log(JSON.stringify(data, null, 2));
          console.log('--------------------\n');
      }
    };
    const next = (err) => {
        console.error('Next called with error:');
        console.error(err);
    };

    console.log('Calling getPlaylistCategories...');
    await getPlaylistCategories(req, res, next);

  } catch (err) {
    console.error('Test failed:');
    console.error(err);
  } finally {
    console.log('Done.');
    process.exit();
  }
}

test();
