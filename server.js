require('dotenv').config();
const express = require('express');
const path = require('path');
const cors = require('cors');
const { sequelize } = require('./models');
const homeRoutes = require('./routes/homeRoutes');
const adminRoutes = require('./routes/adminRoutes');
const userRoutes = require('./routes/userRoutes');
const searchRoutes = require('./routes/searchRoutes');
const playlistRoutes = require('./routes/playlistRoutes');
const likeRoutes = require('./routes/likeRoutes');
const artistRoutes = require('./routes/artistRoutes');
const adRoutes = require('./routes/adRoutes');
const { notFound, errorHandler } = require('./controllers/errorController');

const app = express();
const PORT = process.env.PORT || 3000;

app.use(cors());
app.use((req, res, next) => {
  console.log(`[REQUEST] ${req.method} ${req.url}`);
  next();
});
app.use(express.json());
app.use(express.urlencoded({ extended: true }));
app.use('/uploads', express.static('uploads'));


// Routes
app.get('/api', (req, res) => {
  res.status(200).json({ 
    status: 'success', 
    message: 'Music App API is running',
    timestamp: new Date().toISOString()
  });
});

app.use('/api/admin', adminRoutes);

// Mount home routes FIRST so exact matches like /api/artist are handled
// by homeRoutes (which has no strict user auth) before falling through
// to artistRoutes which has strict requireUserAuth middleware.
app.use('/api', homeRoutes);

app.use('/api/artist', artistRoutes);
app.use('/api/search', searchRoutes);
app.use('/api/user', userRoutes);
app.use('/api/likes', likeRoutes);
app.use('/api/playlist', playlistRoutes);
app.use('/api/ad', adRoutes);

// Fallback for root /api mounts for specific routes that rely on it
app.use('/api', userRoutes);
app.use('/api', likeRoutes);
app.use('/api', playlistRoutes);


// Serve Static Frontend in Production
if (process.env.NODE_ENV === 'production') {
  app.use(express.static(path.join(__dirname, 'frontend/dist')));

  // For any non-API routes, serve the React app
  app.get(/^(?!\/api).*/, (req, res) => {
    const indexPath = path.resolve(__dirname, 'frontend', 'dist', 'index.html');
    const fs = require('fs');
    if (fs.existsSync(indexPath)) {
      res.sendFile(indexPath);
    } else {
      res.status(200).json({ message: "Aruvi API is running successfully. Frontend UI is not yet deployed." });
    }
  });
}

app.use(notFound);
app.use(errorHandler);

// Start server first, then sync database
app.listen(PORT, () => {
  console.log(`Server is running on port ${PORT}`);
  
  // Sync database in the background - alter:true adds new columns without dropping data
  sequelize.sync({ alter: true })
    .then(() => {
      console.log('Database connected and synced (alter mode)');
    })
    .catch((err) => {
      console.error('Failed to sync database:', err);
      console.log('API is still running, but database-dependent routes will fail.');
    });

  // Always attempt to seed attributes, ensuring the table exists even if global sync fails
  setTimeout(async () => {
    try {
      const { DataTypes } = require('sequelize');
      await sequelize.getQueryInterface().changeColumn('songs', 'categoryId', {
        type: DataTypes.INTEGER,
        allowNull: true
      });
      await sequelize.getQueryInterface().changeColumn('categories', 'sectionId', {
        type: DataTypes.INTEGER,
        allowNull: true
      });
      console.log('Successfully altered categoryId and sectionId to allow null');
      
      const { Blend, CollaborativePlaylist, CollaborativePlaylistUser, CollaborativePlaylistSong } = require('./models');
      await Blend.sync({ alter: true });
      await CollaborativePlaylist.sync({ alter: true });
      await CollaborativePlaylistUser.sync({ alter: true });
      await CollaborativePlaylistSong.sync({ alter: true });
      console.log('Blend and CollaborativePlaylist tables ensured');
    } catch (err) {
      console.error('Failed to alter columns:', err);
    }
    
    const seedAttributes = require('./seed_attributes');
    await seedAttributes();
  }, 2000);
});
