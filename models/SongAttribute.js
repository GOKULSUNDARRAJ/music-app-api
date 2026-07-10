const { DataTypes } = require('sequelize');
const sequelize = require('../config/database');

const SongAttribute = sequelize.define('SongAttribute', {
  id: {
    type: DataTypes.INTEGER,
    autoIncrement: true,
    primaryKey: true,
  },
  type: {
    type: DataTypes.STRING, // e.g. 'Actor', 'Heroine', 'Singer', 'Movie', 'MusicDirector', 'Genre'
    allowNull: false,
  },
  name: {
    type: DataTypes.STRING,
    allowNull: false,
  }
}, {
  tableName: 'song_attributes',
  timestamps: true,
  indexes: [
    {
      unique: true,
      fields: ['type', 'name']
    }
  ]
});

module.exports = SongAttribute;
