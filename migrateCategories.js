const sequelize = require('./config/database');
const { Song, Category } = require('./models');
const SongCategory = require('./models/SongCategory');

async function migrate() {
  try {
    await sequelize.authenticate();
    console.log('Connection has been established successfully.');

    // Sync just the new table
    await SongCategory.sync({ alter: true });
    console.log('SongCategory table synced.');

    // Fetch all songs with a categoryId
    const songs = await Song.findAll();
    console.log(`Found ${songs.length} songs.`);

    let migrated = 0;
    for (const song of songs) {
      if (song.categoryId) {
        // Insert into junction table
        await SongCategory.findOrCreate({
          where: {
            songId: song.id,
            categoryId: song.categoryId
          }
        });
        migrated++;
      }
    }

    console.log(`Successfully migrated ${migrated} songs to SongCategories table.`);
    process.exit(0);
  } catch (error) {
    console.error('Unable to migrate:', error);
    process.exit(1);
  }
}

migrate();
