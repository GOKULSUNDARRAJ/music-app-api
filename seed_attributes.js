require('dotenv').config();
const { SongAttribute } = require('./models');

const data = [
  // Actors
  { type: 'Actor', name: 'Rajinikanth' },
  { type: 'Actor', name: 'Kamal Haasan' },
  { type: 'Actor', name: 'Vijay' },
  { type: 'Actor', name: 'Ajith Kumar' },
  { type: 'Actor', name: 'Suriya' },
  { type: 'Actor', name: 'Dhanush' },
  { type: 'Actor', name: 'Sivakarthikeyan' },
  { type: 'Actor', name: 'Vikram' },
  { type: 'Actor', name: 'Karthi' },
  { type: 'Actor', name: 'Jayam Ravi' },

  // Heroines
  { type: 'Heroine', name: 'Nayanthara' },
  { type: 'Heroine', name: 'Trisha' },
  { type: 'Heroine', name: 'Keerthy Suresh' },
  { type: 'Heroine', name: 'Samantha' },
  { type: 'Heroine', name: 'Sai Pallavi' },
  { type: 'Heroine', name: 'Tamannaah' },
  { type: 'Heroine', name: 'Anushka Shetty' },
  { type: 'Heroine', name: 'Aishwarya Rajesh' },
  { type: 'Heroine', name: 'Priya Bhavani Shankar' },
  { type: 'Heroine', name: 'Rashmika Mandanna' },

  // Singers
  { type: 'Singer', name: 'S. P. Balasubrahmanyam' },
  { type: 'Singer', name: 'K. S. Chithra' },
  { type: 'Singer', name: 'Shreya Ghoshal' },
  { type: 'Singer', name: 'Sid Sriram' },
  { type: 'Singer', name: 'Hariharan' },
  { type: 'Singer', name: 'S. Janaki' },
  { type: 'Singer', name: 'Unnikrishnan' },
  { type: 'Singer', name: 'Chinmayi' },
  { type: 'Singer', name: 'Anirudh Ravichander' },
  { type: 'Singer', name: 'Haricharan' },

  // Movies
  { type: 'Movie', name: 'Baasha' },
  { type: 'Movie', name: 'Thalapathi' },
  { type: 'Movie', name: 'Ghilli' },
  { type: 'Movie', name: 'Vikram' },
  { type: 'Movie', name: 'Soorarai Pottru' },
  { type: 'Movie', name: 'Asuran' },
  { type: 'Movie', name: 'Kaithi' },
  { type: 'Movie', name: 'Mersal' },
  { type: 'Movie', name: 'Jai Bhim' },
  { type: 'Movie', name: 'Love Today' },

  // Music Directors
  { type: 'MusicDirector', name: 'A. R. Rahman' },
  { type: 'MusicDirector', name: 'Ilaiyaraaja' },
  { type: 'MusicDirector', name: 'Anirudh Ravichander' },
  { type: 'MusicDirector', name: 'Yuvan Shankar Raja' },
  { type: 'MusicDirector', name: 'Harris Jayaraj' },
  { type: 'MusicDirector', name: 'G. V. Prakash Kumar' },
  { type: 'MusicDirector', name: 'D. Imman' },
  { type: 'MusicDirector', name: 'Santhosh Narayanan' },
  { type: 'MusicDirector', name: 'Vidyasagar' },
  { type: 'MusicDirector', name: 'Deva' },

  // Genres
  { type: 'Genre', name: 'Melody' },
  { type: 'Genre', name: 'Romantic' },
  { type: 'Genre', name: 'Folk' },
  { type: 'Genre', name: 'Devotional' },
  { type: 'Genre', name: 'Classical' },
  { type: 'Genre', name: 'Mass' },
  { type: 'Genre', name: 'Dance' },
  { type: 'Genre', name: 'Sad' },
  { type: 'Genre', name: 'Hip Hop' },
  { type: 'Genre', name: 'Gaana' }
];

async function seedAttributes() {
  try {
    const count = await SongAttribute.count();
    if (count > 0) return; // Only seed if empty

    console.log('Seeding attributes...');
    for (const item of data) {
      await SongAttribute.findOrCreate({
        where: { type: item.type, name: item.name },
        defaults: item
      });
    }
    console.log('Successfully seeded attributes!');
  } catch (error) {
    console.error('Failed to seed attributes:', error);
  }
}

module.exports = seedAttributes;
