import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'dart:convert';
import 'models/artist_category.dart';
import 'playlist_screen.dart';
import 'collaborative_playlist_details_screen.dart';

class RecentlyPlayedPlaylistsScreen extends StatefulWidget {
  const RecentlyPlayedPlaylistsScreen({Key? key}) : super(key: key);

  @override
  State<RecentlyPlayedPlaylistsScreen> createState() => _RecentlyPlayedPlaylistsScreenState();
}

class _RecentlyPlayedPlaylistsScreenState extends State<RecentlyPlayedPlaylistsScreen> {
  List<ArtistCategory> _playlists = [];
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _loadPlaylists();
  }

  Future<void> _loadPlaylists() async {
    final prefs = await SharedPreferences.getInstance();
    final cachedData = prefs.getString('offline_recent_playlists');
    
    if (cachedData != null && cachedData.isNotEmpty) {
      try {
        final decoded = json.decode(cachedData) as List;
        setState(() {
          _playlists = decoded.map((c) => ArtistCategory.fromJson(c)).toList();
          _isLoading = false;
        });
      } catch (e) {
        setState(() { _isLoading = false; });
      }
    } else {
      setState(() { _isLoading = false; });
    }
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
          'Recently Played',
          style: TextStyle(
            color: Colors.white,
            fontSize: 20,
            fontWeight: FontWeight.bold,
          ),
        ),
      ),
      body: _isLoading 
          ? const Center(child: CircularProgressIndicator(color: Color(0xFFEB1C24)))
          : _playlists.isEmpty
              ? const Center(
                  child: Text(
                    'No recently played playlists',
                    style: TextStyle(color: Colors.grey, fontSize: 16),
                  ),
                )
              : ListView.builder(
                  padding: const EdgeInsets.symmetric(vertical: 10),
                  itemCount: _playlists.length,
                  itemBuilder: (context, index) {
                    final playlist = _playlists[index];
                    return ListTile(
                      contentPadding: const EdgeInsets.symmetric(horizontal: 20, vertical: 8),
                      leading: Container(
                        width: 50,
                        height: 50,
                        decoration: BoxDecoration(
                          color: const Color(0xFF2B2B2B),
                          borderRadius: BorderRadius.circular(4),
                          image: playlist.categoryImage != null && playlist.categoryImage!.isNotEmpty
                              ? DecorationImage(
                                  image: NetworkImage(playlist.categoryImage!),
                                  fit: BoxFit.cover,
                                )
                              : null,
                        ),
                        child: playlist.categoryImage == null || playlist.categoryImage!.isEmpty
                            ? const Icon(Icons.music_note, color: Colors.grey)
                            : null,
                      ),
                      title: Text(
                        playlist.categoryName ?? 'Unknown',
                        style: const TextStyle(
                          color: Colors.white,
                          fontSize: 16,
                          fontWeight: FontWeight.w500,
                        ),
                      ),
                      subtitle: Text(
                        playlist.songs.isNotEmpty ? '${playlist.songs.length} Songs' : 'Playlist',
                        style: const TextStyle(
                          color: Colors.grey,
                          fontSize: 13,
                        ),
                      ),
                      onTap: () {
                        if (playlist.adapterType == 3 || playlist.adapterType == 4) {
                          Navigator.push(
                            context,
                            MaterialPageRoute(
                              builder: (context) => CollaborativePlaylistDetailsScreen(
                                playlistId: playlist.categoryId ?? '',
                                playlistName: playlist.categoryName ?? 'Unknown',
                                isOwner: playlist.adapterType == 3,
                                inviteCode: '', // Not required for just viewing
                              ),
                            ),
                          ).then((_) => _loadPlaylists());
                        } else {
                          Navigator.push(
                            context,
                            MaterialPageRoute(
                              builder: (context) => PlaylistScreen(
                                title: playlist.categoryName ?? 'Unknown',
                                subtitle: playlist.songs.isNotEmpty ? '${playlist.songs.length} Songs' : 'Playlist',
                                imageUrl: playlist.categoryImage ?? '',
                                categoryId: playlist.categoryId?.toString() ?? '',
                                songs: playlist.songs,
                                isArtist: playlist.adapterType == 2,
                              ),
                            ),
                          ).then((_) => _loadPlaylists());
                        }
                      },
                    );
                  },
                ),
    );
  }
}
