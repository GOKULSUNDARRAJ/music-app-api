const { DataTypes } = require('sequelize');
const sequelize = require('../config/database');

const Like = sequelize.define('Like', {
  id: {
    type: DataTypes.INTEGER,
    primaryKey: true,
    autoIncrement: true
  },
  userId: {
    type: DataTypes.INTEGER,
    allowNull: false,
    references: {
      model: 'users',
      key: 'id'
    }
  },
  categoryId: {
    type: DataTypes.INTEGER,
    allowNull: true,
    references: {
      model: 'categories',
      key: 'id'
    }
  },
  songId: {
    type: DataTypes.INTEGER,
    allowNull: true,
    references: {
      model: 'songs',
      key: 'id'
    }
  }
}, {
  timestamps: true,
  tableName: 'likes',
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

module.exports = Like;
