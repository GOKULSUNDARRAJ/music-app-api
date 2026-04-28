const { DataTypes } = require('sequelize');

module.exports = (sequelize) => {
  const PlaylistItem = sequelize.define('PlaylistItem', {
    id: {
      type: DataTypes.INTEGER,
      autoIncrement: true,
      primaryKey: true
    },
    playlistId: {
      type: DataTypes.INTEGER,
      allowNull: false
    },
    categoryId: {
      type: DataTypes.INTEGER,
      allowNull: true
    },
    songId: {
      type: DataTypes.INTEGER,
      allowNull: true
    }
  }, {
    tableName: 'playlist_items',
    timestamps: true
  });

  return PlaylistItem;
};
