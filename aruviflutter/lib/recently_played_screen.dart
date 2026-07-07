import 'package:flutter/material.dart';
import 'models/audio_model.dart';
import 'services/database_service.dart';
import 'services/audio_service.dart';
import 'widgets/add_to_playlist_sheet.dart';

class RecentlyPlayedScreen extends StatefulWidget {
  const RecentlyPlayedScreen({super.key});

  @override
  State<RecentlyPlayedScreen> createState() => _RecentlyPlayedScreenState();
}

class _RecentlyPlayedScreenState extends State<RecentlyPlayedScreen> {
  List<AudioModel> _songs = [];
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _loadSongs();
  }

  Future<void> _loadSongs() async {
    final songs = await DatabaseService().getRecentlyPlayedSongs();
    if (mounted) {
      setState(() {
        _songs = songs;
        _isLoading = false;
      });
    }
  }

  void _playAll({bool shuffle = false}) {
    if (_songs.isEmpty) return;
    
    List<AudioModel> playList = List.from(_songs);
    if (shuffle) {
      playList.shuffle();
    }
    
    AudioService().playSongs(playList, playlistName: 'Recently Played');
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFF181A20),
      appBar: AppBar(
        backgroundColor: const Color(0xFF181A20),
        elevation: 0,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back, color: Colors.white),
          onPressed: () => Navigator.pop(context),
        ),
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator(color: Color(0xFFEB1C24)))
          : Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                // Header section
                Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 16.0),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const Text(
                        'Recently Played',
                        style: TextStyle(
                          color: Colors.white,
                          fontSize: 24,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                      const SizedBox(height: 4),
                      Text(
                        '\${_songs.length} Songs',
                        style: const TextStyle(
                          color: Colors.white54,
                          fontSize: 14,
                        ),
                      ),
                      const SizedBox(height: 16),
                      // Controls
                      Row(
                        children: [
                          _buildControlButton(Icons.shuffle, () => _playAll(shuffle: true)),
                          const SizedBox(width: 16),
                          _buildControlButton(Icons.play_arrow, () => _playAll()),
                          const Spacer(),
                          FloatingActionButton(
                            backgroundColor: const Color(0xFFEB1C24),
                            onPressed: () => _playAll(),
                            child: const Icon(Icons.play_arrow, color: Colors.white, size: 32),
                          ),
                        ],
                      ),
                      const SizedBox(height: 24),
                    ],
                  ),
                ),
                // Song list
                Expanded(
                  child: _songs.isEmpty
                      ? const Center(
                          child: Text(
                            'No recently played songs',
                            style: TextStyle(color: Colors.white54, fontSize: 16),
                          ),
                        )
                      : ListView.builder(
                          itemCount: _songs.length,
                          itemBuilder: (context, index) {
                            final song = _songs[index];
                            final isPlaying = AudioService().currentSong?.songId == song.songId;

                            return ListTile(
                              contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
                              leading: ClipRRect(
                                borderRadius: BorderRadius.circular(4),
                                child: song.imageUrl != null && song.imageUrl!.isNotEmpty
                                    ? Image.network(
                                        song.imageUrl!,
                                        width: 50,
                                        height: 50,
                                        fit: BoxFit.cover,
                                        errorBuilder: (c, e, s) => _buildPlaceholder(),
                                      )
                                    : _buildPlaceholder(),
                              ),
                              title: Text(
                                song.audioName ?? 'Unknown',
                                style: TextStyle(
                                  color: isPlaying ? const Color(0xFFEB1C24) : Colors.white,
                                  fontWeight: FontWeight.w500,
                                ),
                                maxLines: 1,
                                overflow: TextOverflow.ellipsis,
                              ),
                              subtitle: Text(
                                song.categoryName ?? 'Unknown',
                                style: const TextStyle(color: Colors.white54, fontSize: 12),
                                maxLines: 1,
                                overflow: TextOverflow.ellipsis,
                              ),
                              trailing: IconButton(
                                icon: const Icon(Icons.more_vert, color: Colors.white54),
                                onPressed: () {
                                  showModalBottomSheet(
                                    context: context,
                                    backgroundColor: Colors.transparent,
                                    isScrollControlled: true,
                                    useRootNavigator: true,
                                    builder: (context) => AddToPlaylistSheet(song: song),
                                  );
                                },
                              ),
                              onTap: () {
                                AudioService().playSongs(_songs, initialIndex: index, playlistName: 'Recently Played');
                              },
                            );
                          },
                        ),
                ),
              ],
            ),
    );
  }

  Widget _buildPlaceholder() {
    return Container(
      width: 50,
      height: 50,
      color: Colors.grey[800],
      child: const Icon(Icons.music_note, color: Colors.white54),
    );
  }

  Widget _buildControlButton(IconData icon, VoidCallback onTap) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(24),
      child: Container(
        padding: const EdgeInsets.all(8),
        decoration: BoxDecoration(
          shape: BoxShape.circle,
          border: Border.all(color: Colors.white54),
        ),
        child: Icon(icon, color: Colors.white, size: 20),
      ),
    );
  }
}
