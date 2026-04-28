async function test() {
  try {
    const loginRes = await fetch("http://localhost:3000/api/admin/login", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ username: "admin", password: "admin123" })
    });
    const loginData = await loginRes.json();
    const token = loginData.response.access_token;
    console.log("Login OK, token:", token.substring(0, 10));

    const headers = { Authorization: `Bearer ${token}`, "Content-Type": "application/json" };

    const artistDataRes = await fetch("http://localhost:3000/api/artist");
    console.log("Public API /api/artist:", await artistDataRes.json());
    
    // Add section check
    const newSectionRes = await fetch("http://localhost:3000/api/admin/section", {
      method: "POST",
      headers,
      body: JSON.stringify({ sectionTitle: "Top Artists", contentType: "artist" })
    });
    const sectionData = await newSectionRes.json();
    console.log("Create Artist Section:", sectionData);

  } catch (e) {
    console.error(e);
  }
}
test();
