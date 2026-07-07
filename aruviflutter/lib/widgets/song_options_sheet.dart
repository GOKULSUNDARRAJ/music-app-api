import 'package:flutter/material.dart';
import '../models/audio_model.dart';
import '../services/audio_service.dart';
import '../services/download_service.dart';
import '../services/database_service.dart';
import 'add_to_playlist_sheet.dart';

class SongOptionsSheet extends StatefulWidget {
  final AudioModel song;

  const SongOptionsSheet({super.key, required this.song});

  @override
  State<SongOptionsSheet> createState() => _SongOptionsSheetState();
}

class _SongOptionsSheetState extends State<SongOptionsSheet> {
  bool _isLiked = false;
  bool _isDownloaded = false;
  bool _isDownloading = false;

  @override
  void initState() {
    super.initState();
    _checkStatus();
  }

  Future<void> _checkStatus() async {
    if (widget.song.songId != null) {
      final isLiked = await DatabaseService().isSongLiked(widget.song.songId!);
      final isDownloaded = await DatabaseService().isSingleDownloaded(widget.song.songId!);
      if (mounted) {
        setState(() {
          _isLiked = isLiked;
          _isDownloaded = isDownloaded;
        });
      }
    }
  }

  Future<void> _toggleLike() async {
    if (widget.song.songId == null) return;
    
    setState(() {
      _isLiked = !_isLiked;
    });

    if (_isLiked) {
      await DatabaseService().likeSong(widget.song);
    } else {
      await DatabaseService().unlikeSong(widget.song.songId!);
    }
  }

  void _showAddToPlaylist(BuildContext context) {
    Navigator.pop(context); // Close this sheet
    showModalBottomSheet(
      context: context,
      backgroundColor: Colors.transparent,
      isScrollControlled: true,
      builder: (context) => AddToPlaylistSheet(song: widget.song),
    );
  }

  Widget _buildOptionTile(IconData icon, String title, VoidCallback onTap, {Color iconColor = Colors.white, Widget? trailing}) {
    return ListTile(
      leading: Icon(icon, color: iconColor, size: 28),
      title: Text(
        title,
        style: const TextStyle(color: Colors.white, fontSize: 16),
      ),
      trailing: trailing,
      onTap: onTap,
    );
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: const BoxDecoration(
        color: Color(0xFF1E1E1E),
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          // Header without Cover Art
          Padding(
            padding: const EdgeInsets.only(top: 30, left: 20, right: 20, bottom: 20),
            child: Text(
              widget.song.audioName ?? 'Unknown Song',
              style: const TextStyle(
                color: Colors.white,
                fontSize: 20,
                fontWeight: FontWeight.bold,
              ),
              textAlign: TextAlign.center,
              maxLines: 2,
              overflow: TextOverflow.ellipsis,
            ),
          ),
          
          const Divider(color: Colors.white24, height: 1),
          
          // Options List
          Padding(
            padding: const EdgeInsets.symmetric(vertical: 10),
            child: Column(
              children: [
                _buildOptionTile(
                  _isLiked ? Icons.favorite : Icons.favorite_border,
                  _isLiked ? 'Liked' : 'Like',
                  () {
                    _toggleLike();
                    Navigator.pop(context);
                  },
                  iconColor: _isLiked ? const Color(0xFFEB1C24) : Colors.white,
                ),
                AnimatedBuilder(
                  animation: AudioService(),
                  builder: (context, child) {
                    final isCurrentSong = AudioService().currentSong?.songId == widget.song.songId;
                    final isPlaying = isCurrentSong && AudioService().isPlaying;
                    
                    return _buildOptionTile(
                      isPlaying ? Icons.pause : Icons.play_arrow,
                      isPlaying ? 'Pause' : 'Play',
                      () {
                        Navigator.pop(context);
                        if (isCurrentSong) {
                          AudioService().togglePlayPause();
                        } else {
                          AudioService().playSongs([widget.song], initialIndex: 0, playlistName: widget.song.categoryName ?? 'Song');
                        }
                      },
                    );
                  },
                ),
                _buildOptionTile(Icons.queue_music, 'Add to Playlist', () => _showAddToPlaylist(context)),
                _buildOptionTile(
                  _isDownloaded ? Icons.download_done : Icons.download_outlined,
                  _isDownloading 
                      ? 'Downloading...' 
                      : (_isDownloaded ? 'Remove from download' : 'Download'),
                  () async {
                    if (_isDownloading) return;
                    
                    setState(() {
                      _isDownloading = true;
                    });

                    if (_isDownloaded) {
                      await DownloadService().removeSingleSong(widget.song);
                      if (context.mounted) {
                        ScaffoldMessenger.of(context).showSnackBar(
                          const SnackBar(content: Text('Removed from downloads'), backgroundColor: Colors.red),
                        );
                      }
                    } else {
                      await DownloadService().downloadSingleSong(widget.song);
                      if (context.mounted) {
                        ScaffoldMessenger.of(context).showSnackBar(
                          const SnackBar(content: Text('Downloaded successfully'), backgroundColor: Colors.green),
                        );
                      }
                    }

                    if (mounted) {
                      setState(() {
                        _isDownloading = false;
                      });
                      _checkStatus();
                    }
                  },
                  iconColor: _isDownloaded ? Colors.green : Colors.white,
                  trailing: _isDownloading 
                      ? const SizedBox(
                          width: 24, 
                          height: 24, 
                          child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white)
                        )
                      : null,
                ),
              ],
            ),
          ),
          
          const SizedBox(height: 10),
        ],
      ),
    );
  }
}
