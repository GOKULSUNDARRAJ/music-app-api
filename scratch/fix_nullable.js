const sequelize = require('../config/database');

async function allowNullableColumns() {
  try {
    console.log('Starting nullable fix...');
    
    // Modify playlist_items columns to allow NULL
    try {
      await sequelize.query('ALTER TABLE playlist_items MODIFY COLUMN categoryId INTEGER NULL');
      console.log('Modified categoryId to allow NULL');
    } catch (e) {
      console.log('Error modifying categoryId:', e.message);
    }

    try {
      await sequelize.query('ALTER TABLE playlist_items MODIFY COLUMN songId INTEGER NULL');
      console.log('Modified songId to allow NULL');
    } catch (e) {
      console.log('Error modifying songId:', e.message);
    }

    console.log('Nullable fix completed.');
    process.exit(0);
  } catch (error) {
    console.error('Failed to fix nullable columns:', error);
    process.exit(1);
  }
}

allowNullableColumns();
