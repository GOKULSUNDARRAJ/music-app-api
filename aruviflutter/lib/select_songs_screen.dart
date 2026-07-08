import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;
import 'dart:convert';
import 'package:shared_preferences/shared_preferences.dart';
import 'models/audio_model.dart';
import 'models/home_response.dart';
import 'services/database_service.dart';

class SelectSongsScreen extends StatefulWidget {
  final String playlistId;
  final String playlistName;

  const SelectSongsScreen({
    super.key,
    required this.playlistId,
    required this.playlistName,
  });

  @override
  State<SelectSongsScreen> createState() => _SelectSongsScreenState();
}

class _SelectSongsScreenState extends State<SelectSongsScreen> {
  bool _isLoading = true;
  String? _errorMessage;
  List<AudioModel> _allSongs = [];
  List<AudioModel> _filteredSongs = [];
  final Set<String> _selectedSongIds = {};
  final TextEditingController _searchController = TextEditingController();

  @override
  void initState() {
    super.initState();
    _fetchAllSongs();
  }

  Future<void> _fetchAllSongs() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final token = prefs.getString('access_token') ?? '';
      final authHeader = token.startsWith('Bearer ') ? token : 'Bearer $token';
      final headers = {'Authorization': authHeader};

      // Fetch all endpoints
      final responses = await Future.wait([
        http.get(Uri.parse('https://music-app-api-1.onrender.com/api/home'), headers: headers),
        http.get(Uri.parse('https://music-app-api-1.onrender.com/api/artist'), headers: headers),
        http.get(Uri.parse('https://music-app-api-1.onrender.com/api/devotional'), headers: headers),
      ]);

      final Map<String, AudioModel> uniqueSongs = {};

      for (var response in responses) {
        if (response.statusCode == 200) {
          final data = json.decode(response.body);
          final homeResponse = HomeResponse.fromJson(data);
          
          for (var section in homeResponse.sections) {
            for (var category in section.categories ?? []) {
              for (var song in category.songs ?? []) {
                if (song.songId != null && song.songId!.isNotEmpty) {
                  uniqueSongs[song.songId!] = song;
                }
              }
            }
          }
        }
      }

      if (mounted) {
        setState(() {
          _allSongs = uniqueSongs.values.toList();
          // Sort alphabetically by name
          _allSongs.sort((a, b) => (a.audioName ?? '').compareTo(b.audioName ?? ''));
          _filteredSongs = List.from(_allSongs);
          _isLoading = false;
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _errorMessage = 'Failed to load songs: $e';
          _isLoading = false;
        });
      }
    }
  }

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  void _onSearchChanged(String query) {
    setState(() {
      if (query.isEmpty) {
        _filteredSongs = List.from(_allSongs);
      } else {
        _filteredSongs = _allSongs.where((song) {
          final name = song.audioName?.toLowerCase() ?? '';
          final category = song.categoryName?.toLowerCase() ?? '';
          final searchLower = query.toLowerCase();
          return name.contains(searchLower) || category.contains(searchLower);
        }).toList();
      }
    });
  }

  void _toggleSelection(String songId) {
    setState(() {
      if (_selectedSongIds.contains(songId)) {
        _selectedSongIds.remove(songId);
      } else {
        _selectedSongIds.add(songId);
      }
    });
  }

  Future<void> _saveSelection() async {
    setState(() {
      _isLoading = true;
    });

    final selectedSongs = _allSongs.where((s) => _selectedSongIds.contains(s.songId)).toList();
    
    if (selectedSongs.isNotEmpty) {
      await DatabaseService().addMultipleSongsToCustomPlaylist(widget.playlistId, selectedSongs);
    }
    
    if (mounted) {
      Navigator.pop(context, true); // Return true to indicate changes were made
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
          icon: const Icon(Icons.close, color: Colors.white),
          onPressed: () => Navigator.pop(context),
        ),
        title: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              'Add Songs',
              style: TextStyle(color: Colors.white, fontSize: 18),
            ),
            Text(
              'to ${widget.playlistName}',
              style: const TextStyle(color: Colors.white54, fontSize: 12),
            ),
          ],
        ),
        actions: [
          TextButton(
            onPressed: _selectedSongIds.isEmpty || _isLoading ? null : _saveSelection,
            child: Text(
              'Done (${_selectedSongIds.length})',
              style: TextStyle(
                color: _selectedSongIds.isEmpty ? Colors.white38 : const Color(0xFFEB1C24),
                fontWeight: FontWeight.bold,
                fontSize: 16
              ),
            ),
          )
        ],
      ),
      body: Column(
        children: [
          Padding(
            padding: const EdgeInsets.all(16.0),
            child: TextField(
              controller: _searchController,
              onChanged: _onSearchChanged,
              style: const TextStyle(color: Colors.white),
              decoration: InputDecoration(
                hintText: 'Search songs to add...',
                hintStyle: const TextStyle(color: Colors.white54),
                prefixIcon: const Icon(Icons.search, color: Colors.white54),
                filled: true,
                fillColor: Colors.grey[900],
                border: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(8),
                  borderSide: BorderSide.none,
                ),
                contentPadding: const EdgeInsets.symmetric(vertical: 0),
              ),
            ),
          ),
          Expanded(
            child: _isLoading
                ? const Center(child: CircularProgressIndicator(color: Color(0xFFEB1C24)))
                : _errorMessage != null
                    ? Center(
                        child: Padding(
                          padding: const EdgeInsets.all(20.0),
                          child: Text(
                            _errorMessage!,
                            style: const TextStyle(color: Colors.white70),
                            textAlign: TextAlign.center,
                          ),
                        ),
                      )
                    : _filteredSongs.isEmpty
                        ? const Center(
                            child: Text(
                              'No songs found.',
                              style: TextStyle(color: Colors.white70, fontSize: 16),
                            ),
                          )
                        : ListView.builder(
                            itemCount: _filteredSongs.length,
                            itemBuilder: (context, index) {
                              final song = _filteredSongs[index];
                              final isSelected = _selectedSongIds.contains(song.songId);
                              
                              return ListTile(
                                visualDensity: const VisualDensity(horizontal: 0, vertical: -4),
                                contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 0),
                                minVerticalPadding: 0,
                                leading: ClipRRect(
                                  borderRadius: BorderRadius.circular(4),
                                  child: song.imageUrl != null && song.imageUrl!.isNotEmpty
                                      ? Image.network(
                                          song.imageUrl!,
                                          width: 50,
                                          height: 50,
                                          fit: BoxFit.cover,
                                          errorBuilder: (c, e, s) => Container(width: 50, height: 50, color: Colors.grey[850], child: const Icon(Icons.music_note, color: Colors.white54)),
                                        )
                                      : Container(width: 50, height: 50, color: Colors.grey[850], child: const Icon(Icons.music_note, color: Colors.white54)),
                                ),
                                title: Text(
                                  song.audioName ?? 'Unknown Song',
                                  style: const TextStyle(color: Colors.white, fontSize: 16),
                                  maxLines: 1,
                                  overflow: TextOverflow.ellipsis,
                                ),
                                subtitle: Text(
                                  song.categoryName ?? '',
                                  style: const TextStyle(color: Colors.white54, fontSize: 13),
                                  maxLines: 1,
                                  overflow: TextOverflow.ellipsis,
                                ),
                                trailing: Theme(
                                  data: ThemeData(
                                    unselectedWidgetColor: Colors.white54,
                                  ),
                                  child: Checkbox(
                                    value: isSelected,
                                    activeColor: const Color(0xFFEB1C24),
                                    checkColor: Colors.white,
                                    onChanged: (bool? value) {
                                      if (song.songId != null) {
                                        _toggleSelection(song.songId!);
                                      }
                                    },
                                  ),
                                ),
                                onTap: () {
                                  if (song.songId != null) {
                                    _toggleSelection(song.songId!);
                                  }
                                },
                              );
                            },
                          ),
          ),
        ],
      ),
    );
  }
}
