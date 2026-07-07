import 'package:flutter/material.dart';
import 'models/audio_model.dart';
import 'services/audio_service.dart';
import 'widgets/song_options_sheet.dart';
import 'widgets/safe_network_image.dart';
import 'dart:io';

class PlaylistSearchScreen extends StatefulWidget {
  final List<AudioModel> songs;
  final String title;

  const PlaylistSearchScreen({
    super.key,
    required this.songs,
    required this.title,
  });

  @override
  State<PlaylistSearchScreen> createState() => _PlaylistSearchScreenState();
}

class _PlaylistSearchScreenState extends State<PlaylistSearchScreen> {
  final TextEditingController _searchController = TextEditingController();
  List<AudioModel> _filteredSongs = [];

  @override
  void initState() {
    super.initState();
    _filteredSongs = widget.songs;
  }

  void _filterSongs(String query) {
    setState(() {
      if (query.isEmpty) {
        _filteredSongs = widget.songs;
      } else {
        _filteredSongs = widget.songs.where((song) {
          final titleMatch = song.audioName?.toLowerCase().contains(query.toLowerCase()) ?? false;
          final artistMatch = song.categoryName?.toLowerCase().contains(query.toLowerCase()) ?? false;
          return titleMatch || artistMatch;
        }).toList();
      }
    });
  }

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
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
        title: TextField(
          controller: _searchController,
          autofocus: true,
          style: const TextStyle(color: Colors.white),
          decoration: InputDecoration(
            hintText: "Find in ${widget.title}",
            hintStyle: TextStyle(color: Colors.white.withOpacity(0.5)),
            border: InputBorder.none,
          ),
          onChanged: _filterSongs,
        ),
        actions: [
          if (_searchController.text.isNotEmpty)
            IconButton(
              icon: const Icon(Icons.clear, color: Colors.white),
              onPressed: () {
                _searchController.clear();
                _filterSongs('');
              },
            ),
        ],
      ),
      body: _filteredSongs.isEmpty
          ? Center(
              child: Text(
                'No songs found',
                style: TextStyle(color: Colors.white.withOpacity(0.5), fontSize: 16),
              ),
            )
          : ListView.builder(
              itemCount: _filteredSongs.length,
              itemBuilder: (context, index) {
                final song = _filteredSongs[index];
                
                // Find original index to play correctly in context of full playlist
                final originalIndex = widget.songs.indexOf(song);

                return ListTile(
                  contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
                  leading: ClipRRect(
                    borderRadius: BorderRadius.circular(4),
                    child: SizedBox(
                      width: 50,
                      height: 50,
                      child: song.imageUrl != null
                          ? SafeNetworkImage(
                              url: song.imageUrl!,
                              fit: BoxFit.cover,
                            )
                          : Container(
                              color: const Color(0xFF2B2B2B),
                              child: const Icon(Icons.music_note, color: Colors.white54),
                            ),
                    ),
                  ),
                  title: Text(
                    song.audioName ?? 'Unknown Title',
                    style: const TextStyle(
                      color: Colors.white,
                      fontSize: 16,
                      fontWeight: FontWeight.w500,
                    ),
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                  ),
                  subtitle: Text(
                    song.categoryName ?? 'Unknown Artist',
                    style: TextStyle(
                      color: Colors.white.withOpacity(0.5),
                      fontSize: 14,
                    ),
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                  ),
                  trailing: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      if (song.isDownloaded)
                        const Padding(
                          padding: EdgeInsets.only(right: 12),
                          child: Icon(Icons.check_circle_outline, color: Colors.red, size: 20),
                        ),
                      IconButton(
                        icon: const Icon(Icons.more_vert, color: Colors.white54),
                        onPressed: () {
                          showModalBottomSheet(
                            context: context,
                            backgroundColor: Colors.transparent,
                            isScrollControlled: true,
                            builder: (context) => SongOptionsSheet(song: song),
                          );
                        },
                      ),
                    ],
                  ),
                  onTap: () {
                    // Play the original playlist starting from the searched song's index
                    AudioService().playSongs(widget.songs, initialIndex: originalIndex, playlistName: widget.title);
                  },
                );
              },
            ),
    );
  }
}
