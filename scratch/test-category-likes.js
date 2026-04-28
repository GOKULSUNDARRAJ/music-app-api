const axios = require('axios');

const BASE_URL = 'http://localhost:3000/api';
let TOKEN = ''; // Add a valid token here for manual testing

async function testCategoryLikes() {
  try {
    console.log('--- Testing Category Like API ---');

    // 1. Login to get token (assuming test credentials exist)
    // For this test, you'd need a real token.
    if (!TOKEN) {
        console.log('Skipping automated test: Please provide a valid TOKEN in the script.');
        return;
    }

    const headers = { Authorization: `Bearer ${TOKEN}` };

    // 2. Toggle Like
    console.log('Toggling like for category cat_001...');
    const toggleRes = await axios.post(`${BASE_URL}/category/toggle`, { categoryId: 'cat_001' }, { headers });
    console.log('Toggle Response:', toggleRes.data);

    // 3. Check Status
    console.log('Checking status for cat_001...');
    const statusRes = await axios.get(`${BASE_URL}/category/status/cat_001`, { headers });
    console.log('Status Response:', statusRes.data);

    // 4. Get Liked Categories
    console.log('Fetching all liked categories...');
    const listRes = await axios.get(`${BASE_URL}/categories`, { headers });
    console.log('Liked Categories Count:', listRes.data.sections[0].categories.length);
    console.log('First Section Title:', listRes.data.sections[0].sectionTitle);

    console.log('--- Test Completed ---');
  } catch (err) {
    console.error('Test Failed:', err.response ? err.response.data : err.message);
  }
}

// testCategoryLikes();
console.log('To run tests, uncomment testCategoryLikes() and provide a valid JWT TOKEN.');
