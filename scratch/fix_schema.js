const sequelize = require('../config/database');

async function fixSchema() {
  try {
    console.log('Starting schema fix...');
    
    // Add columns to playlists table
    try {
      await sequelize.query('ALTER TABLE playlists ADD COLUMN userId INTEGER NOT NULL AFTER id');
      console.log('Added userId to playlists');
    } catch (e) {
      console.log('userId already exists or error in playlists:', e.message);
    }

    // Add columns to playlist_items table
    try {
      await sequelize.query('ALTER TABLE playlist_items ADD COLUMN playlistId INTEGER NOT NULL AFTER id');
      console.log('Added playlistId to playlist_items');
    } catch (e) {
      console.log('playlistId already exists or error in playlist_items:', e.message);
    }

    try {
      await sequelize.query('ALTER TABLE playlist_items ADD COLUMN categoryId INTEGER NULL AFTER playlistId');
      console.log('Added categoryId to playlist_items');
    } catch (e) {
      console.log('categoryId already exists or error in playlist_items:', e.message);
    }

    try {
      await sequelize.query('ALTER TABLE playlist_items ADD COLUMN songId INTEGER NULL AFTER categoryId');
      console.log('Added songId to playlist_items');
    } catch (e) {
      console.log('songId already exists or error in playlist_items:', e.message);
    }

    console.log('Schema fix completed.');
    process.exit(0);
  } catch (error) {
    console.error('Failed to fix schema:', error);
    process.exit(1);
  }
}

fixSchema();
