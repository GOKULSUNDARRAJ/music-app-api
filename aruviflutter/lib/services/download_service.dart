import 'dart:io';
import 'package:flutter/foundation.dart';
import 'package:path_provider/path_provider.dart';
import 'package:http/http.dart' as http;
import '../models/audio_model.dart';
import 'database_service.dart';

class DownloadService extends ChangeNotifier {
  static final DownloadService _instance = DownloadService._internal();
  factory DownloadService() => _instance;
  DownloadService._internal();

  final Map<String, double> _downloadProgress = {};
  Map<String, double> get downloadProgress => _downloadProgress;

  bool isDownloading(String categoryId) {
    return _downloadProgress.containsKey(categoryId);
  }

  double getProgress(String categoryId) {
    return _downloadProgress[categoryId] ?? 0.0;
  }

  Future<void> downloadPlaylist(String categoryId, String categoryName, List<AudioModel> songs) async {
    if (songs.isEmpty || isDownloading(categoryId)) return;

    _downloadProgress[categoryId] = 0.0;
    notifyListeners();

    try {
      final dir = await getApplicationDocumentsDirectory();
      
      // Download playlist image (from the first song's imageUrl, as they usually share it)
      if (songs.isNotEmpty && songs[0].imageUrl != null && songs[0].imageUrl!.isNotEmpty) {
        try {
          final imgUri = Uri.parse(songs[0].imageUrl!);
          final imgResponse = await http.get(imgUri);
          if (imgResponse.statusCode == 200) {
            final imgFile = File('${dir.path}/playlist_$categoryId.jpg');
            await imgFile.writeAsBytes(imgResponse.bodyBytes);
          }
        } catch (e) {
          debugPrint('Error downloading playlist image: $e');
        }
      }

      int completed = 0;
      for (int i = 0; i < songs.length; i++) {
        final song = songs[i];
        if (song.songId == null || song.audioUrl == null || song.audioUrl!.isEmpty) {
          completed++;
          continue;
        }

        // Check if already downloaded
        final isDownloaded = await DatabaseService().isDownloaded(song.songId!);
        if (isDownloaded) {
          completed++;
          _downloadProgress[categoryId] = completed / songs.length;
          notifyListeners();
          continue;
        }

        // Download Audio
        try {
          // Download audio
          final uri = Uri.parse(song.audioUrl!);
          final response = await http.get(uri);
          
          if (response.statusCode == 200) {
            // Force the category name so it saves correctly in DB
            song.categoryName = categoryName;

            final file = File('${dir.path}/song_${song.songId}.mp3');
            await file.writeAsBytes(response.bodyBytes);
            
            // Also download the individual song image if it exists
            if (song.imageUrl != null && song.imageUrl!.isNotEmpty) {
              try {
                final imgUri = Uri.parse(song.imageUrl!);
                final imgResponse = await http.get(imgUri);
                if (imgResponse.statusCode == 200) {
                  final imgFile = File('${dir.path}/song_${song.songId}.jpg');
                  await imgFile.writeAsBytes(imgResponse.bodyBytes);
                }
              } catch (e) {
                debugPrint('Failed to download song image ${song.songId}: $e');
              }
            }
            
            // Save to DB
            await DatabaseService().insertDownload(song, file.path);
          }
        } catch (e) {
          debugPrint('Error downloading song ${song.songId}: $e');
        }

        completed++;
        _downloadProgress[categoryId] = completed / songs.length;
        notifyListeners();
      }
    } catch (e) {
      debugPrint('Error in downloadPlaylist: $e');
    } finally {
      // Small delay so UI can show 100% before removing
      await Future.delayed(const Duration(seconds: 1));
      _downloadProgress.remove(categoryId);
      notifyListeners();
    }
  }

  Future<void> downloadSingleSong(AudioModel song) async {
    if (song.songId == null || song.audioUrl == null || song.audioUrl!.isEmpty) return;

    final isDownloaded = await DatabaseService().isDownloaded(song.songId!);
    if (isDownloaded) return; // Already downloaded

    try {
      final dir = await getApplicationDocumentsDirectory();
      
      // Download audio
      final uri = Uri.parse(song.audioUrl!);
      final response = await http.get(uri);
      
      if (response.statusCode == 200) {
        final file = File('${dir.path}/song_${song.songId}.mp3');
        await file.writeAsBytes(response.bodyBytes);
        
        // Also download image if it exists
        if (song.imageUrl != null && song.imageUrl!.isNotEmpty) {
          try {
            final imgUri = Uri.parse(song.imageUrl!);
            final imgResponse = await http.get(imgUri);
            if (imgResponse.statusCode == 200) {
              final imgFile = File('${dir.path}/song_${song.songId}.jpg');
              await imgFile.writeAsBytes(imgResponse.bodyBytes);
            }
          } catch (e) {
            debugPrint('Failed to download song image ${song.songId}: $e');
          }
        }
        
        // Save to DB (as a generic download without forcing category)
        await DatabaseService().insertDownload(song, file.path, isSingle: true);
      }
    } catch (e) {
      debugPrint('Error downloading single song ${song.songId}: $e');
    }
  }

  Future<void> removeSingleSong(AudioModel song) async {
    if (song.songId == null) return;

    try {
      final dir = await getApplicationDocumentsDirectory();
      
      final audioFile = File('${dir.path}/song_${song.songId}.mp3');
      if (audioFile.existsSync()) {
        await audioFile.delete();
      }

      final imgFile = File('${dir.path}/song_${song.songId}.jpg');
      if (imgFile.existsSync()) {
        await imgFile.delete();
      }

      await DatabaseService().removeDownload(song.songId!);
    } catch (e) {
      debugPrint('Error removing single song ${song.songId}: $e');
    }
  }
}
