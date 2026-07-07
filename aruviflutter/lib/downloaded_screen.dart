import 'package:flutter/material.dart';
import 'package:path_provider/path_provider.dart';
import 'dart:io';
import 'models/artist_category.dart';
import 'services/database_service.dart';
import 'playlist_screen.dart';
import 'services/audio_service.dart';

class DownloadedScreen extends StatefulWidget {
  const DownloadedScreen({super.key});

  @override
  State<DownloadedScreen> createState() => _DownloadedScreenState();
}

class _DownloadedScreenState extends State<DownloadedScreen> {
  bool _isLoading = true;
  List<ArtistCategory> _downloadedPlaylists = [];

  @override
  void initState() {
    super.initState();
    _fetchDownloads();
  }

  Future<void> _fetchDownloads() async {
    final playlists = await DatabaseService().getDownloadedPlaylists();
    setState(() {
      _downloadedPlaylists = playlists;
      _isLoading = false;
    });
  }

  Widget _buildGridCardItem(BuildContext context, ArtistCategory category) {
    return GestureDetector(
      onTap: () {
        Navigator.push(
          context,
          MaterialPageRoute(
            builder: (context) => PlaylistScreen(
              categoryId: category.categoryId ?? '',
              imageUrl: category.categoryImage ?? '',
              title: category.categoryName ?? 'Unknown Playlist',
              subtitle: '${category.songs.length} Songs',
              songs: category.songs,
              isLocal: true,
            ),
          ),
        );
      },
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Expanded(
            child: ClipRRect(
              borderRadius: BorderRadius.circular(12),
              child: FutureBuilder<String>(
                future: () async {
                  final dir = await getApplicationDocumentsDirectory();
                  final file = File('${dir.path}/playlist_${category.categoryId}.jpg');
                  if (file.existsSync()) {
                    return file.path;
                  }
                  return '';
                }(),
                builder: (context, snapshot) {
                  if (snapshot.connectionState == ConnectionState.waiting) {
                    return Container(color: Colors.grey[800]);
                  }
                  final localPath = snapshot.data ?? '';
                  if (localPath.isNotEmpty) {
                    return Image.file(
                      File(localPath),
                      fit: BoxFit.cover,
                      width: double.infinity,
                      errorBuilder: (context, error, stackTrace) => Container(
                        color: Colors.grey[800],
                        child: const Center(child: Icon(Icons.broken_image, color: Colors.white54)),
                      ),
                    );
                  }
                  return Image.network(
                    category.categoryImage ?? '',
                    fit: BoxFit.cover,
                    width: double.infinity,
                    errorBuilder: (context, error, stackTrace) => Container(
                      color: Colors.grey[800],
                      child: const Center(child: Icon(Icons.broken_image, color: Colors.white54)),
                    ),
                  );
                }
              ),
            ),
          ),
          const SizedBox(height: 8),
          AnimatedBuilder(
            animation: AudioService(),
            builder: (context, child) {
              final audioService = AudioService();
              final isActive = audioService.currentSong != null && 
                  (audioService.currentSong?.categoryId == category.categoryId?.toString() || 
                   audioService.currentSong?.categoryName == category.categoryName);
              return Text(
                category.categoryName ?? '',
                style: TextStyle(
                  color: isActive ? const Color(0xFFEB1C24) : Colors.white,
                  fontSize: 14,
                  fontWeight: FontWeight.w500,
                ),
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
              );
            }
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFF151515),
      appBar: AppBar(
        backgroundColor: const Color(0xFF151515),
        title: const Text('Downloaded', style: TextStyle(color: Colors.white)),
        iconTheme: const IconThemeData(color: Colors.white),
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator(color: Color(0xFFEB1C24)))
          : _downloadedPlaylists.isEmpty
              ? const Center(
                  child: Text(
                    'No downloaded playlists yet.',
                    style: TextStyle(color: Colors.white54, fontSize: 16),
                  ),
                )
              : GridView.builder(
                  padding: const EdgeInsets.all(16),
                  gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                    crossAxisCount: 2,
                    childAspectRatio: 0.8, // Adjust to leave room for text
                    crossAxisSpacing: 16,
                    mainAxisSpacing: 16,
                  ),
                  itemCount: _downloadedPlaylists.length,
                  itemBuilder: (context, index) {
                    return _buildGridCardItem(context, _downloadedPlaylists[index]);
                  },
                ),
    );
  }
}
