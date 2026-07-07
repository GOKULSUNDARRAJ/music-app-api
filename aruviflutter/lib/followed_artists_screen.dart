import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;
import 'dart:convert';
import 'package:shared_preferences/shared_preferences.dart';
import 'models/playlist_section.dart';
import 'models/artist_category.dart';
import 'playlist_screen.dart';
import 'services/audio_service.dart';

class FollowedArtistsScreen extends StatefulWidget {
  const FollowedArtistsScreen({super.key});

  @override
  State<FollowedArtistsScreen> createState() => _FollowedArtistsScreenState();
}

class _FollowedArtistsScreenState extends State<FollowedArtistsScreen> {
  bool _isLoading = true;
  List<ArtistCategory> _followedArtists = [];

  @override
  void initState() {
    super.initState();
    _fetchFollowedArtists();
  }

  Future<void> _fetchFollowedArtists() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      // We store both liked playlists and followed artists in the same key
      final likedListStr = prefs.getStringList('local_liked_playlists_data') ?? [];
      
      final List<ArtistCategory> loadedItems = [];
      for (var item in likedListStr) {
        try {
          final decoded = json.decode(item);
          final category = ArtistCategory.fromJson(decoded);
          // Only show artists (adapterType == 2)
          if (category.adapterType == 2) {
            loadedItems.add(category);
          }
        } catch (e) {
          debugPrint('Error parsing followed artist: $e');
        }
      }

      if (mounted) {
        setState(() {
          _followedArtists = loadedItems.reversed.toList(); // Newest first
          _isLoading = false;
        });
      }
    } catch (e) {
      debugPrint('Failed to load local followed artists: $e');
      if (mounted) setState(() => _isLoading = false);
    }
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
              title: category.categoryName ?? '',
              subtitle: '',
              songs: category.songs,
              isArtist: true,
            ),
          ),
        ).then((_) => _fetchFollowedArtists()); // Refresh if they unfollow
      },
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.center,
        children: [
          Expanded(
            child: ClipRRect(
              borderRadius: BorderRadius.circular(100), // Circle for artists
              child: Image.network(
                category.categoryImage ?? '',
                fit: BoxFit.cover,
                width: double.infinity,
                errorBuilder: (context, error, stackTrace) => Container(
                  color: Colors.grey[800],
                  child: const Center(child: Icon(Icons.broken_image, color: Colors.white54)),
                ),
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
        elevation: 0,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back, color: Colors.white),
          onPressed: () => Navigator.pop(context),
        ),
        title: const Text(
          'Followed Artists',
          style: TextStyle(color: Colors.white, fontSize: 18),
        ),
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator(color: Color(0xFFEB1C24)))
          : _followedArtists.isEmpty
              ? const Center(
                  child: Text(
                    'No followed artists yet.',
                    style: TextStyle(color: Colors.white70, fontSize: 16),
                  ),
                )
              : GridView.builder(
                  padding: const EdgeInsets.all(16),
                  gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                    crossAxisCount: 2,
                    mainAxisSpacing: 16,
                    crossAxisSpacing: 16,
                    childAspectRatio: 0.85,
                  ),
                  itemCount: _followedArtists.length,
                  itemBuilder: (context, index) {
                    return _buildGridCardItem(context, _followedArtists[index]);
                  },
                ),
    );
  }
}
