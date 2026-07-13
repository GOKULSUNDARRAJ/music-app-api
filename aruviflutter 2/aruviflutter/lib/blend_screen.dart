import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:http/http.dart' as http;
import 'dart:convert';
import 'package:shared_preferences/shared_preferences.dart';
import 'playlist_screen.dart';
import 'models/audio_model.dart';
import 'create_blend_screen.dart';
import 'join_blend_screen.dart';

class BlendScreen extends StatefulWidget {
  const BlendScreen({super.key});

  @override
  State<BlendScreen> createState() => _BlendScreenState();
}

class _BlendScreenState extends State<BlendScreen> {
  bool _isLoading = true;
  List<dynamic> _blends = [];

  @override
  void initState() {
    super.initState();
    _fetchBlends();
  }

  Future<String?> _getToken() async {
    final prefs = await SharedPreferences.getInstance();
    return prefs.getString('access_token');
  }

  Future<void> _fetchBlends() async {
    setState(() => _isLoading = true);
    try {
      final token = await _getToken();
      if (token == null) return;
      
      final response = await http.get(
        Uri.parse('https://music-app-api-1.onrender.com/api/user/blends'),
        headers: {'Authorization': 'Bearer $token'},
      );

      if (response.statusCode == 200) {
        final data = json.decode(response.body);
        if (data['status'] == true) {
          setState(() {
            _blends = data['blends'];
          });
        }
      }
    } catch (e) {
      debugPrint('Error fetching blends: $e');
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  void _createBlend() {
    Navigator.push(
      context,
      MaterialPageRoute(
        builder: (context) => CreateBlendScreen(
          onBlendCreated: _fetchBlends,
        ),
      ),
    );
  }

  void _joinBlend() {
    Navigator.push(
      context,
      MaterialPageRoute(
        builder: (context) => JoinBlendScreen(
          onBlendJoined: _fetchBlends,
        ),
      ),
    );
  }

  Future<void> _openBlend(dynamic blend) async {
    setState(() => _isLoading = true);
    try {
      final token = await _getToken();
      if (token == null) return;
      
      final response = await http.get(
        Uri.parse('https://music-app-api-1.onrender.com/api/user/blend/${blend['blendId']}'),
        headers: {'Authorization': 'Bearer $token'},
      );

      if (response.statusCode == 200) {
        final data = json.decode(response.body);
        if (data['status'] == true) {
          final List<dynamic> songsJson = data['songs'] ?? [];
          final songs = songsJson.map((s) => AudioModel.fromJson(s)).toList();

          if (mounted) {
            Navigator.push(
              context,
              MaterialPageRoute(
                builder: (context) => PlaylistScreen(
                  title: blend['blendName'],
                  subtitle: 'A mixed playlist for you both',
                  imageUrl: 'https://cdn.pixabay.com/photo/2016/11/23/15/48/audience-1853662_1280.jpg',
                  categoryId: blend['blendId'].toString(),
                  songs: songs,
                ),
              ),
            );
          }
        }
      }
    } catch (e) {
      debugPrint('Error fetching blend playlist: $e');
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Failed to load blend playlist')));
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFF121212),
      appBar: AppBar(
        backgroundColor: Colors.transparent,
        elevation: 0,
        iconTheme: const IconThemeData(color: Colors.white),
        title: const Text('Blends', style: TextStyle(fontWeight: FontWeight.bold, color: Colors.white)),
        centerTitle: true,
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator(color: Color(0xFFEB1C24)))
          : Column(
              children: [
                Padding(
                  padding: const EdgeInsets.all(16.0),
                  child: Row(
                    children: [
                      Expanded(
                        child: ElevatedButton.icon(
                          onPressed: _createBlend,
                          icon: const Icon(Icons.add, color: Colors.white),
                          label: const Text('Create', style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold)),
                          style: ElevatedButton.styleFrom(
                            backgroundColor: const Color(0xFFEB1C24),
                            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
                            padding: const EdgeInsets.symmetric(vertical: 12),
                          ),
                        ),
                      ),
                      const SizedBox(width: 16),
                      Expanded(
                        child: OutlinedButton.icon(
                          onPressed: _joinBlend,
                          icon: const Icon(Icons.group_add, color: Colors.white),
                          label: const Text('Join', style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold)),
                          style: OutlinedButton.styleFrom(
                            side: const BorderSide(color: Colors.white54),
                            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
                            padding: const EdgeInsets.symmetric(vertical: 12),
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
                const Divider(color: Colors.white24, height: 1),
                Expanded(
                  child: _blends.isEmpty
                      ? const Center(
                          child: Text(
                            "You don't have any blends yet.\nCreate or join one to mix music with friends!",
                            textAlign: TextAlign.center,
                            style: TextStyle(color: Colors.white54, fontSize: 16),
                          ),
                        )
                      : ListView.builder(
                          itemCount: _blends.length,
                          itemBuilder: (context, index) {
                            final blend = _blends[index];
                            return ListTile(
                              contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                              leading: const CircleAvatar(
                                backgroundColor: Color(0xFF282828),
                                radius: 28,
                                child: Icon(Icons.merge_type, color: Colors.white, size: 28),
                              ),
                              title: Text(
                                blend['blendName'],
                                style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 16),
                              ),
                              subtitle: const Text(
                                'Tap to listen to your shared tastes',
                                style: TextStyle(color: Colors.white70, fontSize: 13),
                              ),
                              trailing: _buildOverlappingCircles(blend['blendName']),
                              onTap: () => _openBlend(blend),
                            );
                          },
                        ),
                ),
              ],
            ),
    );
  }

  Widget _buildOverlappingCircles(String name) {
    String initial = 'B';
    if (name.toLowerCase().contains('with ')) {
      final parts = name.split(RegExp(r'with ', caseSensitive: false));
      if (parts.length > 1 && parts.last.trim().isNotEmpty) {
        initial = parts.last.trim()[0].toUpperCase();
      }
    } else if (name.isNotEmpty) {
      initial = name[0].toUpperCase();
    }

    return SizedBox(
      width: 54,
      height: 36,
      child: Stack(
        alignment: Alignment.center,
        children: [
          Positioned(
            right: 0,
            child: Container(
              width: 36,
              height: 36,
              decoration: const BoxDecoration(
                color: Color(0xFF282828),
                shape: BoxShape.circle,
              ),
            ),
          ),
          Positioned(
            left: 0,
            child: Container(
              width: 36,
              height: 36,
              decoration: BoxDecoration(
                color: const Color(0xFF8B5A2B), // Brownish
                shape: BoxShape.circle,
                border: Border.all(color: const Color(0xFF121212), width: 3),
              ),
              child: Center(
                child: Text(
                  initial,
                  style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: Colors.black87),
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}
