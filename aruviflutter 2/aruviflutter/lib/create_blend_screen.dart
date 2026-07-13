import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:http/http.dart' as http;
import 'dart:convert';
import 'package:share_plus/share_plus.dart';

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

  void _shareCode() {
    if (_inviteCode.isNotEmpty) {
      final text = '$_userName has invited you to join a Blend on this App. Use this invite code to join: $_inviteCode';
      Share.share(text);
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
              padding: const EdgeInsets.only(left: 24, right: 24, top: 20, bottom: 140),
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
                        // Share button (native)
                        SizedBox(
                          width: double.infinity,
                          height: 50,
                          child: ElevatedButton(
                            onPressed: _shareCode,
                            style: ElevatedButton.styleFrom(
                              backgroundColor: Colors.white,
                              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(25)),
                            ),
                            child: const Text(
                              'Share Link',
                              style: TextStyle(color: Colors.black, fontSize: 16, fontWeight: FontWeight.bold),
                            ),
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
}
