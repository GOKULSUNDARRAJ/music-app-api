require('dotenv').config();
const { Sequelize } = require('sequelize');

const sequelize = new Sequelize(process.env.DB_NAME, process.env.DB_USER, process.env.DB_PASS, {
  host: process.env.DB_HOST,
  dialect: 'mysql',
  logging: false
});

async function run() {
  try {
    await sequelize.query("ALTER TABLE sections MODIFY contentType VARCHAR(255) NOT NULL DEFAULT 'all';");
    await sequelize.query("UPDATE sections SET contentType = 'all' WHERE contentType = 'home';");
    await sequelize.query("UPDATE sections SET contentType = 'divotional' WHERE contentType = 'devotional';");
    console.log('Database fixed successfully!');
    process.exit(0);
  } catch (err) {
    console.error(err);
    process.exit(1);
  }
}
run();
