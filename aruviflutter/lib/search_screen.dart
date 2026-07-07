import 'package:flutter/material.dart';
import 'dart:async';
import 'services/database_service.dart';
import 'services/audio_service.dart';
import 'models/audio_model.dart';
import 'models/artist_category.dart';
import 'widgets/safe_network_image.dart';
import 'widgets/song_options_sheet.dart';
import 'playlist_screen.dart';

class SearchScreen extends StatefulWidget {
  const SearchScreen({super.key});

  @override
  State<SearchScreen> createState() => _SearchScreenState();
}

class _SearchScreenState extends State<SearchScreen> {
  final TextEditingController _searchController = TextEditingController();
  Timer? _debounce;

  String _currentQuery = '';
  
  // Pagination State
  int _currentPage = 1;
  bool _isLoading = false;
  bool _hasMore = false;
  
  List<AudioModel> _songs = [];
  List<ArtistCategory> _playlists = [];
  
  final ScrollController _scrollController = ScrollController();

  @override
  void initState() {
    super.initState();
    _scrollController.addListener(_onScroll);
  }

  @override
  void dispose() {
    _searchController.dispose();
    _debounce?.cancel();
    _scrollController.dispose();
    super.dispose();
  }

  void _onScroll() {
    if (_scrollController.position.pixels >= _scrollController.position.maxScrollExtent - 200) {
      if (!_isLoading && _hasMore && _currentQuery.isNotEmpty) {
        _loadMore();
      }
    }
  }

  void _onSearchChanged(String query) {
    if (_debounce?.isActive ?? false) _debounce!.cancel();
    _debounce = Timer(const Duration(milliseconds: 500), () {
      if (query != _currentQuery) {
        setState(() {
          _currentQuery = query;
        });
        if (query.isNotEmpty) {
          _performSearch(query);
        } else {
          setState(() {
            _songs = [];
            _playlists = [];
            _hasMore = false;
          });
        }
      }
    });
  }

  Future<void> _performSearch(String query) async {
    setState(() {
      _isLoading = true;
      _currentPage = 1;
      _songs = [];
      _playlists = [];
    });

    final result = await DatabaseService().searchApi(query, page: _currentPage);

    if (mounted) {
      setState(() {
        _songs = result['songs'] as List<AudioModel>;
        _playlists = result['playlists'] as List<ArtistCategory>;
        _hasMore = result['hasMore'] as bool;
        _isLoading = false;
      });
    }
  }

  Future<void> _loadMore() async {
    setState(() {
      _isLoading = true;
      _currentPage++;
    });

    final result = await DatabaseService().searchApi(_currentQuery, page: _currentPage);

    if (mounted) {
      setState(() {
        _songs.addAll(result['songs'] as List<AudioModel>);
        _playlists.addAll(result['playlists'] as List<ArtistCategory>);
        _hasMore = result['hasMore'] as bool;
        _isLoading = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFF181A20),
      appBar: AppBar(
        backgroundColor: const Color(0xFF181A20),
        elevation: 0,
        title: Container(
          height: 40,
          decoration: BoxDecoration(
            color: Colors.white.withValues(alpha: 0.15),
            borderRadius: BorderRadius.circular(6),
          ),
          child: TextField(
            controller: _searchController,
            style: const TextStyle(color: Colors.white),
            decoration: InputDecoration(
              hintText: "Search songs and playlists",
              hintStyle: TextStyle(color: Colors.white.withValues(alpha: 0.5)),
              border: InputBorder.none,
              prefixIcon: Icon(Icons.search, color: Colors.white.withValues(alpha: 0.7), size: 20),
              suffixIcon: _searchController.text.isNotEmpty
                  ? IconButton(
                      icon: const Icon(Icons.clear, color: Colors.white, size: 20),
                      onPressed: () {
                        _searchController.clear();
                        _onSearchChanged('');
                      },
                    )
                  : null,
            ),
            onChanged: _onSearchChanged,
          ),
        ),
      ),
      body: _buildUnifiedList(),
    );
  }

  Widget _buildUnifiedList() {
    if (_currentQuery.isEmpty) {
      return _buildEmptyState("Search for your favorite songs and playlists");
    }

    if (_isLoading && _currentPage == 1) {
      return const Center(child: CircularProgressIndicator(color: Colors.red));
    }

    final totalItems = _songs.length + _playlists.length;

    if (totalItems == 0) {
      return _buildEmptyState("No results found");
    }

    return ListView.builder(
      controller: _scrollController,
      padding: const EdgeInsets.only(bottom: 100),
      itemCount: totalItems + (_hasMore ? 1 : 0),
      itemBuilder: (context, index) {
        if (index == totalItems) {
          return const Padding(
            padding: EdgeInsets.all(16.0),
            child: Center(child: CircularProgressIndicator(color: Colors.red)),
          );
        }

        if (index < _songs.length) {
          return _buildSongTile(_songs[index], index);
        } else {
          return _buildPlaylistTile(_playlists[index - _songs.length]);
        }
      },
    );
  }

  Widget _buildSongTile(AudioModel song, int index) {
    return ListTile(
      contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
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
        "Song • ${song.categoryName ?? 'Unknown Artist'}",
        style: TextStyle(
          color: Colors.white.withValues(alpha: 0.5),
          fontSize: 14,
        ),
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
            builder: (context) => SongOptionsSheet(song: song),
          );
        },
      ),
      onTap: () async {
        if (song.categoryId == null || song.categoryId!.isEmpty) {
          AudioService().playSongs([song], initialIndex: 0, playlistName: song.categoryName ?? "Search Results");
          return;
        }

        setState(() {
          _isLoading = true;
        });

        // Fetch the full playlist this song belongs to
        final categoryId = song.categoryId!.replaceAll('cat_', '');
        final categorySongs = await DatabaseService().getCategorySongs(categoryId);

        if (mounted) {
          setState(() {
            _isLoading = false;
          });

          if (categorySongs.isNotEmpty) {
            // Find the index of the tapped song in the full playlist
            final initialIndex = categorySongs.indexWhere((s) => s.songId == song.songId);
            AudioService().playSongs(
              categorySongs,
              initialIndex: initialIndex != -1 ? initialIndex : 0,
              playlistName: song.categoryName ?? "Search Results"
            );
          } else {
            // Fallback to playing just the song if playlist is empty or fails
            AudioService().playSongs(
              [song],
              initialIndex: 0,
              playlistName: song.categoryName ?? "Search Results"
            );
          }
        }
      },
    );
  }

  Widget _buildPlaylistTile(ArtistCategory playlist) {
    return ListTile(
      contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      leading: ClipRRect(
        borderRadius: BorderRadius.circular(4),
        child: SizedBox(
          width: 50,
          height: 50,
          child: playlist.categoryImage != null
              ? SafeNetworkImage(
                  url: playlist.categoryImage!,
                  fit: BoxFit.cover,
                )
              : Container(
                  color: const Color(0xFF2B2B2B),
                  child: const Icon(Icons.album, color: Colors.white54),
                ),
        ),
      ),
      title: Text(
        playlist.categoryName ?? 'Unknown',
        style: const TextStyle(
          color: Colors.white,
          fontSize: 16,
          fontWeight: FontWeight.w500,
        ),
        maxLines: 1,
        overflow: TextOverflow.ellipsis,
      ),
      subtitle: Text(
        "Playlist",
        style: TextStyle(
          color: Colors.white.withValues(alpha: 0.5),
          fontSize: 14,
        ),
        maxLines: 1,
        overflow: TextOverflow.ellipsis,
      ),
      onTap: () {
        Navigator.push(
          context,
          MaterialPageRoute(
            builder: (context) => PlaylistScreen(
              title: playlist.categoryName ?? 'Unknown',
              subtitle: 'Playlist',
              categoryId: playlist.categoryId ?? '',
              imageUrl: playlist.categoryImage ?? '',
              songs: playlist.songs,
            ),
          ),
        );
      },
    );
  }

  Widget _buildEmptyState(String message) {
    return Center(
      child: Text(
        message,
        style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 16),
      ),
    );
  }
}
