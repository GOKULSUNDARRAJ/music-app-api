const { DataTypes } = require('sequelize');
const sequelize = require('../config/database');

const Blend = sequelize.define('Blend', {
  id: {
    type: DataTypes.INTEGER,
    primaryKey: true,
    autoIncrement: true
  },
  user1Id: {
    type: DataTypes.INTEGER,
    allowNull: false,
    references: {
      model: 'users',
      key: 'id'
    }
  },
  user2Id: {
    type: DataTypes.INTEGER,
    allowNull: true,
    references: {
      model: 'users',
      key: 'id'
    }
  },
  inviteCode: {
    type: DataTypes.STRING,
    allowNull: false,
    unique: true
  },
  status: {
    type: DataTypes.ENUM('pending', 'accepted'),
    defaultValue: 'pending'
  }
}, {
  timestamps: true,
  tableName: 'blends'
});

module.exports = Blend;
