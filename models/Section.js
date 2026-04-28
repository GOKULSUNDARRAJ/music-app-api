const { DataTypes } = require('sequelize');
const sequelize = require('../config/database');

const Section = sequelize.define('Section', {
  id: {
    type: DataTypes.INTEGER,
    autoIncrement: true,
    primaryKey: true,
  },
  sectionTitle: {
    type: DataTypes.STRING,
    allowNull: false,
  },
  layoutType: {
    type: DataTypes.INTEGER,
    defaultValue: 1,
    validate: {
      min: 1
    }
  },
  spanCount: {
    type: DataTypes.INTEGER,
    defaultValue: 1,
    validate: {
      min: 1
    }
  },
  contentType: {
    type: DataTypes.ENUM('home', 'devotional', 'artist'),
    allowNull: false,
    defaultValue: 'home'
  }
}, {
  tableName: 'sections',
  timestamps: true
});

module.exports = Section;
