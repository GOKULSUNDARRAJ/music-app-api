const { DataTypes } = require('sequelize');
const sequelize = require('../config/database');

const MenuItem = sequelize.define('MenuItem', {
  id: {
    type: DataTypes.INTEGER,
    autoIncrement: true,
    primaryKey: true
  },
  menuType: {
    type: DataTypes.ENUM('top', 'bottom'),
    allowNull: false
  },
  menuName: {
    type: DataTypes.STRING,
    allowNull: false
  },
  menuStatusId: {
    type: DataTypes.INTEGER,
    allowNull: false,
    defaultValue: 1
  },
  menuStatus: {
    type: DataTypes.STRING,
    allowNull: false,
    defaultValue: 'Active'
  },
  menuActiveIcon: {
    type: DataTypes.STRING,
    allowNull: false
  },
  menuInActiveIcon: {
    type: DataTypes.STRING,
    allowNull: false
  },
  sortOrder: {
    type: DataTypes.INTEGER,
    allowNull: false,
    defaultValue: 1
  }
}, {
  tableName: 'menu_items',
  timestamps: true
});

module.exports = MenuItem;
