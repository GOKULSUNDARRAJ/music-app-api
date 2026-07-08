import 'package:flutter/material.dart';
import 'services/database_service.dart';
import 'playlist_screen.dart';
import 'widgets/create_playlist_dialog.dart';
import 'select_songs_screen.dart';

class CustomPlaylistsScreen extends StatefulWidget {
  const CustomPlaylistsScreen({super.key});

  @override
  State<CustomPlaylistsScreen> createState() => _CustomPlaylistsScreenState();
}

class _CustomPlaylistsScreenState extends State<CustomPlaylistsScreen> {
  List<Map<String, dynamic>> _playlists = [];
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _loadPlaylists();
  }

  Future<void> _loadPlaylists() async {
    final playlists = await DatabaseService().getCustomPlaylists();
    if (mounted) {
      setState(() {
        _playlists = playlists;
        _isLoading = false;
      });
    }
  }

  Future<void> _openPlaylist(String id, String name, String? imageUrl) async {
    final songs = await DatabaseService().getCustomPlaylistSongs(id);
    if (mounted) {
      Navigator.push(
        context,
        MaterialPageRoute(
          builder: (context) => PlaylistScreen(
            categoryId: id,
            title: name,
            subtitle: '${songs.length} Songs',
            imageUrl: imageUrl ?? '',
            songs: songs,
            isLocal: true,
            isCustomPlaylist: true,
          ),
        ),
      ).then((_) => _loadPlaylists()); // Refresh if they changed it
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFF151515),
      appBar: AppBar(
        backgroundColor: const Color(0xFF151515),
        elevation: 0,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back, color: Colors.white),
          onPressed: () => Navigator.pop(context),
        ),
        title: const Text(
          'Albums & Playlists',
          style: TextStyle(color: Colors.white, fontSize: 18),
        ),
        actions: [
          IconButton(
            icon: const Icon(Icons.add, color: Colors.white),
            onPressed: () async {
              final name = await showDialog<String>(
                context: context,
                builder: (context) => const CreatePlaylistDialog(),
              );
              if (name != null && name.isNotEmpty) {
                final playlistId = await DatabaseService().createCustomPlaylist(name);
                
                if (mounted) {
                  await Navigator.push(
                    context,
                    MaterialPageRoute(
                      builder: (context) => SelectSongsScreen(
                        playlistId: playlistId,
                        playlistName: name,
                      ),
                    ),
                  );
                }
                
                _loadPlaylists(); // Refresh
              }
            },
          ),
        ],
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator(color: Color(0xFFEB1C24)))
          : _playlists.isEmpty
              ? const Center(
                  child: Text(
                    'No custom playlists found.',
                    style: TextStyle(color: Colors.white70, fontSize: 16),
                  ),
                )
              : ListView.builder(
                  itemCount: _playlists.length,
                  padding: const EdgeInsets.only(top: 10),
                  itemBuilder: (context, index) {
                    final playlist = _playlists[index];
                    final name = playlist['name'] as String;
                    final id = playlist['playlistId'] as String;
                    final img = playlist['imageUrl'] as String?;
                    
                    return ListTile(
                      contentPadding: const EdgeInsets.symmetric(horizontal: 20, vertical: 8),
                      leading: ClipRRect(
                        borderRadius: BorderRadius.circular(4),
                        child: img != null && img.isNotEmpty
                            ? Image.network(img, width: 56, height: 56, fit: BoxFit.cover, errorBuilder: (c, e, s) => Container(width: 56, height: 56, color: Colors.grey[850], child: const Icon(Icons.album, color: Colors.white54)))
                            : Container(
                                width: 56,
                                height: 56,
                                color: Colors.grey[850],
                                child: const Icon(Icons.album, color: Colors.white54),
                              ),
                      ),
                      title: Text(
                        name,
                        style: const TextStyle(color: Colors.white, fontSize: 16, fontWeight: FontWeight.w500),
                      ),
                      subtitle: const Padding(
                        padding: EdgeInsets.only(top: 4.0),
                        child: Text(
                          'Local Custom Playlist',
                          style: TextStyle(color: Colors.white54, fontSize: 13),
                        ),
                      ),
                      trailing: const Icon(Icons.chevron_right, color: Colors.white54),
                      onTap: () => _openPlaylist(id, name, img),
                    );
                  },
                ),
    );
  }
}
