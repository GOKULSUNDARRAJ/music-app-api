const { sequelize, User, Playlist, PlaylistItem, Song, Category } = require('../models');
const { getPlaylistItems, checkPlaylistStatus } = require('../controllers/playlistController');

async function test() {
  try {
    await sequelize.sync();
    
    let user = await User.findOne({ where: { userEmail: 'test@example.com' } });
    if (!user) {
        user = await User.create({ 
            userName: 'Test User', 
            userEmail: 'test@example.com', 
            userMobile: '1234567890',
            userPassword: 'password' 
          });
    }

    const [playlist] = await Playlist.findOrCreate({ 
      where: { userId: user.id, name: 'Status Test Playlist' } 
    });

    const { Section } = require('../models');
    const [section] = await Section.findOrCreate({
        where: { sectionTitle: 'Test Section' },
        defaults: { layoutType: 1, spanCount: 1, contentType: 'home' }
    });

    const [cat] = await Category.findOrCreate({
        where: { categoryName: 'Test Category' },
        defaults: { sectionId: section.id }
    });

    const [song] = await Song.findOrCreate({
        where: { audioName: 'Status Test Song' },
        defaults: { audioUrl: '/test.mp3', imageUrl: '/test.jfif', categoryId: cat.id }
    });

    // Add song to playlist
    await PlaylistItem.findOrCreate({
        where: { playlistId: playlist.id, songId: song.id }
    });

    const req = {
      userId: user.id,
      protocol: 'http',
      get: (header) => header === 'host' ? 'localhost:3000' : '',
      params: { playlistId: playlist.id },
      query: { songId: `song_99999` }
    };
    const res = {
      json: (data) => {
          console.log('--- API RESPONSE (Check Song) ---');
          console.log(JSON.stringify(data, null, 2));
      }
    };
    const next = (err) => console.error(err);

    console.log('Testing getPlaylistItems with songId query...');
    await getPlaylistItems(req, res, next);

    console.log('\nTesting checkPlaylistStatus...');
    await checkPlaylistStatus(req, res, next);

  } catch (err) {
    console.error(err);
  } finally {
    process.exit();
  }
}

test();
