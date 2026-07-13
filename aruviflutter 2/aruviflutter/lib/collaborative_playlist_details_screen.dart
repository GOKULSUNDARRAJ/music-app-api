import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:http/http.dart' as http;
import 'dart:convert';
import 'package:share_plus/share_plus.dart';
import 'services/audio_service.dart';
import 'models/audio_model.dart';

class CollaborativePlaylistDetailsScreen extends StatefulWidget {
  final String playlistId;
  final String playlistName;
  final bool isOwner;
  final String inviteCode;

  const CollaborativePlaylistDetailsScreen({
    super.key,
    required this.playlistId,
    required this.playlistName,
    required this.isOwner,
    required this.inviteCode,
  });

  @override
  State<CollaborativePlaylistDetailsScreen> createState() => _CollaborativePlaylistDetailsScreenState();
}

class _CollaborativePlaylistDetailsScreenState extends State<CollaborativePlaylistDetailsScreen> {
  bool _isLoading = true;
  List<dynamic> _songs = [];
  int? _currentUserId;

  @override
  void initState() {
    super.initState();
    _loadUser();
    _fetchSongs();
  }

  Future<void> _loadUser() async {
    final prefs = await SharedPreferences.getInstance();
    // In our backend User model, id is returned, assuming stored as 'userId' or token decoding.
    // For now we check token. Actually the endpoint returns 'addedById'. 
    // We can get our own user ID from shared prefs if it's there.
    final uid = prefs.getInt('userId');
    if (mounted) setState(() => _currentUserId = uid);
  }

  Future<void> _fetchSongs() async {
    setState(() => _isLoading = true);
    try {
      final prefs = await SharedPreferences.getInstance();
      final token = prefs.getString('access_token');
      if (token == null) return;
      
      final response = await http.get(
        Uri.parse('https://music-app-api-1.onrender.com/api/user/collaborative-playlist/\${widget.playlistId}'),
        headers: {'Authorization': 'Bearer $token'},
      );

      if (response.statusCode == 200) {
        final data = json.decode(response.body);
        if (data['status'] == true) {
          setState(() {
            _songs = data['songs'];
          });
        }
      }
    } catch (e) {
      debugPrint('Error fetching songs: $e');
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  Future<void> _removeSong(String entryId, int index) async {
    final song = _songs[index];
    setState(() {
      _songs.removeAt(index);
    });

    try {
      final prefs = await SharedPreferences.getInstance();
      final token = prefs.getString('access_token');
      if (token == null) return;

      final response = await http.delete(
        Uri.parse('https://music-app-api-1.onrender.com/api/user/collaborative-playlist/\${widget.playlistId}/song/$entryId'),
        headers: {'Authorization': 'Bearer $token'},
      );

      if (response.statusCode != 200) {
        // Revert on failure
        setState(() => _songs.insert(index, song));
        if (mounted) ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Failed to remove song')));
      }
    } catch (e) {
      debugPrint('Error removing song: $e');
      setState(() => _songs.insert(index, song));
    }
  }

  void _playAll() {
    if (_songs.isEmpty) return;
    
    final audioService = AudioService();
    final List<AudioModel> audioModels = _songs.map((s) => AudioModel.fromJson(s)).toList();
    
    audioService.playSongs(audioModels, initialIndex: 0, playlistName: widget.playlistName);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFF121212),
      appBar: AppBar(
        backgroundColor: Colors.transparent,
        elevation: 0,
        iconTheme: const IconThemeData(color: Colors.white),
        actions: [
          IconButton(
            icon: const Icon(Icons.share),
            onPressed: () {
              Share.share('Join my collaborative playlist "${widget.playlistName}" using code: \${widget.inviteCode}');
            },
          ),
        ],
      ),
      extendBodyBehindAppBar: true,
      body: Stack(
        children: [
          // Header gradient
          Container(
            height: 300,
            decoration: BoxDecoration(
              gradient: LinearGradient(
                begin: Alignment.topCenter,
                end: Alignment.bottomCenter,
                colors: [
                  const Color(0xFFEB1C24).withOpacity(0.8),
                  const Color(0xFF121212),
                ],
              ),
            ),
          ),
          
          SafeArea(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 20),
                  child: Row(
                    children: [
                      Container(
                        width: 120,
                        height: 120,
                        decoration: BoxDecoration(
                          color: const Color(0xFF282828),
                          borderRadius: BorderRadius.circular(12),
                          boxShadow: [
                            BoxShadow(
                              color: Colors.black.withOpacity(0.5),
                              blurRadius: 15,
                              offset: const Offset(0, 8),
                            )
                          ],
                        ),
                        child: _songs.isNotEmpty && _songs.first['imageUrl'] != null
                            ? ClipRRect(
                                borderRadius: BorderRadius.circular(12),
                                child: Image.network(_songs.first['imageUrl'], fit: BoxFit.cover),
                              )
                            : const Icon(Icons.people_alt, size: 50, color: Colors.white54),
                      ),
                      const SizedBox(width: 20),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              widget.playlistName,
                              style: const TextStyle(color: Colors.white, fontSize: 24, fontWeight: FontWeight.bold),
                            ),
                            const SizedBox(height: 8),
                            Text(
                              widget.isOwner ? 'Your Collaborative Playlist' : 'Shared Playlist',
                              style: const TextStyle(color: Colors.white70, fontSize: 14),
                            ),
                            const SizedBox(height: 16),
                            ElevatedButton.icon(
                              onPressed: _songs.isEmpty ? null : _playAll,
                              icon: const Icon(Icons.play_arrow, color: Colors.white),
                              label: const Text('Play All', style: TextStyle(color: Colors.white)),
                              style: ElevatedButton.styleFrom(
                                backgroundColor: const Color(0xFFEB1C24),
                                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
                              ),
                            )
                          ],
                        ),
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 20),
                const Padding(
                  padding: EdgeInsets.symmetric(horizontal: 20),
                  child: Text(
                    'To add songs, tap the 3 dots on any song and select "Add to Playlist"',
                    style: TextStyle(color: Colors.white54, fontSize: 12, fontStyle: FontStyle.italic),
                  ),
                ),
                const SizedBox(height: 10),
                Expanded(
                  child: _isLoading
                      ? const Center(child: CircularProgressIndicator(color: Color(0xFFEB1C24)))
                      : _songs.isEmpty
                          ? Center(
                              child: Text('No songs yet.\nAdd some songs to get started!',
                                  textAlign: TextAlign.center,
                                  style: TextStyle(color: Colors.white.withOpacity(0.5), fontSize: 16)))
                          : ListView.builder(
                              padding: const EdgeInsets.only(bottom: 100),
                              itemCount: _songs.length,
                              itemBuilder: (context, index) {
                                final song = _songs[index];
                                final bool canDelete = widget.isOwner || (_currentUserId != null && song['addedById'] == _currentUserId);

                                final listItem = ListTile(
                                  contentPadding: const EdgeInsets.symmetric(horizontal: 20, vertical: 4),
                                  leading: ClipRRect(
                                    borderRadius: BorderRadius.circular(4),
                                    child: Image.network(
                                      song['imageUrl'],
                                      width: 48,
                                      height: 48,
                                      fit: BoxFit.cover,
                                      errorBuilder: (_, __, ___) => Container(
                                        width: 48,
                                        height: 48,
                                        color: Colors.grey[900],
                                        child: const Icon(Icons.music_note, color: Colors.white54),
                                      ),
                                    ),
                                  ),
                                  title: Text(
                                    song['audioName'],
                                    maxLines: 1,
                                    overflow: TextOverflow.ellipsis,
                                    style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold),
                                  ),
                                  subtitle: Row(
                                    children: [
                                      CircleAvatar(
                                        radius: 8,
                                        backgroundColor: const Color(0xFFEB1C24),
                                        child: Text(
                                          (song['addedByUserName'] as String).substring(0, 1).toUpperCase(),
                                          style: const TextStyle(color: Colors.white, fontSize: 10, fontWeight: FontWeight.bold),
                                        ),
                                      ),
                                      const SizedBox(width: 6),
                                      Expanded(
                                        child: Text(
                                          'Added by ${song['addedByUserName']}',
                                          maxLines: 1,
                                          overflow: TextOverflow.ellipsis,
                                          style: const TextStyle(color: Colors.white54, fontSize: 12),
                                        ),
                                      ),
                                    ],
                                  ),
                                  onTap: () {
                                    final audioService = AudioService();
                                    final audioModel = AudioModel.fromJson(song);
                                    audioService.playSongs([audioModel], initialIndex: 0, playlistName: widget.playlistName);
                                  },
                                );

                                if (canDelete) {
                                  return Dismissible(
                                    key: Key(song['entryId'].toString()),
                                    direction: DismissDirection.endToStart,
                                    background: Container(
                                      alignment: Alignment.centerRight,
                                      padding: const EdgeInsets.only(right: 20),
                                      color: Colors.red,
                                      child: const Icon(Icons.delete, color: Colors.white),
                                    ),
                                    onDismissed: (direction) {
                                      _removeSong(song['entryId'].toString(), index);
                                    },
                                    child: listItem,
                                  );
                                }

                                return listItem;
                              },
                            ),
                ),
              ],
            ),
          ),
          
        ],
      ),
    );
  }
}
