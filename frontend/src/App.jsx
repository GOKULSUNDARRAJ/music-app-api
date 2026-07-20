import { useEffect, useMemo, useState } from 'react';
import api, { setAuthToken } from './api';

const TABS = ['Dashboard', 'Sections', 'Categories', 'Advanced Bulk', 'Attributes', 'Lyrics', 'Menu', 'Users', 'Ads'];
const formatEntityId = (prefix, id) => `${prefix}_${String(id).padStart(3, '0')}`;
const CONTENT_TYPES = [
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
  const [initialSectionFilter, setInitialSectionFilter] = useState('');
  const [initialFormSection, setInitialFormSection] = useState('');

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
              {tab === 'Advanced Bulk' && '🛸'}
              {tab === 'Attributes' && '🏷️'}
              {tab === 'Lyrics' && '🎤'}
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
        {['Dashboard', 'Sections', 'Categories'].includes(activeTab) && (
          <div className="toolbar content-type-toolbar" style={{ display: 'flex', gap: '10px', alignItems: 'center' }}>
            <span style={{ fontWeight: 'bold', marginRight: '10px', color: 'var(--text-muted)' }}>MANAGE CONTENT:</span>
            <div style={{ display: 'flex', gap: '8px', background: '#000', padding: '12px 16px', borderRadius: '30px' }}>
              <button
                key="all"
                onClick={() => setContentType('')}
                style={{
                  background: contentType === '' ? '#ef4444' : '#333',
                  color: '#fff',
                  border: 'none',
                  borderRadius: '20px',
                  padding: '8px 24px',
                  fontWeight: 'bold',
                  cursor: 'pointer',
                  transition: 'background 0.2s',
                  boxShadow: 'none',
                  transform: 'none'
                }}
              >
                All
              </button>
              {CONTENT_TYPES.map((type) => (
                <button
                  key={type.value}
                  onClick={() => setContentType(type.value)}
                  style={{
                    background: contentType === type.value ? '#ef4444' : '#333',
                    color: '#fff',
                    border: 'none',
                    borderRadius: '20px',
                    padding: '8px 24px',
                    fontWeight: 'bold',
                    cursor: 'pointer',
                    transition: 'background 0.2s',
                    boxShadow: 'none',
                    transform: 'none'
                  }}
                >
                  {type.label}
                </button>
              ))}
            </div>
          </div>
        )}
        {['Sections', 'Categories'].includes(activeTab) && (
          <div className="toolbar content-type-toolbar">
            <small>Working on {contentType.toUpperCase()} content</small>
          </div>
        )}
        {activeTab === 'Dashboard' && <Dashboard refreshKey={refreshKey} contentType={contentType} onDataChange={onDataChange} />}
        {activeTab === 'Sections' && <Sections onDataChange={onDataChange} contentType={contentType} onViewCategories={(sid) => { setInitialSectionFilter(sid); setInitialFormSection(''); setActiveTab('Categories'); }} />}
        {activeTab === 'Categories' && <Categories onDataChange={onDataChange} contentType={contentType} initialSectionFilter={initialSectionFilter} clearInitialSectionFilter={() => setInitialSectionFilter('')} initialFormSection={initialFormSection} />}
        {activeTab === 'Songs' && <Songs onDataChange={onDataChange} contentType={contentType} initialEditSong={songToEdit} clearEditSong={() => setSongToEdit(null)} />}
        {activeTab === 'Bulk Songs' && <BulkSongs onDataChange={onDataChange} contentType={contentType} onEditRequest={(s) => { setSongToEdit(s); setActiveTab('Songs'); }} />}
        {activeTab === 'Advanced Bulk' && <AdvancedBulkSongs onDataChange={onDataChange} contentType={contentType} onEditRequest={(s) => { setSongToEdit(s); setActiveTab('Songs'); }} />}
        {activeTab === 'Attributes' && <AttributesManager onDataChange={onDataChange} />}
        {activeTab === 'Lyrics' && <LyricsManager onDataChange={onDataChange} />}
        {activeTab === 'Menu' && <MenuManager onDataChange={onDataChange} />}
        {activeTab === 'Users' && <Users onDataChange={onDataChange} />}
        {activeTab === 'Ads' && <AdsManager />}

      </main>
    </div>
  );
}

function Dashboard({ refreshKey, contentType, onDataChange }) {
  const [counts, setCounts] = useState({ sections: 0, categories: 0, songs: 0, users: 0 });
  const [nestedData, setNestedData] = useState({ sections: [] });
  const [selectedCategory, setSelectedCategory] = useState(null);
  const [assignSectionId, setAssignSectionId] = useState(null);
  const [assignSongCategoryId, setAssignSongCategoryId] = useState(null);

  useEffect(() => {
    api.get('/admin/dashboard').then((res) => setCounts(res.data));
  }, [refreshKey]);

  useEffect(() => {
    let endpoint = '/home';
    if (contentType === 'devotional') endpoint = '/devotional';
    if (contentType === 'artist') endpoint = '/artist';
    api.get(endpoint).then((res) => {
      setNestedData(res.data);
      // If a category is currently open, update its reference with the newly fetched data
      setSelectedCategory(prev => {
        if (!prev) return null;
        for (const section of (res.data.sections || [])) {
          const updatedCat = section.categories?.find(c => c.categoryId === prev.categoryId);
          if (updatedCat) return updatedCat;
        }
        return prev;
      });
    });
  }, [contentType, refreshKey]);

  const statCards = [
    { label: 'Sections', value: counts.sections, icon: '🗂️', color: '#6366f1' },
    { label: 'Categories', value: counts.categories, icon: '📂', color: '#8b5cf6' },
    { label: 'Songs', value: counts.songs, icon: '🎵', color: '#ec4899' },
    { label: 'Users', value: counts.users || 0, icon: '👥', color: '#14b8a6' },
  ];

  return (
    <div>
      {assignSectionId ? (
        <AssignCategoryView 
          sectionId={assignSectionId} 
          contentType={contentType} 
          onBack={() => setAssignSectionId(null)} 
          onAssigned={() => { setAssignSectionId(null); onDataChange(); }} 
        />
      ) : assignSongCategoryId ? (
        <AssignSongView 
          categoryId={assignSongCategoryId} 
          contentType={contentType} 
          onBack={() => setAssignSongCategoryId(null)} 
          onAssigned={() => { setAssignSongCategoryId(null); onDataChange(); }} 
        />
      ) : selectedCategory ? (
        <CategoryDetailView category={selectedCategory} onBack={() => setSelectedCategory(null)} onAddSong={() => setAssignSongCategoryId(selectedCategory.categoryId)} />
      ) : (
        <>
          <h2 style={{ fontWeight: 800, fontSize: '2rem', marginBottom: 28, background: 'linear-gradient(135deg,#6366f1,#a855f7)', WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent' }}>Dashboard</h2>

      {/* Stat Cards */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit,minmax(200px,1fr))', gap: 20, marginBottom: 40 }}>
        {statCards.map(s => (
          <div key={s.label} style={{
            background: '#fff',
            borderRadius: 16,
            padding: '24px 28px',
            boxShadow: '0 4px 24px rgba(0,0,0,0.07)',
            display: 'flex',
            alignItems: 'center',
            gap: 18,
            border: `2px solid ${s.color}18`,
            transition: 'transform 0.2s,box-shadow 0.2s',
          }}
            onMouseEnter={e => { e.currentTarget.style.transform = 'translateY(-3px)'; e.currentTarget.style.boxShadow = `0 12px 32px ${s.color}22`; }}
            onMouseLeave={e => { e.currentTarget.style.transform = 'translateY(0)'; e.currentTarget.style.boxShadow = '0 4px 24px rgba(0,0,0,0.07)'; }}
          >
            <div style={{ width: 52, height: 52, borderRadius: 14, background: `${s.color}18`, display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 26 }}>{s.icon}</div>
            <div>
              <div style={{ fontSize: 32, fontWeight: 800, color: s.color, lineHeight: 1 }}>{s.value}</div>
              <div style={{ fontSize: 13, fontWeight: 600, color: '#64748b', marginTop: 4 }}>{s.label}</div>
            </div>
          </div>
        ))}
      </div>

      {/* Content Structure Grid */}
      <div>
        <h3 style={{ fontWeight: 700, fontSize: '1.25rem', marginBottom: 8, color: '#1e293b' }}>
          {contentType ? contentType.toUpperCase() : 'ALL'} Content Structure
        </h3>

        {(nestedData.sections || []).length === 0 ? (
          <p className="muted">No sections found for this content type.</p>
        ) : (
          (nestedData.sections || []).map((section) => (
            <div key={section.sectionId} style={{ marginBottom: 48 }}>
              {/* Section header */}
              <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 20 }}>
                <div style={{ width: 4, height: 28, borderRadius: 4, background: 'linear-gradient(180deg,#6366f1,#a855f7)' }} />
                <span style={{ fontFamily: 'monospace', fontSize: 11, fontWeight: 700, background: '#f1f5f9', color: '#475569', padding: '3px 8px', borderRadius: 6 }}>
                  sec_{String(section.sectionId).padStart(3, '0')}
                </span>
                <span style={{ fontWeight: 700, fontSize: '1.1rem', color: '#1e293b' }}>{section.sectionTitle}</span>
              </div>

              {/* Categories as media cards */}
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill,minmax(180px,1fr))', gap: 24, paddingLeft: 16 }}>
                {(section.categories || []).map((category) => (
                  <CategoryCard key={category.categoryId} category={category} onSelect={() => setSelectedCategory(category)} />
                ))}
                
                {/* Add Category Button Card */}
                <div
                  onClick={() => setAssignSectionId(section.sectionId)}
                  style={{
                    cursor: 'pointer', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center',
                    background: '#f8fafc', borderRadius: 14, border: '2px dashed #cbd5e1',
                    aspectRatio: '1/1', transition: 'all 0.2s', color: '#64748b',
                    height: 'auto'
                  }}
                  onMouseEnter={e => { e.currentTarget.style.background = '#f1f5f9'; e.currentTarget.style.borderColor = '#94a3b8'; e.currentTarget.style.color = '#475569'; }}
                  onMouseLeave={e => { e.currentTarget.style.background = '#f8fafc'; e.currentTarget.style.borderColor = '#cbd5e1'; e.currentTarget.style.color = '#64748b'; }}
                >
                  <div style={{ fontSize: 32, marginBottom: 8, fontWeight: 300 }}>+</div>
                  <div style={{ fontWeight: 600, fontSize: 14 }}>Add Category</div>
                </div>
              </div>
            </div>
          ))
        )}
      </div>
        </>
      )}
    </div>
  );
}

function CategoryCard({ category, onSelect }) {
  const [hovered, setHovered] = useState(false);
  const songCount = (category.songs || []).length;
  const imgSrc = category.categoryImage || (category.songs?.[0]?.imageUrl) || null;

  return (
    <>
      {/* Card */}
      <div
        style={{ cursor: 'pointer', display: 'flex', flexDirection: 'column' }}
        onMouseEnter={() => setHovered(true)}
        onMouseLeave={() => setHovered(false)}
        onClick={onSelect}
      >
        {/* Image wrapper */}
        <div style={{ position: 'relative', width: '100%', aspectRatio: '1/1', borderRadius: 14, overflow: 'hidden', boxShadow: hovered ? '0 12px 32px rgba(0,0,0,0.15)' : '0 4px 12px rgba(0,0,0,0.08)', transition: 'all 0.3s ease' }}>
          <img src={imgSrc || 'https://via.placeholder.com/300?text=No+Image'} alt={category.categoryName} style={{ width: '100%', height: '100%', objectFit: 'cover', transform: hovered ? 'scale(1.05)' : 'scale(1)', transition: 'transform 0.5s ease' }} />

          {/* Hover overlay */}
          <div style={{
            position: 'absolute', inset: 0,
            background: 'linear-gradient(to top, rgba(0,0,0,0.85) 0%, rgba(0,0,0,0.3) 60%, transparent 100%)',
            opacity: hovered ? 1 : 0, transition: 'opacity 0.3s ease',
            display: 'flex', flexDirection: 'column', justifyContent: 'flex-end', padding: 12,
          }}>
            <div style={{ color: '#fff', fontSize: 18, fontWeight: 700, textAlign: 'center', marginBottom: 6 }}>
              👁️ View Songs
            </div>
          </div>

          {/* Song count pill */}
          <div style={{
            position: 'absolute', top: 10, right: 10,
            background: 'rgba(0,0,0,0.55)', backdropFilter: 'blur(6px)',
            color: '#fff', fontSize: 11, fontWeight: 700,
            padding: '3px 8px', borderRadius: 20,
          }}>
            {songCount} songs
          </div>
        </div>

        {/* Info below image */}
        <div style={{ paddingTop: 10 }}>
          <div style={{ fontWeight: 700, fontSize: 14, color: '#1e293b', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', marginBottom: 4 }}>
            {category.categoryName}
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 5 }}>
            {songCount > 0 && <span style={{ fontSize: 13, color: '#f59e0b', fontWeight: 600 }}>⚡ Active</span>}
          </div>
        </div>
      </div>
    </>
  );
}

function CategoryDetailView({ category, onBack, onAddSong }) {
  const songCount = (category.songs || []).length;
  const imgSrc = category.categoryImage || (category.songs?.[0]?.imageUrl) || null;

  return (
    <div style={{ background: '#fff', borderRadius: 20, overflow: 'hidden', boxShadow: '0 4px 24px rgba(0,0,0,0.05)', display: 'flex', flexDirection: 'column', minHeight: '100%' }}>
      {/* Header */}
      <div style={{
        display: 'flex', alignItems: 'center', gap: 20,
        padding: '30px 40px',
        background: 'linear-gradient(135deg,#0f172a,#1e1b4b)',
      }}>
        <button
          onClick={onBack}
          style={{ background: 'rgba(255,255,255,0.1)', color: '#fff', border: 'none', borderRadius: 12, padding: '12px 18px', fontWeight: 700, fontSize: 16, cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 8, transition: 'background 0.2s' }}
          onMouseEnter={e => e.currentTarget.style.background = 'rgba(255,255,255,0.2)'}
          onMouseLeave={e => e.currentTarget.style.background = 'rgba(255,255,255,0.1)'}
        >
          <span>←</span> Back
        </button>
        {imgSrc && (
          <img src={imgSrc} alt={category.categoryName}
            style={{ width: 100, height: 100, borderRadius: 16, objectFit: 'cover', flexShrink: 0, boxShadow: '0 8px 24px rgba(0,0,0,0.4)' }}
            onError={e => { e.target.style.display = 'none'; }}
          />
        )}
        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={{ fontWeight: 800, fontSize: '2.5rem', color: '#fff', marginBottom: 8 }}>
            {category.categoryName}
          </div>
          <div style={{ display: 'flex', gap: 12, alignItems: 'center' }}>
            <span style={{ fontSize: 14, color: '#94a3b8', fontWeight: 600 }}>{songCount} songs</span>
          </div>
        </div>
        <div style={{ marginLeft: 'auto' }}>
          <button 
            onClick={onAddSong}
            style={{ background: '#ec4899', color: '#fff', border: 'none', padding: '12px 24px', borderRadius: 12, fontWeight: 800, cursor: 'pointer', transition: 'all 0.2s', boxShadow: '0 8px 24px rgba(236,72,153,0.4)', fontSize: '1.1rem' }}
            onMouseEnter={e => e.currentTarget.style.transform = 'translateY(-2px)'}
            onMouseLeave={e => e.currentTarget.style.transform = 'translateY(0)'}
          >
            + Add Song
          </button>
        </div>
      </div>

      {/* Song List */}
      <div style={{ flex: 1, padding: 20 }}>
        {(category.songs || []).length === 0 ? (
          <div style={{ padding: 60, textAlign: 'center', color: '#94a3b8', fontSize: 18 }}>No songs found in this category</div>
        ) : (
          <div style={{ display: 'grid', gap: 8 }}>
            {(category.songs || []).map((song, idx) => (
              <div key={song.songId} style={{
                display: 'flex', alignItems: 'center', gap: 16,
                padding: '16px 24px',
                borderRadius: 12,
                border: '1px solid #f1f5f9',
                transition: 'all 0.2s',
                cursor: 'default',
              }}
                onMouseEnter={e => { e.currentTarget.style.background = '#f8fafc'; e.currentTarget.style.borderColor = '#e2e8f0'; }}
                onMouseLeave={e => { e.currentTarget.style.background = 'transparent'; e.currentTarget.style.borderColor = '#f1f5f9'; }}
              >
                {/* Track number */}
                <div style={{ width: 32, textAlign: 'center', fontWeight: 700, fontSize: 15, color: '#cbd5e1', flexShrink: 0 }}>
                  {idx + 1}
                </div>

                {/* Album art thumbnail */}
                <div style={{ width: 56, height: 56, borderRadius: 10, overflow: 'hidden', background: '#1e293b', flexShrink: 0 }}>
                  {song.imageUrl ? (
                    <img src={song.imageUrl} alt={song.audioName}
                      style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                      onError={e => { e.target.style.display = 'none'; }}
                    />
                  ) : (
                    <div style={{ width: '100%', height: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 24 }}>🎵</div>
                  )}
                </div>

                {/* Song info */}
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ fontWeight: 700, fontSize: 16, color: '#1e293b', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                    {song.audioName}
                  </div>
                  <div style={{ fontSize: 14, color: '#64748b', marginTop: 4 }}>
                    {song.duration || 'Unknown Duration'}
                  </div>
                </div>

                {/* Song ID badge */}
                <span style={{ fontFamily: 'monospace', fontSize: 12, fontWeight: 700, background: '#f1f5f9', color: '#475569', padding: '6px 12px', borderRadius: 8, flexShrink: 0 }}>
                  {song.songId}
                </span>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

function AssignCategoryView({ sectionId, contentType, onBack, onAssigned }) {
  const [categories, setCategories] = useState([]);
  const [search, setSearch] = useState('');
  const [selectedIds, setSelectedIds] = useState(new Set());
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    api.get(`/admin/categories?contentType=${contentType}&sectionId=unassigned`)
      .then(res => setCategories(res.data))
      .catch(err => console.error(err));
  }, [contentType]);

  const toggleSelect = (id) => {
    const newSet = new Set(selectedIds);
    if (newSet.has(id)) newSet.delete(id);
    else newSet.add(id);
    setSelectedIds(newSet);
  };

  const handleAssign = async () => {
    if (selectedIds.size === 0) return;
    setLoading(true);
    try {
      const rawSectionId = parseInt(String(sectionId).replace(/\D/g, ''), 10);
      await Promise.all(
        Array.from(selectedIds).map(id => {
          const fd = new FormData();
          fd.append('sectionId', rawSectionId);
          return api.put(`/admin/category/${id}`, fd, { headers: { 'Content-Type': 'multipart/form-data' } });
        })
      );
      onAssigned();
    } catch (err) {
      console.error(err);
      alert('Failed to assign some categories.');
    } finally {
      setLoading(false);
    }
  };

  const filtered = categories.filter(c => c.categoryName.toLowerCase().includes(search.toLowerCase()));

  return (
    <div style={{ background: '#fff', borderRadius: 20, padding: 30, boxShadow: '0 4px 24px rgba(0,0,0,0.05)' }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 20, marginBottom: 30 }}>
        <button onClick={onBack} style={{ background: '#f1f5f9', border: 'none', padding: '10px 16px', borderRadius: 10, cursor: 'pointer', fontWeight: 600 }}>← Back</button>
        <h2 style={{ margin: 0, fontSize: '1.8rem' }}>Assign Categories to Section <span style={{ color: '#6366f1' }}>{sectionId}</span></h2>
        <div style={{ marginLeft: 'auto' }}>
          <button 
            onClick={handleAssign} 
            disabled={selectedIds.size === 0 || loading}
            style={{ background: '#6366f1', color: '#fff', border: 'none', padding: '10px 20px', borderRadius: 10, fontWeight: 700, cursor: selectedIds.size > 0 ? 'pointer' : 'not-allowed', opacity: selectedIds.size > 0 ? 1 : 0.5, transition: 'all 0.2s' }}
          >
            {loading ? 'Assigning...' : `Assign Selected (${selectedIds.size})`}
          </button>
        </div>
      </div>

      <input 
        type="text" 
        placeholder="🔍 Search unassigned categories..." 
        value={search} 
        onChange={e => setSearch(e.target.value)} 
        style={{ width: '100%', padding: '14px 20px', fontSize: 16, borderRadius: 12, border: '2px solid #e2e8f0', marginBottom: 30, outline: 'none' }}
      />

      {categories.length === 0 ? (
        <p className="muted" style={{ textAlign: 'center', marginTop: 40, fontSize: 18 }}>No unassigned categories available. Create some in the Categories tab first!</p>
      ) : filtered.length === 0 ? (
        <p className="muted" style={{ textAlign: 'center', marginTop: 40, fontSize: 16 }}>No categories match your search.</p>
      ) : (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(200px, 1fr))', gap: 20 }}>
          {filtered.map(cat => {
            const isSelected = selectedIds.has(cat.id);
            return (
              <div 
                key={cat.id} 
                onClick={() => toggleSelect(cat.id)}
                style={{
                  cursor: 'pointer', borderRadius: 16, overflow: 'hidden', background: '#f8fafc',
                  border: `3px solid ${isSelected ? '#6366f1' : 'transparent'}`,
                  transition: 'all 0.2s', position: 'relative'
                }}
              >
                <img src={cat.categoryImage || 'https://via.placeholder.com/300?text=No+Image'} style={{ width: '100%', aspectRatio: '1/1', objectFit: 'cover' }} onError={e => { e.target.style.display = 'none'; }} />
                <div style={{ padding: '12px 16px' }}>
                  <div style={{ fontWeight: 700, color: '#1e293b', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis', fontSize: 15 }}>{cat.categoryName}</div>
                </div>
                {isSelected && (
                  <div style={{ position: 'absolute', top: 10, right: 10, background: '#6366f1', color: '#fff', width: 28, height: 28, borderRadius: '50%', display: 'flex', alignItems: 'center', justifyContent: 'center', fontWeight: 'bold', boxShadow: '0 4px 12px rgba(99,102,241,0.5)' }}>✓</div>
                )}
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}

function AssignSongView({ categoryId, contentType, onBack, onAssigned }) {
  const [songs, setSongs] = useState([]);
  const [search, setSearch] = useState('');
  const [selectedIds, setSelectedIds] = useState(new Set());
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    api.get(`/admin/songs`)
      .then(res => {
        const preSelected = new Set();
        const rawCategoryId = parseInt(String(categoryId).replace(/\D/g, ''), 10);
        res.data.forEach(s => {
          const inJunction = s.categories?.some(c => String(c.id) === String(rawCategoryId));
          const inPrimary = String(s.categoryId) === String(rawCategoryId);
          if (inJunction || inPrimary) preSelected.add(s.id);
        });
        
        // Sort only once initially
        const sortedData = res.data.sort((a, b) => {
          const aSelected = preSelected.has(a.id) ? 1 : 0;
          const bSelected = preSelected.has(b.id) ? 1 : 0;
          return bSelected - aSelected;
        });

        setSongs(sortedData);
        setSelectedIds(preSelected);
      })
      .catch(err => console.error(err));
  }, [contentType, categoryId]);

  const toggleSelect = (id) => {
    const newSet = new Set(selectedIds);
    if (newSet.has(id)) newSet.delete(id);
    else newSet.add(id);
    setSelectedIds(newSet);
  };

  const handleAssign = async () => {
    setLoading(true);
    try {
      const rawCategoryId = parseInt(String(categoryId).replace(/\D/g, ''), 10);
      await api.put(`/admin/category/${rawCategoryId}/songs`, { 
        selectedSongIds: Array.from(selectedIds) 
      });
      onAssigned();
    } catch (err) {
      console.error(err);
      alert('Failed to assign songs.');
    } finally {
      setLoading(false);
    }
  };

  const filtered = songs.filter(s => s.audioName.toLowerCase().includes(search.toLowerCase()) || (s.songId && s.songId.toLowerCase().includes(search.toLowerCase())));
  return (
    <div style={{ background: '#fff', borderRadius: 20, padding: 30, boxShadow: '0 4px 24px rgba(0,0,0,0.05)' }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 20, marginBottom: 30 }}>
        <button onClick={onBack} style={{ background: '#f1f5f9', border: 'none', padding: '10px 16px', borderRadius: 10, cursor: 'pointer', fontWeight: 600 }}>← Back</button>
        <h2 style={{ margin: 0, fontSize: '1.8rem' }}>Assign Songs to Category</h2>
        <div style={{ marginLeft: 'auto' }}>
          <button 
            onClick={handleAssign} 
            disabled={loading}
            style={{ background: '#ec4899', color: '#fff', border: 'none', padding: '10px 20px', borderRadius: 10, fontWeight: 700, cursor: loading ? 'not-allowed' : 'pointer', opacity: loading ? 0.5 : 1, transition: 'all 0.2s' }}
          >
            {loading ? 'Saving...' : `Save Assignments (${selectedIds.size})`}
          </button>
        </div>
      </div>

      <input 
        type="text" 
        placeholder="🔍 Search available songs by name or ID..." 
        value={search} 
        onChange={e => setSearch(e.target.value)} 
        style={{ width: '100%', padding: '14px 20px', fontSize: 16, borderRadius: 12, border: '2px solid #e2e8f0', marginBottom: 30, outline: 'none' }}
      />

      {songs.length === 0 ? (
        <p className="muted" style={{ textAlign: 'center', marginTop: 40, fontSize: 18 }}>No available songs available. Create some in the Songs tab first!</p>
      ) : filtered.length === 0 ? (
        <p className="muted" style={{ textAlign: 'center', marginTop: 40, fontSize: 16 }}>No songs match your search.</p>
      ) : (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))', gap: 20 }}>
          {filtered.map(song => {
            const isSelected = selectedIds.has(song.id);
            return (
              <div 
                key={song.id} 
                onClick={() => toggleSelect(song.id)}
                style={{
                  cursor: 'pointer', borderRadius: 16, overflow: 'hidden', background: '#f8fafc',
                  border: `3px solid ${isSelected ? '#ec4899' : 'transparent'}`,
                  transition: 'all 0.2s', position: 'relative', display: 'flex', alignItems: 'center', padding: 12, gap: 12
                }}
              >
                <div style={{ width: 48, height: 48, borderRadius: 8, background: '#1e293b', flexShrink: 0, overflow: 'hidden' }}>
                  {song.imageUrl ? (
                    <img src={song.imageUrl} alt={song.audioName} style={{ width: '100%', height: '100%', objectFit: 'cover' }} onError={e => { e.target.style.display = 'none'; }} />
                  ) : (
                    <div style={{ width: '100%', height: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 20 }}>🎵</div>
                  )}
                </div>
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ fontWeight: 700, color: '#1e293b', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis', fontSize: 15 }}>{song.audioName}</div>
                  <div style={{ fontSize: 12, color: '#64748b', marginTop: 4 }}>ID: {song.songId}</div>
                </div>
                {isSelected && (
                  <div style={{ background: '#ec4899', color: '#fff', width: 24, height: 24, borderRadius: '50%', display: 'flex', alignItems: 'center', justifyContent: 'center', fontWeight: 'bold', flexShrink: 0 }}>✓</div>
                )}
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}



function Sections({ onDataChange, contentType, onViewCategories }) {
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
        customActions={[
          {
            label: 'Categories',
            style: { background: '#4bc0c0', color: 'white' },
            onClick: (item) => onViewCategories(String(item.id))
          }
        ]}
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

function Categories({ onDataChange, contentType, initialSectionFilter, clearInitialSectionFilter, initialFormSection }) {
  const [items, setItems] = useState([]);
  const [sections, setSections] = useState([]);
  const [sectionFilter, setSectionFilter] = useState('');
  const [form, setForm] = useState({ categoryName: '', categoryImage: '', imageFile: null, adapterType: 1, sectionId: '' });
  const [editingId, setEditingId] = useState(null);
  const [selectedCategory, setSelectedCategory] = useState(null);

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
    if (initialSectionFilter && initialSectionFilter !== sectionFilter) {
      setSectionFilter(initialSectionFilter);
      if (clearInitialSectionFilter) clearInitialSectionFilter();
    }
  }, [initialSectionFilter]);

  useEffect(() => {
    if (initialFormSection) {
      setForm(prev => ({ ...prev, sectionId: String(initialFormSection) }));
    }
  }, [initialFormSection]);

  useEffect(() => {
    if (!initialSectionFilter) setSectionFilter('');
    if (!initialFormSection) setForm((prev) => ({ ...prev, sectionId: '' }));
    setEditingId(null);
  }, [contentType]);

  const submit = async (e) => {
    e.preventDefault();
    
    const formData = new FormData();
    formData.append('categoryName', form.categoryName);
    formData.append('adapterType', form.adapterType);
    formData.append('sectionId', form.sectionId);
    if (form.categoryImage) formData.append('categoryImage', form.categoryImage);
    if (form.imageFile) formData.append('image', form.imageFile);

    try {
      if (editingId) {
        await api.put(`/admin/category/${editingId}`, formData, { headers: { 'Content-Type': 'multipart/form-data' } });
      } else {
        await api.post('/admin/category', formData, { headers: { 'Content-Type': 'multipart/form-data' } });
      }
      setForm({ categoryName: '', categoryImage: '', imageFile: null, adapterType: 1, sectionId: '' });
      setEditingId(null);
      await load();
      onDataChange();
    } catch (err) {
      console.error(err);
      alert(err.response?.data?.message || 'Failed to save category');
    }
  };

  if (selectedCategory) {
    return (
      <CategorySongsManager 
        category={selectedCategory} 
        onBack={() => setSelectedCategory(null)} 
        contentType={contentType} 
        onDataChange={onDataChange} 
      />
    );
  }

  return (
    <div>
      <h2>Categories</h2>
      <div className="toolbar">
        <label>Filter by section:</label>
        <select value={sectionFilter} onChange={(e) => setSectionFilter(e.target.value)}>
          <option value="">All</option>
          <option value="unassigned">Unassigned (None)</option>
          {sections.map((s) => (
            <option key={s.id} value={s.id}>{s.sectionTitle}</option>
          ))}
        </select>
      </div>
      {/* Form Section */}
      <div className="card form" style={{ marginBottom: '30px' }}>
        <form className="inline" onSubmit={submit}>
          <div>
            <label>Category Name</label>
            <input type="text" value={form.categoryName} onChange={(e) => setForm(p => ({ ...p, categoryName: e.target.value }))} required />
          </div>
          <div>
            <label>Category Image</label>
            <div style={{ display: 'flex', gap: '10px' }}>
              <input type="file" accept="image/*" onChange={(e) => setForm(p => ({ ...p, imageFile: e.target.files[0] }))} style={{ flex: 1 }} />
              <input type="text" placeholder="Or Image URL" value={form.categoryImage} onChange={(e) => setForm(p => ({ ...p, categoryImage: e.target.value }))} style={{ flex: 1 }} />
            </div>
          </div>
          <div>
            <label>Adapter Type</label>
            <input type="number" value={form.adapterType} onChange={(e) => setForm(p => ({ ...p, adapterType: Number(e.target.value) }))} required />
          </div>
          <div>
            <label>Section</label>
            <select value={form.sectionId || ''} onChange={(e) => setForm(p => ({ ...p, sectionId: e.target.value }))}>
              <option value="">Unassigned (None)</option>
              {sections.map((s) => (
                <option key={s.id} value={String(s.id)}>{s.sectionTitle}</option>
              ))}
            </select>
          </div>
          <div className="actions" style={{ alignItems: 'flex-end', display: 'flex', gap: '10px' }}>
            <button type="submit">{editingId ? 'Update' : 'Add'}</button>
            {editingId && <button type="button" onClick={() => { setEditingId(null); setForm({ categoryName: '', categoryImage: '', imageFile: null, adapterType: 1, sectionId: '' }); }}>Cancel</button>}
          </div>
        </form>
      </div>

      {/* Grid Section */}
      {items.length === 0 ? (
        <p className="muted" style={{ textAlign: 'center', marginTop: '40px' }}>No categories found.</p>
      ) : (
        <div className="media-grid">
          {items.map((item) => {
            const section = sections.find(s => s.id === item.sectionId);
            return (
              <div className="media-card" key={item.id}>
                <div className="media-card-img-wrapper">
                  <img src={item.categoryImage || 'https://via.placeholder.com/300?text=No+Image'} alt={item.categoryName} className="media-card-img" />
                  <div className="media-card-overlay">
                    <button onClick={(e) => {
                      e.stopPropagation();
                      setEditingId(item.id);
                      setForm({
                        categoryName: item.categoryName,
                        categoryImage: item.categoryImage || '',
                        imageFile: null,
                        adapterType: item.adapterType,
                        sectionId: item.sectionId ? String(item.sectionId) : (initialFormSection ? String(initialFormSection) : '')
                      });
                      window.scrollTo({ top: 0, behavior: 'smooth' });
                    }}>Edit</button>
                    <button className="danger-btn" onClick={async (e) => {
                      e.stopPropagation();
                      if (window.confirm('Delete category?')) {
                        await api.delete(`/admin/category/${item.id}`);
                        await load();
                        onDataChange();
                      }
                    }}>Delete</button>
                  </div>
                </div>
                <div className="media-card-info">
                  <h3 className="media-card-title">{item.categoryName}</h3>
                  <p className="media-card-subtitle">{section ? section.sectionTitle : 'Unassigned (No Section)'}</p>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}

function CategorySongsManager({ category, onBack, contentType, onDataChange }) {
  const [songs, setSongs] = useState([]);
  const [selectedSongIds, setSelectedSongIds] = useState([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [isSaving, setIsSaving] = useState(false);

  // Metadata Filter State
  const [attributes, setAttributes] = useState([]);
  const [filterActor, setFilterActor] = useState('');
  const [filterHeroine, setFilterHeroine] = useState('');
  const [filterSinger, setFilterSinger] = useState('');
  const [filterMovie, setFilterMovie] = useState('');
  const [filterMusicDirector, setFilterMusicDirector] = useState('');
  const [filterReleaseYear, setFilterReleaseYear] = useState('');
  const [filterGenre, setFilterGenre] = useState('');

  useEffect(() => {
    api.get('/admin/attributes').then(res => setAttributes(res.data)).catch(console.error);
  }, []);

  useEffect(() => {
    const loadSongs = async () => {
      try {
        const { data } = await api.get('/admin/songs');
        setSongs(data);
        // Pre-select songs that already belong to this category
        const preSelected = data.filter(s => s.categoryId === category.id).map(s => s.id);
        setSelectedSongIds(preSelected);
      } catch (err) {
        console.error('Failed to load songs:', err);
        alert('Failed to load songs');
      }
    };
    loadSongs();
  }, [category.id]);

  const toggleSong = (songId) => {
    setSelectedSongIds(prev => 
      prev.includes(songId) ? prev.filter(id => id !== songId) : [...prev, songId]
    );
  };

  const handleSave = async () => {
    setIsSaving(true);
    try {
      await api.put(`/admin/category/${category.id}/songs`, { selectedSongIds });
      alert('Category songs updated successfully!');
      onDataChange();
      onBack();
    } catch (err) {
      console.error(err);
      alert(err.response?.data?.message || 'Failed to update category songs');
    } finally {
      setIsSaving(false);
    }
  };

  const filteredSongs = songs.filter(s => {
    if (searchQuery && !s.audioName.toLowerCase().includes(searchQuery.toLowerCase())) return false;
    if (filterActor && String(s.actorName || '') !== filterActor) return false;
    if (filterHeroine && String(s.heroineName || '') !== filterHeroine) return false;
    if (filterSinger && String(s.singerName || '') !== filterSinger) return false;
    if (filterMovie && String(s.movieName || '') !== filterMovie) return false;
    if (filterMusicDirector && String(s.musicDirector || '') !== filterMusicDirector) return false;
    if (filterReleaseYear && String(s.releaseYear || '') !== filterReleaseYear) return false;
    if (filterGenre && String(s.genre || '') !== filterGenre) return false;
    return true;
  });

  const generateYears = () => {
    const currentYear = new Date().getFullYear();
    const years = [];
    for (let i = 0; i < 40; i++) {
      years.push((currentYear - i).toString());
    }
    return years;
  };

  return (
    <div>
      <div style={{ display: 'flex', alignItems: 'center', gap: '15px', marginBottom: '20px' }}>
        <button onClick={onBack} style={{ padding: '8px 16px', background: '#e2e8f0', border: 'none', borderRadius: '8px', cursor: 'pointer', fontWeight: 'bold' }}>&larr; Back</button>
        <h2 style={{ margin: 0 }}>Assign Songs: <span style={{ color: 'var(--primary)' }}>{category.categoryName}</span></h2>
      </div>

      <div className="card" style={{ marginBottom: '20px', display: 'flex', gap: '20px', alignItems: 'center' }}>
        <input 
          type="text" 
          placeholder="Search all uploaded songs..." 
          value={searchQuery}
          onChange={e => setSearchQuery(e.target.value)}
          style={{ flex: 1, padding: '12px', borderRadius: '8px', border: '1px solid #cbd5e1' }}
        />
        <button onClick={handleSave} disabled={isSaving} style={{ padding: '12px 24px', background: 'var(--primary)', color: 'white', border: 'none', borderRadius: '8px', cursor: 'pointer', fontWeight: 'bold', minWidth: '150px' }}>
          {isSaving ? 'Saving...' : 'Save Songs to Category'}
        </button>
      </div>

      <div className="card" style={{ marginBottom: '20px', background: '#f8fafc', border: '1px solid #e2e8f0', boxShadow: 'none' }}>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '15px' }}>
          <div>
            <label style={{ fontSize: '12px', fontWeight: 'bold', color: '#64748b', textTransform: 'uppercase' }}>Actor Name</label>
            <select value={filterActor} onChange={e => setFilterActor(e.target.value)} style={{ width: '100%', padding: '10px', marginTop: '5px' }}>
              <option value="">All Actors</option>
              {attributes.filter(a => a.type === 'Actor').map(a => <option key={a.id} value={a.name}>{a.name}</option>)}
            </select>
          </div>
          <div>
            <label style={{ fontSize: '12px', fontWeight: 'bold', color: '#64748b', textTransform: 'uppercase' }}>Heroine Name</label>
            <select value={filterHeroine} onChange={e => setFilterHeroine(e.target.value)} style={{ width: '100%', padding: '10px', marginTop: '5px' }}>
              <option value="">All Heroines</option>
              {attributes.filter(a => a.type === 'Heroine').map(a => <option key={a.id} value={a.name}>{a.name}</option>)}
            </select>
          </div>
          <div>
            <label style={{ fontSize: '12px', fontWeight: 'bold', color: '#64748b', textTransform: 'uppercase' }}>Singer Name</label>
            <select value={filterSinger} onChange={e => setFilterSinger(e.target.value)} style={{ width: '100%', padding: '10px', marginTop: '5px' }}>
              <option value="">All Singers</option>
              {attributes.filter(a => a.type === 'Singer').map(a => <option key={a.id} value={a.name}>{a.name}</option>)}
            </select>
          </div>
          <div>
            <label style={{ fontSize: '12px', fontWeight: 'bold', color: '#64748b', textTransform: 'uppercase' }}>Movie Name</label>
            <select value={filterMovie} onChange={e => setFilterMovie(e.target.value)} style={{ width: '100%', padding: '10px', marginTop: '5px' }}>
              <option value="">All Movies</option>
              {attributes.filter(a => a.type === 'Movie').map(a => <option key={a.id} value={a.name}>{a.name}</option>)}
            </select>
          </div>
          <div>
            <label style={{ fontSize: '12px', fontWeight: 'bold', color: '#64748b', textTransform: 'uppercase' }}>Music Director</label>
            <select value={filterMusicDirector} onChange={e => setFilterMusicDirector(e.target.value)} style={{ width: '100%', padding: '10px', marginTop: '5px' }}>
              <option value="">All Directors</option>
              {attributes.filter(a => a.type === 'Music Director').map(a => <option key={a.id} value={a.name}>{a.name}</option>)}
            </select>
          </div>
          <div>
            <label style={{ fontSize: '12px', fontWeight: 'bold', color: '#64748b', textTransform: 'uppercase' }}>Release Year</label>
            <select value={filterReleaseYear} onChange={e => setFilterReleaseYear(e.target.value)} style={{ width: '100%', padding: '10px', marginTop: '5px' }}>
              <option value="">All Years</option>
              {generateYears().map(y => <option key={y} value={y}>{y}</option>)}
            </select>
          </div>
          <div>
            <label style={{ fontSize: '12px', fontWeight: 'bold', color: '#64748b', textTransform: 'uppercase' }}>Genre</label>
            <select value={filterGenre} onChange={e => setFilterGenre(e.target.value)} style={{ width: '100%', padding: '10px', marginTop: '5px' }}>
              <option value="">All Genres</option>
              {attributes.filter(a => a.type === 'Genre').map(a => <option key={a.id} value={a.name}>{a.name}</option>)}
            </select>
          </div>
        </div>
      </div>

      <div className="table-container">
        <table className="table">
          <thead>
            <tr>
              <th style={{ width: '50px', textAlign: 'center' }}>Select</th>
              <th>Song ID</th>
              <th>Audio Name</th>
              <th>Image</th>
              <th>Current Category</th>
            </tr>
          </thead>
          <tbody>
            {filteredSongs.length === 0 ? (
              <tr><td colSpan="5" style={{ textAlign: 'center', padding: '20px' }} className="muted">No songs found.</td></tr>
            ) : (
              filteredSongs.map(song => (
                <tr key={song.id} style={{ background: selectedSongIds.includes(song.id) ? '#f0f9ff' : 'transparent', cursor: 'pointer' }} onClick={() => toggleSong(song.id)}>
                  <td style={{ textAlign: 'center' }}>
                    <input 
                      type="checkbox" 
                      checked={selectedSongIds.includes(song.id)}
                      onChange={() => {}} // Handled by tr onClick
                      style={{ transform: 'scale(1.3)', cursor: 'pointer' }}
                    />
                  </td>
                  <td><span className="id-badge">{song.songId || song.id}</span></td>
                  <td style={{ fontWeight: selectedSongIds.includes(song.id) ? 'bold' : 'normal' }}>{song.audioName}</td>
                  <td>
                    {song.imageUrl && <img src={song.imageUrl} alt="" style={{ width: '40px', height: '40px', objectFit: 'cover', borderRadius: '4px' }} />}
                  </td>
                  <td>
                    {song.categoryId === category.id ? (
                      <span style={{ color: 'var(--primary)', fontWeight: 'bold' }}>This Category</span>
                    ) : song.categoryId ? (
                      <span className="muted">Category ID: {song.categoryId}</span>
                    ) : (
                      <span className="muted">None</span>
                    )}
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
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
      const payload = { 
        ...form, 
        categoryId: form.categoryIds && form.categoryIds.length > 0 ? Number(form.categoryIds[0]) : null,
        categoryIds: form.categoryIds ? form.categoryIds.map(Number) : []
      };
      if (editingId) {
        await api.put(`/admin/song/${editingId}`, payload);
      } else {
        await api.post('/admin/song', payload);
      }
      setForm({ audioName: '', audioUrl: '', imageUrl: '', categoryIds: [] });
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
            <label>Categories (Hold Ctrl/Cmd to select multiple)</label>
            <select
              multiple
              style={{ minHeight: '100px' }}
              value={form.categoryIds || []}
              onChange={(e) => {
                const options = e.target.options;
                const value = [];
                for (let i = 0, l = options.length; i < l; i++) {
                  if (options[i].selected) {
                    value.push(options[i].value);
                  }
                }
                setForm((p) => ({ ...p, categoryIds: value }));
              }}
              required
            >
              {categories.map((c) => (
                <option key={c.id} value={c.id}>{c.categoryName}</option>
              ))}
            </select>
          </div>
          <div className="actions" style={{ alignItems: 'flex-end', display: 'flex', gap: '10px' }}>
            <button type="submit">{editingId ? 'Update' : 'Add'}</button>
            {editingId && <button type="button" onClick={() => { setEditingId(null); setForm({ audioName: '', audioUrl: '', imageUrl: '', categoryIds: [] }); }}>Cancel</button>}
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
                <td>
                  {item.categories && item.categories.length > 0 
                    ? item.categories.map(c => <span key={c.id} className="id-badge" style={{marginRight: 4}}>{c.categoryName || `cat_${String(c.id).padStart(3, '0')}`}</span>) 
                    : <span className="id-badge">{item.categoryIdFormatted || (item.categoryId ? `cat_${String(item.categoryId).padStart(3, '0')}` : 'None')}</span>
                  }
                </td>
                <td>
                  <div style={{ display: 'flex', gap: '8px' }}>
                    <button onClick={() => {
                      setEditingId(item.id);
                      setForm({
                        audioName: item.audioName,
                        audioUrl: item.audioUrl,
                        imageUrl: item.imageUrl || '',
                        categoryIds: item.categories ? item.categories.map(c => String(c.id)) : (item.categoryId ? [String(item.categoryId)] : [])
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
  rowTransform,
  customActions
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
                    {customActions && customActions.map((action, i) => (
                      <button key={i} className={action.className || ''} style={action.style || {}} onClick={() => action.onClick(item)}>
                        {action.label}
                      </button>
                    ))}
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

function LyricsManager() {
  const [songs, setSongs] = useState([]);
  const [selectedSong, setSelectedSong] = useState(null);
  const [lyrics, setLyrics] = useState('');
  const [search, setSearch] = useState('');
  const [saving, setSaving] = useState(false);
  const [saved, setSaved] = useState(false);
  const [loading, setLoading] = useState(false);

  // Load all songs
  const loadSongs = async () => {
    setLoading(true);
    try {
      const { data } = await api.get('/admin/songs');
      setSongs(data || []);
    } catch (err) {
      alert('Failed to load songs');
    }
    setLoading(false);
  };

  useEffect(() => { loadSongs(); }, []);

  const filteredSongs = songs.filter(s =>
    s.audioName?.toLowerCase().includes(search.toLowerCase())
  );

  const selectSong = (song) => {
    setSelectedSong(song);
    setLyrics(song.lyrics || '');
    setSaved(false);
  };

  const saveLyrics = async () => {
    if (!selectedSong) return;
    setSaving(true);
    setSaved(false);
    try {
      await api.put(`/admin/song/${selectedSong.id}/lyrics`, { lyrics });
      setSaved(true);
      // Update local list
      setSongs(prev => prev.map(s => s.id === selectedSong.id ? { ...s, lyrics } : s));
      setSelectedSong(prev => ({ ...prev, lyrics }));
      setTimeout(() => setSaved(false), 3000);
    } catch (err) {
      alert('Failed to save lyrics');
    }
    setSaving(false);
  };

  const clearLyrics = async () => {
    if (!selectedSong || !window.confirm('Remove lyrics for this song?')) return;
    await api.put(`/admin/song/${selectedSong.id}/lyrics`, { lyrics: '' });
    setLyrics('');
    setSongs(prev => prev.map(s => s.id === selectedSong.id ? { ...s, lyrics: null } : s));
    setSelectedSong(prev => ({ ...prev, lyrics: null }));
  };

  const lineCount = lyrics.trim().split('\n').filter(l => l.trim()).length;

  return (
    <div style={{ display: 'flex', gap: '24px', height: 'calc(100vh - 140px)' }}>

      {/* Left Panel: Song List */}
      <div style={{ width: '320px', flexShrink: 0, display: 'flex', flexDirection: 'column', gap: '12px' }}>
        <h2 style={{ margin: 0 }}>🎤 Lyrics Manager</h2>
        <input
          placeholder="🔍 Search songs..."
          value={search}
          onChange={e => setSearch(e.target.value)}
          style={{ padding: '8px 12px', borderRadius: '8px', border: '1px solid #333', background: '#1a1a2e', color: '#fff', fontSize: '14px' }}
        />
        <div style={{ overflowY: 'auto', flex: 1, display: 'flex', flexDirection: 'column', gap: '6px' }}>
          {loading && <p className="muted">Loading songs...</p>}
          {filteredSongs.map(song => (
            <div
              key={song.id}
              onClick={() => selectSong(song)}
              style={{
                padding: '10px 14px',
                borderRadius: '8px',
                cursor: 'pointer',
                background: selectedSong?.id === song.id ? '#2d1b69' : '#1a1a2e',
                border: selectedSong?.id === song.id ? '1px solid #7c3aed' : '1px solid #2a2a4a',
                display: 'flex',
                alignItems: 'center',
                gap: '10px',
                transition: 'all 0.15s'
              }}
            >
              {song.imageUrl && (
                <img src={song.imageUrl} alt="" style={{ width: 36, height: 36, borderRadius: 6, objectFit: 'cover', flexShrink: 0 }} />
              )}
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ fontWeight: 600, fontSize: '13px', color: '#fff', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                  {song.audioName}
                </div>
                <div style={{ fontSize: '11px', color: song.lyrics ? '#a78bfa' : '#666', marginTop: 2 }}>
                  {song.lyrics ? '✓ Has lyrics' : 'No lyrics yet'}
                </div>
              </div>
            </div>
          ))}
          {!loading && filteredSongs.length === 0 && (
            <p className="muted">No songs found</p>
          )}
        </div>
      </div>

      {/* Right Panel: Lyrics Editor */}
      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: '12px' }}>
        {!selectedSong ? (
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', flex: 1, flexDirection: 'column', gap: '12px', opacity: 0.4 }}>
            <span style={{ fontSize: '48px' }}>🎵</span>
            <p>Select a song from the left to edit its lyrics</p>
          </div>
        ) : (
          <>
            {/* Song Header */}
            <div style={{ display: 'flex', alignItems: 'center', gap: '14px', padding: '14px 16px', background: '#1a1a2e', borderRadius: '10px', border: '1px solid #2a2a4a' }}>
              {selectedSong.imageUrl && (
                <img src={selectedSong.imageUrl} alt="" style={{ width: 52, height: 52, borderRadius: 8, objectFit: 'cover' }} />
              )}
              <div>
                <div style={{ fontWeight: 700, fontSize: '16px', color: '#fff' }}>{selectedSong.audioName}</div>
                <div style={{ fontSize: '12px', color: '#888', marginTop: 4 }}>
                  ID: {formatEntityId('song', selectedSong.id)} &nbsp;·&nbsp; {lineCount} lines
                </div>
              </div>
              <div style={{ marginLeft: 'auto', display: 'flex', gap: '8px' }}>
                {selectedSong.lyrics && (
                  <button onClick={clearLyrics} style={{ background: 'transparent', border: '1px solid #ef4444', color: '#ef4444', borderRadius: 6, padding: '6px 12px', cursor: 'pointer', fontSize: '13px' }}>
                    🗑 Clear
                  </button>
                )}
                <button
                  onClick={saveLyrics}
                  disabled={saving}
                  style={{ background: saved ? '#16a34a' : '#7c3aed', border: 'none', color: '#fff', borderRadius: 6, padding: '6px 20px', cursor: 'pointer', fontWeight: 600, fontSize: '13px', transition: 'background 0.2s' }}
                >
                  {saving ? 'Saving...' : saved ? '✓ Saved!' : '💾 Save Lyrics'}
                </button>
              </div>
            </div>

            {/* Instructions */}
            <div style={{ padding: '10px 14px', background: '#0d1117', borderRadius: '8px', border: '1px solid #1e3a5f', fontSize: '12px', color: '#6b9ac7' }}>
              💡 Enter each lyric line on a new line. The app will auto-highlight each line as the song plays.
              Use blank lines to add spacing between verses.
            </div>

            {/* Lyrics Textarea */}
            <textarea
              value={lyrics}
              onChange={e => { setLyrics(e.target.value); setSaved(false); }}
              placeholder={"Enter lyrics here...\n\nExample:\nIdhazhin oru oram sirithaai anbae\nNijamaai ithu pothum sirippaai anbae\n\nSollu nee i love you\nNee thaan en kurinji poo"}
              style={{
                flex: 1,
                padding: '16px',
                borderRadius: '10px',
                border: '1px solid #2a2a4a',
                background: '#0d1117',
                color: '#e2e8f0',
                fontSize: '15px',
                lineHeight: '1.8',
                fontFamily: 'monospace',
                resize: 'none',
                outline: 'none'
              }}
              onFocus={e => e.target.style.borderColor = '#7c3aed'}
              onBlur={e => e.target.style.borderColor = '#2a2a4a'}
            />

            {/* Live Preview */}
            {lyrics.trim() && (
              <div style={{ maxHeight: '160px', overflowY: 'auto', padding: '12px 16px', background: '#0d1117', borderRadius: '8px', border: '1px solid #2a2a4a' }}>
                <div style={{ fontSize: '11px', color: '#666', marginBottom: '8px', fontWeight: 600, letterSpacing: '1px' }}>PREVIEW (as seen in app)</div>
                {lyrics.split('\n').map((line, i) => (
                  <div key={i} style={{
                    fontSize: i === 2 ? '15px' : '13px',
                    color: i === 2 ? '#a78bfa' : line.trim() ? '#888' : 'transparent',
                    fontWeight: i === 2 ? 700 : 400,
                    lineHeight: '1.8',
                    marginBottom: line.trim() ? 0 : '4px'
                  }}>
                    {line || '\u00a0'}
                  </div>
                ))}
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
}

function AdsManager() {
  const [ads, setAds] = useState([]);
  const [form, setForm] = useState({ adTitle: '', adType: 'audio', redirectUrl: '', mediaFile: null, imageFile: null });
  const [isUploading, setIsUploading] = useState(false);

  const loadAds = async () => {
    try {
      const { data } = await api.get('/ad/admin');
      setAds(data);
    } catch (err) {
      console.error(err);
    }
  };

  useEffect(() => { loadAds(); }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!form.mediaFile) return alert('Please select a media file (Audio/Video).');

    setIsUploading(true);
    const formData = new FormData();
    formData.append('adTitle', form.adTitle);
    formData.append('adType', form.adType);
    if (form.redirectUrl) formData.append('redirectUrl', form.redirectUrl);
    formData.append('media', form.mediaFile);
    if (form.imageFile) formData.append('image', form.imageFile);

    try {
      await api.post('/ad/admin', formData, { headers: { 'Content-Type': 'multipart/form-data' } });
      setForm({ adTitle: '', adType: 'audio', redirectUrl: '', mediaFile: null, imageFile: null });
      await loadAds();
      alert('Ad uploaded successfully!');
    } catch (err) {
      alert(err.response?.data?.message || 'Error uploading ad');
    }
    setIsUploading(false);
  };

  const deleteAd = async (id) => {
    if (!window.confirm('Delete this ad?')) return;
    await api.delete(`/ad/admin/${id}`);
    await loadAds();
  };

  return (
    <div>
      <h2>Advertisements</h2>
      <div className="card form">
        <form onSubmit={handleSubmit} className="inline">
          <div>
            <label>Ad Title</label>
            <input type="text" value={form.adTitle} onChange={(e) => setForm(p => ({...p, adTitle: e.target.value}))} required />
          </div>
          <div>
            <label>Ad Type</label>
            <select value={form.adType} onChange={(e) => setForm(p => ({...p, adType: e.target.value}))}>
              <option value="audio">Audio Ad (MP3)</option>
              <option value="video">Video Ad (MP4)</option>
            </select>
          </div>
          <div>
            <label>Redirect URL (Optional)</label>
            <input type="text" value={form.redirectUrl} onChange={(e) => setForm(p => ({...p, redirectUrl: e.target.value}))} />
          </div>
          <div>
            <label>{form.adType === 'video' ? 'Video File' : 'Audio File'}</label>
            <input type="file" accept={form.adType === 'video' ? "video/*" : "audio/*"} onChange={(e) => setForm(p => ({...p, mediaFile: e.target.files[0]}))} required />
          </div>
          {form.adType === 'audio' && (
            <div>
              <label>Cover Image (Optional)</label>
              <input type="file" accept="image/*" onChange={(e) => setForm(p => ({...p, imageFile: e.target.files[0]}))} />
            </div>
          )}
          <div className="actions" style={{ alignItems: 'flex-end', display: 'flex' }}>
            <button type="submit" disabled={isUploading}>{isUploading ? 'Uploading...' : 'Upload Ad'}</button>
          </div>
        </form>
      </div>

      <div className="table-container" style={{ marginTop: '20px' }}>
        <table className="table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Preview</th>
              <th>Title</th>
              <th>Type</th>
              <th>Media Link</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {ads.map(ad => (
              <tr key={ad.id}>
                <td><span className="id-badge">ad_{String(ad.id).padStart(3, '0')}</span></td>
                <td>
                  {ad.imageUrl ? (
                    <img src={ad.imageUrl} alt="Ad" style={{width: 50, height: 50, objectFit: 'cover', borderRadius: 4}} />
                  ) : ad.adType === 'video' ? (
                    <div style={{width: 50, height: 50, background: '#222', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 10, borderRadius: 4}}>VIDEO</div>
                  ) : (
                    <div style={{width: 50, height: 50, background: '#222', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 10, borderRadius: 4}}>AUDIO</div>
                  )}
                </td>
                <td>{ad.adTitle}</td>
                <td><span className="id-badge" style={{background: ad.adType === 'video' ? '#e11d48' : '#7c3aed'}}>{ad.adType.toUpperCase()}</span></td>
                <td>
                  <a href={ad.mediaUrl} target="_blank" rel="noreferrer" style={{color: '#a78bfa'}}>View Media</a>
                </td>
                <td>
                  <button className="btn-danger" onClick={() => deleteAd(ad.id)}>Delete</button>
                </td>
              </tr>
            ))}
            {ads.length === 0 && (
              <tr><td colSpan="6" style={{textAlign: 'center', padding: '20px'}}>No advertisements found</td></tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}

export default App;

function AttributesManager({ onDataChange }) {
  const [items, setItems] = useState([]);
  const [form, setForm] = useState({ type: 'Actor', name: '' });
  
  const types = ['Actor', 'Heroine', 'Singer', 'Movie', 'MusicDirector', 'Genre'];

  const load = async () => {
    try {
      const { data } = await api.get('/admin/attributes');
      setItems(data);
    } catch (err) {
      console.error(err);
    }
  };

  useEffect(() => {
    load();
  }, []);

  const submit = async (e) => {
    e.preventDefault();
    try {
      await api.post('/admin/attribute', form);
      setForm({ ...form, name: '' });
      await load();
      onDataChange();
    } catch (err) {
      alert(err.response?.data?.message || 'Failed to add attribute');
    }
  };

  const remove = async (id) => {
    try {
      await api.delete(`/admin/attribute/${id}`);
      await load();
      onDataChange();
    } catch (err) {
      console.error(err);
    }
  };

  return (
    <div>
      <h2>Manage Predefined Song Attributes</h2>
      <p className="muted">Add predefined names that will show up as dropdowns when uploading songs.</p>
      
      <form className="card form inline" onSubmit={submit} style={{ display: 'flex', gap: '10px', alignItems: 'flex-end', maxWidth: '600px' }}>
        <div style={{ flex: 1 }}>
          <label>Type</label>
          <select value={form.type} onChange={(e) => setForm({ ...form, type: e.target.value })} style={{ width: '100%', padding: '10px' }}>
            {types.map(t => <option key={t} value={t}>{t}</option>)}
          </select>
        </div>
        <div style={{ flex: 2 }}>
          <label>Name</label>
          <input 
            type="text" 
            value={form.name} 
            onChange={(e) => setForm({ ...form, name: e.target.value })} 
            placeholder="e.g. Vijay"
            style={{ width: '100%', padding: '10px' }}
            required
          />
        </div>
        <button type="submit" style={{ padding: '10px 20px', height: '40px' }}>Add</button>
      </form>

      <div className="table-container" style={{ marginTop: '30px' }}>
        <table className="table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Type</th>
              <th>Name</th>
              <th>Action</th>
            </tr>
          </thead>
          <tbody>
            {items.map((item) => (
              <tr key={item.id}>
                <td>{item.id}</td>
                <td><span className="id-badge">{item.type}</span></td>
                <td>{item.name}</td>
                <td>
                  <button className="danger small" onClick={() => remove(item.id)}>Delete</button>
                </td>
              </tr>
            ))}
            {items.length === 0 && (
              <tr><td colSpan="4" style={{ textAlign: 'center' }}>No attributes added yet.</td></tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}

function AdvancedBulkSongs({ onDataChange, contentType, onEditRequest }) {
  const [categories, setCategories] = useState([]);
  
  // Smart Sync State
  const [smartAudioFiles, setSmartAudioFiles] = useState([]);
  const [smartImageFiles, setSmartImageFiles] = useState([]);
  const [matchedPairs, setMatchedPairs] = useState([]);
  const [smartCategoryId, setSmartCategoryId] = useState('');
  const [isUploading, setIsUploading] = useState(false);

  // Metadata State
  const [attributes, setAttributes] = useState([]);
  const [smartActorName, setSmartActorName] = useState('');
  const [smartHeroineName, setSmartHeroineName] = useState('');
  const [smartSingerName, setSmartSingerName] = useState('');
  const [smartMovieName, setSmartMovieName] = useState('');
  const [smartMusicDirector, setSmartMusicDirector] = useState('');
  const [smartReleaseYear, setSmartReleaseYear] = useState('');
  const [smartGenre, setSmartGenre] = useState('');

  useEffect(() => {
    api.get(`/admin/categories?contentType=${contentType}`).then(res => setCategories(res.data));
    api.get('/admin/attributes').then(res => setAttributes(res.data)).catch(console.error);
  }, [contentType]);

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
        name: audio.name.replace(/\.[^/.]+$/, "").trim()
      });
    });
    setMatchedPairs(pairs);
  }, [smartAudioFiles, smartImageFiles]);

  const handleSmartUpload = async () => {
    if (matchedPairs.length === 0) return alert('No matched pairs to upload');

    setIsUploading(true);
    let successCount = 0;
    let failCount = 0;
    let lastErrorMsg = '';

    for (const pair of matchedPairs) {
      const formData = new FormData();
      if (smartCategoryId) formData.append('categoryId', smartCategoryId);
      formData.append('audioName', pair.name);
      formData.append('files', pair.audio);
      if (pair.image) formData.append('files', pair.image);

      if (smartActorName) formData.append('actorName', smartActorName);
      if (smartHeroineName) formData.append('heroineName', smartHeroineName);
      if (smartSingerName) formData.append('singerName', smartSingerName);
      if (smartMovieName) formData.append('movieName', smartMovieName);
      if (smartMusicDirector) formData.append('musicDirector', smartMusicDirector);
      if (smartReleaseYear) formData.append('releaseYear', smartReleaseYear);
      if (smartGenre) formData.append('genre', smartGenre);

      try {
        await api.post('/admin/song/upload', formData, { headers: { 'Content-Type': 'multipart/form-data' } });
        successCount++;
      } catch (err) {
        console.error('Failed smart upload', pair.name, err);
        failCount++;
        lastErrorMsg = err.response?.data?.message || err.message || 'Unknown error';
      }
    }

    setSmartAudioFiles([]);
    setSmartImageFiles([]);
    setMatchedPairs([]);
    onDataChange();
    setIsUploading(false);
    if (failCount > 0) {
      alert(`Sync Complete: ${successCount} successful, ${failCount} failed. Error: ${lastErrorMsg}`);
    } else {
      alert(`Advanced Sync Complete: ${successCount} songs uploaded!`);
    }
  };

  return (
    <div className="bulk-page">
      <section style={{ marginBottom: '30px' }}>
        <div className="card" style={{ background: 'linear-gradient(135deg, #f0f4ff 0%, #ffffff 100%)', border: '1px solid #c2d2e0' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '10px' }}>
            <h2>🛸 Advanced Bulk Sync (Auto-Match + Metadata)</h2>
            <p className="muted">Upload bulk files and assign actors, singers, etc., to all of them.</p>
          </div>
          
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px', marginBottom: '20px' }}>
            <div>
              <label>1. Select Audio Files</label>
              <input type="file" multiple accept="audio/*" onChange={e => setSmartAudioFiles(e.target.files)} style={{ width: '100%' }} />
            </div>
            <div>
              <label>2. Select Image Files</label>
              <input type="file" multiple accept="image/*" onChange={e => setSmartImageFiles(e.target.files)} style={{ width: '100%' }} />
            </div>
          </div>
          
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '20px', marginBottom: '20px' }}>
            <div>
              <label>Actor Name (Optional)</label>
              <select value={smartActorName} onChange={e => setSmartActorName(e.target.value)} style={{ width: '100%', padding: '10px', borderRadius: '4px', border: '1px solid #ccc' }}>
                <option value="">Select Actor</option>
                {attributes.filter(a => a.type === 'Actor').map(a => <option key={a.id} value={a.name}>{a.name}</option>)}
              </select>
            </div>
            <div>
              <label>Heroine Name (Optional)</label>
              <select value={smartHeroineName} onChange={e => setSmartHeroineName(e.target.value)} style={{ width: '100%', padding: '10px', borderRadius: '4px', border: '1px solid #ccc' }}>
                <option value="">Select Heroine</option>
                {attributes.filter(a => a.type === 'Heroine').map(a => <option key={a.id} value={a.name}>{a.name}</option>)}
              </select>
            </div>
            <div>
              <label>Singer Name (Optional)</label>
              <select value={smartSingerName} onChange={e => setSmartSingerName(e.target.value)} style={{ width: '100%', padding: '10px', borderRadius: '4px', border: '1px solid #ccc' }}>
                <option value="">Select Singer</option>
                {attributes.filter(a => a.type === 'Singer').map(a => <option key={a.id} value={a.name}>{a.name}</option>)}
              </select>
            </div>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr 1fr', gap: '20px', marginBottom: '20px' }}>
            <div>
              <label>Movie Name (Optional)</label>
              <select value={smartMovieName} onChange={e => setSmartMovieName(e.target.value)} style={{ width: '100%', padding: '10px', borderRadius: '4px', border: '1px solid #ccc' }}>
                <option value="">Select Movie</option>
                {attributes.filter(a => a.type === 'Movie').map(a => <option key={a.id} value={a.name}>{a.name}</option>)}
              </select>
            </div>
            <div>
              <label>Music Director (Optional)</label>
              <select value={smartMusicDirector} onChange={e => setSmartMusicDirector(e.target.value)} style={{ width: '100%', padding: '10px', borderRadius: '4px', border: '1px solid #ccc' }}>
                <option value="">Select Director</option>
                {attributes.filter(a => a.type === 'MusicDirector').map(a => <option key={a.id} value={a.name}>{a.name}</option>)}
              </select>
            </div>
            <div>
              <label>Release Year (Optional)</label>
              <select value={smartReleaseYear} onChange={e => setSmartReleaseYear(e.target.value)} style={{ width: '100%', padding: '10px', borderRadius: '4px', border: '1px solid #ccc' }}>
                <option value="">Select Year</option>
                {Array.from({length: 40}, (_, i) => new Date().getFullYear() - i).map(year => (
                  <option key={year} value={year}>{year}</option>
                ))}
              </select>
            </div>
            <div>
              <label>Genre (Optional)</label>
              <select value={smartGenre} onChange={e => setSmartGenre(e.target.value)} style={{ width: '100%', padding: '10px', borderRadius: '4px', border: '1px solid #ccc' }}>
                <option value="">Select Genre</option>
                {attributes.filter(a => a.type === 'Genre').map(a => <option key={a.id} value={a.name}>{a.name}</option>)}
              </select>
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
    </div>
  );
}
