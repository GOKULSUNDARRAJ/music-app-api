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

    console.log("\\nTesting /fetch-metadata...");
    const metaRes = await fetch("http://localhost:3000/api/admin/fetch-metadata?url=https://github.com/microsoft/vscode", {
      headers
    });
    const metaData = await metaRes.json();
    console.log("Metadata:", metaData);

    console.log("\\nTesting /song/bulk...");
    const bulkRes = await fetch("http://localhost:3000/api/admin/song/bulk", {
      method: "POST",
      headers,
      body: JSON.stringify({
        songs: [
          { audioName: "Test 1", audioUrl: "http://test1", imageUrl: "img1", categoryId: 1 },
          { audioName: "Test 2", audioUrl: "http://test2", imageUrl: "img2", categoryId: 1 }
        ]
      })
    });
    console.log("Bulk response status:", bulkRes.status);
    const bulkData = await bulkRes.json();
    console.log("Bulk Data:", Array.isArray(bulkData) ? bulkData.length + ' inserted' : bulkData);

  } catch (e) {
    console.error(e);
  }
}
test();
