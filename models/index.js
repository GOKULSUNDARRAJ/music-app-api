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
const CollaborativePlaylist = require('./CollaborativePlaylist');
const CollaborativePlaylistUser = require('./CollaborativePlaylistUser');
const CollaborativePlaylistSong = require('./CollaborativePlaylistSong');
const SongCategory = require('./SongCategory');

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

// Many categories have many songs
Category.belongsToMany(Song, {
  through: SongCategory,
  foreignKey: 'categoryId',
  otherKey: 'songId',
  as: 'songs'
});
Song.belongsToMany(Category, {
  through: SongCategory,
  foreignKey: 'songId',
  otherKey: 'categoryId',
  as: 'categories'
});

// Keep One-to-Many for backward compatibility in controllers that use `as: 'category'`
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

// Collaborative Playlist Associations
User.hasMany(CollaborativePlaylist, { foreignKey: 'ownerId', as: 'ownedCollaborativePlaylists' });
CollaborativePlaylist.belongsTo(User, { foreignKey: 'ownerId', as: 'owner' });

CollaborativePlaylist.hasMany(CollaborativePlaylistUser, { foreignKey: 'playlistId', as: 'members', onDelete: 'CASCADE' });
CollaborativePlaylistUser.belongsTo(CollaborativePlaylist, { foreignKey: 'playlistId', as: 'playlist' });

User.hasMany(CollaborativePlaylistUser, { foreignKey: 'userId', as: 'collaborativeMemberships', onDelete: 'CASCADE' });
CollaborativePlaylistUser.belongsTo(User, { foreignKey: 'userId', as: 'user' });

CollaborativePlaylist.hasMany(CollaborativePlaylistSong, { foreignKey: 'playlistId', as: 'songs', onDelete: 'CASCADE' });
CollaborativePlaylistSong.belongsTo(CollaborativePlaylist, { foreignKey: 'playlistId', as: 'playlist' });

Song.hasMany(CollaborativePlaylistSong, { foreignKey: 'songId', as: 'collaborativePlaylistEntries', onDelete: 'CASCADE' });
CollaborativePlaylistSong.belongsTo(Song, { foreignKey: 'songId', as: 'song' });

User.hasMany(CollaborativePlaylistSong, { foreignKey: 'addedById', as: 'collaborativeAddedSongs' });
CollaborativePlaylistSong.belongsTo(User, { foreignKey: 'addedById', as: 'addedBy' });

module.exports = { sequelize, Section, Category, Song, MenuItem, User, RecentlyPlayed, Like, Follow, Playlist, PlaylistItem, Advertisement, SongAttribute, Blend, CollaborativePlaylist, CollaborativePlaylistUser, CollaborativePlaylistSong };
