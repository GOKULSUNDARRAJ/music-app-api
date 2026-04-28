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
app.use('/api/admin', adminRoutes);
app.use('/api/artist', artistRoutes);
app.use('/api/search', searchRoutes);
app.use('/api/user', userRoutes);
app.use('/api/likes', likeRoutes);
app.use('/api/playlist', playlistRoutes);
app.use('/api', homeRoutes);
app.use('/api', userRoutes);
app.use('/api', likeRoutes);
app.use('/api', playlistRoutes);


// Serve Static Frontend in Production
if (process.env.NODE_ENV === 'production') {
  app.use(express.static(path.join(__dirname, 'frontend/dist')));

  // For any non-API routes, serve the React app
  app.get(/^(?!\/api).*/, (req, res) => {
    res.sendFile(path.resolve(__dirname, 'frontend', 'dist', 'index.html'));
  });
}

app.use(notFound);
app.use(errorHandler);

// Sync database and start server
sequelize.sync()
  .then(() => {
    console.log('Database connected and synced');
    app.listen(PORT, () => {
      console.log(`Server is running on port ${PORT}`);
    });
  })
  .catch((err) => {
    console.error('Failed to sync database:', err);
  });
