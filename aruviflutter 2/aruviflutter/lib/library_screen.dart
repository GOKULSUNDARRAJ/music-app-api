import 'package:flutter/material.dart';
import 'dart:convert';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:cached_network_image/cached_network_image.dart';

import 'liked_screen.dart';
import 'my_playlists_screen.dart';
import 'downloaded_screen.dart';
import 'custom_playlists_screen.dart';
import 'followed_artists_screen.dart';
import 'services/database_service.dart';
import 'playlist_screen.dart';
import 'recently_played_playlists_screen.dart';
import 'models/artist_category.dart';
import 'services/audio_service.dart';
import 'settings_screen.dart';
import 'scanner_screen.dart';
import 'blend_screen.dart';

class LibraryScreen extends StatefulWidget {
  const LibraryScreen({super.key});

  @override
  State<LibraryScreen> createState() => _LibraryScreenState();
}

class _LibraryScreenState extends State<LibraryScreen> {
  List<ArtistCategory> _followedArtists = [];

  @override
  void initState() {
    super.initState();
    _loadArtists();
  }
  
  // Refresh when switching back to this tab if needed
  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    _loadArtists();
  }

  Future<void> _loadArtists() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final likedListStr = prefs.getStringList('local_liked_playlists_data') ?? [];
      
      final List<ArtistCategory> loadedItems = [];
      for (var item in likedListStr) {
        try {
          final decoded = json.decode(item);
          final category = ArtistCategory.fromJson(decoded);
          if (category.adapterType == 2) {
            loadedItems.add(category);
          }
        } catch (e) {
          debugPrint('Error parsing followed artist: $e');
        }
      }

      if (mounted) {
        setState(() {
          _followedArtists = loadedItems.reversed.toList();
        });
      }
    } catch (e) {
      debugPrint('Failed to load local followed artists: $e');
    }
  }

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

  Widget _buildArtistItem(ArtistCategory category) {
    return InkWell(
      onTap: () {
        Navigator.push(
          context,
          MaterialPageRoute(
            builder: (context) => PlaylistScreen(
              categoryId: category.categoryId ?? '',
              imageUrl: category.categoryImage ?? '',
              title: category.categoryName ?? '',
              subtitle: '',
              songs: category.songs,
              isArtist: true,
            ),
          ),
        ).then((_) => _loadArtists());
      },
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 12),
        child: Row(
          children: [
            Container(
              width: 50,
              height: 50,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                color: const Color(0xFF2B2B2B),
                image: DecorationImage(
                  image: CachedNetworkImageProvider(category.categoryImage ?? ''),
                  fit: BoxFit.cover,
                ),
              ),
            ),
            const SizedBox(width: 15),
            Expanded(
              child: AnimatedBuilder(
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
                      fontSize: 16,
                      fontWeight: FontWeight.w500,
                    ),
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                  );
                }
              ),
            ),
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
              child: InkWell(
                onTap: () {
                  Navigator.push(
                    context,
                    MaterialPageRoute(builder: (context) => const ScannerScreen()),
                  );
                },
                borderRadius: BorderRadius.circular(16),
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
          ),
        ],
      ),
      body: ListView(
        padding: const EdgeInsets.only(top: 10, bottom: 20),
        children: [
          _buildListItem(Icons.favorite_border, 'Your Likes', () {
            Navigator.push(context, MaterialPageRoute(builder: (_) => const LikedScreen())).then((_) => _loadArtists());
          }),
          _buildListItem(Icons.album_outlined, 'Albums & Playlists', () {
            Navigator.push(context, MaterialPageRoute(builder: (_) => const CustomPlaylistsScreen())).then((_) => _loadArtists());
          }),
          _buildListItem(Icons.person_outline, 'Artist', () {
            Navigator.push(context, MaterialPageRoute(builder: (_) => const FollowedArtistsScreen())).then((_) => _loadArtists());
          }),
          _buildListItem(Icons.queue_music, 'Add to Playlist', () {
            Navigator.push(context, MaterialPageRoute(builder: (_) => const MyPlaylistsScreen())).then((_) => _loadArtists());
          }),
          _buildListItem(Icons.cloud_download, 'Downloaded Songs', () {
            Navigator.push(context, MaterialPageRoute(builder: (context) => const DownloadedScreen()));
          }),
          _buildListItem(Icons.merge_type, 'My Blends', () {
            Navigator.push(context, MaterialPageRoute(builder: (context) => const BlendScreen()));
          }),
          _buildListItem(Icons.auto_awesome, 'Recently Played', () {
            Navigator.push(context, MaterialPageRoute(builder: (_) => const RecentlyPlayedPlaylistsScreen())).then((_) => _loadArtists());
          }),
          _buildListItem(Icons.headset_mic_outlined, 'Settings', () {
            Navigator.push(context, MaterialPageRoute(builder: (_) => const SettingsScreen()));
          }),
          
          if (_followedArtists.isNotEmpty) ...[
            const Padding(
              padding: EdgeInsets.only(left: 20, top: 25, bottom: 10),
              child: Text(
                'Following',
                style: TextStyle(
                  color: Colors.white,
                  fontSize: 18,
                  fontWeight: FontWeight.bold,
                ),
              ),
            ),
            ..._followedArtists.map((artist) => _buildArtistItem(artist)).toList(),
          ],
        ],
      ),
    );
  }
}
