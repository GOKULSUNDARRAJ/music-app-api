import 'package:flutter/material.dart';
import '../models/audio_model.dart';
import '../services/audio_service.dart';
import '../services/download_service.dart';
import 'add_to_playlist_sheet.dart';
import '../aruvi_code_generator_sheet.dart';
import 'package:cached_network_image/cached_network_image.dart';

class SongOptionsSheet extends StatelessWidget {
  final AudioModel song;

  const SongOptionsSheet({super.key, required this.song});

  void _showAriviCode(BuildContext context) {
    Navigator.pop(context); // Close this sheet
    showModalBottomSheet(
      context: context,
      backgroundColor: Colors.transparent,
      isScrollControlled: true,
      useRootNavigator: true,
      builder: (context) => AruviCodeGeneratorSheet(
        categoryId: song.songId ?? 'unknown',
        title: song.audioName ?? 'Unknown Song',
        imageUrl: song.imageUrl ?? '',
      ),
    );
  }

  void _showAddToPlaylist(BuildContext context) {
    Navigator.pop(context); // Close this sheet
    showModalBottomSheet(
      context: context,
      backgroundColor: Colors.transparent,
      isScrollControlled: true,
      builder: (context) => AddToPlaylistSheet(song: song),
    );
  }

  Widget _buildOptionTile(IconData icon, String title, VoidCallback onTap) {
    return ListTile(
      leading: Icon(icon, color: Colors.white, size: 28),
      title: Text(
        title,
        style: const TextStyle(color: Colors.white, fontSize: 16),
      ),
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
          // Header with Cover Art
          Padding(
            padding: const EdgeInsets.only(top: 20, left: 20, right: 20, bottom: 10),
            child: Column(
              children: [
                ClipRRect(
                  borderRadius: BorderRadius.circular(8),
                  child: CachedNetworkImage(
                    imageUrl: song.imageUrl ?? '',
                    width: 100,
                    height: 100,
                    fit: BoxFit.cover,
                    errorWidget: (context, error, stackTrace) => Container(
                      width: 100,
                      height: 100,
                      color: Colors.grey[800],
                      child: const Icon(Icons.music_note, color: Colors.white54, size: 50),
                    ),
                  ),
                ),
                const SizedBox(height: 12),
                Text(
                  song.audioName ?? 'Unknown Song',
                  style: const TextStyle(
                    color: Colors.white,
                    fontSize: 18,
                    fontWeight: FontWeight.bold,
                  ),
                  textAlign: TextAlign.center,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                ),
              ],
            ),
          ),
          
          const Divider(color: Colors.white24, height: 1),
          
          // Options List
          Padding(
            padding: const EdgeInsets.symmetric(vertical: 10),
            child: Column(
              children: [
                _buildOptionTile(Icons.favorite_border, 'Like', () {
                  // TODO: Implement Like functionality for single song
                  Navigator.pop(context);
                }),
                _buildOptionTile(Icons.play_arrow, 'Play', () {
                  Navigator.pop(context);
                  AudioService().playSongs([song], initialIndex: 0, playlistName: song.categoryName ?? 'Song');
                }),
                _buildOptionTile(Icons.queue_music, 'Add to Playlist', () => _showAddToPlaylist(context)),
                _buildOptionTile(Icons.download_outlined, 'Download', () {
                  Navigator.pop(context);
                  DownloadService().downloadSingleSong(song);
                  ScaffoldMessenger.of(context).showSnackBar(
                    const SnackBar(content: Text('Downloading...'), backgroundColor: Colors.green),
                  );
                }),
                _buildOptionTile(Icons.reply, 'Share', () {
                  Navigator.pop(context);
                }),
                _buildOptionTile(Icons.playlist_add, 'Add to Queue', () {
                  Navigator.pop(context);
                  // TODO: Implement Queue logic in AudioService
                }),
                _buildOptionTile(Icons.graphic_eq, 'Show Arivi Code', () => _showAriviCode(context)),
              ],
            ),
          ),
          
          const SizedBox(height: 10),
        ],
      ),
    );
  }
}
