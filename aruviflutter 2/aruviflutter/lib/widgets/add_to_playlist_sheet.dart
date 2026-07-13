import 'package:flutter/material.dart';
import '../models/audio_model.dart';
import '../services/database_service.dart';
import '../services/download_service.dart';
import '../create_playlist_screen.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:http/http.dart' as http;
import 'dart:convert';

class AddToPlaylistSheet extends StatefulWidget {
  final AudioModel song;

  const AddToPlaylistSheet({super.key, required this.song});

  @override
  State<AddToPlaylistSheet> createState() => _AddToPlaylistSheetState();
}

class _AddToPlaylistSheetState extends State<AddToPlaylistSheet> {
  List<Map<String, dynamic>> _playlists = [];
  List<dynamic> _collaborativePlaylists = [];
  List<String> _addedPlaylistIds = [];
  List<String> _addedCollaborativeIds = [];
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _loadPlaylists();
  }

  Future<void> _loadPlaylists() async {
    final playlists = await DatabaseService().getCustomPlaylists();
    final addedIds = await DatabaseService().getPlaylistsContainingSong(widget.song.songId ?? '');
    
    // Fetch up to 4 images per playlist for the 2x2 grid
    List<Map<String, dynamic>> mutablePlaylists = [];
    for (var p in playlists) {
      final pId = p['playlistId'] as String;
      final songs = await DatabaseService().getCustomPlaylistSongs(pId);
      final distinctImages = songs
          .map((s) => s.imageUrl)
          .where((url) => url != null && url.isNotEmpty)
          .cast<String>()
          .toSet()
          .toList();
      
      final mutableP = Map<String, dynamic>.from(p);
      mutableP['gridImages'] = distinctImages.take(4).toList();
      mutablePlaylists.add(mutableP);
    }

    // Fetch collaborative playlists
    List<dynamic> collabPlaylists = [];
    List<String> addedCollabIds = [];
    try {
      final prefs = await SharedPreferences.getInstance();
      final token = prefs.getString('access_token');
      if (token != null) {
        final uri = widget.song.audioUrl != null && widget.song.audioUrl!.isNotEmpty 
            ? 'https://music-app-api-1.onrender.com/api/user/collaborative-playlists?checkAudioUrl=${Uri.encodeComponent(widget.song.audioUrl!)}'
            : 'https://music-app-api-1.onrender.com/api/user/collaborative-playlists';
        final response = await http.get(
          Uri.parse(uri),
          headers: {'Authorization': 'Bearer $token'},
        );
        if (response.statusCode == 200) {
          final data = json.decode(response.body);
          if (data['status'] == true) {
            collabPlaylists = data['playlists'];
            for (var p in collabPlaylists) {
              if (p['hasSong'] == true) {
                addedCollabIds.add(p['id'].toString());
              }
            }
          }
        }
      }
    } catch (e) {
      debugPrint('Error fetching collaborative playlists: $e');
    }

    if (mounted) {
      setState(() {
        _playlists = mutablePlaylists;
        _collaborativePlaylists = collabPlaylists;
        _addedPlaylistIds = addedIds;
        _addedCollaborativeIds = addedCollabIds;
        _isLoading = false;
      });
    }
  }

  Future<void> _createNewPlaylist() async {
    final name = await Navigator.push<String>(
      context,
      MaterialPageRoute(builder: (context) => const CreatePlaylistScreen()),
    );

    if (name != null && name.isNotEmpty) {
      // Create new playlist
      final playlistId = await DatabaseService().createCustomPlaylist(name);
      
      // Add song to it immediately
      await _addSongToPlaylist(playlistId, name);
    }
  }

  Future<void> _addSongToPlaylist(String playlistId, String playlistName) async {
    // Save to DB
    await DatabaseService().addSongToCustomPlaylist(playlistId, widget.song);
    
    // Trigger download for offline playback
    DownloadService().downloadSingleSong(widget.song);

    if (mounted) {
      Navigator.pop(context); // Close sheet
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('Added to $playlistName'),
          backgroundColor: Colors.green,
          duration: const Duration(seconds: 2),
        ),
      );
    }
  }

  Future<void> _addSongToCollaborativePlaylist(String playlistId, String playlistName) async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final token = prefs.getString('access_token');
      if (token == null) return;

      final response = await http.post(
        Uri.parse('https://music-app-api-1.onrender.com/api/user/collaborative-playlist/$playlistId/song'),
        headers: {
          'Authorization': 'Bearer $token',
          'Content-Type': 'application/json',
        },
        body: json.encode({
          'songId': widget.song.songId,
          'audioUrl': widget.song.audioUrl,
          'audioName': widget.song.audioName,
          'imageUrl': widget.song.imageUrl,
          'categoryName': widget.song.categoryName,
        }),
      );

      if (response.statusCode == 201) {
        if (mounted) {
          setState(() {
            _addedCollaborativeIds.add(playlistId);
          });
          Navigator.pop(context);
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text('Added to $playlistName'), backgroundColor: Colors.green),
          );
        }
      } else {
        final data = json.decode(response.body);
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text(data['message'] ?? 'Failed to add song'), backgroundColor: Colors.red),
          );
        }
      }
    } catch (e) {
      debugPrint('Error adding to collaborative playlist: $e');
    }
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: const BoxDecoration(
        color: Color(0xFF1E1E1E),
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      child: SingleChildScrollView(
        padding: const EdgeInsets.only(top: 20, bottom: 90),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Padding(
            padding: EdgeInsets.symmetric(horizontal: 20, vertical: 10),
            child: Text(
              'Save in',
              style: TextStyle(
                color: Colors.white,
                fontSize: 20,
                fontWeight: FontWeight.bold,
              ),
            ),
          ),
          const Divider(color: Colors.white24),
          
          if (_isLoading)
            const Padding(
              padding: EdgeInsets.all(20.0),
              child: Center(child: CircularProgressIndicator(color: Color(0xFFEB1C24))),
            )
          else ...[
            // Existing custom playlists
            ..._playlists.map((playlist) {
              final img = playlist['imageUrl'] as String?;
              final name = playlist['name'] as String;
              final id = playlist['playlistId'] as String;
              final gridImages = playlist['gridImages'] as List<String>? ?? [];
              
              Widget leadingWidget;
              if (gridImages.length >= 4) {
                leadingWidget = SizedBox(
                  width: 48,
                  height: 48,
                  child: ClipRRect(
                    borderRadius: BorderRadius.circular(4),
                    child: GridView.count(
                      crossAxisCount: 2,
                      mainAxisSpacing: 0,
                      crossAxisSpacing: 0,
                      physics: const NeverScrollableScrollPhysics(),
                      children: gridImages.map((url) => Image.network(url, fit: BoxFit.cover)).toList(),
                    ),
                  ),
                );
              } else {
                final img = gridImages.isNotEmpty ? gridImages.first : (playlist['imageUrl'] as String?);
                leadingWidget = ClipRRect(
                  borderRadius: BorderRadius.circular(4),
                  child: img != null && img.isNotEmpty
                      ? Image.network(img, width: 48, height: 48, fit: BoxFit.cover, errorBuilder: (c, e, s) => const Icon(Icons.music_note, color: Colors.white54, size: 30))
                      : Container(
                          width: 48,
                          height: 48,
                          color: Colors.grey[800],
                          child: const Icon(Icons.music_note, color: Colors.white54),
                        ),
                );
              }
              
              return ListTile(
                leading: leadingWidget,
                title: Text(name, style: const TextStyle(color: Colors.white)),
                trailing: _addedPlaylistIds.contains(id) 
                    ? const Icon(Icons.check_circle, color: Colors.green)
                    : const Icon(Icons.add_circle_outline, color: Colors.white54),
                onTap: () {
                  if (!_addedPlaylistIds.contains(id)) {
                    _addSongToPlaylist(id, name);
                  }
                },
              );
            }),
            
            // Collaborative Playlists
            if (_collaborativePlaylists.isNotEmpty) ...[
              const Padding(
                padding: EdgeInsets.symmetric(horizontal: 20, vertical: 10),
                child: Text('Collaborative Playlists', style: TextStyle(color: Colors.white70, fontSize: 14)),
              ),
              ..._collaborativePlaylists.map((playlist) {
                final id = playlist['id'] as String;
                final name = playlist['name'] as String;
                final img = playlist['imageUrl'] as String?;
                
                return ListTile(
                  leading: Container(
                    width: 48,
                    height: 48,
                    decoration: BoxDecoration(
                      color: const Color(0xFF282828),
                      borderRadius: BorderRadius.circular(4),
                    ),
                    child: img != null
                        ? ClipRRect(
                            borderRadius: BorderRadius.circular(4),
                            child: Image.network(img, fit: BoxFit.cover),
                          )
                        : const Icon(Icons.people_alt, color: Colors.white54),
                  ),
                  title: Text(name, style: const TextStyle(color: Colors.white)),
                  trailing: _addedCollaborativeIds.contains(id) 
                      ? const Icon(Icons.check_circle, color: Colors.green)
                      : const Icon(Icons.add_circle_outline, color: Colors.white54),
                  onTap: () {
                    if (!_addedCollaborativeIds.contains(id)) {
                      _addSongToCollaborativePlaylist(id, name);
                    }
                  },
                );
              }),
            ],
            
            // New Playlist Button
            ListTile(
              leading: Container(
                width: 48,
                height: 48,
                decoration: BoxDecoration(
                  color: Colors.grey[850],
                  borderRadius: BorderRadius.circular(4),
                ),
                child: const Icon(Icons.add, color: Colors.white),
              ),
              title: const Text('New playlist', style: TextStyle(color: Colors.white)),
              onTap: _createNewPlaylist,
            ),
          ],
        ],
      ),
      ),
    );
  }
}
