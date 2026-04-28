const { sequelize, Section, Category, Song, MenuItem } = require('./models');

const seedData = async () => {
  try {
    await sequelize.sync({ force: true });
    console.log('Database synced');

    const section1 = await Section.create({
      sectionTitle: 'Recently Played',
      layoutType: 1,
      spanCount: 1,
      contentType: 'home'
    });

    const section2 = await Section.create({
      sectionTitle: 'Recommended for You',
      layoutType: 2,
      spanCount: 2,
      contentType: 'home'
    });

    const section3 = await Section.create({
      sectionTitle: 'Morning Devotional',
      layoutType: 1,
      spanCount: 1,
      contentType: 'devotional'
    });

    const category1 = await Category.create({
      categoryName: 'Pop Hits',
      categoryImage: 'http://example.com/pop.jpg',
      adapterType: 1,
      sectionId: section1.id
    });

    const category2 = await Category.create({
      categoryName: 'Rock Classics',
      categoryImage: 'http://example.com/rock.jpg',
      adapterType: 1,
      sectionId: section1.id
    });

    await Category.create({
      categoryName: 'Empty Category',
      categoryImage: 'http://example.com/empty.jpg',
      adapterType: 1,
      sectionId: section2.id
    });

    const devotionalCategory = await Category.create({
      categoryName: 'Bhajans',
      categoryImage: 'http://example.com/bhajan.jpg',
      adapterType: 1,
      sectionId: section3.id
    });

    await Song.create({
      audioName: 'Shape of You',
      audioUrl: 'http://example.com/shape.mp3',
      imageUrl: 'http://example.com/shape.jpg',
      categoryId: category1.id
    });

    await Song.create({
      audioName: 'Blinding Lights',
      audioUrl: 'http://example.com/blinding.mp3',
      imageUrl: 'http://example.com/blinding.jpg',
      categoryId: category1.id
    });

    await Song.create({
      audioName: 'Bohemian Rhapsody',
      audioUrl: 'http://example.com/bohemian.mp3',
      imageUrl: 'http://example.com/bohemian.jpg',
      categoryId: category2.id
    });

    await Song.create({
      audioName: 'Om Jai Jagdish Hare',
      audioUrl: 'http://example.com/om-jai-jagdish.mp3',
      imageUrl: 'http://example.com/om-jai-jagdish.jpg',
      categoryId: devotionalCategory.id
    });

    await MenuItem.bulkCreate([
      {
        menuType: 'top',
        menuName: 'All',
        menuStatusId: 1,
        menuStatus: 'Active',
        menuActiveIcon: 'https://staging.saalai.tv/saalai_app/icon/all_white.png',
        menuInActiveIcon: 'https://staging.saalai.tv/saalai_app/icon/all.png',
        sortOrder: 1
      },
      {
        menuType: 'top',
        menuName: 'Live Tv',
        menuStatusId: 1,
        menuStatus: 'Active',
        menuActiveIcon: 'https://staging.saalai.tv/saalai_app/icon/livetv_active.png',
        menuInActiveIcon: 'https://staging.saalai.tv/saalai_app/icon/livetv.png',
        sortOrder: 2
      },
      {
        menuType: 'top',
        menuName: 'Movies',
        menuStatusId: 1,
        menuStatus: 'Active',
        menuActiveIcon: 'https://staging.saalai.tv/saalai_app/icon/movies_selected.png',
        menuInActiveIcon: 'https://staging.saalai.tv/saalai_app/icon/movies_unselected.png',
        sortOrder: 3
      },
      {
        menuType: 'top',
        menuName: 'Tv Shows',
        menuStatusId: 1,
        menuStatus: 'Active',
        menuActiveIcon: 'https://staging.saalai.tv/saalai_app/icon/tv_shows_active.png',
        menuInActiveIcon: 'https://staging.saalai.tv/saalai_app/icon/tv_shows.png',
        sortOrder: 4
      },
      {
        menuType: 'top',
        menuName: 'Catch Up',
        menuStatusId: 1,
        menuStatus: 'Active',
        menuActiveIcon: 'https://staging.saalai.tv/saalai_app/icon/catchup_active.png',
        menuInActiveIcon: 'https://staging.saalai.tv/saalai_app/icon/catchup.png',
        sortOrder: 5
      },
      {
        menuType: 'bottom',
        menuName: 'Thirai',
        menuStatusId: 1,
        menuStatus: 'Active',
        menuActiveIcon: 'https://staging.saalai.tv/saalai_app/icon/home_selected.png',
        menuInActiveIcon: 'https://staging.saalai.tv/saalai_app/icon/home_unselected.png',
        sortOrder: 1
      },
      {
        menuType: 'bottom',
        menuName: 'Radio',
        menuStatusId: 1,
        menuStatus: 'Active',
        menuActiveIcon: 'https://staging.saalai.tv/saalai_app/icon/radio_selected.png',
        menuInActiveIcon: 'https://staging.saalai.tv/saalai_app/icon/radio_unselected.png',
        sortOrder: 2
      },
      {
        menuType: 'bottom',
        menuName: 'Profile',
        menuStatusId: 1,
        menuStatus: 'Active',
        menuActiveIcon: 'https://staging.saalai.tv/saalai_app/icon/profileSelected.png',
        menuInActiveIcon: 'https://staging.saalai.tv/saalai_app/icon/profileInActive.png',
        sortOrder: 3
      }
    ]);

    console.log('Seed data inserted successfully!');
    process.exit(0);
  } catch (error) {
    console.error('Error seeding data:', error);
    process.exit(1);
  }
};

seedData();
