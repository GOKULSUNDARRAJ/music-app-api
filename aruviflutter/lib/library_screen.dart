import 'package:flutter/material.dart';
import 'liked_screen.dart';
import 'my_playlists_screen.dart';
import 'downloaded_screen.dart';
import 'custom_playlists_screen.dart';
import 'services/database_service.dart';
import 'playlist_screen.dart';
import 'recently_played_playlists_screen.dart';

class LibraryScreen extends StatelessWidget {
  const LibraryScreen({super.key});

  Widget _buildListItem(IconData icon, String title, VoidCallback onTap) {
    return InkWell(
      onTap: onTap,
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 16),
        child: Row(
          children: [
            Icon(icon, color: Colors.white, size: 24),
            const SizedBox(width: 20),
            Expanded(
              child: Text(
                title,
                style: const TextStyle(
                  color: Colors.white,
                  fontSize: 16,
                  fontWeight: FontWeight.w400,
                ),
              ),
            ),
            const Icon(Icons.chevron_right, color: Colors.white, size: 20),
          ],
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFF151515),
      appBar: AppBar(
        backgroundColor: const Color(0xFF151515),
        elevation: 0,
        leading: Padding(
          padding: const EdgeInsets.only(left: 16.0),
          child: Center(
            child: Container(
              width: 32,
              height: 32,
              decoration: const BoxDecoration(
                shape: BoxShape.circle,
                color: Color(0xFFEB1C24),
              ),
              child: const Icon(Icons.waves, color: Colors.white, size: 20), // Placeholder for logo
            ),
          ),
        ),
        title: const Text(
          'Library',
          style: TextStyle(
            color: Colors.white,
            fontSize: 20,
            fontWeight: FontWeight.bold,
          ),
        ),
        centerTitle: true,
        actions: [
          Padding(
            padding: const EdgeInsets.only(right: 16.0),
            child: Center(
              child: Container(
                width: 32,
                height: 32,
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  border: Border.all(color: Colors.white, width: 1.5),
                ),
                child: const Icon(Icons.support_agent, color: Colors.white, size: 20),
              ),
            ),
          ),
        ],
      ),
      body: ListView(
        padding: const EdgeInsets.only(top: 10, bottom: 20),
        children: [
          _buildListItem(Icons.favorite_border, 'Your Likes', () {
            Navigator.push(context, MaterialPageRoute(builder: (_) => const LikedScreen()));
          }),
          _buildListItem(Icons.album_outlined, 'Albums & Playlists', () {
            Navigator.push(context, MaterialPageRoute(builder: (_) => const CustomPlaylistsScreen()));
          }),
          _buildListItem(Icons.person_outline, 'Artist', () {}),
          _buildListItem(Icons.queue_music, 'Add to Playlist', () {
            Navigator.push(context, MaterialPageRoute(builder: (_) => const MyPlaylistsScreen()));
          }),
          _buildListItem(Icons.download_outlined, 'Downloaded', () {
            Navigator.push(context, MaterialPageRoute(builder: (_) => const DownloadedScreen()));
          }),
          _buildListItem(Icons.auto_awesome, 'Recently Played', () {
            Navigator.push(context, MaterialPageRoute(builder: (_) => const RecentlyPlayedPlaylistsScreen()));
          }),
          _buildListItem(Icons.headphones_outlined, 'Listening Library', () {}),
          _buildListItem(Icons.headset_mic_outlined, 'Settings', () {}),
        ],
      ),
    );
  }
}
