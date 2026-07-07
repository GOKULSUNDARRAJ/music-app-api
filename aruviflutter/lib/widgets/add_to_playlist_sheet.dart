import 'package:flutter/material.dart';
import '../models/audio_model.dart';
import '../services/database_service.dart';
import '../services/download_service.dart';
import 'create_playlist_dialog.dart';

class AddToPlaylistSheet extends StatefulWidget {
  final AudioModel song;

  const AddToPlaylistSheet({super.key, required this.song});

  @override
  State<AddToPlaylistSheet> createState() => _AddToPlaylistSheetState();
}

class _AddToPlaylistSheetState extends State<AddToPlaylistSheet> {
  List<Map<String, dynamic>> _playlists = [];
  List<String> _addedPlaylistIds = [];
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _loadPlaylists();
  }

  Future<void> _loadPlaylists() async {
    final playlists = await DatabaseService().getCustomPlaylists();
    final addedIds = await DatabaseService().getPlaylistsContainingSong(widget.song.songId ?? '');
    if (mounted) {
      setState(() {
        _playlists = playlists;
        _addedPlaylistIds = addedIds;
        _isLoading = false;
      });
    }
  }

  Future<void> _createNewPlaylist() async {
    final name = await showDialog<String>(
      context: context,
      builder: (context) => const CreatePlaylistDialog(),
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
          content: Text('Added to \$playlistName'),
          backgroundColor: Colors.green,
          duration: const Duration(seconds: 2),
        ),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(vertical: 20),
      decoration: const BoxDecoration(
        color: Color(0xFF1E1E1E),
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
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
              
              return ListTile(
                leading: ClipRRect(
                  borderRadius: BorderRadius.circular(4),
                  child: img != null && img.isNotEmpty
                      ? Image.network(img, width: 48, height: 48, fit: BoxFit.cover, errorBuilder: (c, e, s) => const Icon(Icons.music_note, color: Colors.white54, size: 30))
                      : Container(
                          width: 48,
                          height: 48,
                          color: Colors.grey[800],
                          child: const Icon(Icons.music_note, color: Colors.white54),
                        ),
                ),
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
    );
  }
}
