const { DataTypes } = require('sequelize');
const sequelize = require('../config/database');

const User = sequelize.define('User', {
  id: {
    type: DataTypes.INTEGER,
    autoIncrement: true,
    primaryKey: true
  },
  userName: {
    type: DataTypes.STRING(100),
    allowNull: false,
    defaultValue: ''
  },
  userEmail: {
    type: DataTypes.STRING(150),
    allowNull: true,
    defaultValue: null
  },
  userMobile: {
    type: DataTypes.STRING(20),
    allowNull: false,
    unique: true
  },
  userPassword: {
    type: DataTypes.STRING(255),
    allowNull: true,
    defaultValue: null
  },
  userCountry: {
    type: DataTypes.STRING(100),
    allowNull: false,
    defaultValue: ''
  },
  userCountryId: {
    type: DataTypes.STRING(10),
    allowNull: false,
    defaultValue: ''
  },
  userCountryCode: {
    type: DataTypes.STRING(10),
    allowNull: false,
    defaultValue: ''
  },
  deviceID: {
    type: DataTypes.STRING(255),
    allowNull: true,
    defaultValue: null
  },
  mobileType: {
    type: DataTypes.STRING(5),
    allowNull: true,
    defaultValue: null
  },
  device_token: {
    type: DataTypes.TEXT,
    allowNull: true,
    defaultValue: null
  },
  referalCode: {
    type: DataTypes.STRING(50),
    allowNull: true,
    defaultValue: null
  },
  isVerified: {
    type: DataTypes.BOOLEAN,
    allowNull: false,
    defaultValue: false
  }
}, {
  tableName: 'users',
  timestamps: true,
  createdAt: 'userCreatedDate',
  updatedAt: 'userUpdatedDate'
});

module.exports = User;
