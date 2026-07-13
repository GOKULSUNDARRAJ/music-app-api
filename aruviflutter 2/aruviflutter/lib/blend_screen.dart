import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:http/http.dart' as http;
import 'dart:convert';
import 'package:shared_preferences/shared_preferences.dart';
import 'playlist_screen.dart';
import 'models/artist_category.dart';

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

  Future<void> _createBlend() async {
    try {
      final token = await _getToken();
      if (token == null) return;
      
      final response = await http.post(
        Uri.parse('https://music-app-api-1.onrender.com/api/user/blend/invite'),
        headers: {'Authorization': 'Bearer $token'},
      );

      if (response.statusCode == 201) {
        final data = json.decode(response.body);
        final inviteCode = data['inviteCode'];
        if (mounted) {
          showDialog(
            context: context,
            builder: (ctx) => AlertDialog(
              backgroundColor: const Color(0xFF282828),
              title: const Text('Blend Invite Code', style: TextStyle(color: Colors.white)),
              content: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  const Text('Share this code with a friend to create a Blend:', style: TextStyle(color: Colors.white70)),
                  const SizedBox(height: 16),
                  Container(
                    padding: const EdgeInsets.all(12),
                    decoration: BoxDecoration(
                      color: Colors.black26,
                      borderRadius: BorderRadius.circular(8),
                    ),
                    child: SelectableText(
                      inviteCode,
                      style: const TextStyle(color: Colors.white, fontSize: 20, fontWeight: FontWeight.bold, letterSpacing: 2),
                    ),
                  ),
                ],
              ),
              actions: [
                TextButton(
                  onPressed: () {
                    Clipboard.setData(ClipboardData(text: inviteCode));
                    ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Code copied to clipboard')));
                    Navigator.pop(ctx);
                  },
                  child: const Text('Copy', style: TextStyle(color: Color(0xFF1DB954))),
                ),
                TextButton(
                  onPressed: () => Navigator.pop(ctx),
                  child: const Text('Done', style: TextStyle(color: Colors.white)),
                ),
              ],
            ),
          );
        }
      }
    } catch (e) {
      debugPrint('Error creating blend: $e');
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Failed to create blend')));
    }
  }

  Future<void> _joinBlend() async {
    final controller = TextEditingController();
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: const Color(0xFF282828),
        title: const Text('Join a Blend', style: TextStyle(color: Colors.white)),
        content: TextField(
          controller: controller,
          style: const TextStyle(color: Colors.white),
          decoration: InputDecoration(
            hintText: 'Enter Invite Code',
            hintStyle: const TextStyle(color: Colors.white54),
            enabledBorder: UnderlineInputBorder(borderSide: BorderSide(color: Colors.white24)),
            focusedBorder: const UnderlineInputBorder(borderSide: BorderSide(color: Color(0xFF1DB954))),
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx),
            child: const Text('Cancel', style: TextStyle(color: Colors.white)),
          ),
          TextButton(
            onPressed: () async {
              final code = controller.text.trim();
              if (code.isEmpty) return;
              Navigator.pop(ctx);
              
              try {
                final token = await _getToken();
                if (token == null) return;
                
                final response = await http.post(
                  Uri.parse('https://music-app-api-1.onrender.com/api/user/blend/join'),
                  headers: {
                    'Authorization': 'Bearer $token',
                    'Content-Type': 'application/json',
                  },
                  body: json.encode({'inviteCode': code}),
                );

                final data = json.decode(response.body);
                if (response.statusCode == 200 && data['status'] == true) {
                  ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Successfully joined blend!')));
                  _fetchBlends();
                } else {
                  ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(data['message'] ?? 'Failed to join blend')));
                }
              } catch (e) {
                ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Error joining blend')));
              }
            },
            child: const Text('Join', style: TextStyle(color: Color(0xFF1DB954))),
          ),
        ],
      ),
    );
  }

  void _openBlend(dynamic blend) {
    // Create a mock category object to pass to PlaylistScreen
    final fakeCategory = ArtistCategory(
      id: blend['blendId'],
      categoryName: blend['blendName'],
      categoryImage: 'https://cdn.pixabay.com/photo/2016/11/23/15/48/audience-1853662_1280.jpg', // Placeholder blend image
      adapterType: 1,
    );

    Navigator.push(
      context,
      MaterialPageRoute(
        builder: (context) => PlaylistScreen(
          category: fakeCategory,
          isBlend: true, // We'll pass a flag if needed, or we can just fetch internally if we modify PlaylistScreen
          blendId: blend['blendId'].toString(),
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFF121212),
      appBar: AppBar(
        backgroundColor: Colors.transparent,
        elevation: 0,
        title: const Text('Blends', style: TextStyle(fontWeight: FontWeight.bold)),
        centerTitle: true,
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator(color: Color(0xFF1DB954)))
          : Column(
              children: [
                Padding(
                  padding: const EdgeInsets.all(16.0),
                  child: Row(
                    children: [
                      Expanded(
                        child: ElevatedButton.icon(
                          onPressed: _createBlend,
                          icon: const Icon(Icons.add, color: Colors.black),
                          label: const Text('Create', style: TextStyle(color: Colors.black, fontWeight: FontWeight.bold)),
                          style: ElevatedButton.styleFrom(
                            backgroundColor: const Color(0xFF1DB954),
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
                              onTap: () => _openBlend(blend),
                            );
                          },
                        ),
                ),
              ],
            ),
    );
  }
}
