const { DataTypes } = require('sequelize');
const sequelize = require('../config/database');

const RecentlyPlayed = sequelize.define('RecentlyPlayed', {
  id: {
    type: DataTypes.INTEGER,
    autoIncrement: true,
    primaryKey: true,
  },
  userId: {
    type: DataTypes.INTEGER,
    allowNull: false,
  },
  categoryId: {
    type: DataTypes.INTEGER,
    allowNull: false,
  },
  playedAt: {
    type: DataTypes.DATE,
    defaultValue: DataTypes.NOW,
  }
}, {
  tableName: 'recently_played',
  timestamps: false, // We'll use playedAt instead
});

module.exports = RecentlyPlayed;
