import 'dart:convert';
import 'package:flutter/foundation.dart';
import 'package:just_audio/just_audio.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:path_provider/path_provider.dart';
import 'dart:io';
import '../models/audio_model.dart';
import 'database_service.dart';

class AudioService extends ChangeNotifier {
  static final AudioService _instance = AudioService._internal();
  factory AudioService() => _instance;
  AudioService._internal() {
    _init();
  }

  final AudioPlayer _player = AudioPlayer();
  
  List<AudioModel> _playlist = [];
  int _currentIndex = -1;
  String? _currentPlaylistName;

  AudioModel? get currentSong => _currentIndex >= 0 && _currentIndex < _playlist.length ? _playlist[_currentIndex] : null;
  String? get currentPlaylistName => _currentPlaylistName;
  bool get isPlaying => _player.playing;
  Duration get position => _player.position;
  Duration get duration => _player.duration ?? Duration.zero;

  void _init() {
    _player.playerStateStream.listen((state) {
      notifyListeners();
    });
    
    _player.positionStream.listen((pos) {
      notifyListeners();
    });

    _player.currentIndexStream.listen((index) {
      if (index != null && index != _currentIndex) {
        _currentIndex = index;
        _saveState();
        notifyListeners();
      }
    });

    _restoreState();
  }

  Future<void> _saveState() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      if (_playlist.isNotEmpty && _currentIndex >= 0 && _currentIndex < _playlist.length) {
        final playlistJson = json.encode(_playlist.map((s) => s.toJson()).toList());
        await prefs.setString('last_playlist', playlistJson);
        await prefs.setInt('last_index', _currentIndex);
        if (_currentPlaylistName != null) {
          await prefs.setString('last_playlist_name', _currentPlaylistName!);
        }
      }
    } catch (e) {
      debugPrint('Error saving audio state: $e');
    }
  }

  Future<void> _restoreState() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final savedPlaylist = prefs.getString('last_playlist');
      final savedIndex = prefs.getInt('last_index') ?? 0;
      final savedName = prefs.getString('last_playlist_name');
      
      if (savedPlaylist != null && savedPlaylist.isNotEmpty) {
        final List<dynamic> parsed = json.decode(savedPlaylist);
        _playlist = parsed.map((json) => AudioModel.fromJson(json)).toList();
        _currentIndex = savedIndex;
        _currentPlaylistName = savedName;
        
        if (_playlist.isNotEmpty && _currentIndex >= 0 && _currentIndex < _playlist.length) {
          final List<AudioSource> audioSources = [];
          final dir = await getApplicationDocumentsDirectory();
          
          for (var song in _playlist) {
            if (song.songId != null) {
              final dbSong = await DatabaseService().getDownload(song.songId!);
              if (dbSong != null) {
                final localFile = File('${dir.path}/song_${song.songId}.mp3');
                if (localFile.existsSync()) {
                  audioSources.add(AudioSource.uri(Uri.file(localFile.path)));
                  continue;
                }
              }
            }
            if (song.audioUrl != null && song.audioUrl!.isNotEmpty) {
              audioSources.add(AudioSource.uri(Uri.parse(song.audioUrl!)));
            } else {
              audioSources.add(AudioSource.uri(Uri.parse('asset:///assets/empty.mp3')));
            }
          }
          final audioSource = ConcatenatingAudioSource(children: audioSources);
          // Set source but don't play
          await _player.setAudioSource(audioSource, initialIndex: _currentIndex, initialPosition: Duration.zero);
          notifyListeners();
        }
      }
    } catch (e) {
      debugPrint('Error restoring audio state: $e');
    }
  }

  Future<void> playSongs(List<AudioModel> songs, {int initialIndex = 0, String? playlistName}) async {
    if (songs.isEmpty) return;
    
    _playlist = songs;
    _currentIndex = initialIndex;
    _currentPlaylistName = playlistName;
    
    try {
      final List<AudioSource> audioSources = [];
      final dir = await getApplicationDocumentsDirectory();
      
      for (var song in songs) {
        if (song.songId != null) {
          final dbSong = await DatabaseService().getDownload(song.songId!);
          if (dbSong != null) {
            final localFile = File('${dir.path}/song_${song.songId}.mp3');
            if (localFile.existsSync()) {
              audioSources.add(AudioSource.uri(Uri.file(localFile.path)));
              continue;
            }
          }
        }
        if (song.audioUrl != null && song.audioUrl!.isNotEmpty) {
          audioSources.add(AudioSource.uri(Uri.parse(song.audioUrl!)));
        } else {
          audioSources.add(AudioSource.uri(Uri.parse('asset:///assets/empty.mp3')));
        }
      }
      final audioSource = ConcatenatingAudioSource(children: audioSources);
      
      await _player.setAudioSource(audioSource, initialIndex: initialIndex, initialPosition: Duration.zero);
      _saveState(); // Save after setting new playlist
      await _player.play();
    } catch (e) {
      debugPrint("Error loading audio source: $e");
    }
    notifyListeners();
  }

  Future<void> play() async {
    await _player.play();
  }

  Future<void> pause() async {
    await _player.pause();
  }
  
  Future<void> togglePlayPause() async {
    if (_player.playing) {
      await _player.pause();
    } else {
      await _player.play();
    }
  }

  Future<void> seek(Duration position) async {
    await _player.seek(position);
  }

  Future<void> skipToNext() async {
    await _player.seekToNext();
  }

  Future<void> skipToPrevious() async {
    await _player.seekToPrevious();
  }
  
  void disposeService() {
    _player.dispose();
  }
}
