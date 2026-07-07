import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;
import 'dart:convert';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:cached_network_image/cached_network_image.dart';

import '../models/home_response.dart';
import '../models/playlist_section.dart';
import '../models/artist_category.dart';
import '../playlist_screen.dart';
import '../services/audio_service.dart';
import 'package:shimmer/shimmer.dart';

class HomeArtistTab extends StatefulWidget {
  const HomeArtistTab({super.key});

  @override
  State<HomeArtistTab> createState() => _HomeArtistTabState();
}

class _HomeArtistTabState extends State<HomeArtistTab> {
  bool _isLoading = true;
  String? _errorMessage;
  List<PlaylistSection> _sections = [];

  @override
  void initState() {
    super.initState();
    _fetchArtistData();
  }

  Future<void> _fetchArtistData() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final token = prefs.getString('access_token') ?? '';
      final authHeader = token.startsWith('Bearer ') ? token : 'Bearer $token';

      final response = await http.get(
        Uri.parse('https://music-app-api-1.onrender.com/api/artist'),
        headers: {'Authorization': authHeader},
      );

      if (response.statusCode == 200) {
        final data = json.decode(response.body);
        final homeResponse = HomeResponse.fromJson(data);

        if (mounted) {
          setState(() {
            _sections = homeResponse.sections;
            _isLoading = false;
          });
        }
      } else {
        if (mounted) {
          setState(() {
            _errorMessage = 'Failed to load data: ${response.statusCode}';
            _isLoading = false;
          });
        }
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          if (e.toString().contains('SocketException')) {
            _errorMessage = 'You are currently offline.\nCheck your connection.';
          } else {
            _errorMessage = 'Network Error. Please try again later.';
          }
          _isLoading = false;
        });
      }
    }
  }

  Widget _buildImage(String? url, {bool isCircle = false, double borderRadius = 8}) {
    if (url == null || url.isEmpty) {
      return Container(
        decoration: BoxDecoration(
          color: const Color(0xFF2B2B2B),
          shape: isCircle ? BoxShape.circle : BoxShape.rectangle,
          borderRadius: isCircle ? null : BorderRadius.circular(borderRadius),
        ),
      );
    }
    return Container(
      decoration: BoxDecoration(
        color: const Color(0xFF2B2B2B),
        shape: isCircle ? BoxShape.circle : BoxShape.rectangle,
        borderRadius: isCircle ? null : BorderRadius.circular(borderRadius),
        image: DecorationImage(
          image: CachedNetworkImageProvider(url),
          fit: BoxFit.cover,
          onError: (exception, stackTrace) {
            debugPrint('home_artist_tab: Failed to load image $url — $exception');
          },
        ),
      ),
    );
  }

  Widget _buildGridCardItem(ArtistCategory category) {
    return GestureDetector(
      onTap: () async {
        await Navigator.push(
          context,
          MaterialPageRoute(
            builder: (context) => PlaylistScreen(
              title: category.categoryName ?? 'Unknown',
              subtitle: category.songs.isNotEmpty ? '${category.songs.length} Songs' : 'Artist',
              imageUrl: category.categoryImage ?? '',
              categoryId: category.categoryId?.toString() ?? '',
              songs: category.songs,
            ),
          ),
        );
        _fetchArtistData();
      },
      child: Container(
        decoration: BoxDecoration(
          color: const Color(0xFF1E1E1E),
          borderRadius: BorderRadius.circular(6),
        ),
        child: Row(
          children: [
            SizedBox(
              width: 55,
              height: 55,
              child: ClipRRect(
                borderRadius: const BorderRadius.only(
                  topLeft: Radius.circular(6),
                  bottomLeft: Radius.circular(6),
                ),
                child: _buildImage(category.categoryImage, borderRadius: 0),
              ),
            ),
            const SizedBox(width: 10),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  AnimatedBuilder(
                    animation: AudioService(),
                    builder: (context, child) {
                      final audioService = AudioService();
                      final isActive = audioService.currentSong != null && 
                          (audioService.currentSong?.categoryId == category.categoryId?.toString() || 
                           audioService.currentSong?.categoryName == category.categoryName);
                      return Text(
                        category.categoryName ?? 'Unknown',
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: TextStyle(
                          color: isActive ? const Color(0xFFEB1C24) : Colors.white,
                          fontSize: 13,
                          fontWeight: FontWeight.w600,
                        ),
                      );
                    }
                  ),
                  if (category.songs.isNotEmpty) ...[
                    const SizedBox(height: 2),
                    Text(
                      '${category.songs.length} Songs',
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(
                        color: Color(0xFFAAAAAA),
                        fontSize: 10,
                      ),
                    ),
                  ]
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildSquareItem(ArtistCategory category) {
    double size = 135;
    return GestureDetector(
      onTap: () async {
        await Navigator.push(
          context,
          MaterialPageRoute(
            builder: (context) => PlaylistScreen(
              title: category.categoryName ?? 'Unknown',
              subtitle: category.songs.isNotEmpty ? '${category.songs.length} Songs' : 'Album',
              imageUrl: category.categoryImage ?? '',
              categoryId: category.categoryId?.toString() ?? '',
              songs: category.songs,
            ),
          ),
        );
        _fetchArtistData();
      },
      child: Container(
        width: size,
        margin: const EdgeInsets.only(right: 15),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            SizedBox(
              width: size,
              height: size,
              child: _buildImage(category.categoryImage, borderRadius: 12),
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
                  category.categoryName ?? 'Unknown',
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  textAlign: TextAlign.center,
                  style: TextStyle(
                    color: isActive ? const Color(0xFFEB1C24) : Colors.white,
                    fontSize: 14,
                    fontWeight: FontWeight.w500,
                  ),
                );
              }
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildCircleItem(ArtistCategory category) {
    double size = 120;
    return GestureDetector(
      onTap: () async {
        await Navigator.push(
          context,
          MaterialPageRoute(
            builder: (context) => PlaylistScreen(
              title: category.categoryName ?? 'Unknown',
              subtitle: 'Artist',
              imageUrl: category.categoryImage ?? '',
              categoryId: category.categoryId?.toString() ?? '',
              songs: category.songs,
            ),
          ),
        );
        _fetchArtistData();
      },
      child: Container(
        width: size,
        margin: const EdgeInsets.only(right: 15),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.center,
          children: [
            SizedBox(
              width: size,
              height: size,
              child: _buildImage(category.categoryImage, isCircle: true),
            ),
            const SizedBox(height: 10),
            AnimatedBuilder(
              animation: AudioService(),
              builder: (context, child) {
                final audioService = AudioService();
                final isActive = audioService.currentSong != null && 
                    (audioService.currentSong?.categoryId == category.categoryId?.toString() || 
                     audioService.currentSong?.categoryName == category.categoryName);
                return Text(
                  category.categoryName ?? 'Unknown',
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  textAlign: TextAlign.center,
                  style: TextStyle(
                    color: isActive ? const Color(0xFFEB1C24) : Colors.white,
                    fontSize: 14,
                    fontWeight: FontWeight.w600,
                  ),
                );
              }
            ),
            if (category.songs.isNotEmpty) ...[
              const SizedBox(height: 2),
              Text(
                '${category.songs.length} Songs',
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                textAlign: TextAlign.center,
                style: const TextStyle(
                  color: Color(0xFFAAAAAA),
                  fontSize: 11,
                ),
              ),
            ]
          ],
        ),
      ),
    );
  }

  Widget _buildSection(PlaylistSection section) {
    if (section.categories.isEmpty) return const SizedBox.shrink();

    bool isGrid = section.layoutType == 3;

    return Padding(
      padding: const EdgeInsets.only(left: 15, bottom: 25, top: 10),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Padding(
            padding: const EdgeInsets.only(right: 15, bottom: 12),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text(
                  section.sectionTitle ?? '',
                  style: const TextStyle(
                    color: Colors.white,
                    fontSize: 18,
                    fontWeight: FontWeight.w600,
                  ),
                ),
                if (!isGrid)
                  GestureDetector(
                    onTap: () {},
                    child: Row(
                      children: const [
                        Text(
                          'See All',
                          style: TextStyle(
                            color: Color(0xFFEB1C24),
                            fontSize: 13,
                            fontWeight: FontWeight.w500,
                          ),
                        ),
                        Icon(Icons.chevron_right, color: Color(0xFFEB1C24), size: 16),
                      ],
                    ),
                  ),
              ],
            ),
          ),
          if (isGrid)
            Padding(
              padding: const EdgeInsets.only(right: 15),
              child: GridView.builder(
                padding: EdgeInsets.zero,
                physics: const NeverScrollableScrollPhysics(),
                shrinkWrap: true,
                gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                  crossAxisCount: 2,
                  childAspectRatio: 2.8,
                  crossAxisSpacing: 10,
                  mainAxisSpacing: 10,
                ),
                itemCount: section.categories.length,
                itemBuilder: (context, index) {
                  return _buildGridCardItem(section.categories[index]);
                },
              ),
            )
          else
            SizedBox(
              height: 180,
              child: ListView.builder(
                scrollDirection: Axis.horizontal,
                itemCount: section.categories.length,
                itemBuilder: (context, index) {
                  final category = section.categories[index];
                  if (category.adapterType == 2) {
                    return _buildCircleItem(category);
                  }
                  return _buildSquareItem(category);
                },
              ),
            ),
        ],
      ),
    );
  }

  Widget _buildShimmerLoading() {
    final List<PlaylistSection> dummySections = [
      PlaylistSection(
        sectionTitle: '████████████',
        layoutType: 3,
        categories: List.generate(4, (index) => ArtistCategory(
          categoryId: index.toString(),
          categoryName: '████████',
          categoryImage: '',
          songs: [],
        )),
      ),
      PlaylistSection(
        sectionTitle: '████████████████',
        layoutType: 1,
        categories: List.generate(4, (index) => ArtistCategory(
          categoryId: index.toString(),
          categoryName: '████████',
          categoryImage: '',
          songs: [],
        )),
      ),
      PlaylistSection(
        sectionTitle: '████████████████',
        layoutType: 1,
        categories: List.generate(4, (index) => ArtistCategory(
          categoryId: index.toString(),
          categoryName: '████████',
          categoryImage: '',
          songs: [],
        )),
      ),
    ];

    return Shimmer.fromColors(
      baseColor: Colors.grey[900]!,
      highlightColor: Colors.grey[800]!,
      child: IgnorePointer(
        child: ListView.builder(
          padding: const EdgeInsets.only(top: 0, bottom: 20),
          itemCount: dummySections.length,
          itemBuilder: (context, index) {
            return _buildSection(dummySections[index]);
          },
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    if (_isLoading) {
      return _buildShimmerLoading();
    }

    if (_errorMessage != null) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Text(_errorMessage!, style: const TextStyle(color: Colors.white)),
            const SizedBox(height: 15),
            ElevatedButton(
              onPressed: () {
                setState(() {
                  _isLoading = true;
                  _errorMessage = null;
                });
                _fetchArtistData();
              },
              child: const Text('Retry'),
            ),
          ],
        ),
      );
    }

    if (_sections.isEmpty) {
      return const Center(
        child: Text('No data found.', style: TextStyle(color: Colors.white)),
      );
    }

    return RefreshIndicator(
      onRefresh: _fetchArtistData,
      color: const Color(0xFFEB1C24),
      child: ListView.builder(
        padding: const EdgeInsets.only(top: 0, bottom: 20),
        itemCount: _sections.length,
        itemBuilder: (context, index) {
          return _buildSection(_sections[index]);
        },
      ),
    );
  }
}
