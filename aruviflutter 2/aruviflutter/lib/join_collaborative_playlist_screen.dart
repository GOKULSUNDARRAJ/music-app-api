import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:http/http.dart' as http;
import 'dart:convert';

class JoinCollaborativePlaylistScreen extends StatefulWidget {
  final VoidCallback onJoined;

  const JoinCollaborativePlaylistScreen({super.key, required this.onJoined});

  @override
  State<JoinCollaborativePlaylistScreen> createState() => _JoinCollaborativePlaylistScreenState();
}

class _JoinCollaborativePlaylistScreenState extends State<JoinCollaborativePlaylistScreen> {
  final TextEditingController _codeController = TextEditingController();
  bool _isLoading = false;

  @override
  void dispose() {
    _codeController.dispose();
    super.dispose();
  }

  Future<void> _joinPlaylist() async {
    final code = _codeController.text.trim();
    if (code.isEmpty) return;

    setState(() => _isLoading = true);
    try {
      final prefs = await SharedPreferences.getInstance();
      final token = prefs.getString('access_token');
      if (token == null) return;

      final response = await http.post(
        Uri.parse('https://music-app-api-1.onrender.com/api/user/collaborative-playlist/join'),
        headers: {
          'Authorization': 'Bearer $token',
          'Content-Type': 'application/json',
        },
        body: json.encode({'inviteCode': code}),
      );

      final data = json.decode(response.body);

      if (mounted) {
        setState(() => _isLoading = false);
        if (response.statusCode == 200) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text('Successfully joined "${data['playlist']['name']}"!'), backgroundColor: Colors.green),
          );
          widget.onJoined();
          Navigator.pop(context);
        } else {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text(data['message'] ?? 'Failed to join playlist'), backgroundColor: Colors.red),
          );
        }
      }
    } catch (e) {
      debugPrint('Error joining playlist: $e');
      if (mounted) {
        setState(() => _isLoading = false);
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Network error. Please try again.'), backgroundColor: Colors.red));
      }
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
        title: const Text('Join Collaborative Playlist', style: TextStyle(color: Colors.white, fontSize: 16)),
        centerTitle: true,
      ),
      body: SingleChildScrollView(
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 40),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.center,
            children: [
              const Icon(Icons.people_alt, size: 80, color: Colors.white54),
              const SizedBox(height: 24),
              const Text(
                'Enter Invite Code',
                style: TextStyle(color: Colors.white, fontSize: 24, fontWeight: FontWeight.bold),
              ),
              const SizedBox(height: 12),
              const Text(
                'Paste the invite code from your friend to join their collaborative playlist.',
                style: TextStyle(color: Colors.white54, fontSize: 14),
                textAlign: TextAlign.center,
              ),
              const SizedBox(height: 40),
              TextField(
                controller: _codeController,
                autofocus: true,
                style: const TextStyle(color: Colors.white, fontSize: 24, letterSpacing: 4, fontWeight: FontWeight.bold),
                textAlign: TextAlign.center,
                decoration: InputDecoration(
                  hintText: 'A1B2C3D4',
                  hintStyle: const TextStyle(color: Colors.white24, letterSpacing: 4),
                  filled: true,
                  fillColor: const Color(0xFF282828),
                  border: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(12),
                    borderSide: BorderSide.none,
                  ),
                  contentPadding: const EdgeInsets.symmetric(vertical: 20),
                ),
                onSubmitted: (_) => _joinPlaylist(),
              ),
              const SizedBox(height: 40),
              SizedBox(
                width: double.infinity,
                height: 50,
                child: ElevatedButton(
                  onPressed: _isLoading ? null : _joinPlaylist,
                  style: ElevatedButton.styleFrom(
                    backgroundColor: const Color(0xFFEB1C24),
                    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(25)),
                  ),
                  child: _isLoading
                      ? const SizedBox(width: 24, height: 24, child: CircularProgressIndicator(color: Colors.white, strokeWidth: 2))
                      : const Text('Join', style: TextStyle(color: Colors.white, fontSize: 16, fontWeight: FontWeight.bold)),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
