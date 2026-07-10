const { DataTypes } = require('sequelize');
const sequelize = require('../config/database');

const Song = sequelize.define('Song', {
  id: {
    type: DataTypes.INTEGER,
    autoIncrement: true,
    primaryKey: true,
  },
  audioName: {
    type: DataTypes.STRING,
    allowNull: false,
  },
  audioUrl: {
    type: DataTypes.STRING,
    allowNull: false,
  },
  imageUrl: {
    type: DataTypes.STRING,
  },
  categoryId: {
    type: DataTypes.INTEGER,
    allowNull: true,
    references: {
      model: 'categories',
      key: 'id'
    }
  },
  actorName: {
    type: DataTypes.STRING,
    allowNull: true,
  },
  heroineName: {
    type: DataTypes.STRING,
    allowNull: true,
  },
  singerName: {
    type: DataTypes.STRING,
    allowNull: true,
  },
  movieName: {
    type: DataTypes.STRING,
    allowNull: true,
  },
  musicDirector: {
    type: DataTypes.STRING,
    allowNull: true,
  },
  releaseYear: {
    type: DataTypes.STRING,
    allowNull: true,
  },
  genre: {
    type: DataTypes.STRING,
    allowNull: true,
  },
  lyrics: {
    type: DataTypes.TEXT,
    allowNull: true,
    defaultValue: null,
  }
}, {
  tableName: 'songs',
  timestamps: true
});


module.exports = Song;
