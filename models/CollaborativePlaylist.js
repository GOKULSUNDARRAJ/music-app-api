const { DataTypes } = require('sequelize');
const sequelize = require('../config/database');

const CollaborativePlaylist = sequelize.define('CollaborativePlaylist', {
  id: {
    type: DataTypes.INTEGER,
    autoIncrement: true,
    primaryKey: true
  },
  name: {
    type: DataTypes.STRING,
    allowNull: false
  },
  inviteCode: {
    type: DataTypes.STRING,
    allowNull: false,
    unique: true
  },
  ownerId: {
    type: DataTypes.INTEGER,
    allowNull: false
  },
  imageUrl: {
    type: DataTypes.STRING,
    allowNull: true
  },
  adminOnlyRemove: {
    type: DataTypes.BOOLEAN,
    defaultValue: false
  }
}, {
  tableName: 'collaborative_playlists',
  timestamps: true
});

module.exports = CollaborativePlaylist;
