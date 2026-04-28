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
      model: 'Users',
      key: 'id'
    }
  },
  categoryId: {
    type: DataTypes.INTEGER,
    allowNull: true,
    references: {
      model: 'Categories',
      key: 'id'
    }
  },
  songId: {
    type: DataTypes.INTEGER,
    allowNull: true,
    references: {
      model: 'Songs',
      key: 'id'
    }
  }
}, {
  timestamps: true,
  tableName: 'Likes',
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
