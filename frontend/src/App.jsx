import { useEffect, useMemo, useState } from 'react';
import api, { setAuthToken } from './api';

const TABS = ['Dashboard', 'Sections', 'Categories', 'Songs', 'Bulk Songs', 'Menu', 'Users'];
const formatEntityId = (prefix, id) => `${prefix}_${String(id).padStart(3, '0')}`;
const CONTENT_TYPES = [
  { value: '', label: 'All' },
  { value: 'home', label: 'Home' },
  { value: 'devotional', label: 'Devotional' },
  { value: 'artist', label: 'Artist' }
];


function Login({ onSuccess }) {
  const [form, setForm] = useState({ username: 'admin', password: 'admin123' });
  const [error, setError] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    try {
      const { data } = await api.post('/admin/login', form);
      const token = data?.response?.access_token || data?.token;
      if (!token) {
        throw new Error('No access token in response');
      }
      setAuthToken(token);
      onSuccess();
    } catch (err) {
      setError(err.response?.data?.message || 'Login failed');
    }
  };

  return (
    <div className="centered">
      <form className="card form" onSubmit={handleSubmit}>
        <h2>Admin Login</h2>
        <input
          placeholder="Username"
          value={form.username}
          onChange={(e) => setForm((p) => ({ ...p, username: e.target.value }))}
        />
        <input
          type="password"
          placeholder="Password"
          value={form.password}
          onChange={(e) => setForm((p) => ({ ...p, password: e.target.value }))}
        />
        {error && <p className="error">{error}</p>}
        <button type="submit">Login</button>
      </form>
    </div>
  );
}

function App() {
  const [isLoggedIn, setIsLoggedIn] = useState(Boolean(localStorage.getItem('adminToken')));
  const [activeTab, setActiveTab] = useState('Dashboard');
  const [contentType, setContentType] = useState('home');
  const [refreshKey, setRefreshKey] = useState(0);
  const [songToEdit, setSongToEdit] = useState(null);

  const onDataChange = () => setRefreshKey((p) => p + 1);

  if (!isLoggedIn) {
    return <Login onSuccess={() => setIsLoggedIn(true)} />;
  }

  return (
    <div className="layout">
      <aside className="sidebar">
        <h1>Music Admin</h1>
        {TABS.map((tab) => (
          <button
            key={tab}
            className={activeTab === tab ? 'active' : ''}
            onClick={() => setActiveTab(tab)}
          >
            <span style={{ fontSize: '1.2rem' }}>
              {tab === 'Dashboard' && '📊'}
              {tab === 'Sections' && '🗂️'}
              {tab === 'Categories' && '📂'}
              {tab === 'Songs' && '🎵'}
              {tab === 'Bulk Songs' && '🚀'}
              {tab === 'Menu' && '🍔'}
              {tab === 'Users' && '👥'}
            </span>
            {tab}
          </button>
        ))}
        <button
          className="logout"
          onClick={() => {
            setAuthToken(null);
            setIsLoggedIn(false);
          }}
        >
          Logout
        </button>
      </aside>
      <main className="content">
        <div className="toolbar content-type-toolbar">
          <label>Manage content:</label>
          <select value={contentType} onChange={(e) => setContentType(e.target.value)}>
            {CONTENT_TYPES.map((type) => (
              <option key={type.value} value={type.value}>
                {type.label}
              </option>
            ))}
          </select>
        </div>
        {activeTab !== 'Dashboard' && (
          <div className="toolbar content-type-toolbar">
            <small>Working on {contentType.toUpperCase()} content</small>
          </div>
        )}
        {activeTab === 'Dashboard' && <Dashboard refreshKey={refreshKey} contentType={contentType} />}
        {activeTab === 'Sections' && <Sections onDataChange={onDataChange} contentType={contentType} />}
        {activeTab === 'Categories' && <Categories onDataChange={onDataChange} contentType={contentType} />}
        {activeTab === 'Songs' && <Songs onDataChange={onDataChange} contentType={contentType} initialEditSong={songToEdit} clearEditSong={() => setSongToEdit(null)} />}
        {activeTab === 'Bulk Songs' && <BulkSongs onDataChange={onDataChange} contentType={contentType} onEditRequest={(s) => { setSongToEdit(s); setActiveTab('Songs'); }} />}
        {activeTab === 'Menu' && <MenuManager onDataChange={onDataChange} />}
        {activeTab === 'Users' && <Users onDataChange={onDataChange} />}

      </main>
    </div>
  );
}

function Dashboard({ refreshKey, contentType }) {
  const [counts, setCounts] = useState({ sections: 0, categories: 0, songs: 0, users: 0 });
  const [nestedData, setNestedData] = useState({ sections: [] });

  useEffect(() => {
    api.get('/admin/dashboard').then((res) => setCounts(res.data));
  }, [refreshKey]);

  useEffect(() => {
    let endpoint = '/home';
    if (contentType === 'devotional') endpoint = '/devotional';
    if (contentType === 'artist') endpoint = '/artist';
    api.get(endpoint).then((res) => setNestedData(res.data));
  }, [contentType, refreshKey]);


  return (
    <div>
      <h2>Dashboard</h2>
      <div className="cards">
        <div className="card">Sections: {counts.sections}</div>
        <div className="card">Categories: {counts.categories}</div>
        <div className="card">Songs: {counts.songs}</div>
        <div className="card">Users: {counts.users || 0}</div>


      </div>
      <div className="dashboard-tree">
        <h3>{contentType ? contentType.toUpperCase() : 'ALL'} Content Structure</h3>
        {(nestedData.sections || []).length === 0 ? (
          <p className="muted">No sections found for this content type.</p>
        ) : (
          (nestedData.sections || []).map((section) => (
            <div className="section-block" key={section.sectionId}>
              <div className="node-title">
                <span className="id-badge">{section.sectionId}</span>
                <strong>{section.sectionTitle}</strong>
              </div>
              {(section.categories || []).length === 0 ? (
                <p className="muted">No categories</p>
              ) : (
                (section.categories || []).map((category) => (
                  <div className="category-block" key={category.categoryId}>
                    <div className="node-title">
                      <span className="id-badge">{category.categoryId}</span>
                      <span>{category.categoryName}</span>
                    </div>
                    {(category.songs || []).length === 0 ? (
                      <p className="muted">No songs</p>
                    ) : (
                      <div className="songs-list">
                        {(category.songs || []).map((song) => (
                          <div className="song-row" key={song.songId}>
                            <span className="id-badge">{song.songId}</span>
                            <span>{song.audioName}</span>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                ))
              )}
            </div>
          ))
        )}
      </div>
    </div>
  );
}

function Sections({ onDataChange, contentType }) {
  const [items, setItems] = useState([]);
  const [form, setForm] = useState({ sectionTitle: '', layoutType: 1, spanCount: 1, contentType });
  const [editingId, setEditingId] = useState(null);

  const load = async () => {
    try {
      const { data } = await api.get(`/admin/sections?contentType=${contentType}`);
      setItems(data);
    } catch (err) {
      console.error('Failed to load sections:', err);
      alert('Failed to load sections. Check console for details.');
    }
  };

  useEffect(() => {
    load();
    setForm((prev) => ({ ...prev, contentType }));
    setEditingId(null);
  }, [contentType]);

  const submit = async (e) => {
    e.preventDefault();
    const payload = { ...form, contentType };
    if (editingId) {
      await api.put(`/admin/section/${editingId}`, payload);
    } else {
      await api.post('/admin/section', payload);
    }
    setForm({ sectionTitle: '', layoutType: 1, spanCount: 1, contentType });
    setEditingId(null);
    await load();
    onDataChange();
  };

  const remove = async (id) => {
    await api.delete(`/admin/section/${id}`);
    await load();
    onDataChange();
  };

  return (
    <div>
      <h2>Sections</h2>
      <EntityPage
        title={null}
        form={form}
        setForm={setForm}
        editingId={editingId}
        setEditingId={setEditingId}
        onSubmit={submit}
        items={items}
        onEdit={(item) => {
          setEditingId(item.id);
          setForm({
            sectionTitle: item.sectionTitle,
            layoutType: item.layoutType,
            spanCount: item.spanCount
          });
        }}
        onDelete={remove}
        fields={[
          { key: 'sectionTitle', label: 'Section Title' },
          { key: 'layoutType', label: 'Layout Type', type: 'number' },
          { key: 'spanCount', label: 'Span Count', type: 'number' }
        ]}
        columns={['sectionId', 'sectionTitle', 'layoutType', 'spanCount', 'contentType']}
        rowTransform={(item) => ({
          ...item,
          sectionId: item.sectionId || formatEntityId('sec', item.id)
        })}
      />
    </div>
  );
}

function Categories({ onDataChange, contentType }) {
  const [items, setItems] = useState([]);
  const [sections, setSections] = useState([]);
  const [sectionFilter, setSectionFilter] = useState('');
  const [form, setForm] = useState({ categoryName: '', categoryImage: '', adapterType: 1, sectionId: '' });
  const [editingId, setEditingId] = useState(null);

  const load = async () => {
    try {
      const [{ data: cats }, { data: secs }] = await Promise.all([
        api.get(`/admin/categories?contentType=${contentType}${sectionFilter ? `&sectionId=${sectionFilter}` : ''}`),
        api.get(`/admin/sections?contentType=${contentType}`)
      ]);
      setItems(cats);
      setSections(secs);
    } catch (err) {
      console.error('Failed to load categories/sections:', err);
      alert('Failed to load data. Check console for details.');
    }
  };

  useEffect(() => {
    load();
  }, [sectionFilter, contentType]);

  useEffect(() => {
    setSectionFilter('');
    setForm((prev) => ({ ...prev, sectionId: '' }));
    setEditingId(null);
  }, [contentType]);

  const submit = async (e) => {
    e.preventDefault();
    const payload = { ...form, sectionId: Number(form.sectionId) };
    if (editingId) {
      await api.put(`/admin/category/${editingId}`, payload);
    } else {
      await api.post('/admin/category', payload);
    }
    setForm({ categoryName: '', categoryImage: '', adapterType: 1, sectionId: '' });
    setEditingId(null);
    await load();
    onDataChange();
  };

  return (
    <div>
      <h2>Categories</h2>
      <div className="toolbar">
        <label>Filter by section:</label>
        <select value={sectionFilter} onChange={(e) => setSectionFilter(e.target.value)}>
          <option value="">All</option>
          {sections.map((s) => (
            <option key={s.id} value={s.id}>{s.sectionTitle}</option>
          ))}
        </select>
      </div>
      <EntityPage
        title={null}
        form={form}
        setForm={setForm}
        editingId={editingId}
        setEditingId={setEditingId}
        onSubmit={submit}
        items={items}
        onEdit={(item) => {
          setEditingId(item.id);
          setForm({
            categoryName: item.categoryName,
            categoryImage: item.categoryImage || '',
            adapterType: item.adapterType,
            sectionId: String(item.sectionId)
          });
        }}
        onDelete={async (id) => {
          await api.delete(`/admin/category/${id}`);
          await load();
          onDataChange();
        }}
        fields={[
          { key: 'categoryName', label: 'Category Name' },
          { key: 'categoryImage', label: 'Category Image URL', allowUpload: true },
          { key: 'adapterType', label: 'Adapter Type', type: 'number' },
          {
            key: 'sectionId',
            label: 'Section',
            type: 'select',
            options: sections.map((s) => ({ value: String(s.id), label: s.sectionTitle }))
          }
        ]}
        columns={['categoryId', 'categoryName', 'categoryImage', 'adapterType', 'sectionIdFormatted']}
        rowTransform={(item) => ({
          ...item,
          categoryId: item.categoryId || formatEntityId('cat', item.id),
          sectionIdFormatted: item.sectionIdFormatted || formatEntityId('sec', item.sectionId)
        })}
      />
    </div>
  );
}

function Songs({ onDataChange, contentType, initialEditSong, clearEditSong }) {
  const [items, setItems] = useState([]);
  const [categories, setCategories] = useState([]);
  const [categoryFilter, setCategoryFilter] = useState('');
  const [form, setForm] = useState({ audioName: '', audioUrl: '', imageUrl: '', categoryId: '' });
  const [editingId, setEditingId] = useState(null);

  useEffect(() => {
    if (initialEditSong) {
      setEditingId(initialEditSong.id);
      setForm({
        audioName: initialEditSong.audioName,
        audioUrl: initialEditSong.audioUrl,
        imageUrl: initialEditSong.imageUrl || '',
        categoryId: String(initialEditSong.categoryId)
      });
      clearEditSong();
    }
  }, [initialEditSong, clearEditSong]);

  const [isSearchOpen, setIsSearchOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [searchResults, setSearchResults] = useState([]);

  const load = async () => {
    try {
      const [{ data: songs }, { data: cats }] = await Promise.all([
        api.get(`/admin/songs?contentType=${contentType}${categoryFilter ? `&categoryId=${categoryFilter}` : ''}`),
        api.get(`/admin/categories?contentType=${contentType}`)
      ]);
      setItems(songs);
      setCategories(cats);
    } catch (err) {
      console.error('Failed to load songs/categories:', err);
      alert('Failed to load data. Check console for details.');
    }
  };

  useEffect(() => {
    load();
  }, [categoryFilter, contentType]);

  useEffect(() => {
    setCategoryFilter('');
    setForm((prev) => ({ ...prev, categoryId: '' }));
    setEditingId(null);
  }, [contentType]);

  const submit = async (e) => {
    e.preventDefault();
    try {
      const payload = { ...form, categoryId: Number(form.categoryId) };
      if (editingId) {
        await api.put(`/admin/song/${editingId}`, payload);
      } else {
        await api.post('/admin/song', payload);
      }
      setForm({ audioName: '', audioUrl: '', imageUrl: '', categoryId: '' });
      setEditingId(null);
      await load();
      onDataChange();
    } catch (err) {
      alert(err.response?.data?.message || 'Error submitting form');
    }
  };

  const handleFetchMetadata = async (url) => {
    if (!url) return alert('Enter a URL first');
    try {
      const { data } = await api.get(`/admin/fetch-metadata?url=${encodeURIComponent(url)}`);
      setForm(p => ({ ...p, audioName: data.title || p.audioName, imageUrl: data.imageUrl || p.imageUrl }));
    } catch (e) {
      alert('Failed to fetch metadata');
    }
  };

  const handleSearch = async (query) => {
    setSearchQuery(query);
    if (query.length < 2) {
      setSearchResults([]);
      return;
    }
    try {
      const { data } = await api.get(`/search?q=${encodeURIComponent(query)}`);
      setSearchResults(data.songs || []);
    } catch (err) {
      console.error('Search failed:', err);
    }
  };

  const handleSelectSearchedSong = (song) => {
    setForm({
      audioName: song.audioName,
      audioUrl: song.audioUrl,
      imageUrl: song.imageUrl || '',
      categoryId: form.categoryId // Keep current category
    });
    setIsSearchOpen(false);
  };

  return (
    <div>
      <h2>Songs</h2>
      <div className="toolbar">
        <label>Filter by category:</label>
        <select value={categoryFilter} onChange={(e) => setCategoryFilter(e.target.value)}>
          <option value="">All</option>
          {categories.map((c) => (
            <option key={c.id} value={c.id}>{c.categoryName}</option>
          ))}
        </select>
      </div>

      <div className="card form">
        <form className="inline" onSubmit={submit}>
          <div>
            <label>Audio URL</label>
            <input type="text" value={form.audioUrl} onChange={(e) => setForm((p) => ({ ...p, audioUrl: e.target.value }))} required />
          </div>
          <div>
            <label>Audio Name</label>
            <input type="text" value={form.audioName} onChange={(e) => setForm((p) => ({ ...p, audioName: e.target.value }))} required />
          </div>
          <div>
            <label>Image URL</label>
            <input type="text" value={form.imageUrl} onChange={(e) => setForm((p) => ({ ...p, imageUrl: e.target.value }))} />
          </div>
          <div>
            <label>Category</label>
            <select value={form.categoryId} onChange={(e) => setForm((p) => ({ ...p, categoryId: e.target.value }))} required>
              <option value="">Select</option>
              {categories.map((c) => (
                <option key={c.id} value={c.id}>{c.categoryName}</option>
              ))}
            </select>
          </div>
          <div className="actions" style={{ alignItems: 'flex-end', display: 'flex', gap: '10px' }}>
            <button type="submit">{editingId ? 'Update' : 'Add'}</button>
            {editingId && <button type="button" onClick={() => { setEditingId(null); setForm({ audioName: '', audioUrl: '', imageUrl: '', categoryId: '' }); }}>Cancel</button>}
          </div>
        </form>
      </div>


      <div className="table-container" style={{ marginTop: '20px' }}>
        <table className="table">
          <thead>
            <tr>
              <th>songId</th>
              <th>audioName</th>
              <th>audioUrl</th>
              <th>imageUrl</th>
              <th>categoryIdFormatted</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {items.map((item) => (
              <tr key={item.id}>
                <td><span className="id-badge">{item.songId || formatEntityId('song', item.id)}</span></td>
                <td>{item.audioName}</td>
                <td>{item.audioUrl}</td>
                <td>{item.imageUrl}</td>
                <td><span className="id-badge">{item.categoryIdFormatted || formatEntityId('cat', item.categoryId)}</span></td>
                <td>
                  <div style={{ display: 'flex', gap: '8px' }}>
                    <button onClick={() => {
                      setEditingId(item.id);
                      setForm({
                        audioName: item.audioName,
                        audioUrl: item.audioUrl,
                        imageUrl: item.imageUrl || '',
                        categoryId: String(item.categoryId)
                      });
                    }}>Edit</button>
                    <button className="danger" onClick={async () => {
                      await api.delete(`/admin/song/${item.id}`);
                      await load();
                      onDataChange();
                    }}>Delete</button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

function BulkSongs({ onDataChange, contentType, onEditRequest }) {
  const [categories, setCategories] = useState([]);
  const [rows, setRows] = useState([{ id: Date.now(), audioName: '', audioUrl: '', imageUrl: '', categoryId: '' }]);
  const [isSearchOpen, setIsSearchOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [searchResults, setSearchResults] = useState([]);
  const [targetIdx, setTargetIdx] = useState(null);

  // File Upload State
  const [uploadCategoryId, setUploadCategoryId] = useState('');
  const [selectedFiles, setSelectedFiles] = useState([]);
  const [isUploading, setIsUploading] = useState(false);
  const [uploadImageUrl, setUploadImageUrl] = useState('');
  const [existingSongs, setExistingSongs] = useState([]);

  // Smart Sync State
  const [smartAudioFiles, setSmartAudioFiles] = useState([]);
  const [smartImageFiles, setSmartImageFiles] = useState([]);
  const [matchedPairs, setMatchedPairs] = useState([]);
  const [smartCategoryId, setSmartCategoryId] = useState('');

  const loadExisting = async (catId) => {
    if (!catId) return setExistingSongs([]);
    try {
      const { data } = await api.get(`/admin/songs?contentType=${contentType}&categoryId=${catId}`);
      setExistingSongs(data);
    } catch (err) {
      console.error('Failed to load existing songs', err);
    }
  };

  useEffect(() => {
    loadExisting(uploadCategoryId || smartCategoryId);
  }, [uploadCategoryId, smartCategoryId, contentType]);

  useEffect(() => {
    const pairs = [];
    const audioArray = Array.from(smartAudioFiles);
    const imageArray = Array.from(smartImageFiles);

    audioArray.forEach(audio => {
      const audioBase = audio.name.replace(/\.[^/.]+$/, "").trim().toLowerCase();
      const imageMatch = imageArray.find(img => img.name.replace(/\.[^/.]+$/, "").trim().toLowerCase() === audioBase);
      pairs.push({
        id: Math.random(),
        audio,
        image: imageMatch || null,
        name: audio.name.replace(/\.[^/.]+$/, "").trim() // Keep original casing for title
      });
    });
    setMatchedPairs(pairs);
  }, [smartAudioFiles, smartImageFiles]);

  useEffect(() => {
    api.get(`/admin/categories?contentType=${contentType}`).then(res => setCategories(res.data));
  }, [contentType]);

  const handleFetch = async (url, idx) => {
    if (!url) return;
    const { data } = await api.get(`/admin/fetch-metadata?url=${encodeURIComponent(url)}`);
    setRows(prev => prev.map((r, i) => i === idx ? { ...r, audioName: data.title || r.audioName, imageUrl: data.imageUrl || r.imageUrl } : r));
  };

  const handleSearch = async (q) => {
    setSearchQuery(q);
    if (q.length < 2) return setSearchResults([]);
    const { data } = await api.get(`/search?q=${encodeURIComponent(q)}`);
    setSearchResults(data.songs || []);
  };

  const useSong = (song) => {
    setRows(prev => prev.map((r, i) => i === targetIdx ? { ...r, audioName: song.audioName, audioUrl: song.audioUrl, imageUrl: song.imageUrl || '' } : r));
    setIsSearchOpen(false);
  };

  const submitUrls = async () => {
    try {
      const payload = rows.filter(r => r.audioUrl && r.audioName && r.categoryId).map(r => ({ ...r, categoryId: Number(r.categoryId) }));
      if (payload.length === 0) return alert('Please fill at least one song completely');
      await api.post('/admin/song/bulk', { songs: payload });
      setRows([{ id: Date.now(), audioName: '', audioUrl: '', imageUrl: '', categoryId: '' }]);
      onDataChange();
      alert('Bulk URL upload successful!');
    } catch (e) {
      alert(e.response?.data?.message || 'Error uploading');
    }
  };

  const [bulkFileRows, setBulkFileRows] = useState([{ id: Date.now(), audioFile: null, imageFile: null, audioName: '', categoryId: '' }]);

  const handleBulkFileUpload = async () => {
    const validRows = bulkFileRows.filter(r => r.audioFile && r.categoryId);
    if (validRows.length === 0) return alert('Please select at least one audio file and category');

    setIsUploading(true);
    let successCount = 0;

    for (const row of validRows) {
      const formData = new FormData();
      formData.append('categoryId', row.categoryId);
      formData.append('audioName', row.audioName || row.audioFile.name.replace(/\.[^/.]+$/, ""));
      formData.append('files', row.audioFile); // Audio
      if (row.imageFile) {
        formData.append('files', row.imageFile); // Image
      }

      try {
        await api.post('/admin/song/upload', formData, {
          headers: { 'Content-Type': 'multipart/form-data' }
        });
        successCount++;
      } catch (err) {
        console.error('Failed to upload row', row.id, err);
      }
    }

    setBulkFileRows([{ id: Date.now(), audioFile: null, imageFile: null, audioName: '', categoryId: '' }]);
    onDataChange();
    await loadExisting(uploadCategoryId);
    setIsUploading(false);
    alert(`Successfully uploaded ${successCount} songs!`);
  };

  const handleSmartUpload = async () => {
    if (!smartCategoryId) return alert('Select a category');
    if (matchedPairs.length === 0) return alert('No matched pairs to upload');

    setIsUploading(true);
    let successCount = 0;
    let failCount = 0;

    for (const pair of matchedPairs) {
      const formData = new FormData();
      formData.append('categoryId', smartCategoryId);
      formData.append('audioName', pair.name);
      formData.append('files', pair.audio);
      if (pair.image) formData.append('files', pair.image);

      try {
        await api.post('/admin/song/upload', formData, { headers: { 'Content-Type': 'multipart/form-data' } });
        successCount++;
      } catch (err) {
        console.error('Failed smart upload', pair.name, err);
        failCount++;
      }
    }

    setSmartAudioFiles([]);
    setSmartImageFiles([]);
    setMatchedPairs([]);
    onDataChange();
    setIsUploading(false);
    if (failCount > 0) {
      alert(`Sync Complete: ${successCount} successful, ${failCount} failed. Check console for details.`);
    } else {
      alert(`Smart Sync Complete: ${successCount} songs uploaded!`);
    }
  };

  const handleFileUpload = async (e) => {
    e.preventDefault();
    if (!uploadCategoryId) return alert('Please select a category');
    if (selectedFiles.length === 0) return alert('Please select files to upload');

    setIsUploading(true);
    const formData = new FormData();
    formData.append('categoryId', uploadCategoryId);
    formData.append('imageUrl', uploadImageUrl);
    for (let i = 0; i < selectedFiles.length; i++) {
      formData.append('files', selectedFiles[i]);
    }

    try {
      await api.post('/admin/song/upload', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      });
      setSelectedFiles([]);
      setUploadCategoryId('');
      setUploadImageUrl('');
      onDataChange();
      await loadExisting(uploadCategoryId);
      alert('Files uploaded successfully!');
    } catch (err) {
      alert(err.response?.data?.message || 'Upload failed');
    } finally {
      setIsUploading(false);
    }
  };

  return (
    <div className="bulk-page">
      {/* Section 1: Smart Bulk Sync (Auto-Match) */}
      <section style={{ marginBottom: '30px' }}>
        <div className="card" style={{ background: 'linear-gradient(135deg, #f8faff 0%, #ffffff 100%)', border: '1px solid #e2e8f0' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '10px' }}>
            <h2>🚀 Smart Bulk Sync (Auto-Match Files)</h2>
            <p className="muted">Pair audio and images automatically by filename.</p>
          </div>
          
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '20px', marginBottom: '20px' }}>
            <div>
              <label>1. Target Category</label>
              <select value={smartCategoryId} onChange={e => setSmartCategoryId(e.target.value)} style={{ width: '100%', padding: '10px' }}>
                <option value="">Select Category</option>
                {categories.map(c => <option key={c.id} value={c.id}>{c.categoryName}</option>)}
              </select>
            </div>
            <div>
              <label>2. Select Audio Files</label>
              <input type="file" multiple accept="audio/*" onChange={e => setSmartAudioFiles(e.target.files)} style={{ width: '100%' }} />
            </div>
            <div>
              <label>3. Select Image Files</label>
              <input type="file" multiple accept="image/*" onChange={e => setSmartImageFiles(e.target.files)} style={{ width: '100%' }} />
            </div>
          </div>

          {matchedPairs.length > 0 && (
            <div className="table-container" style={{ maxHeight: '300px', overflowY: 'auto', border: '1px solid #eee', borderRadius: '8px', marginBottom: '20px' }}>
              <table className="table small">
                <thead>
                  <tr>
                    <th>Audio File</th>
                    <th>Matched Image</th>
                    <th>Final Name</th>
                  </tr>
                </thead>
                <tbody>
                  {matchedPairs.map(p => (
                    <tr key={p.id}>
                      <td>{p.audio.name}</td>
                      <td>{p.image ? <span style={{ color: 'green' }}>✅ {p.image.name}</span> : <span style={{ color: 'orange' }}>⚠️ No match (Upload anyway)</span>}</td>
                      <td><input value={p.name} onChange={e => setMatchedPairs(prev => prev.map(item => item.id === p.id ? { ...item, name: e.target.value } : item))} style={{ width: '100%', padding: '4px' }} /></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}

          <button className="primary" onClick={handleSmartUpload} disabled={isUploading} style={{ width: '100%', padding: '15px', fontWeight: '600' }}>
            {isUploading ? 'Syncing...' : `Sync and Upload ${matchedPairs.length} Songs`}
          </button>
        </div>
      </section>

      {/* Section 2: Existing Songs Preview */}
      {(smartCategoryId || uploadCategoryId) && (
        <section style={{ marginTop: '30px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '15px' }}>
            <h2>Existing Songs in Selected Category</h2>
            <span className="id-badge">{existingSongs.length} Songs</span>
          </div>
          <div className="table-container card">
            <table className="table">
              <thead>
                <tr>
                  <th>songId</th>
                  <th>audioName</th>
                  <th>audioUrl</th>
                  <th>imageUrl</th>
                  <th>catId</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {existingSongs.length === 0 ? (
                  <tr><td colSpan="6" className="muted" style={{ textAlign: 'center', padding: '20px' }}>No songs found in this category</td></tr>
                ) : (
                  existingSongs.map(s => (
                    <tr key={s.id}>
                      <td><span className="id-badge">{s.songId || formatEntityId('song', s.id)}</span></td>
                      <td>{s.audioName}</td>
                      <td style={{ fontSize: '0.8rem', opacity: 0.7 }}>{s.audioUrl}</td>
                      <td style={{ fontSize: '0.8rem', opacity: 0.7 }}>{s.imageUrl}</td>
                      <td><span className="id-badge">{s.categoryIdFormatted || formatEntityId('cat', s.categoryId)}</span></td>
                      <td>
                        <div style={{ display: 'flex', gap: '8px' }}>
                          <button className="small" onClick={() => onEditRequest(s)}>Edit</button>
                          <button className="danger small" onClick={async () => {
                            if (window.confirm(`Delete ${s.audioName}?`)) {
                              await api.delete(`/admin/song/${s.id}`);
                              await loadExisting(smartCategoryId || uploadCategoryId);
                              onDataChange();
                            }
                          }}>Delete</button>
                        </div>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </section>
      )}

      {isSearchOpen && (
        <div className="modal-overlay" style={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, backgroundColor: 'rgba(0,0,0,0.5)', display: 'flex', justifyContent: 'center', alignItems: 'center', zIndex: 100 }}>
          <div className="card" style={{ width: '500px', maxHeight: '80vh', overflowY: 'auto' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <h3>Search & Select</h3>
              <button onClick={() => setIsSearchOpen(false)}>Close</button>
            </div>
            <input type="text" placeholder="Search library..." value={searchQuery} onChange={e => handleSearch(e.target.value)} style={{ width: '100%', margin: '10px 0' }} />
            <div className="search-results">
              {searchResults.map(s => (
                <div key={s.songId} style={{ display: 'flex', justifyContent: 'space-between', padding: '10px', borderBottom: '1px solid #eee' }}>
                  <span>{s.audioName}</span>
                  <button type="button" onClick={() => useSong(s)}>Pick</button>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

function MenuManager({ onDataChange }) {
  const [items, setItems] = useState([]);
  const [menuTypeFilter, setMenuTypeFilter] = useState('top');
  const [form, setForm] = useState({
    menuType: 'top',
    menuName: '',
    menuStatusId: 1,
    menuStatus: 'Active',
    menuActiveIcon: '',
    menuInActiveIcon: '',
    sortOrder: 1
  });
  const [editingId, setEditingId] = useState(null);

  const load = async () => {
    const { data } = await api.get(`/admin/menu?menuType=${menuTypeFilter}`);
    setItems(data);
  };

  useEffect(() => {
    load();
    setEditingId(null);
    setForm((prev) => ({ ...prev, menuType: menuTypeFilter }));
  }, [menuTypeFilter]);

  const submit = async (e) => {
    e.preventDefault();
    const payload = {
      ...form,
      menuStatusId: Number(form.menuStatusId),
      sortOrder: Number(form.sortOrder),
      menuType: menuTypeFilter
    };

    if (editingId) {
      await api.put(`/admin/menu/${editingId}`, payload);
    } else {
      await api.post('/admin/menu', payload);
    }

    setForm({
      menuType: menuTypeFilter,
      menuName: '',
      menuStatusId: 1,
      menuStatus: 'Active',
      menuActiveIcon: '',
      menuInActiveIcon: '',
      sortOrder: 1
    });
    setEditingId(null);
    await load();
    onDataChange();
  };

  const remove = async (id) => {
    await api.delete(`/admin/menu/${id}`);
    await load();
    onDataChange();
  };

  return (
    <div>
      <h2>Menu Management</h2>
      <div className="toolbar">
        <label>Menu type:</label>
        <select value={menuTypeFilter} onChange={(e) => setMenuTypeFilter(e.target.value)}>
          <option value="top">Top Menu</option>
          <option value="bottom">Bottom Menu</option>
        </select>
      </div>
      <EntityPage
        title={null}
        form={form}
        setForm={setForm}
        editingId={editingId}
        setEditingId={setEditingId}
        onSubmit={submit}
        items={items}
        onEdit={(item) => {
          setEditingId(item.id);
          setForm({
            menuType: item.menuType,
            menuName: item.menuName,
            menuStatusId: item.menuStatusId,
            menuStatus: item.menuStatus,
            menuActiveIcon: item.menuActiveIcon,
            menuInActiveIcon: item.menuInActiveIcon,
            sortOrder: item.sortOrder
          });
        }}
        onDelete={remove}
        fields={[
          { key: 'menuName', label: 'Menu Name' },
          { key: 'menuStatusId', label: 'Status ID', type: 'number' },
          { key: 'menuStatus', label: 'Status' },
          { key: 'menuActiveIcon', label: 'Active Icon URL' },
          { key: 'menuInActiveIcon', label: 'Inactive Icon URL' },
          { key: 'sortOrder', label: 'Sort Order', type: 'number' }
        ]}
        columns={['id', 'menuType', 'menuName', 'menuStatusId', 'menuStatus', 'sortOrder']}
      />
    </div>
  );
}

function EntityPage({
  title,
  form,
  setForm,
  editingId,
  setEditingId,
  onSubmit,
  items,
  onEdit,
  onDelete,
  fields,
  columns,
  rowTransform
}) {
  const titleText = useMemo(() => (editingId ? 'Update' : 'Add'), [editingId]);
  const isIdColumn = (columnName) => columnName.endsWith('Id') || columnName.endsWith('Formatted');

  const handleFileChange = async (key, file) => {
    if (!file) return;
    const formData = new FormData();
    formData.append('file', file);
    try {
      const { data } = await api.post('/admin/upload', formData);
      setForm(p => ({ ...p, [key]: data.url }));
    } catch (e) {
      alert('Upload failed');
    }
  };

  return (
    <div>
      {title && <h2>{title}</h2>}
      <form className="card form inline" onSubmit={onSubmit}>
        {fields.map((field) => (
          <div key={field.key}>
            <label>{field.label}</label>
            {field.type === 'select' ? (
              <select
                value={form[field.key]}
                onChange={(e) => setForm((p) => ({ ...p, [field.key]: e.target.value }))}
                required
              >
                <option value="">Select</option>
                {field.options.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            ) : (
              <div style={{ display: 'flex', gap: '5px' }}>
                <input
                  type={field.type || 'text'}
                  value={form[field.key]}
                  onChange={(e) => setForm((p) => ({ ...p, [field.key]: e.target.value }))}
                  required={field.required !== false}
                  style={{ flex: 1 }}
                />
                {field.allowUpload && (
                  <>
                    <input
                      type="file"
                      id={`file-${field.key}`}
                      style={{ display: 'none' }}
                      accept="image/*"
                      onChange={(e) => handleFileChange(field.key, e.target.files[0])}
                    />
                    <button 
                      type="button" 
                      className="small" 
                      onClick={() => document.getElementById(`file-${field.key}`).click()}
                      title="Upload Image"
                    >
                      📁
                    </button>
                  </>
                )}
              </div>
            )}
          </div>
        ))}
        <div className="actions">
          <button type="submit">{titleText}</button>
          {editingId && (
            <button
              type="button"
              onClick={() => {
                setEditingId(null);
                const reset = {};
                fields.forEach((f) => {
                  reset[f.key] = f.type === 'number' ? 1 : '';
                });
                setForm((p) => ({ ...p, ...reset }));
              }}
            >
              Cancel
            </button>
          )}
        </div>
      </form>
      <div className="table-container">
        <table className="table">
          <thead>
            <tr>
              {columns.map((col) => (
                <th key={col}>{col}</th>
              ))}
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {items.map((item, index) => {
              const row = rowTransform ? rowTransform(item, index) : item;
              return (
              <tr key={item.id}>
                {columns.map((col) => (
                  <td key={col}>
                    {isIdColumn(col) ? <span className="id-badge">{row[col]}</span> : row[col]}
                  </td>
                ))}
                <td>
                  <div style={{ display: 'flex', gap: '8px' }}>
                    <button onClick={() => onEdit(item)}>Edit</button>
                    <button className="danger" onClick={() => onDelete(item.id)}>Delete</button>
                  </div>
                </td>
              </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </div>
  );
}

function Users({ onDataChange }) {
  const [items, setItems] = useState([]);

  const load = async () => {
    const { data } = await api.get('/admin/users');
    setItems(data);
  };

  useEffect(() => {
    load();
  }, []);

  const remove = async (id) => {
    if (window.confirm('Are you sure you want to delete this user?')) {
      await api.delete(`/admin/user/${id}`);
      await load();
      onDataChange();
    }
  };

  return (
    <div>
      <h2>User Management</h2>
      <table className="table">
        <thead>
          <tr>
            <th>ID</th>
            <th>Name</th>
            <th>Email</th>
            <th>Mobile</th>
            <th>Country</th>
            <th>Verified</th>
            <th>Created Date</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          {items.map((user) => (
            <tr key={user.id}>
              <td><span className="id-badge">user_{String(user.id).padStart(3, '0')}</span></td>
              <td>{user.userName}</td>
              <td>{user.userEmail || <span className="muted">N/A</span>}</td>
              <td>{user.userMobile}</td>
              <td>{user.userCountry} ({user.userCountryCode})</td>
              <td>{user.isVerified ? '✅' : '❌'}</td>
              <td>{user.userCreatedDate ? new Date(user.userCreatedDate).toLocaleString() : 'N/A'}</td>
              <td>
                <button className="danger" onClick={() => remove(user.id)}>Delete</button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

export default App;
