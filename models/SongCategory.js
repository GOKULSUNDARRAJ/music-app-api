const { DataTypes } = require('sequelize');
const sequelize = require('../config/database');

const SongCategory = sequelize.define('SongCategory', {
  id: {
    type: DataTypes.INTEGER,
    autoIncrement: true,
    primaryKey: true,
  },
  songId: {
    type: DataTypes.INTEGER,
    allowNull: false,
    references: {
      model: 'Songs',
      key: 'id'
    }
  },
  categoryId: {
    type: DataTypes.INTEGER,
    allowNull: false,
    references: {
      model: 'Categories',
      key: 'id'
    }
  }
}, {
  tableName: 'SongCategories',
  timestamps: false
});

module.exports = SongCategory;
