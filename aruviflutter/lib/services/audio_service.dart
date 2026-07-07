import 'dart:async';
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

  // Sleep Timer State
  Timer? _sleepTimer;
  DateTime? _sleepTimerEndTime;
  bool get isSleepTimerActive => _sleepTimer != null && _sleepTimer!.isActive;
  Duration? get sleepTimerRemaining => _sleepTimerEndTime?.difference(DateTime.now());

  // Clip Mode State
  bool _isClipModeActive = false;
  Duration _clipStartPosition = Duration.zero;
  final Duration _clipDuration = const Duration(seconds: 30);
  bool get isClipModeActive => _isClipModeActive;
  Duration get clipStartPosition => _clipStartPosition;
  Duration get clipDuration => _clipDuration;

  void _init() {
    _player.playerStateStream.listen((state) {
      notifyListeners();
    });
    
    int lastSavedPositionInSeconds = 0;

    _player.positionStream.listen((pos) {
      final seconds = pos.inSeconds;
      // Save position every 5 seconds to avoid spamming SharedPreferences
      if ((seconds - lastSavedPositionInSeconds).abs() >= 5) {
        lastSavedPositionInSeconds = seconds;
        SharedPreferences.getInstance().then((prefs) {
          prefs.setInt('last_position', seconds);
        });
      }
      
      // Clip Mode Logic
      if (_isClipModeActive) {
        if (pos >= _clipStartPosition + _clipDuration) {
          _player.seek(_clipStartPosition);
        }
      }

      notifyListeners();
    });

    _player.currentIndexStream.listen((index) {
      if (index != null && index != _currentIndex) {
        _currentIndex = index;
        lastSavedPositionInSeconds = 0; // Reset saved position for new song
        SharedPreferences.getInstance().then((prefs) {
          prefs.setInt('last_position', 0);
        });
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
      final savedPositionSeconds = prefs.getInt('last_position') ?? 0;
      
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
          // Set source but don't play, preserving the last known position
          await _player.setAudioSource(audioSource, initialIndex: _currentIndex, initialPosition: Duration(seconds: savedPositionSeconds));
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
    final prefs = await SharedPreferences.getInstance();
    await prefs.setInt('last_position', _player.position.inSeconds);
  }
  
  Future<void> togglePlayPause() async {
    if (_player.playing) {
      await _player.pause();
      final prefs = await SharedPreferences.getInstance();
      await prefs.setInt('last_position', _player.position.inSeconds);
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
    _sleepTimer?.cancel();
    _player.dispose();
  }

  // Sleep Timer Actions
  void startSleepTimer(Duration duration) {
    _sleepTimer?.cancel();
    _sleepTimerEndTime = DateTime.now().add(duration);
    _sleepTimer = Timer(duration, () {
      pause();
      _sleepTimer = null;
      _sleepTimerEndTime = null;
      notifyListeners();
    });
    notifyListeners();
  }

  void cancelSleepTimer() {
    _sleepTimer?.cancel();
    _sleepTimer = null;
    _sleepTimerEndTime = null;
    notifyListeners();
  }

  // Clip Mode Actions
  void toggleClipMode() {
    _isClipModeActive = !_isClipModeActive;
    if (_isClipModeActive) {
      _clipStartPosition = _player.position;
    }
    notifyListeners();
  }
}
