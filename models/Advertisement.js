const { DataTypes } = require('sequelize');
const sequelize = require('../config/database');

const Advertisement = sequelize.define('Advertisement', {
  id: {
    type: DataTypes.INTEGER,
    autoIncrement: true,
    primaryKey: true
  },
  adTitle: {
    type: DataTypes.STRING,
    allowNull: false
  },
  adType: {
    type: DataTypes.ENUM('audio', 'video'),
    allowNull: false,
    defaultValue: 'audio'
  },
  mediaUrl: {
    type: DataTypes.STRING,
    allowNull: false
  },
  imageUrl: {
    type: DataTypes.STRING,
    allowNull: true
  },
  redirectUrl: {
    type: DataTypes.STRING,
    allowNull: true
  },
  active: {
    type: DataTypes.BOOLEAN,
    defaultValue: true
  }
}, {
  timestamps: true,
  tableName: 'advertisements'
});

module.exports = Advertisement;
