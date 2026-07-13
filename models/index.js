const sequelize = require('../config/database');
const Section = require('./Section');
const Category = require('./Category');
const Song = require('./Song');
const MenuItem = require('./MenuItem');
const User = require('./User');
const RecentlyPlayed = require('./RecentlyPlayed');
const Like = require('./Like');
const Follow = require('./Follow');
const Advertisement = require('./Advertisement');
const Playlist = require('./Playlist')(sequelize);
const PlaylistItem = require('./PlaylistItem')(sequelize);
const SongAttribute = require('./SongAttribute');
const Blend = require('./Blend');

// Define Relationships
// One section has many categories
Section.hasMany(Category, {
  foreignKey: 'sectionId',
  as: 'categories',
  onDelete: 'CASCADE'
});
Category.belongsTo(Section, {
  foreignKey: 'sectionId',
  as: 'section'
});

// One category has many songs
Category.hasMany(Song, {
  foreignKey: 'categoryId',
  as: 'songs',
  onDelete: 'CASCADE'
});
Song.belongsTo(Category, {
  foreignKey: 'categoryId',
  as: 'category'
});

// Recently Played Associations
User.hasMany(RecentlyPlayed, { foreignKey: 'userId', as: 'recentPlays' });
Category.hasMany(RecentlyPlayed, { foreignKey: 'categoryId', as: 'recentPlays' });
RecentlyPlayed.belongsTo(User, { foreignKey: 'userId', as: 'user' });
RecentlyPlayed.belongsTo(Category, { foreignKey: 'categoryId', as: 'category' });

// Like Associations
User.hasMany(Like, { foreignKey: 'userId', as: 'likes' });
Like.belongsTo(User, { foreignKey: 'userId', as: 'user' });

Category.hasMany(Like, { foreignKey: 'categoryId', as: 'likes' });
Like.belongsTo(Category, { foreignKey: 'categoryId', as: 'category' });

Song.hasMany(Like, { foreignKey: 'songId', as: 'likes' });
Like.belongsTo(Song, { foreignKey: 'songId', as: 'song' });

// Follow Associations
User.hasMany(Follow, { foreignKey: 'userId', as: 'follows' });
Follow.belongsTo(User, { foreignKey: 'userId', as: 'user' });

Category.hasMany(Follow, { foreignKey: 'artistId', as: 'follows' });
Follow.belongsTo(Category, { foreignKey: 'artistId', as: 'artist' });

// User <-> Playlist (One-to-Many)
User.hasMany(Playlist, { foreignKey: 'userId', as: 'playlists', onDelete: 'CASCADE' });
Playlist.belongsTo(User, { foreignKey: 'userId', as: 'user' });

// Playlist <-> PlaylistItem (One-to-Many)
Playlist.hasMany(PlaylistItem, { foreignKey: 'playlistId', as: 'items', onDelete: 'CASCADE' });
PlaylistItem.belongsTo(Playlist, { foreignKey: 'playlistId', as: 'playlist' });

// PlaylistItem <-> Category/Song (Many-to-One)
Category.hasMany(PlaylistItem, { foreignKey: 'categoryId', as: 'playlistEntries' });
PlaylistItem.belongsTo(Category, { foreignKey: 'categoryId', as: 'category' });

Song.hasMany(PlaylistItem, { foreignKey: 'songId', as: 'playlistEntries' });
PlaylistItem.belongsTo(Song, { foreignKey: 'songId', as: 'song' });



// Blend Associations
User.hasMany(Blend, { foreignKey: 'user1Id', as: 'blendsInitiated' });
Blend.belongsTo(User, { foreignKey: 'user1Id', as: 'user1' });

User.hasMany(Blend, { foreignKey: 'user2Id', as: 'blendsAccepted' });
Blend.belongsTo(User, { foreignKey: 'user2Id', as: 'user2' });

module.exports = { sequelize, Section, Category, Song, MenuItem, User, RecentlyPlayed, Like, Follow, Playlist, PlaylistItem, Advertisement, SongAttribute, Blend };
