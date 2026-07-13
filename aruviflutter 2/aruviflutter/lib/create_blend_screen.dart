import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:http/http.dart' as http;
import 'dart:convert';

class CreateBlendScreen extends StatefulWidget {
  final VoidCallback onBlendCreated;

  const CreateBlendScreen({super.key, required this.onBlendCreated});

  @override
  State<CreateBlendScreen> createState() => _CreateBlendScreenState();
}

class _CreateBlendScreenState extends State<CreateBlendScreen> {
  bool _isLoading = true;
  String _inviteCode = '';
  String _userName = '';

  @override
  void initState() {
    super.initState();
    _initBlend();
  }

  Future<void> _initBlend() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final token = prefs.getString('access_token');
      _userName = prefs.getString('userName') ?? 'You';
      
      if (token == null) {
        if (mounted) Navigator.pop(context);
        return;
      }

      final response = await http.post(
        Uri.parse('https://music-app-api-1.onrender.com/api/user/blend/invite'),
        headers: {'Authorization': 'Bearer $token'},
      );

      if (response.statusCode == 201 || response.statusCode == 200) {
        final data = json.decode(response.body);
        if (mounted) {
          setState(() {
            _inviteCode = data['inviteCode'];
            _isLoading = false;
          });
          widget.onBlendCreated();
        }
      } else {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Failed to create blend')));
          Navigator.pop(context);
        }
      }
    } catch (e) {
      debugPrint('Error creating blend: $e');
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Failed to create blend')));
        Navigator.pop(context);
      }
    }
  }

  void _copyCode() {
    if (_inviteCode.isNotEmpty) {
      Clipboard.setData(ClipboardData(text: _inviteCode));
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Code copied to clipboard!')));
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
        title: const Text(
          'Create a Blend',
          style: TextStyle(color: Colors.white, fontSize: 16, fontWeight: FontWeight.bold),
        ),
        centerTitle: true,
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator(color: Color(0xFFEB1C24)))
          : SingleChildScrollView(
              padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 20),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.center,
                children: [
                  const SizedBox(height: 20),
                  // Overlapping circles graphic
                  SizedBox(
                    height: 140,
                    width: 200,
                    child: Stack(
                      alignment: Alignment.center,
                      children: [
                        Positioned(
                          right: 20,
                          child: Container(
                            width: 120,
                            height: 120,
                            decoration: const BoxDecoration(
                              color: Color(0xFF282828),
                              shape: BoxShape.circle,
                            ),
                            child: const Center(
                              child: Icon(Icons.add, size: 60, color: Colors.white54),
                            ),
                          ),
                        ),
                        Positioned(
                          left: 20,
                          child: Container(
                            width: 120,
                            height: 120,
                            decoration: BoxDecoration(
                              color: const Color(0xFF8B5A2B), // Brownish
                              shape: BoxShape.circle,
                              border: Border.all(color: const Color(0xFF121212), width: 4),
                            ),
                            child: Center(
                              child: Text(
                                _userName.isNotEmpty ? _userName[0].toUpperCase() : 'U',
                                style: const TextStyle(fontSize: 50, fontWeight: FontWeight.bold, color: Colors.black87),
                              ),
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 40),
                  const Text(
                    'Invite friends to Blend',
                    style: TextStyle(color: Colors.white, fontSize: 24, fontWeight: FontWeight.bold),
                    textAlign: TextAlign.center,
                  ),
                  const SizedBox(height: 16),
                  const Text(
                    'Invite up to 10 friends to a Blend, a shared playlist that gives you social recommendations based on all of your music tastes.',
                    style: TextStyle(color: Colors.white54, fontSize: 14, height: 1.5),
                    textAlign: TextAlign.center,
                  ),
                  const SizedBox(height: 40),
                  Container(
                    decoration: const BoxDecoration(
                      color: Color(0xFF282828),
                      borderRadius: BorderRadius.vertical(top: Radius.circular(16)),
                    ),
                    padding: const EdgeInsets.all(20),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        const Text(
                          'Sharing text',
                          style: TextStyle(color: Colors.white, fontSize: 18, fontWeight: FontWeight.bold),
                        ),
                        const SizedBox(height: 16),
                        Container(
                          padding: const EdgeInsets.all(16),
                          decoration: BoxDecoration(
                            color: const Color(0xFF3E3E3E),
                            borderRadius: BorderRadius.circular(8),
                          ),
                          child: Row(
                            children: [
                              Expanded(
                                child: Text(
                                  '$_userName has invited you to join a Blend on this App. Use this invite code to join: $_inviteCode',
                                  style: const TextStyle(color: Colors.white70, fontSize: 14, height: 1.4),
                                ),
                              ),
                              const SizedBox(width: 16),
                              IconButton(
                                icon: const Icon(Icons.copy, color: Colors.white54),
                                onPressed: _copyCode,
                              ),
                            ],
                          ),
                        ),
                        const SizedBox(height: 24),
                        const Divider(color: Colors.white24, height: 1),
                        const SizedBox(height: 24),
                        // Contacts Row
                        SingleChildScrollView(
                          scrollDirection: Axis.horizontal,
                          child: Row(
                            children: [
                              _buildShareContact('Deepika\nAkka💕', 'https://cdn.pixabay.com/photo/2017/08/30/12/45/girl-2696947_1280.jpg'),
                              const SizedBox(width: 20),
                              _buildShareContact('My Akka💕', 'https://cdn.pixabay.com/photo/2016/11/29/13/14/attractive-1869761_1280.jpg'),
                              const SizedBox(width: 20),
                              _buildShareContact('vasanth\nAnna', null, initials: 'VA'),
                              const SizedBox(width: 20),
                              _buildShareContact('Mom', 'https://cdn.pixabay.com/photo/2015/03/03/08/55/portrait-657116_1280.jpg'),
                            ],
                          ),
                        ),
                        const SizedBox(height: 24),
                        // Apps Row
                        SingleChildScrollView(
                          scrollDirection: Axis.horizontal,
                          child: Row(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              _buildShareApp(
                                'Quick\nShare',
                                Container(
                                  width: 54,
                                  height: 54,
                                  decoration: const BoxDecoration(color: Color(0xFF1A73E8), shape: BoxShape.circle),
                                  child: const Icon(Icons.sync_alt, color: Colors.white, size: 28),
                                ),
                              ),
                              const SizedBox(width: 24),
                              _buildShareApp(
                                'WhatsApp',
                                Container(
                                  width: 54,
                                  height: 54,
                                  decoration: const BoxDecoration(color: Color(0xFF25D366), shape: BoxShape.circle),
                                  child: const Icon(Icons.chat_bubble, color: Colors.white, size: 28),
                                ),
                              ),
                              const SizedBox(width: 24),
                              _buildShareApp(
                                'Gmail',
                                Container(
                                  width: 54,
                                  height: 54,
                                  decoration: const BoxDecoration(color: Colors.white, shape: BoxShape.circle),
                                  child: const Icon(Icons.mail, color: Color(0xFFD44638), size: 28),
                                ),
                              ),
                              const SizedBox(width: 24),
                              _buildShareApp(
                                'Instagram',
                                Container(
                                  width: 54,
                                  height: 54,
                                  decoration: const BoxDecoration(
                                    gradient: LinearGradient(
                                      colors: [Color(0xFF833AB4), Color(0xFFFD1D1D), Color(0xFFF56040)],
                                      begin: Alignment.topLeft,
                                      end: Alignment.bottomRight,
                                    ),
                                    shape: BoxShape.circle,
                                  ),
                                  child: const Icon(Icons.camera_alt, color: Colors.white, size: 28),
                                ),
                              ),
                              const SizedBox(width: 24),
                              _buildShareApp(
                                'Drive',
                                Container(
                                  width: 54,
                                  height: 54,
                                  decoration: const BoxDecoration(color: Colors.white, shape: BoxShape.circle),
                                  child: const Icon(Icons.add_to_drive, color: Color(0xFF1FA463), size: 28),
                                ),
                              ),
                            ],
                          ),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            ),
    );
  }

  Widget _buildShareContact(String name, String? imageUrl, {String? initials}) {
    return GestureDetector(
      onTap: _copyCode,
      child: SizedBox(
        width: 70,
        child: Column(
          children: [
            Stack(
              children: [
                CircleAvatar(
                  radius: 28,
                  backgroundColor: const Color(0xFF3E3E3E),
                  backgroundImage: imageUrl != null ? NetworkImage(imageUrl) : null,
                  child: imageUrl == null && initials != null
                      ? Text(initials, style: const TextStyle(color: Colors.white, fontSize: 20, fontWeight: FontWeight.bold))
                      : null,
                ),
                Positioned(
                  bottom: 0,
                  right: 0,
                  child: Container(
                    decoration: BoxDecoration(
                      color: const Color(0xFF25D366),
                      shape: BoxShape.circle,
                      border: Border.all(color: const Color(0xFF282828), width: 2),
                    ),
                    padding: const EdgeInsets.all(2),
                    child: const Icon(Icons.chat_bubble, color: Colors.white, size: 10),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 8),
            Text(
              name,
              style: const TextStyle(color: Colors.white, fontSize: 12),
              textAlign: TextAlign.center,
              maxLines: 2,
              overflow: TextOverflow.ellipsis,
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildShareApp(String name, Widget iconWidget) {
    return GestureDetector(
      onTap: _copyCode,
      child: SizedBox(
        width: 65,
        child: Column(
          children: [
            iconWidget,
            const SizedBox(height: 8),
            Text(
              name,
              style: const TextStyle(color: Colors.white, fontSize: 12),
              textAlign: TextAlign.center,
              maxLines: 2,
              overflow: TextOverflow.ellipsis,
            ),
          ],
        ),
      ),
    );
  }
}
