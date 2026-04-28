const { sequelize, User, Playlist, PlaylistItem, Category, Song } = require('../models');
const { getMyPlaylist } = require('../controllers/playlistController');

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
      where: { userId: user.id, name: 'My Favorites' } 
    });

    const req = {
      userId: user.id,
      protocol: 'http',
      get: (header) => header === 'host' ? 'localhost:3000' : '',
      params: {},
      query: {}
    };
    const res = {
      json: (data) => {
          console.log(JSON.stringify(data, null, 2));
      }
    };
    const next = (err) => console.error(err);

    await getMyPlaylist(req, res, next);

  } catch (err) {
    console.error(err);
  } finally {
    process.exit();
  }
}

test();
