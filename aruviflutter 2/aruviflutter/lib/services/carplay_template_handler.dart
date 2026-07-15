import 'dart:convert';
import 'dart:io';
import 'dart:typed_data';
import 'package:flutter_carplay/flutter_carplay.dart';
import 'package:audio_service/audio_service.dart';
import 'package:http/http.dart' as http;
import 'package:path_provider/path_provider.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../models/playlist_section.dart';
import '../models/audio_model.dart';

class CarPlayTemplateHandler {
  final AudioHandler audioHandler;
  static const String _baseUrl = 'https://music-app-api-1.onrender.com/api';

  CarPlayTemplateHandler(this.audioHandler);

  void init() {
    FlutterCarplay.addListenerOnConnectionChange(_onCarPlayConnectionChange);
    _buildAndSetTemplates();
  }

  void dispose() {
    FlutterCarplay.removeListenerOnConnectionChange(_onCarPlayConnectionChange);
  }

  void _onCarPlayConnectionChange(CPConnectionStatusTypes status) {
    if (status == CPConnectionStatusTypes.connected) {
      _buildAndSetTemplates();
    }
  }

  Future<String?> _getToken() async {
    final prefs = await SharedPreferences.getInstance();
    final token = prefs.getString('access_token') ?? '';
    if (token.isEmpty) return null;
    return token.startsWith('Bearer ') ? token : 'Bearer $token';
  }

  /// Downloads an image URL and saves it to the temp directory.
  /// Returns the local file path so flutter_carplay can read it as an asset.
  Future<String?> _downloadImage(String? url, String cacheKey) async {
    if (url == null || url.isEmpty) return null;
    try {
      final dir = await getTemporaryDirectory();
      final filePath = '${dir.path}/cp_img_$cacheKey.jpg';
      final file = File(filePath);

      // Use cached version if already downloaded
      if (await file.exists()) return filePath;

      final response = await http.get(Uri.parse(url)).timeout(
        const Duration(seconds: 6),
      );
      if (response.statusCode == 200) {
        await file.writeAsBytes(response.bodyBytes);
        return filePath;
      }
    } catch (_) {}
    return null;
  }

  Future<void> _buildAndSetTemplates() async {
    try {
      // Show a loading template immediately so CarPlay isn't blank
      _showLoadingTemplate();

      // Fetch the home data from the real API
      final token = await _getToken();
      final headers = token != null
          ? {'Authorization': token}
          : <String, String>{};

      final response = await http.get(
        Uri.parse('$_baseUrl/home'),
        headers: headers,
      ).timeout(const Duration(seconds: 15));

      if (response.statusCode != 200) {
        _showErrorTemplate(
            'Could not connect to server (${response.statusCode})');
        return;
      }

      final data = json.decode(response.body);
      final List<dynamic> sectionsJson =
          data['data'] ?? data['sections'] ?? [];
      final List<PlaylistSection> sections =
          sectionsJson.map((s) => PlaylistSection.fromJson(s)).toList();

      if (sections.isEmpty) {
        _showErrorTemplate('No music content found.');
        return;
      }

      // Build one CPListTemplate per Section (max 4 tabs for CarPlay)
      final List<CPListTemplate> sectionTemplates = [];

      for (final section in sections.take(4)) {
        if (section.categories.isEmpty) continue;

        final List<CPListSection> cpSections = [];

        for (final category in section.categories) {
          if (category.songs.isEmpty) continue;

          final List<CPListItem> songItems = [];

          for (final song in category.songs) {
            // Download album art and get local path
            final safeKey = (song.songId ?? song.audioName ?? 'x')
                .replaceAll(RegExp(r'[^a-zA-Z0-9]'), '_');
            final imagePath =
                await _downloadImage(song.imageUrl, safeKey);

            songItems.add(
              CPListItem(
                text: song.audioName ?? 'Unknown',
                detailText: category.categoryName ?? '',
                image: imagePath, // local file path or null
                onPress: (complete, self) async {
                  await _playSong(song, category.songs);
                  complete();
                },
              ),
            );
          }

          if (songItems.isNotEmpty) {
            cpSections.add(
              CPListSection(
                header: category.categoryName ?? 'Songs',
                items: songItems,
              ),
            );
          }
        }

        if (cpSections.isNotEmpty) {
          sectionTemplates.add(
            CPListTemplate(
              sections: cpSections,
              title: section.sectionTitle ?? 'Music',
              systemIcon: 'music.note.list',
            ),
          );
        }
      }

      if (sectionTemplates.isEmpty) {
        _showErrorTemplate('No songs available right now.');
        return;
      }

      if (sectionTemplates.length == 1) {
        // Only one section — show it directly without a tab bar
        FlutterCarplay.setRootTemplate(
            rootTemplate: sectionTemplates.first, animated: true);
      } else {
        // Multiple sections — wrap in a tab bar
        final tabBarTemplate = CPTabBarTemplate(
          templates: sectionTemplates,
        );
        FlutterCarplay.setRootTemplate(
            rootTemplate: tabBarTemplate, animated: true);
      }
    } catch (e) {
      _showErrorTemplate('Error loading music: ${e.toString()}');
    }
  }

  void _showLoadingTemplate() {
    final loadingTemplate = CPListTemplate(
      sections: [
        CPListSection(
          items: [
            CPListItem(
              text: 'Loading music...',
              detailText: 'Please wait',
              onPress: (complete, self) async {
                complete();
              },
            ),
          ],
        ),
      ],
      title: 'Aruvi Music',
      systemIcon: 'music.note',
    );
    FlutterCarplay.setRootTemplate(
        rootTemplate: loadingTemplate, animated: false);
  }

  void _showErrorTemplate(String message) {
    final errorTemplate = CPListTemplate(
      sections: [
        CPListSection(
          items: [
            CPListItem(
              text: message,
              detailText: 'Tap to retry',
              onPress: (complete, self) async {
                complete();
                _buildAndSetTemplates();
              },
            ),
          ],
        ),
      ],
      title: 'Aruvi Music',
      systemIcon: 'exclamationmark.triangle',
    );
    FlutterCarplay.setRootTemplate(
        rootTemplate: errorTemplate, animated: true);
  }

  Future<void> _playSong(AudioModel song, List<AudioModel> queue) async {
    try {
      final mediaItems = queue
          .map(
            (s) => MediaItem(
              id: s.audioUrl ?? '',
              title: s.audioName ?? 'Unknown',
              artist: s.categoryName ?? '',
              artUri: s.imageUrl != null ? Uri.parse(s.imageUrl!) : null,
            ),
          )
          .toList();

      final index = queue.indexWhere((s) => s.songId == song.songId);
      await audioHandler.updateQueue(mediaItems);
      await audioHandler.skipToQueueItem(index >= 0 ? index : 0);
      await audioHandler.play();
    } catch (e) {
      print('CarPlay play error: $e');
    }
  }
}
