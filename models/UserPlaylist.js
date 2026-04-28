const { DataTypes } = require('sequelize');
const sequelize = require('../config/database');

const UserPlaylist = sequelize.define('UserPlaylist', {
  id: {
    type: DataTypes.INTEGER,
    autoIncrement: true,
    primaryKey: true
  },
  userId: {
    type: DataTypes.INTEGER,
    allowNull: false
  },
  categoryId: {
    type: DataTypes.INTEGER,
    allowNull: true
  },
  songId: {
    type: DataTypes.INTEGER,
    allowNull: true
  }
}, {
  tableName: 'user_playlists',
  timestamps: true,
  indexes: [
    {
      unique: true,
      fields: ['userId', 'categoryId'],
      where: {
        categoryId: { [require('sequelize').Op.ne]: null }
      }
    },
    {
      unique: true,
      fields: ['userId', 'songId'],
      where: {
        songId: { [require('sequelize').Op.ne]: null }
      }
    }
  ]
});

module.exports = UserPlaylist;
