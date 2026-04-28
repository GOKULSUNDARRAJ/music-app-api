async function test() {
  try {
    const baseUrl = "http://localhost:3000/api";
    
    // 1. Register/Login a test user to get a JWT
    console.log("Registering/Logging in test user...");
    const loginRes = await fetch(`${baseUrl}/checkRegister`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ 
        userMobile: "9999999999",
        name: "Test User"
      })
    });
    const loginData = await loginRes.json();
    const token = loginData.response.access_token;
    console.log("User JWT obtained.");

    const headers = { Authorization: `Bearer ${token}`, "Content-Type": "application/json" };

    // 2. Get a category ID to play (from standard sections)
    const homeRes = await fetch(`${baseUrl}/home`);
    const homeData = await homeRes.json();
    
    // Find a category that is NOT in a static "Recently Played" section
    let catToPlay = null;
    for (const section of homeData.sections) {
        if (section.sectionId !== 'sec_001' && section.categories.length > 0) {
            catToPlay = section.categories[0];
            break;
        }
    }

    if (!catToPlay && homeData.sections.length > 0 && homeData.sections[0].categories.length > 0) {
        catToPlay = homeData.sections[0].categories[0];
    }

    if (catToPlay) {
      const formattedId = catToPlay.categoryId; // e.g., "cat_001"
      console.log(`Recording play for Category: ${catToPlay.categoryName} (ID: ${formattedId})`);

      // 3. Record Play
      const recordRes = await fetch(`${baseUrl}/user/recordRecentPlay`, {
        method: "POST",
        headers,
        body: JSON.stringify({ categoryId: formattedId })
      });

      const recordResult = await recordRes.json();
      console.log("Record Play Result:", recordResult);

      // 4. Check Home with Auth
      console.log("Fetching home with auth...");
      const finalHomeRes = await fetch(`${baseUrl}/home`, { headers });
      const finalHomeData = await finalHomeRes.json();
      
      const dynamicRecent = finalHomeData.sections.find(s => s.sectionId === 'sec_recent');
      if (dynamicRecent) {
        console.log("✅ SUCCESS: Dynamic 'Recently Played' section found!");
        console.log("Items in Recent:", dynamicRecent.categories.map(c => c.categoryName));
      } else {
        console.log("❌ FAILURE: Dynamic 'Recently Played' section (sec_recent) not found.");
        console.log("Sections returned:", finalHomeData.sections.map(s => `${s.sectionTitle} (${s.sectionId})`));
      }
    } else {
      console.log("No categories found to test recording.");
    }

  } catch (e) {
    console.error("Test Error:", e);
  }
}
test();
