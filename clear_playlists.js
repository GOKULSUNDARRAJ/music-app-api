const { Playlist, PlaylistItem } = require('./models');

async function clearPlaylists() {
  try {
    console.log('--- Clearing Playlists and Items ---');
    
    // Delete all items first (foreign key constraints)
    const itemsDeleted = await PlaylistItem.destroy({ where: {}, truncate: false });
    console.log(`Deleted ${itemsDeleted} items from playlists.`);
    
    // Delete all playlists
    const playlistsDeleted = await Playlist.destroy({ where: {}, truncate: false });
    console.log(`Deleted ${playlistsDeleted} playlists.`);
    
    console.log('--- Success: Database is now empty for playlists ---');
    process.exit(0);
  } catch (error) {
    console.error('Error clearing database:', error);
    process.exit(1);
  }
}

clearPlaylists();
