import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:http/http.dart' as http;
import 'dart:convert';
import 'package:share_plus/share_plus.dart';

class CreateCollaborativePlaylistScreen extends StatefulWidget {
  final VoidCallback onCreated;

  const CreateCollaborativePlaylistScreen({super.key, required this.onCreated});

  @override
  State<CreateCollaborativePlaylistScreen> createState() => _CreateCollaborativePlaylistScreenState();
}

class _CreateCollaborativePlaylistScreenState extends State<CreateCollaborativePlaylistScreen> {
  final TextEditingController _nameController = TextEditingController();
  bool _isLoading = false;
  String _inviteCode = '';
  String _playlistName = '';
  String _userName = '';

  @override
  void initState() {
    super.initState();
    _loadUser();
  }

  Future<void> _loadUser() async {
    final prefs = await SharedPreferences.getInstance();
    setState(() {
      _userName = prefs.getString('username') ?? 'A friend';
    });
  }

  @override
  void dispose() {
    _nameController.dispose();
    super.dispose();
  }

  Future<void> _createPlaylist() async {
    final name = _nameController.text.trim();
    if (name.isEmpty) return;

    setState(() => _isLoading = true);
    try {
      final prefs = await SharedPreferences.getInstance();
      final token = prefs.getString('access_token');
      if (token == null) return;

      final response = await http.post(
        Uri.parse('https://music-app-api-1.onrender.com/api/user/collaborative-playlist'),
        headers: {
          'Authorization': 'Bearer $token',
          'Content-Type': 'application/json',
        },
        body: json.encode({'name': name}),
      );

      if (response.statusCode == 201 || response.statusCode == 200) {
        final data = json.decode(response.body);
        if (mounted) {
          setState(() {
            _inviteCode = data['playlist']['inviteCode'];
            _playlistName = name;
            _isLoading = false;
          });
          widget.onCreated();
        }
      } else {
        final errorData = json.decode(response.body);
        throw Exception(errorData['message'] ?? 'Failed to create playlist: ${response.statusCode}');
      }
    } catch (e) {
      debugPrint('Error creating playlist: $e');
      if (mounted) {
        setState(() => _isLoading = false);
        String errorMsg = e.toString().replaceAll('Exception: ', '');
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(errorMsg), backgroundColor: Colors.red));
      }
    }
  }

  void _shareCode() {
    if (_inviteCode.isNotEmpty) {
      final text = '$_userName has invited you to collaborate on a playlist named "$_playlistName". Use this invite code to join: $_inviteCode';
      Share.share(text);
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
        title: const Text('Create Collaborative Playlist', style: TextStyle(color: Colors.white, fontSize: 16)),
        centerTitle: true,
      ),
      body: _inviteCode.isEmpty
          ? SingleChildScrollView(
              child: Padding(
                padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 40),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.center,
                  children: [
                    const Icon(Icons.group_add, size: 80, color: Colors.white54),
                    const SizedBox(height: 24),
                    const Text(
                      'Name your shared playlist',
                      style: TextStyle(color: Colors.white, fontSize: 24, fontWeight: FontWeight.bold),
                    ),
                    const SizedBox(height: 12),
                    const Text(
                      'Create a playlist and invite friends to build it together.',
                      style: TextStyle(color: Colors.white54, fontSize: 14),
                      textAlign: TextAlign.center,
                    ),
                    const SizedBox(height: 40),
                    TextField(
                      controller: _nameController,
                      autofocus: true,
                      style: const TextStyle(color: Colors.white, fontSize: 18),
                      textAlign: TextAlign.center,
                      decoration: InputDecoration(
                        hintText: 'My Awesome Playlist',
                        hintStyle: const TextStyle(color: Colors.white24),
                        filled: true,
                        fillColor: const Color(0xFF282828),
                        border: OutlineInputBorder(
                          borderRadius: BorderRadius.circular(12),
                          borderSide: BorderSide.none,
                        ),
                        contentPadding: const EdgeInsets.symmetric(vertical: 20),
                      ),
                      onSubmitted: (_) => _createPlaylist(),
                    ),
                    const SizedBox(height: 40),
                    SizedBox(
                      width: double.infinity,
                      height: 50,
                      child: ElevatedButton(
                        onPressed: _isLoading ? null : _createPlaylist,
                        style: ElevatedButton.styleFrom(
                          backgroundColor: const Color(0xFFEB1C24),
                          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(25)),
                        ),
                        child: _isLoading
                            ? const SizedBox(width: 24, height: 24, child: CircularProgressIndicator(color: Colors.white, strokeWidth: 2))
                            : const Text('Create', style: TextStyle(color: Colors.white, fontSize: 16, fontWeight: FontWeight.bold)),
                      ),
                    ),
                  ],
                ),
              ),
            )
          : Center(
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Container(
                    width: 120,
                    height: 120,
                    decoration: BoxDecoration(
                      color: const Color(0xFF282828),
                      borderRadius: BorderRadius.circular(60),
                      border: Border.all(color: const Color(0xFFEB1C24), width: 3),
                    ),
                    child: const Icon(Icons.check, size: 60, color: Color(0xFFEB1C24)),
                  ),
                  const SizedBox(height: 32),
                  Text(
                    '"$_playlistName" created!',
                    style: const TextStyle(color: Colors.white, fontSize: 24, fontWeight: FontWeight.bold),
                  ),
                  const SizedBox(height: 12),
                  const Text(
                    'Share this invite code with your friends:',
                    style: TextStyle(color: Colors.white70, fontSize: 16),
                  ),
                  const SizedBox(height: 24),
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 32, vertical: 16),
                    decoration: BoxDecoration(
                      color: const Color(0xFF282828),
                      borderRadius: BorderRadius.circular(12),
                    ),
                    child: Text(
                      _inviteCode,
                      style: const TextStyle(color: Colors.white, fontSize: 32, fontWeight: FontWeight.bold, letterSpacing: 4),
                    ),
                  ),
                  const SizedBox(height: 40),
                  SizedBox(
                    width: 200,
                    height: 50,
                    child: ElevatedButton.icon(
                      onPressed: _shareCode,
                      icon: const Icon(Icons.share, color: Colors.white),
                      label: const Text('Share Code', style: TextStyle(color: Colors.white, fontSize: 16, fontWeight: FontWeight.bold)),
                      style: ElevatedButton.styleFrom(
                        backgroundColor: const Color(0xFFEB1C24),
                        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(25)),
                      ),
                    ),
                  ),
                ],
              ),
            ),
    );
  }
}
