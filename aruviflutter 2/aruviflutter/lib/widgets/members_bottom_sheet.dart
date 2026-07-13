import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;
import 'dart:convert';
import 'package:shared_preferences/shared_preferences.dart';

class MembersBottomSheet extends StatefulWidget {
  final String playlistId;
  final bool isOwner;
  final bool initialAdminOnlyRemove;
  final Function(bool) onAdminSettingChanged;

  const MembersBottomSheet({
    super.key,
    required this.playlistId,
    required this.isOwner,
    required this.initialAdminOnlyRemove,
    required this.onAdminSettingChanged,
  });

  @override
  State<MembersBottomSheet> createState() => _MembersBottomSheetState();
}

class _MembersBottomSheetState extends State<MembersBottomSheet> {
  bool _isLoading = true;
  String? _errorMessage;
  List<dynamic> _members = [];
  late bool _adminOnlyRemove;

  @override
  void initState() {
    super.initState();
    _adminOnlyRemove = widget.initialAdminOnlyRemove;
    _fetchMembers();
  }

  Future<void> _fetchMembers() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final token = prefs.getString('access_token');
      if (token == null) return;

      final response = await http.get(
        Uri.parse('https://music-app-api-1.onrender.com/api/user/collaborative-playlist/${widget.playlistId}/members'),
        headers: {'Authorization': 'Bearer $token'},
      );

      if (response.statusCode == 200) {
        final data = json.decode(response.body);
        if (data['status'] == true) {
          setState(() {
            _members = data['members'];
            _adminOnlyRemove = data['adminOnlyRemove'] ?? false;
            _isLoading = false;
          });
        }
      } else {
        setState(() {
          _errorMessage = 'Failed to load members';
          _isLoading = false;
        });
      }
    } catch (e) {
      setState(() {
        _errorMessage = e.toString();
        _isLoading = false;
      });
    }
  }

  Future<void> _toggleAdminOnly(bool value) async {
    setState(() {
      _adminOnlyRemove = value;
    });
    
    // Notify parent immediately for responsive UI
    widget.onAdminSettingChanged(value);

    try {
      final prefs = await SharedPreferences.getInstance();
      final token = prefs.getString('access_token');
      if (token == null) return;

      await http.put(
        Uri.parse('https://music-app-api-1.onrender.com/api/user/collaborative-playlist/${widget.playlistId}/settings'),
        headers: {
          'Authorization': 'Bearer $token',
          'Content-Type': 'application/json',
        },
        body: json.encode({'adminOnlyRemove': value}),
      );
    } catch (e) {
      // Revert if failed
      setState(() {
        _adminOnlyRemove = !value;
      });
      widget.onAdminSettingChanged(!value);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: const BoxDecoration(
        color: Color(0xFF1A1A1A),
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          // Drag handle
          Container(
            margin: const EdgeInsets.only(top: 12, bottom: 20),
            width: 40,
            height: 4,
            decoration: BoxDecoration(
              color: Colors.white24,
              borderRadius: BorderRadius.circular(2),
            ),
          ),
          
          const Text(
            'Members',
            style: TextStyle(
              color: Colors.white,
              fontSize: 20,
              fontWeight: FontWeight.bold,
            ),
          ),
          const SizedBox(height: 16),
          
          if (widget.isOwner) ...[
            SwitchListTile(
              title: const Text(
                'Only Admin can remove songs',
                style: TextStyle(color: Colors.white),
              ),
              subtitle: const Text(
                'When enabled, members cannot remove any songs',
                style: TextStyle(color: Colors.white54, fontSize: 12),
              ),
              value: _adminOnlyRemove,
              activeColor: const Color(0xFFEB1C24),
              onChanged: _toggleAdminOnly,
            ),
            const Divider(color: Colors.white12),
          ],
          
          Flexible(
            child: _isLoading
                ? const Padding(
                    padding: EdgeInsets.all(40.0),
                    child: CircularProgressIndicator(color: Color(0xFFEB1C24)),
                  )
                : _errorMessage != null
                    ? Padding(
                        padding: const EdgeInsets.all(20.0),
                        child: Text(_errorMessage!, style: const TextStyle(color: Colors.red)),
                      )
                    : ListView.builder(
                        shrinkWrap: true,
                        itemCount: _members.length,
                        itemBuilder: (context, index) {
                          final member = _members[index];
                          final isOwner = member['isOwner'] ?? false;
                          final initial = (member['userName'] as String).substring(0, 1).toUpperCase();
                          
                          return ListTile(
                            leading: CircleAvatar(
                              backgroundColor: isOwner ? const Color(0xFFEB1C24) : Colors.grey[800],
                              child: Text(
                                initial,
                                style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold),
                              ),
                            ),
                            title: Text(
                              member['userName'],
                              style: const TextStyle(color: Colors.white),
                            ),
                            trailing: isOwner
                                ? const Text(
                                    'Admin',
                                    style: TextStyle(color: Color(0xFFEB1C24), fontWeight: FontWeight.bold),
                                  )
                                : null,
                          );
                        },
                      ),
          ),
          const SizedBox(height: 20),
        ],
      ),
    );
  }
}
