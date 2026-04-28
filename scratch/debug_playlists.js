const sequelize = require('../config/database');
const { Playlist, PlaylistItem, User } = require('../models');

async function debugPlaylists() {
  try {
    const playlists = await Playlist.findAll({
      include: [
        { model: PlaylistItem, as: 'items' },
        { model: User, as: 'user' }
      ]
    });
    
    console.log('Total Playlists in DB:', playlists.length);
    playlists.forEach(p => {
      console.log(`- ID: ${p.id}, Name: ${p.name}, User: ${p.user ? p.user.name : 'Unknown'} (${p.userId}), Items: ${p.items.length}`);
    });
    
    process.exit(0);
  } catch (error) {
    console.error('Debug failed:', error);
    process.exit(1);
  }
}

debugPlaylists();
