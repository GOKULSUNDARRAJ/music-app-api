import 'package:flutter/material.dart';
import 'package:connectivity_plus/connectivity_plus.dart';
import 'package:http/http.dart' as http;
import 'package:shared_preferences/shared_preferences.dart';
import 'dart:convert';
import 'dart:async';
import 'package:flutter/services.dart';
import 'package:url_launcher/url_launcher.dart';
import 'custom_drawer.dart';
import 'mini_player.dart';
import 'bottom_sheet_player.dart';
import 'services/audio_service.dart';
import 'widgets/add_to_playlist_sheet.dart';
import 'home_screen.dart';
import 'search_screen.dart';
import 'library_screen.dart';
import 'podcast_screen.dart';
import 'widgets/create_playlist_dialog.dart';
import 'select_songs_screen.dart';
import 'custom_playlists_screen.dart';
import 'services/database_service.dart';
import 'blend_screen.dart';

class MainActivity extends StatefulWidget {
  const MainActivity({super.key});

  @override
  State<MainActivity> createState() => _MainActivityState();
}

class _MainActivityState extends State<MainActivity> {
  final GlobalKey<ScaffoldState> _scaffoldKey = GlobalKey<ScaffoldState>();
  
  // Navigation State
  int _currentIndex = 0;
  List<dynamic> _bottomNavItems = [
    {"bottommenuName": "Home"},
    {"bottommenuName": "Search"},
    {"bottommenuName": "Library"}
  ];
  bool _isLoadingNav = false;
  final List<GlobalKey<NavigatorState>> _navigatorKeys = [
    GlobalKey<NavigatorState>(),
    GlobalKey<NavigatorState>(),
    GlobalKey<NavigatorState>(),
    GlobalKey<NavigatorState>()
  ];

  // Connectivity State
  bool _isOffline = false;
  late StreamSubscription<List<ConnectivityResult>> _connectivitySubscription;

  // Player State
  bool _showMiniPlayer = true; // Set to true to show it by default for UI testing

  @override
  void initState() {
    super.initState();
    _initConnectivity();
    _fetchNavigationMenu();
  }

  void _initConnectivity() {
    _connectivitySubscription = Connectivity().onConnectivityChanged.listen((List<ConnectivityResult> results) {
      bool isOffline = results.contains(ConnectivityResult.none) || results.isEmpty;
      if (_isOffline != isOffline) {
        setState(() {
          _isOffline = isOffline;
        });
      }
    });
  }

  Future<void> _fetchNavigationMenu() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final bottomNavJson = prefs.getString('bottom_navigation');

      if (bottomNavJson != null && bottomNavJson.isNotEmpty) {
        final List<dynamic> parsed = json.decode(bottomNavJson);
        setState(() {
          _bottomNavItems = parsed;
          _navigatorKeys.clear();
          for (int i = 0; i < parsed.length; i++) {
            _navigatorKeys.add(GlobalKey<NavigatorState>());
          }
          _isLoadingNav = false;
        });
        return;
      }

      // If cache is empty, fetch from API
      final token = prefs.getString('access_token') ?? '';
      if (token.isNotEmpty) {
        final authHeader = token.startsWith('Bearer ') ? token : 'Bearer $token';
        final response = await http.post(
          Uri.parse('https://music-app-api-1.onrender.com/api/secure/appMenuList'),
          headers: {'Authorization': authHeader},
        ).timeout(const Duration(seconds: 5));

        if (response.statusCode == 200) {
          final data = json.decode(response.body);
          if (data['status'] == true) {
            final topMenuJson = json.encode(data['topMenu']);
            final bottomMenuJson = json.encode(data['bottomMenu']);
            await prefs.setString('top_navigation', topMenuJson);
            await prefs.setString('bottom_navigation', bottomMenuJson);
            
            final List<dynamic> parsed = data['bottomMenu'];
            setState(() {
              _bottomNavItems = parsed;
              _navigatorKeys.clear();
              for (int i = 0; i < parsed.length; i++) {
                _navigatorKeys.add(GlobalKey<NavigatorState>());
              }
              _isLoadingNav = false;
            });
            return;
          }
        }
      }
    } catch (e) {
      debugPrint('Error loading bottom navigation: $e');
    }

    // Fallback if no cached data and API fails
    setState(() {
      _bottomNavItems = [
        {"bottommenuName": "Home"},
        {"bottommenuName": "Search"},
        {"bottommenuName": "Library"}
      ];
      _navigatorKeys.clear();
      for (int i = 0; i < _bottomNavItems.length; i++) {
        _navigatorKeys.add(GlobalKey<NavigatorState>());
      }
      _isLoadingNav = false;
    });
  }

  @override
  void dispose() {
    _connectivitySubscription.cancel();
    super.dispose();
  }

  Widget _buildNestedNavigator(int index) {
    if (index >= _navigatorKeys.length) return const SizedBox.shrink();
    
    String currentTabName = _bottomNavItems[index]['bottommenuName'].toString().toLowerCase().trim();

    return Navigator(
      key: _navigatorKeys[index],
      onGenerateRoute: (routeSettings) {
        return MaterialPageRoute(
          builder: (context) {
            switch (currentTabName) {
              case 'home': return const HomeScreen();
              case 'search': return const SearchScreen();
              case 'library': return const LibraryScreen();
              case 'podcast':
              case 'potcast': return const PodcastScreen();
              default: return const HomeScreen();
            }
          },
        );
      },
    );
  }

  IconData _getIconForTab(String name) {
    switch (name.toLowerCase().trim()) {
      case 'home': return Icons.home;
      case 'search': return Icons.search;
      case 'library': return Icons.library_music;
      case 'podcast':
      case 'potcast': return Icons.podcasts;
      case 'create': return Icons.add_circle_outline;
      default: return Icons.circle;
    }
  }

  void _showCreateBottomSheet(BuildContext parentContext) {
    showModalBottomSheet(
      context: parentContext,
      backgroundColor: Colors.transparent,
      isScrollControlled: true,
      useRootNavigator: true,
      builder: (BuildContext sheetContext) {
        return AnimatedBuilder(
          animation: AudioService(),
          builder: (context, child) {
            final bool hasMiniPlayer = AudioService().currentSong != null;
            // Standard bottom nav is ~60px, mini player is ~70px.
            // If mini player is active, we add extra padding.
            final double bottomPadding = hasMiniPlayer ? 130.0 : 64.0;
            
            // Calculate exactly where the 'create' tab is (4th out of 4 tabs)
            final double screenWidth = MediaQuery.of(context).size.width;
            final double cancelRightPadding = (screenWidth / 8) - 18; // 18 is half the button width

            return SafeArea(
              child: Stack(
                clipBehavior: Clip.none,
                children: [
                  // The Menu, with dynamic bottom padding
                  Padding(
                    padding: EdgeInsets.only(left: 12, right: 12, bottom: bottomPadding),
                    child: Container(
                      decoration: BoxDecoration(
                        color: const Color(0xFF242424),
                        borderRadius: BorderRadius.circular(16),
                      ),
                      child: Column(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          const SizedBox(height: 12),
                          ListTile(
                            leading: CircleAvatar(
                              backgroundColor: Colors.white.withOpacity(0.1),
                              radius: 24,
                              child: const Icon(Icons.music_note, color: Colors.white70, size: 26),
                            ),
                            title: const Text('Playlist', style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 16)),
                            subtitle: const Text('Create a playlist with songs', style: TextStyle(color: Colors.white70, fontSize: 13)),
                            onTap: () async {
                              Navigator.pop(sheetContext);
                              final name = await showDialog<String>(
                                context: parentContext,
                                builder: (context) => const CreatePlaylistDialog(),
                              );
                              if (name != null && name.isNotEmpty) {
                                final playlistId = await DatabaseService().createCustomPlaylist(name);
                                if (parentContext.mounted) {
                                  _navigatorKeys[_currentIndex].currentState?.push(
                                    MaterialPageRoute(
                                      builder: (context) => SelectSongsScreen(
                                        playlistId: playlistId,
                                        playlistName: name,
                                      ),
                                    ),
                                  );
                                }
                              }
                            },
                          ),
                          ListTile(
                            leading: CircleAvatar(
                              backgroundColor: Colors.white.withOpacity(0.1),
                              radius: 24,
                              child: const Icon(Icons.queue_music, color: Colors.white70, size: 26),
                            ),
                            title: const Text('View your playlists', style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 16)),
                            subtitle: const Text('See all your created playlists', style: TextStyle(color: Colors.white70, fontSize: 13)),
                            onTap: () {
                              Navigator.pop(sheetContext);
                              _navigatorKeys[_currentIndex].currentState?.push(
                                MaterialPageRoute(
                                  builder: (context) => const CustomPlaylistsScreen(),
                                ),
                              );
                            },
                          ),
                          ListTile(
                            leading: CircleAvatar(
                              backgroundColor: Colors.white.withOpacity(0.1),
                              radius: 24,
                              child: const Icon(Icons.merge_type, color: Colors.white70, size: 26),
                            ),
                            title: const Text('Blend', style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 16)),
                            subtitle: const Text('Combine your friends\' tastes into a playlist', style: TextStyle(color: Colors.white70, fontSize: 13)),
                            onTap: () {
                              Navigator.pop(sheetContext);
                              _navigatorKeys[_currentIndex].currentState?.push(
                                MaterialPageRoute(
                                  builder: (context) => const BlendScreen(),
                                ),
                              );
                            },
                          ),
                          const SizedBox(height: 12),
                        ],
                      ),
                    ),
                  ),
                  
                  // The Cancel Button, pinned perfectly over the Create tab
                  Positioned(
                    bottom: 12, // Moved slightly up so it's perfectly centered on the nav bar
                    right: cancelRightPadding,
                    child: GestureDetector(
                      onTap: () => Navigator.pop(sheetContext),
                      child: Container(
                        width: 36,
                        height: 36,
                        decoration: const BoxDecoration(
                          color: Colors.white,
                          shape: BoxShape.circle,
                        ),
                        child: const Icon(Icons.close, color: Colors.black, size: 16),
                      ),
                    ),
                  ),
                ],
              ),
            );
          },
        );
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    final isKeyboardOpen = MediaQuery.of(context).viewInsets.bottom > 0;

    return PopScope(
      canPop: false,
      onPopInvokedWithResult: (didPop, result) {
        if (didPop) return;
        if (_navigatorKeys.isEmpty) {
          SystemNavigator.pop();
          return;
        }
        
        final NavigatorState? currentNavigator = _navigatorKeys[_currentIndex].currentState;
        if (currentNavigator != null && currentNavigator.canPop()) {
          currentNavigator.pop();
        } else {
          SystemNavigator.pop();
        }
      },
      child: AnimatedBuilder(
        animation: AudioService(),
        builder: (context, child) {
          return Stack(
            children: [
              Scaffold(
                key: _scaffoldKey,
        backgroundColor: const Color(0xFF181A20), // darkGray
        drawer: const CustomDrawer(),
      body: Stack(
        children: [
          // 1. Fragment Container (takes full height)
          Column(
            children: [
              Expanded(
                child: _isLoadingNav 
                    ? const Center(child: CircularProgressIndicator(color: Colors.red))
                    : IndexedStack(
                        index: _currentIndex,
                        children: List.generate(
                          _bottomNavItems.length,
                          (index) => _buildNestedNavigator(index),
                        ),
                      ),
              ),
            ],
          ),
          
          // 2. Floating Overlay (Offline Banner + Mini Player)
          Positioned(
            left: 0,
            right: 0,
            bottom: 0,
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                // Offline Banner
                AnimatedSize(
                  duration: const Duration(milliseconds: 300),
                  curve: Curves.easeInOut,
                  child: _isOffline 
                    ? Container(
                        margin: const EdgeInsets.only(left: 10, right: 10, bottom: 8, top: 4),
                        padding: const EdgeInsets.symmetric(vertical: 8),
                        width: double.infinity,
                        child: const Text(
                          'Aruvi is set to Offline',
                          textAlign: TextAlign.center,
                          style: TextStyle(
                            color: Color(0xFFEB1C24),
                            fontSize: 13,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                      )
                    : const SizedBox.shrink(),
                ),
                
                // Mini Player
                AnimatedBuilder(
                  animation: AudioService(),
                  builder: (context, child) {
                    final audioService = AudioService();
                    if (audioService.currentSong == null || isKeyboardOpen) return const SizedBox.shrink();

                    double progress = 0.0;
                    if (audioService.duration.inMilliseconds > 0) {
                      progress = audioService.position.inMilliseconds / audioService.duration.inMilliseconds;
                    }

                    return MiniPlayer(
                      songTitle: audioService.isAdPlaying 
                          ? (audioService.currentAd?.adTitle ?? 'Advertisement') 
                          : (audioService.currentSong?.audioName ?? 'Unknown'),
                      artistName: audioService.isAdPlaying 
                          ? 'Advertisement' 
                          : (audioService.currentPlaylistName ?? audioService.currentSong?.categoryName ?? 'Unknown'),
                      imageUrl: audioService.isAdPlaying 
                          ? audioService.currentAd?.imageUrl 
                          : audioService.currentSong?.imageUrl,
                      isPlaying: audioService.isPlaying,
                      progress: progress,
                      isAdPlaying: audioService.isAdPlaying,
                      onLearnMore: () {
                        if (audioService.currentAd?.redirectUrl != null) {
                          launchUrl(Uri.parse(audioService.currentAd!.redirectUrl!));
                        }
                      },
                      onPlayPause: () {
                        audioService.togglePlayPause();
                      },
                      onNext: () {
                        if (!audioService.isAdPlaying) audioService.skipToNext();
                      },
                      onPrevious: () {
                        if (!audioService.isAdPlaying) audioService.skipToPrevious();
                      },
                      onAdd: () {
                        if (!audioService.isAdPlaying && audioService.currentSong != null) {
                          showModalBottomSheet(
                            context: context,
                            backgroundColor: Colors.transparent,
                            isScrollControlled: true,
                            useRootNavigator: true,
                            builder: (context) => AddToPlaylistSheet(song: audioService.currentSong!),
                          );
                        }
                      },
                      onTap: () {
                        if (audioService.currentSong != null) {
                          showModalBottomSheet(
                            context: context,
                            isScrollControlled: true,
                            useRootNavigator: true,
                            backgroundColor: Colors.transparent,
                            builder: (context) => BottomSheetPlayer(
                              currentSong: audioService.currentSong!,
                            ),
                          );
                        }
                      },
                    );
                  },
                ),
                // Add a small safe area at the bottom to ensure it clears navigation bar perfectly if needed, 
                // but since bottomNavigationBar is used, this sits just above it anyway.
              ],
            ),
          ),
        ],
      ),
      
      // Dynamic Bottom Navigation
      bottomNavigationBar: _isLoadingNav || _bottomNavItems.isEmpty ? null : BottomNavigationBar(
        backgroundColor: const Color(0xFF181A20), // darkGray
        type: BottomNavigationBarType.fixed,
        currentIndex: _currentIndex,
        selectedItemColor: const Color(0xFFEB1C24), // Active color
        unselectedItemColor: Colors.grey, // Inactive color
        onTap: (index) {
          String name = _bottomNavItems[index]['bottommenuName'].toString().toLowerCase().trim();
          if (name == 'create') {
            _showCreateBottomSheet(context);
            return;
          }

          if (_currentIndex == index) {
            // If already on this tab, pop to root of the tab
            _navigatorKeys[index].currentState?.popUntil((route) => route.isFirst);
          } else {
            setState(() {
              _currentIndex = index;
            });
          }
        },
        items: _bottomNavItems.map((item) {
          String name = item['bottommenuName'].toString();
          return BottomNavigationBarItem(
            icon: Icon(_getIconForTab(name)),
            label: name,
          );
        }).toList(),
      ),
              ), // End Scaffold
            ],
          );
        },
      ),
    );
  }
}
