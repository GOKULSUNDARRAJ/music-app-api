import 'package:flutter/material.dart';
import 'package:palette_generator/palette_generator.dart';
import 'dart:convert';
import 'package:http/http.dart' as http;
import 'package:shared_preferences/shared_preferences.dart';
import 'package:path_provider/path_provider.dart';
import 'package:cached_network_image/cached_network_image.dart';
import 'dart:io';

import 'models/audio_model.dart';
import 'models/artist_category.dart';
import 'models/playlist_section.dart';
import 'services/audio_service.dart';
import 'services/download_service.dart';
import 'services/database_service.dart';
import 'bottom_sheet_player.dart';
import 'widgets/song_options_sheet.dart';
import 'widgets/add_to_playlist_sheet.dart';
import 'widgets/safe_network_image.dart';
import 'playlist_search_screen.dart';
import 'aruvi_code_generator_sheet.dart';
import 'select_songs_screen.dart';

class PlaylistScreen extends StatefulWidget {
  final String title;
  final String subtitle;
  final String imageUrl;
  final String categoryId;
  final List<AudioModel> songs;
  final bool isLocal;
  final bool isCustomPlaylist;
  final bool isArtist;

  const PlaylistScreen({
    super.key,
    required this.title,
    required this.subtitle,
    required this.imageUrl,
    required this.categoryId,
    required this.songs,
    this.isLocal = false,
    this.isCustomPlaylist = false,
    this.isArtist = false,
  });

  @override
  State<PlaylistScreen> createState() => _PlaylistScreenState();
}

class _PlaylistScreenState extends State<PlaylistScreen> {
  Color _dominantColor = const Color(0xFF181A20); // Default dark background
  bool _isLiked = false;
  bool _isLoadingLike = true;
  bool _isAddedToPlaylist = false;
  bool _isLoadingPlaylistStatus = true;
  bool _showFloatingPlayButton = false;
  bool _isLoadingFollow = false;
  
  late List<AudioModel> _songs;

  final ScrollController _scrollController = ScrollController();
  bool _showTitleInAppBar = false;
  
  // Cache for smooth scrolling
  String? _appDirPath;
  Set<String> _downloadedSongIds = {};

  List<ArtistCategory>? _recommendedPlaylists;

  @override
  void initState() {
    super.initState();
    _songs = List.from(widget.songs);
    _loadCustomOrder();
    _updatePalette();
    _fetchLikeStatus();
    _checkPlaylistStatus();
    _recordRecentPlay();
    _initScrollCache();
    _fetchRecommendedPlaylists();
    
    // Automatically fetch latest songs if this is a blend playlist
    if (widget.categoryId.startsWith('blend_')) {
      _fetchBlendSongs();
    }
    
    _scrollController.addListener(() {
      // Toggle app bar title at 300px
      if (_scrollController.offset > 300 && !_showTitleInAppBar) {
        setState(() => _showTitleInAppBar = true);
      } else if (_scrollController.offset <= 300 && _showTitleInAppBar) {
        setState(() => _showTitleInAppBar = false);
      }
      
      // Toggle floating play button at 380px
      if (_scrollController.offset > 380 && !_showFloatingPlayButton) {
        setState(() => _showFloatingPlayButton = true);
      } else if (_scrollController.offset <= 380 && _showFloatingPlayButton) {
        setState(() => _showFloatingPlayButton = false);
      }
    });
  }

  @override
  void dispose() {
    _scrollController.dispose();
    super.dispose();
  }

  Future<void> _fetchBlendSongs() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final token = prefs.getString('access_token') ?? '';
      if (token.isEmpty) return;

      final blendId = widget.categoryId.replaceAll('blend_', '');
      final url = Uri.parse('https://music-app-api-1.onrender.com/api/user/blend/$blendId');
      
      final response = await http.get(url, headers: {
        'Authorization': 'Bearer $token',
      });

      if (response.statusCode == 200) {
        final data = json.decode(response.body);
        if (data['status'] == true) {
          final List<dynamic> songsData = data['data'] ?? [];
          final latestSongs = songsData.map((s) => AudioModel.fromJson(s)).toList();
          
          if (mounted) {
            setState(() {
              _songs = latestSongs;
            });
            // Update the cached version in Library
            _forceRefreshStorageIfCached();
          }
        }
      }
    } catch (e) {
      debugPrint('Failed to fetch latest blend songs: $e');
    }
  }

  Future<void> _forceRefreshStorageIfCached() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      
      final category = ArtistCategory(
        categoryId: widget.categoryId,
        categoryName: widget.title,
        categoryImage: widget.imageUrl,
        songs: _songs,
        adapterType: (widget.isArtist || widget.subtitle.toLowerCase() == 'artist') ? 2 : 1,
      );
      final categoryJson = json.encode(category.toJson());

      // Check liked
      final likedListStr = prefs.getStringList('local_liked_playlists_data') ?? [];
      final likedIndex = likedListStr.indexWhere((item) {
        try { return json.decode(item)['categoryId'] == widget.categoryId; } catch (_) { return false; }
      });
      if (likedIndex != -1) {
        likedListStr[likedIndex] = categoryJson;
        await prefs.setStringList('local_liked_playlists_data', likedListStr);
      }

      // Check added
      final addedListStr = prefs.getStringList('local_added_playlists_data') ?? [];
      final addedIndex = addedListStr.indexWhere((item) {
        try { return json.decode(item)['categoryId'] == widget.categoryId; } catch (_) { return false; }
      });
      if (addedIndex != -1) {
        addedListStr[addedIndex] = categoryJson;
        await prefs.setStringList('local_added_playlists_data', addedListStr);
      }
    } catch (e) {
      debugPrint('Failed to force refresh storage: $e');
    }
  }

  Future<void> _initScrollCache() async {
    // 1. Cache app directory path
    try {
      final dir = await getApplicationDocumentsDirectory();
      _appDirPath = dir.path;
    } catch (e) {
      debugPrint("Error getting app dir: $e");
    }

    // 2. Cache downloaded statuses efficiently
    final allDownloaded = await DatabaseService().getAllDownloadedSongIds();

    if (mounted) {
      setState(() {
        _downloadedSongIds = allDownloaded;
      });
    }
  }

  Future<void> _fetchRecommendedPlaylists() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final token = prefs.getString('access_token') ?? '';
      final response = await http.get(
        Uri.parse('https://music-app-api-1.onrender.com/api/home'),
        headers: {'Authorization': token.startsWith('Bearer') ? token : 'Bearer $token'},
      );
      if (response.statusCode == 200) {
        final data = json.decode(response.body);
        final sections = (data['sections'] as List).map((s) => PlaylistSection.fromJson(s)).toList();
        
        // 1. Get all unique categories from the API (excluding Recently Played)
        final Map<String, ArtistCategory> allCategories = {};
        for (var section in sections) {
          if (section.sectionTitle?.toLowerCase().contains('recently played') ?? false) continue;
          for (var category in section.categories) {
            if (category.categoryId != null && category.categoryId != widget.categoryId) {
              allCategories[category.categoryId!] = category;
            }
          }
        }

        if (allCategories.isNotEmpty) {
          // 2. Extract keywords from current playlist
          final commonWords = {'the', 'of', 'in', 'and', 'songs', 'playlist', 'hits', 'mix', 'top', 'best', 'new', 'a', 'an'};
          final keywords = widget.title.toLowerCase().split(RegExp(r'\W+'))
              .where((w) => w.length > 2 && !commonWords.contains(w))
              .toSet();
          
          final isArtist = widget.isArtist;

          // 3. Score categories
          final scoredCategories = allCategories.values.map((category) {
            int score = 0;
            final catName = category.categoryName?.toLowerCase() ?? '';
            
            // Match keywords
            for (var kw in keywords) {
              if (catName.contains(kw)) {
                score += 3; // High weight for title keyword match
              }
            }
            
            // Match type (artist vs playlist)
            final catIsArtist = category.adapterType == 2;
            if (catIsArtist == isArtist) {
              score += 1;
            }

            // Small random jitter to shuffle ties
            final randomJitter = (DateTime.now().millisecondsSinceEpoch % 100) / 100.0;
            
            return MapEntry(category, score + randomJitter);
          }).toList();

          // 4. Sort by score descending
          scoredCategories.sort((a, b) => b.value.compareTo(a.value));

          if (mounted) {
            setState(() {
              _recommendedPlaylists = scoredCategories.take(6).map((e) => e.key).toList();
            });
          }
        }
      }
    } catch (e) {
      debugPrint("Error fetching recommended: $e");
    }
  }

  Future<void> _loadCustomOrder() async {
    final prefs = await SharedPreferences.getInstance();
    final customOrder = prefs.getStringList('custom_order_${widget.categoryId}');
    if (customOrder != null && customOrder.isNotEmpty) {
      if (mounted) {
        setState(() {
          _songs.sort((a, b) {
            int indexA = customOrder.indexOf(a.songId ?? '');
            int indexB = customOrder.indexOf(b.songId ?? '');
            // If a song isn't in the saved order, push it to the end
            if (indexA == -1) indexA = 99999;
            if (indexB == -1) indexB = 99999;
            return indexA.compareTo(indexB);
          });
        });
      }
    }
  }

  Future<void> _saveCustomOrder() async {
    final prefs = await SharedPreferences.getInstance();
    final orderList = _songs.map((s) => s.songId ?? '').toList();
    await prefs.setStringList('custom_order_${widget.categoryId}', orderList);
  }

  Future<void> _updatePalette() async {
    if (widget.imageUrl.isEmpty) return;

    try {
      final PaletteGenerator generator = await PaletteGenerator.fromImageProvider(
        CachedNetworkImageProvider(widget.imageUrl),
        maximumColorCount: 10,
      );
      
      if (generator.dominantColor != null && mounted) {
        setState(() {
          // Darken the dominant color slightly for better text contrast
          _dominantColor = _darken(generator.dominantColor!.color, 0.3);
        });
      }
    } catch (e) {
      debugPrint('Failed to extract palette: $e');
    }
  }

  Color _darken(Color c, [double amount = .1]) {
    assert(amount >= 0 && amount <= 1);
    final hsl = HSLColor.fromColor(c);
    final hslDark = hsl.withLightness((hsl.lightness - amount).clamp(0.0, 1.0));
    return hslDark.toColor();
  }

  Future<void> _fetchLikeStatus() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final likedListStr = prefs.getStringList('local_liked_playlists_data') ?? [];
      if (mounted) {
        setState(() {
          _isLiked = likedListStr.any((item) {
            try {
              final decoded = json.decode(item);
              return decoded['categoryId'] == widget.categoryId;
            } catch (e) {
              return false;
            }
          });
          _isLoadingLike = false;
        });
      }
    } catch (e) {
      if (mounted) setState(() => _isLoadingLike = false);
    }
  }

  Future<void> _checkPlaylistStatus() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final addedListStr = prefs.getStringList('local_added_playlists_data') ?? [];
      if (mounted) {
        setState(() {
          _isAddedToPlaylist = addedListStr.any((item) {
            try {
              final decoded = json.decode(item);
              return decoded['categoryId'] == widget.categoryId;
            } catch (e) {
              return false;
            }
          });
          _isLoadingPlaylistStatus = false;
        });
      }
    } catch (e) {
      if (mounted) setState(() => _isLoadingPlaylistStatus = false);
    }
  }

  Future<void> _recordRecentPlay() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final token = prefs.getString('access_token') ?? '';
      if (token.isNotEmpty) {
        final authHeader = token.startsWith('Bearer ') ? token : 'Bearer $token';
        await http.post(
          Uri.parse('https://music-app-api-1.onrender.com/api/user/recordRecentPlay'),
          headers: {
            'Authorization': authHeader,
            'Content-Type': 'application/json'
          },
          body: json.encode({"categoryId": widget.categoryId}),
        );
      }
    } catch (e) {
      debugPrint('Failed to record recent play to API: $e');
    }

    try {
      final prefs = await SharedPreferences.getInstance();
      
      // Save locally
      final category = ArtistCategory(
        categoryId: widget.categoryId,
        categoryName: widget.title,
        categoryImage: widget.imageUrl,
        songs: _songs,
        adapterType: (widget.isArtist || widget.subtitle.toLowerCase() == 'artist') ? 2 : 1,
      );

      final cachedListJson = prefs.getString('offline_recent_playlists');
      List<ArtistCategory> recentList = [];
      if (cachedListJson != null && cachedListJson.isNotEmpty) {
        final decoded = json.decode(cachedListJson) as List;
        recentList = decoded.map((c) => ArtistCategory.fromJson(c)).toList();
      }

      // Remove if already exists
      recentList.removeWhere((c) => c.categoryId == widget.categoryId || c.categoryName == widget.title);

      // Add to front
      recentList.insert(0, category);

      // Keep up to 50 for the full history screen
      if (recentList.length > 50) {
        recentList = recentList.sublist(0, 50);
      }

      await prefs.setString('offline_recent_playlists', json.encode(recentList.map((c) => c.toJson()).toList()));
    } catch (e) {
      debugPrint('Failed to save recent play locally: $e');
    }
  }

  Future<void> _toggleLike() async {
    setState(() {
      _isLiked = !_isLiked; // Optimistic UI update
    });

    try {
      final prefs = await SharedPreferences.getInstance();
      final likedListStr = prefs.getStringList('local_liked_playlists_data') ?? [];
      
      likedListStr.removeWhere((item) {
        try {
          final decoded = json.decode(item);
          return decoded['categoryId'] == widget.categoryId;
        } catch (e) {
          return false;
        }
      });

      if (_isLiked) {
        final category = ArtistCategory(
          categoryId: widget.categoryId,
          categoryName: widget.title,
          categoryImage: widget.imageUrl,
          songs: _songs,
          adapterType: (widget.isArtist || widget.subtitle.toLowerCase() == 'artist') ? 2 : 1,
        );
        likedListStr.add(json.encode(category.toJson()));
      }
      
      await prefs.setStringList('local_liked_playlists_data', likedListStr);
    } catch (e) {
      debugPrint('Failed to toggle like locally: $e');
      if (mounted) {
        setState(() {
          _isLiked = !_isLiked;
        });
      }
    }
  }

  Future<void> _togglePlaylist() async {
    setState(() {
      _isAddedToPlaylist = !_isAddedToPlaylist; // Optimistic UI update
    });

    try {
      final prefs = await SharedPreferences.getInstance();
      final addedListStr = prefs.getStringList('local_added_playlists_data') ?? [];
      
      addedListStr.removeWhere((item) {
        try {
          final decoded = json.decode(item);
          return decoded['categoryId'] == widget.categoryId;
        } catch (e) {
          return false;
        }
      });

      if (_isAddedToPlaylist) {
        final category = ArtistCategory(
          categoryId: widget.categoryId,
          categoryName: widget.title,
          categoryImage: widget.imageUrl,
          songs: _songs,
          adapterType: (widget.isArtist || widget.subtitle.toLowerCase() == 'artist') ? 2 : 1,
        );
        addedListStr.add(json.encode(category.toJson()));
      }
      
      await prefs.setStringList('local_added_playlists_data', addedListStr);
    } catch (e) {
      debugPrint('Failed to toggle playlist status locally: $e');
      if (mounted) {
        setState(() {
          _isAddedToPlaylist = !_isAddedToPlaylist;
        });
      }
    }
  }

  Future<void> _refreshStorageWithNewSongs() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      
      final category = ArtistCategory(
        categoryId: widget.categoryId,
        categoryName: widget.title,
        categoryImage: widget.imageUrl,
        songs: _songs,
        adapterType: (widget.isArtist || widget.subtitle.toLowerCase() == 'artist') ? 2 : 1,
      );
      final categoryJson = json.encode(category.toJson());

      if (_isLiked) {
        final likedListStr = prefs.getStringList('local_liked_playlists_data') ?? [];
        likedListStr.removeWhere((item) {
          try { return json.decode(item)['categoryId'] == widget.categoryId; } catch (_) { return false; }
        });
        likedListStr.add(categoryJson);
        await prefs.setStringList('local_liked_playlists_data', likedListStr);
      }

      if (_isAddedToPlaylist) {
        final addedListStr = prefs.getStringList('local_added_playlists_data') ?? [];
        addedListStr.removeWhere((item) {
          try { return json.decode(item)['categoryId'] == widget.categoryId; } catch (_) { return false; }
        });
        addedListStr.add(categoryJson);
        await prefs.setStringList('local_added_playlists_data', addedListStr);
      }
    } catch (e) {
      debugPrint('Failed to refresh storage: $e');
    }
  }

  Widget _buildIconButton(IconData icon, VoidCallback onTap) {
    return Container(
      margin: const EdgeInsets.only(right: 8),
      decoration: BoxDecoration(
        shape: BoxShape.circle,
        border: Border.all(color: Colors.white70, width: 1),
      ),
      child: IconButton(
        icon: Icon(icon, color: Colors.white, size: 20),
        onPressed: onTap,
        padding: const EdgeInsets.all(8),
        constraints: const BoxConstraints(),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final double statusBarHeight = MediaQuery.of(context).padding.top;
    // Play button straddles the bottom of the pinned app bar
    final double playBtnTop = statusBarHeight + kToolbarHeight - 28;

    return Scaffold(
      backgroundColor: const Color(0xFF121212),
      body: Stack(
        children: [
          CustomScrollView(
        controller: _scrollController,
        slivers: [
          // Collapsing App Bar with Gradient
          SliverAppBar(
            pinned: true,
            elevation: 0,
            backgroundColor: _dominantColor,
            expandedHeight: 320.0,
            leading: IconButton(
              icon: const Icon(Icons.arrow_back, color: Colors.white, size: 28),
              onPressed: () => Navigator.pop(context),
            ),
            title: AnimatedOpacity(
              opacity: _showTitleInAppBar ? 1.0 : 0.0,
              duration: const Duration(milliseconds: 200),
              child: Text(
                widget.title,
                style: const TextStyle(
                  color: Colors.white,
                  fontWeight: FontWeight.bold,
                  fontSize: 18,
                ),
              ),
            ),


            flexibleSpace: FlexibleSpaceBar(
              collapseMode: CollapseMode.pin,
              background: AnimatedBuilder(
                animation: _scrollController,
                builder: (context, child) {
                  const double expandedHeight = 320.0;
                  final double statusBarHeight = MediaQuery.of(context).padding.top;
                  final double collapsedHeight = kToolbarHeight + statusBarHeight;
                  final double offset = _scrollController.hasClients ? _scrollController.offset : 0.0;
                  // progress: 1.0 = fully expanded, 0.0 = fully collapsed
                  final double progress = (1.0 - offset / (expandedHeight - collapsedHeight)).clamp(0.0, 1.0);

                  final double imageSize = 185.0 * progress;
                  final double opacity = (progress * 1.5).clamp(0.0, 1.0);

                  return Container(
                    decoration: BoxDecoration(
                      gradient: LinearGradient(
                        colors: [
                          _dominantColor,
                          _dominantColor.withOpacity(0.5),
                          const Color(0xFF121212),
                        ],
                        begin: Alignment.topCenter,
                        end: Alignment.bottomCenter,
                        stops: const [0.0, 0.5, 1.0],
                      ),
                    ),
                    child: Stack(
                      fit: StackFit.expand,
                      children: [
                        // Search bar
                        Positioned(
                          top: collapsedHeight + 8,
                          left: 20,
                          right: 20,
                          child: Opacity(
                            opacity: (progress * 2).clamp(0.0, 1.0),
                            child: GestureDetector(
                              onTap: () {
                                Navigator.push(
                                  context,
                                  MaterialPageRoute(
                                    builder: (context) => PlaylistSearchScreen(
                                      songs: _songs,
                                      title: widget.title,
                                    ),
                                  ),
                                );
                              },
                              child: Container(
                                height: 40,
                                decoration: BoxDecoration(
                                  color: Colors.white.withOpacity(0.15),
                                  borderRadius: BorderRadius.circular(6),
                                ),
                                child: Row(
                                  children: [
                                    const SizedBox(width: 15),
                                    Icon(Icons.search, color: Colors.white.withOpacity(0.7), size: 20),
                                    const SizedBox(width: 10),
                                    Text(
                                      "Find on this page",
                                      style: TextStyle(
                                        color: Colors.white.withOpacity(0.7),
                                        fontSize: 14,
                                      ),
                                    ),
                                  ],
                                ),
                              ),
                            ),
                          ),
                        ),
                        // Album art — centered, shrinks and fades as bar collapses
                        if (imageSize > 5)
                          Positioned(
                            bottom: 20,
                            left: 0,
                            right: 0,
                            child: Opacity(
                              opacity: opacity,
                              child: Center(
                                child: Container(
                                  width: imageSize,
                                  height: imageSize,
                                  decoration: BoxDecoration(
                                    color: const Color(0xFF2B2B2B),
                                    borderRadius: BorderRadius.circular(12),
                                    boxShadow: [
                                      BoxShadow(
                                        color: Colors.black.withOpacity(0.4),
                                        blurRadius: 12,
                                        offset: const Offset(0, 6),
                                      ),
                                    ],
                                  ),
                                  child: FutureBuilder<String>(
                                    future: () async {
                                      final dir = await getApplicationDocumentsDirectory();
                                      final file = File('${dir.path}/playlist_${widget.categoryId}.jpg');
                                      if (file.existsSync()) return file.path;
                                      return '';
                                    }(),
                                    builder: (context, snapshot) {
                                      final localPath = snapshot.data ?? '';
                                      if (localPath.isNotEmpty) {
                                        return ClipRRect(
                                          borderRadius: BorderRadius.circular(12),
                                          child: Image.file(
                                            File(localPath), 
                                            fit: BoxFit.cover,
                                            errorBuilder: (context, error, stackTrace) => Icon(Icons.movie_creation_outlined, size: imageSize * 0.4, color: Colors.white24),
                                          ),
                                        );
                                      }
                                      if (widget.imageUrl.isNotEmpty) {
                                        return ClipRRect(
                                          borderRadius: BorderRadius.circular(12),
                                          child: CachedNetworkImage(
                                            imageUrl: widget.imageUrl,
                                            fit: BoxFit.cover,
                                            errorWidget: (context, url, error) => Icon(Icons.movie_creation_outlined, size: imageSize * 0.4, color: Colors.white24),
                                          ),
                                        );
                                      }
                                      return Icon(Icons.movie_creation_outlined,
                                          size: imageSize * 0.4,
                                          color: Colors.white24);
                                    }
                                  ),
                                ),
                              ),
                            ),
                          ),
                      ],
                    ),
                  );
                },
              ),
            ),
          ),



          
          // Header Texts and Actions
          SliverToBoxAdapter(
            child: Padding(
              padding: const EdgeInsets.symmetric(horizontal: 20.0, vertical: 10.0),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  AnimatedBuilder(
                    animation: AudioService(),
                    builder: (context, child) {
                      final audioService = AudioService();
                      final isThisPlaylistActive = widget.songs.isNotEmpty &&
                          audioService.currentSong != null &&
                          widget.songs.any((s) => s.audioUrl == audioService.currentSong!.audioUrl);
                          
                      return Text(
                        widget.title,
                        style: TextStyle(
                          color: isThisPlaylistActive ? const Color(0xFFEB1C24) : Colors.white,
                          fontSize: 24,
                          fontWeight: FontWeight.bold,
                        ),
                      );
                    },
                  ),
                  const SizedBox(height: 4),
                  Text(
                    widget.isLocal ? '${_songs.length} Songs' : widget.subtitle,
                    style: TextStyle(
                      color: Colors.white.withOpacity(0.7),
                      fontSize: 14,
                    ),
                  ),
                  const SizedBox(height: 20),
                  // Action row — scrolls naturally under the app bar
                      Row(
                    crossAxisAlignment: CrossAxisAlignment.center,
                    children: [
                      AnimatedBuilder(
                        animation: DownloadService(),
                        builder: (context, child) {
                          final isDownloading = DownloadService().isDownloading(widget.categoryId);
                          if (isDownloading) {
                            return Padding(
                              padding: const EdgeInsets.only(right: 12.0),
                              child: Stack(
                                alignment: Alignment.center,
                                children: [
                                  SizedBox(
                                    width: 38,
                                    height: 38,
                                    child: CircularProgressIndicator(
                                      value: DownloadService().getProgress(widget.categoryId),
                                      color: const Color(0xFFEB1C24),
                                      strokeWidth: 2,
                                    ),
                                  ),
                                  const Icon(Icons.arrow_downward, color: Colors.white, size: 16),
                                ],
                              ),
                            );
                          }
                          
                          // Check if all songs are downloaded
                          return FutureBuilder<bool>(
                            future: () async {
                              if (_songs.isEmpty) return false;
                              for (var song in _songs) {
                                if (song.songId != null) {
                                  final isDownloaded = await DatabaseService().isDownloaded(song.songId!);
                                  if (!isDownloaded) return false;
                                }
                              }
                              return true;
                            }(),
                            builder: (context, snapshot) {
                              final isAllDownloaded = snapshot.data == true;
                              if (isAllDownloaded) {
                                return _buildIconButton(Icons.download_done, () {});
                              }
                              return _buildIconButton(Icons.download_outlined, () {
                                DownloadService().downloadPlaylist(widget.categoryId, widget.title, _songs);
                              });
                            }
                          );
                        }
                      ),

                      _buildIconButton(Icons.reply, () {}),
                      if (widget.subtitle.toLowerCase() != 'artist' && !widget.isArtist) ...[
                        _buildIconButton(
                          _isLoadingPlaylistStatus ? Icons.add : (_isAddedToPlaylist ? Icons.check : Icons.add),
                          _isLoadingPlaylistStatus ? () {} : _togglePlaylist,
                        ),
                      ],
                      const SizedBox(width: 5),
                      IconButton(
                        padding: EdgeInsets.zero,
                        constraints: const BoxConstraints(),
                        icon: const Icon(Icons.more_vert, color: Colors.white, size: 24),
                        onPressed: () {
                          showModalBottomSheet(
                            context: context,
                            backgroundColor: Colors.transparent,
                            isScrollControlled: true,
                            useRootNavigator: true,
                            builder: (context) => AruviCodeGeneratorSheet(
                              categoryId: widget.categoryId,
                              title: widget.title,
                              imageUrl: widget.imageUrl,
                            ),
                          );
                        },
                      ),
                      const Spacer(),
                        if (widget.subtitle.toLowerCase() == 'artist' || widget.isArtist)
                          GestureDetector(
                            onTap: _isLoadingLike ? null : _toggleLike,
                            child: Container(
                              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 6),
                              decoration: BoxDecoration(
                                color: _isLiked ? Colors.transparent : Colors.white,
                                border: Border.all(color: Colors.white),
                                borderRadius: BorderRadius.circular(20),
                              ),
                              child: Text(
                                _isLiked ? 'Following' : 'Follow',
                                style: TextStyle(
                                  color: _isLiked ? Colors.white : Colors.black,
                                  fontWeight: FontWeight.bold,
                                  fontSize: 13,
                                ),
                              ),
                            ),
                          )
                        else
                          IconButton(
                            icon: Icon(
                              _isLoadingLike ? Icons.favorite_border : (_isLiked ? Icons.favorite : Icons.favorite_border),
                              color: _isLoadingLike ? Colors.white54 : (_isLiked ? const Color(0xFFEB1C24) : Colors.white),
                              size: 28,
                            ),
                            onPressed: _isLoadingLike ? null : _toggleLike,
                          ),
                      const SizedBox(width: 4),
                      // Big Red Play Button in row — fades out when floating one appears
                      AnimatedOpacity(
                        opacity: _showFloatingPlayButton ? 0.0 : 1.0,
                        duration: const Duration(milliseconds: 200),
                        child: IgnorePointer(
                          ignoring: _showFloatingPlayButton,
                          child: AnimatedBuilder(
                            animation: AudioService(),
                            builder: (context, child) {
                              final audioService = AudioService();
                              final isThisPlaylistActive = _songs.isNotEmpty &&
                                  audioService.currentSong != null &&
                                  _songs.any((s) => s.audioUrl == audioService.currentSong!.audioUrl);
                              final isPlaying = isThisPlaylistActive && audioService.isPlaying;

                              return Container(
                                width: 55,
                                height: 55,
                                decoration: const BoxDecoration(
                                  color: Color(0xFFEB1C24),
                                  shape: BoxShape.circle,
                                ),
                                child: IconButton(
                                  icon: Icon(isPlaying ? Icons.pause : Icons.play_arrow, color: Colors.white, size: 30),
                                  onPressed: () {
                                      if (_songs.isNotEmpty) {
                                        if (isThisPlaylistActive) {
                                          audioService.togglePlayPause();
                                        } else {
                                          audioService.playSongs(_songs);
                                        }
                                      }
                                  },
                                ),
                              );
                            },
                          ),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 20),
                ],
              ),
            ),
          ),

          // Songs List
          if (widget.isCustomPlaylist && _songs.isEmpty)
            SliverFillRemaining(
              hasScrollBody: false,
              child: Padding(
                padding: const EdgeInsets.only(top: 40.0, bottom: 40.0),
                child: Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    const Icon(Icons.music_note, color: Colors.white24, size: 64),
                    const SizedBox(height: 16),
                    const Text(
                      "It's a bit empty here...",
                      style: TextStyle(color: Colors.white, fontSize: 18, fontWeight: FontWeight.bold),
                    ),
                    const SizedBox(height: 8),
                    const Text(
                      "Let's find some songs for your playlist",
                      style: TextStyle(color: Colors.white54, fontSize: 14),
                    ),
                    const SizedBox(height: 24),
                    ElevatedButton(
                      style: ElevatedButton.styleFrom(
                        backgroundColor: Colors.white,
                        foregroundColor: Colors.black,
                        padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 12),
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(24),
                        ),
                      ),
                      onPressed: () async {
                        final result = await Navigator.push(
                          context,
                          MaterialPageRoute(
                            builder: (context) => SelectSongsScreen(
                              playlistId: widget.categoryId,
                              playlistName: widget.title,
                            ),
                          ),
                        );
                        if (result == true) {
                          final updatedSongs = await DatabaseService().getCustomPlaylistSongs(widget.categoryId);
                          if (mounted) {
                            setState(() {
                              _songs = updatedSongs;
                            });
                            _refreshStorageWithNewSongs();
                          }
                        }
                      },
                      child: const Text('Add Songs', style: TextStyle(fontWeight: FontWeight.bold)),
                    ),
                  ],
                ),
              ),
            )
          else
            SliverReorderableList(
              itemBuilder: (context, index) {
              final song = _songs[index];
              return ReorderableDelayedDragStartListener(
                key: ValueKey(song.songId ?? index.toString()),
                index: index,
                child: ListTile(
                  contentPadding: const EdgeInsets.symmetric(horizontal: 20, vertical: 4),
                  leading: Container(
                        width: 50,
                        height: 50,
                        decoration: BoxDecoration(
                          color: const Color(0xFF2A2A2A),
                          borderRadius: BorderRadius.circular(8),
                        ),
                        child: Builder(
                          builder: (context) {
                            String localPath = '';
                            if (_appDirPath != null) {
                              if (song.songId != null) {
                                final songFile = File('$_appDirPath/song_${song.songId}.jpg');
                                if (songFile.existsSync()) localPath = songFile.path;
                              }
                              if (localPath.isEmpty) {
                                final playlistFile = File('$_appDirPath/playlist_${widget.categoryId}.jpg');
                                if (playlistFile.existsSync()) localPath = playlistFile.path;
                              }
                            }
                            
                            if (localPath.isNotEmpty) {
                              return ClipRRect(
                                borderRadius: BorderRadius.circular(8),
                                child: Image.file(
                                  File(localPath), 
                                  fit: BoxFit.cover,
                                  errorBuilder: (context, error, stackTrace) => const Icon(Icons.movie_creation_outlined, color: Colors.white24),
                                ),
                              );
                            }
                            final imgUrl = song.imageUrl ?? '';
                            if (imgUrl.isNotEmpty) {
                              return ClipRRect(
                                borderRadius: BorderRadius.circular(8),
                                child: SafeNetworkImage(
                                  url: imgUrl,
                                  fit: BoxFit.cover,
                                  memCacheWidth: 150,
                                ),
                              );
                            }
                            return const Icon(Icons.movie_creation_outlined, color: Colors.white24);
                          }
                        ),
                      ),
                      title: AnimatedBuilder(
                    animation: AudioService(),
                    builder: (context, child) {
                      final isPlaying = AudioService().currentSong?.audioUrl == song.audioUrl;
                      return Text(
                        song.audioName ?? 'Unknown Song',
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: TextStyle(
                          color: isPlaying ? const Color(0xFFEB1C24) : Colors.white,
                          fontSize: 15,
                          fontWeight: FontWeight.w500,
                        ),
                      );
                    }
                  ),
                  subtitle: Padding(
                        padding: const EdgeInsets.only(top: 4.0),
                        child: Text(
                          song.categoryName ?? 'Unknown Artist',
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          style: TextStyle(
                            color: Colors.white.withOpacity(0.6),
                            fontSize: 13,
                          ),
                        ),
                      ),
                      trailing: Row(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          if (song.songId != null && _downloadedSongIds.contains(song.songId))
                            const Padding(
                              padding: EdgeInsets.only(right: 8.0),
                              child: Icon(Icons.download_done, color: Color(0xFFEB1C24), size: 18),
                            ),
                          IconButton(
                            icon: const Icon(Icons.more_vert, color: Colors.white54, size: 20),
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
                    AudioService().playSongs(_songs, initialIndex: index, playlistName: widget.title);
                  },
                ),
              );
            },
            itemCount: _songs.length,
            onReorder: (int oldIndex, int newIndex) {
              setState(() {
                if (oldIndex < newIndex) {
                  newIndex -= 1;
                }
                final AudioModel item = _songs.removeAt(oldIndex);
                _songs.insert(newIndex, item);
                
                // Save this new order to SharedPreferences
                _saveCustomOrder();
              });
            },
          ),
          
          // Bottom padding / Recommended
          SliverToBoxAdapter(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                if (widget.isCustomPlaylist && _songs.isNotEmpty)
                  Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 20.0, vertical: 20.0),
                    child: InkWell(
                      onTap: () async {
                        final result = await Navigator.push(
                          context,
                          MaterialPageRoute(
                            builder: (context) => SelectSongsScreen(
                              playlistId: widget.categoryId,
                              playlistName: widget.title,
                            ),
                          ),
                        );
                        if (result == true) {
                          final updatedSongs = await DatabaseService().getCustomPlaylistSongs(widget.categoryId);
                          if (mounted) {
                            setState(() {
                              _songs = updatedSongs;
                            });
                            _refreshStorageWithNewSongs();
                          }
                        }
                      },
                      borderRadius: BorderRadius.circular(8),
                      child: Container(
                        padding: const EdgeInsets.symmetric(vertical: 14),
                        decoration: BoxDecoration(
                          border: Border.all(color: Colors.white24),
                          borderRadius: BorderRadius.circular(8),
                        ),
                        child: const Row(
                          mainAxisAlignment: MainAxisAlignment.center,
                          children: [
                            Icon(Icons.add, color: Colors.white, size: 20),
                            SizedBox(width: 8),
                            Text(
                              'Add songs',
                              style: TextStyle(
                                color: Colors.white,
                                fontSize: 16,
                                fontWeight: FontWeight.bold,
                              ),
                            ),
                          ],
                        ),
                      ),
                    ),
                  ),
                if (_recommendedPlaylists != null && _recommendedPlaylists!.isNotEmpty) ...[
                  const Padding(
                    padding: EdgeInsets.fromLTRB(20, 20, 20, 10),
                    child: Text(
                      'More like this',
                      style: TextStyle(color: Colors.white, fontSize: 18, fontWeight: FontWeight.bold),
                    ),
                  ),
                  SizedBox(
                    height: 180,
                    child: ListView.builder(
                      padding: const EdgeInsets.symmetric(horizontal: 20),
                      scrollDirection: Axis.horizontal,
                      itemCount: _recommendedPlaylists!.length,
                      itemBuilder: (context, index) {
                        final category = _recommendedPlaylists![index];
                        return GestureDetector(
                          onTap: () {
                            Navigator.pushReplacement(
                              context,
                              MaterialPageRoute(
                                builder: (context) => PlaylistScreen(
                                  title: category.categoryName ?? 'Unknown',
                                  subtitle: 'Playlist',
                                  imageUrl: category.categoryImage ?? '',
                                  categoryId: category.categoryId ?? '',
                                  songs: category.songs,
                                ),
                              ),
                            );
                          },
                          child: Container(
                            width: 135,
                            margin: const EdgeInsets.only(right: 15),
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                ClipRRect(
                                  borderRadius: BorderRadius.circular(12),
                                  child: SizedBox(
                                    width: 135,
                                    height: 135,
                                    child: category.categoryImage != null && category.categoryImage!.isNotEmpty
                                        ? SafeNetworkImage(
                                            url: category.categoryImage!,
                                            fit: BoxFit.cover,
                                            memCacheWidth: 200,
                                          )
                                        : Container(color: const Color(0xFF2B2B2B)),
                                  ),
                                ),
                                const SizedBox(height: 8),
                                Text(
                                  category.categoryName ?? 'Unknown',
                                  maxLines: 1,
                                  overflow: TextOverflow.ellipsis,
                                  style: const TextStyle(
                                    color: Colors.white,
                                    fontSize: 14,
                                    fontWeight: FontWeight.w500,
                                  ),
                                ),
                              ],
                            ),
                          ),
                        );
                      },
                    ),
                  ),
                ],
                const SizedBox(height: 100),
              ],
            ),
          ),
        ],
      ), // END CustomScrollView

          // Floating play button — straddles the bottom of the pinned app bar
          // Positioned must be a direct child of Stack
          Positioned(
            top: playBtnTop,
            right: 16,
            child: AnimatedOpacity(
              opacity: _showFloatingPlayButton ? 1.0 : 0.0,
              duration: const Duration(milliseconds: 200),
              child: IgnorePointer(
                ignoring: !_showFloatingPlayButton,
                child: AnimatedBuilder(
                  animation: AudioService(),
                  builder: (context, child) {
                    final audioService = AudioService();
                    final isThisPlaylistActive = _songs.isNotEmpty &&
                        audioService.currentSong != null &&
                        _songs.any((s) => s.audioUrl == audioService.currentSong!.audioUrl);
                    final isPlaying = isThisPlaylistActive && audioService.isPlaying;

                    return Container(
                      width: 56,
                      height: 56,
                      decoration: BoxDecoration(
                        color: const Color(0xFFEB1C24),
                        shape: BoxShape.circle,
                        boxShadow: [
                          BoxShadow(
                            color: Colors.black.withOpacity(0.4),
                            blurRadius: 12,
                            offset: const Offset(0, 4),
                          ),
                        ],
                      ),
                      child: IconButton(
                        icon: Icon(isPlaying ? Icons.pause : Icons.play_arrow, color: Colors.white, size: 32),
                        onPressed: () {
                          if (_songs.isNotEmpty) {
                            if (isThisPlaylistActive) {
                              audioService.togglePlayPause();
                            } else {
                              audioService.playSongs(_songs);
                            }
                          }
                        },
                      ),
                    );
                  }
                ),
              ),
            ),
          ),
        ],
      ),
    );


  }
}
