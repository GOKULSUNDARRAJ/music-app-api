import 'dart:async';
import 'dart:convert';
import 'package:video_player/video_player.dart';
import 'package:flutter/foundation.dart';
import 'package:http/http.dart' as http;
import 'package:just_audio/just_audio.dart';
import 'package:audio_service/audio_service.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:path_provider/path_provider.dart';
import 'dart:io';
import '../models/audio_model.dart';
import '../models/ad_model.dart';
import 'database_service.dart';

class AudioService extends ChangeNotifier {
  static final AudioService _instance = AudioService._internal();
  factory AudioService() => _instance;
  AudioService._internal() {
    _init();
  }

  final AudioPlayer _player = AudioPlayer();
  AudioPlayer get player => _player;
  VideoPlayerController? _adPlayer; // Secondary player for ads
  ConcatenatingAudioSource? _audioSource;
  
  List<AudioModel> _playlist = [];
  int _currentIndex = -1;
  String? _currentPlaylistName;
  int _queuedCount = 0;
  
  int get queuedCount => _queuedCount;
  List<AudioModel> get queue {
    if (_currentIndex < 0 || _playlist.isEmpty || _queuedCount == 0) return [];
    final startIndex = _currentIndex + 1;
    final endIndex = startIndex + _queuedCount;
    if (startIndex >= _playlist.length) return [];
    return _playlist.sublist(startIndex, endIndex > _playlist.length ? _playlist.length : endIndex);
  }
  
  List<AudioModel> get upcomingPlaylist {
    if (_currentIndex < 0 || _playlist.isEmpty) return [];
    final startIndex = _currentIndex + 1 + _queuedCount;
    if (startIndex >= _playlist.length) return [];
    return _playlist.sublist(startIndex);
  }
  
  AdModel? _currentAd;
  bool isAdPlaying = false;
  
  bool _isAdPrefetching = false;
  bool _adPrefetchedAndReady = false;
  bool _isCheckingForAd = false;
  AdModel? _prefetchedAd;
  VideoPlayerController? _prefetchedAdController;
  
  AdModel? get currentAd => _currentAd;
  VideoPlayerController? get adVideoController => _adPlayer;

  AudioModel? get currentSong => _currentIndex >= 0 && _currentIndex < _playlist.length ? _playlist[_currentIndex] : null;
  String? get currentPlaylistName => _currentPlaylistName;
  bool get isPlaying => isAdPlaying ? (_adPlayer?.value.isPlaying ?? false) : _player.playing;
  Duration get position => isAdPlaying ? (_adPlayer?.value.position ?? Duration.zero) : _player.position;
  Duration get duration => isAdPlaying ? (_adPlayer?.value.duration ?? Duration.zero) : (_player.duration ?? Duration.zero);
  Duration get bufferedPosition => isAdPlaying ? (_adPlayer != null && _adPlayer!.value.buffered.isNotEmpty ? _adPlayer!.value.buffered.last.end : Duration.zero) : _player.bufferedPosition;

  // Sleep Timer State
  Timer? _sleepTimer;
  DateTime? _sleepTimerEndTime;
  bool get isSleepTimerActive => _sleepTimer != null && _sleepTimer!.isActive;
  Duration? get sleepTimerRemaining => _sleepTimerEndTime?.difference(DateTime.now());

  // Clip Mode State
  bool _isClipModeActive = false;
  Duration _clipStartPosition = Duration.zero;
  Duration _clipDuration = const Duration(seconds: 30);
  bool get isClipModeActive => _isClipModeActive;
  Duration get clipStartPosition => _clipStartPosition;
  Duration get clipDuration => _clipDuration;

  Future<void> setClipDuration(Duration duration) async {
    _clipDuration = duration;
    notifyListeners();
    final prefs = await SharedPreferences.getInstance();
    await prefs.setInt('clip_duration_seconds', duration.inSeconds);
  }

  void _init() {
    SharedPreferences.getInstance().then((prefs) {
      final savedClipDuration = prefs.getInt('clip_duration_seconds');
      if (savedClipDuration != null) {
        _clipDuration = Duration(seconds: savedClipDuration);
        notifyListeners();
      }
    });

    _player.playerStateStream.listen((state) {
      if (state.processingState == ProcessingState.completed) {
        if (_isClipModeActive && _currentIndex < _playlist.length - 1) {
          _player.seekToNext();
        }
      }
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

      // Ad prefetching: 5 seconds before song ends
      if (_player.duration != null && !_isClipModeActive && !isAdPlaying && _player.playing) {
        if (currentSong == null || !currentSong!.isDownloaded) {
          if (_player.duration!.inSeconds - seconds <= 5) {
            _prefetchAd();
          }
        }
      }

      notifyListeners();
    });

    _player.bufferedPositionStream.listen((_) {
      notifyListeners();
    });

    _player.currentIndexStream.listen((index) {
      if (index != null && index != _currentIndex) {
        if (_currentIndex != -1) {
          _checkForAd();
        }
        
        // Queue cleanup logic
        if (index == _currentIndex + 1) {
          if (_queuedCount > 0) _queuedCount--;
        } else {
          _queuedCount = 0;
        }
        
        _currentIndex = index;
        _isClipModeActive = false; // Reset clip mode when song changes
        lastSavedPositionInSeconds = 0; // Reset saved position for new song
        SharedPreferences.getInstance().then((prefs) {
          prefs.setInt('last_position', 0);
        });
        _saveState();
        notifyListeners();
        _refreshCurrentSongLyrics();
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

  AudioSource _createAudioSource(AudioModel song, {String? localPath}) {
    final mediaItem = MediaItem(
      id: song.songId?.toString() ?? song.audioName ?? 'unknown',
      album: song.categoryName ?? 'Unknown Album',
      title: song.audioName ?? 'Unknown Title',
      artUri: song.imageUrl != null ? Uri.parse(song.imageUrl!) : null,
    );

    if (localPath != null) {
      return AudioSource.uri(Uri.file(localPath), tag: mediaItem);
    } else if (song.audioUrl != null && song.audioUrl!.isNotEmpty) {
      return AudioSource.uri(Uri.parse(song.audioUrl!), tag: mediaItem);
    } else {
      return AudioSource.uri(Uri.parse('asset:///assets/empty.mp3'), tag: mediaItem);
    }
  }

  Future<void> _restoreState() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      
      _isClipModeActive = prefs.getBool('clip_mode_active') ?? false;
      final clipStartSeconds = prefs.getInt('clip_start_position') ?? 0;
      final clipDurationSeconds = prefs.getInt('clip_duration') ?? 30;
      
      _clipStartPosition = Duration(seconds: clipStartSeconds);
      _clipDuration = Duration(seconds: clipDurationSeconds);

      final playlistJson = prefs.getString('last_playlist');
      final currentIndex = prefs.getInt('last_index');
      final savedPositionSeconds = prefs.getInt('last_position') ?? 0;
      _currentPlaylistName = prefs.getString('last_playlist_name');

      if (playlistJson != null && currentIndex != null) {
        final List<dynamic> decodedList = jsonDecode(playlistJson);
        final List<AudioModel> songs = decodedList.map((item) => AudioModel.fromJson(item)).toList();
        
        if (songs.isNotEmpty) {
          _playlist = songs;
          _currentIndex = currentIndex;
          
          final List<AudioSource> audioSources = [];
          final dir = await getApplicationDocumentsDirectory();
          
          for (var song in songs) {
            String? localPath;
            if (song.songId != null) {
              final dbSong = await DatabaseService().getDownload(song.songId!);
              if (dbSong != null) {
                final localFile = File('${dir.path}/song_${song.songId}.mp3');
                if (localFile.existsSync()) {
                  localPath = localFile.path;
                }
              }
            }
            audioSources.add(_createAudioSource(song, localPath: localPath));
          }
          _audioSource = ConcatenatingAudioSource(children: audioSources);
          _queuedCount = 0;
          // Set source but don't play, preserving the last known position
          await _player.setAudioSource(_audioSource!, initialIndex: _currentIndex, initialPosition: Duration(seconds: savedPositionSeconds));
          notifyListeners();

          // Fetch fresh lyrics for the current song in the background
          _refreshCurrentSongLyrics();
        }
      }
    } catch (e) {
      debugPrint('Error restoring audio state: $e');
    }
  }

  /// Fetches fresh lyrics for the current song from the server and updates the playlist in memory
  Future<void> _refreshCurrentSongLyrics() async {
    try {
      if (_currentIndex < 0 || _currentIndex >= _playlist.length) return;
      final song = _playlist[_currentIndex];
      if (song.categoryId == null || song.categoryId!.isEmpty) return;

      // categoryId is like "cat_027", extract the numeric part
      final categoryNumericId = song.categoryId!.replaceAll(RegExp(r'[^0-9]'), '');
      if (categoryNumericId.isEmpty) return;

      final freshSongs = await DatabaseService().getCategorySongs(categoryNumericId);
      // Find matching song by songId and grab lyrics
      for (final fresh in freshSongs) {
        if (fresh.songId == song.songId && fresh.lyrics != null) {
          _playlist[_currentIndex] = AudioModel(
            songId: song.songId,
            audioName: song.audioName,
            audioUrl: song.audioUrl,
            categoryName: song.categoryName,
            categoryId: song.categoryId,
            imageUrl: song.imageUrl,
            downloadPath: song.downloadPath,
            isDownloaded: song.isDownloaded,
            fileSize: song.fileSize,
            duration: song.duration,
            durationInMillis: song.durationInMillis,
            playlistId: song.playlistId,
            lyrics: fresh.lyrics,
          );
          notifyListeners();
          break;
        }
      }
    } catch (e) {
      debugPrint('Error refreshing lyrics: $e');
    }
  }

  Future<void> playSongs(List<AudioModel> songs, {int initialIndex = 0, String? playlistName}) async {
    if (songs.isEmpty) return;
    
    _playlist = songs;
    _currentIndex = initialIndex;
    _currentPlaylistName = playlistName;
    _isClipModeActive = false; // Reset clip mode when playing new playlist/song
    
    try {
      final List<AudioSource> audioSources = [];
      final dir = await getApplicationDocumentsDirectory();
      
          for (var song in songs) {
            String? localPath;
            if (song.songId != null) {
              final dbSong = await DatabaseService().getDownload(song.songId!);
              if (dbSong != null) {
                final localFile = File('${dir.path}/song_${song.songId}.mp3');
                if (localFile.existsSync()) {
                  localPath = localFile.path;
                  song.isDownloaded = true;
                }
              }
            }
            audioSources.add(_createAudioSource(song, localPath: localPath));
          }
      _audioSource = ConcatenatingAudioSource(children: audioSources);
      _queuedCount = 0;
      
      // Pause BEFORE setting audio source to prevent 2-sec audio bleed
      await _player.pause();
      await _player.setAudioSource(_audioSource!, initialIndex: initialIndex, initialPosition: Duration.zero);
      _saveState(); // Save after setting new playlist
      
      // Explicitly check for ad when a new playlist/song is selected
      await _checkForAd();
      
      if (!isAdPlaying) {
        await _player.play();
      }
    } catch (e) {
      debugPrint("Error loading audio source: $e");
    }
    notifyListeners();
  }
  
  Future<void> addToQueue(AudioModel song) async {
    if (_audioSource == null || _playlist.isEmpty) {
      // If nothing is playing, just start playing this song
      await playSongs([song]);
      return;
    }

    final insertIndex = _currentIndex + 1 + _queuedCount;
    
    // Create the audio source for the new song
    String? localPath;
    if (song.songId != null) {
      final dbSong = await DatabaseService().getDownload(song.songId!);
      if (dbSong != null) {
        final dir = await getApplicationDocumentsDirectory();
        final localFile = File('${dir.path}/song_${song.songId}.mp3');
        if (localFile.existsSync()) {
          localPath = localFile.path;
          song.isDownloaded = true;
        }
      }
    }
    final source = _createAudioSource(song, localPath: localPath);

    // Insert into lists
    _playlist.insert(insertIndex, song);
    await _audioSource!.insert(insertIndex, source);
    
    _queuedCount++;
    notifyListeners();
    _saveState();
  }

  Future<void> play() async {
    if (isAdPlaying) {
      await _adPlayer?.play();
    } else {
      await _player.play();
    }
  }

  Future<void> pause() async {
    if (isAdPlaying) {
      await _adPlayer?.pause();
    } else {
      await _player.pause();
      final prefs = await SharedPreferences.getInstance();
      await prefs.setInt('last_position', _player.position.inSeconds);
    }
  }
  
  Future<void> togglePlayPause() async {
    if (isAdPlaying) {
      if (_adPlayer?.value.isPlaying ?? false) {
        await _adPlayer?.pause();
      } else {
        await _adPlayer?.play();
      }
    } else {
      if (_player.playing) {
        await _player.pause();
        final prefs = await SharedPreferences.getInstance();
        await prefs.setInt('last_position', _player.position.inSeconds);
      } else {
        await _player.play();
      }
    }
  }

  Future<void> seek(Duration position) async {
    if (isAdPlaying) {
      await _adPlayer?.seekTo(position);
    } else {
      await _player.seek(position);
    }
  }

  Future<void> skipToNext() async {
    if (isAdPlaying) return;
    await _player.pause(); // Pause before seeking to prevent bleed
    await _player.seekToNext();
  }

  Future<void> skipToPrevious() async {
    if (isAdPlaying) return;
    await _player.pause(); // Pause before seeking to prevent bleed
    await _player.seekToPrevious();
  }
  
  void disposeService() {
    _sleepTimer?.cancel();
    _player.dispose();
    _adPlayer?.dispose();
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

  void notifyAdState() {
    notifyListeners();
  }

  void _closeAd() {
    if (!isAdPlaying) return;
    isAdPlaying = false;
    _currentAd = null;
    _adPlayer?.pause();
    _adPlayer?.dispose();
    _adPlayer = null;
    notifyListeners();
    _player.play();
  }

  Future<void> _prefetchAd() async {
    if (_isAdPrefetching || _adPrefetchedAndReady) return;
    _isAdPrefetching = true;
    try {
      final prefs = await SharedPreferences.getInstance();
      final token = prefs.getString('auth_token') ?? '';
      
      final response = await http.get(
        Uri.parse('https://music-app-api-1.onrender.com/api/ad/random'),
        headers: { 'Authorization': 'Bearer $token' },
      );
      if (response.statusCode == 200) {
        final data = jsonDecode(response.body);
        if (data['status'] == true && data['hasAd'] == true && data['response'] != null) {
          _prefetchedAd = AdModel.fromJson(data['response']);
          if (_prefetchedAd!.mediaUrl.isNotEmpty) {
            _prefetchedAdController = VideoPlayerController.networkUrl(Uri.parse(_prefetchedAd!.mediaUrl));
            await _prefetchedAdController!.initialize();
            _adPrefetchedAndReady = true;
          }
        }
      }
    } catch (e) {
      debugPrint('Ad prefetch failed: $e');
    }
    _isAdPrefetching = false;
  }

  Future<void> _checkForAd() async {
    if (_isCheckingForAd) return;
    _isCheckingForAd = true;

    if (currentSong != null && currentSong!.isDownloaded) {
      _isCheckingForAd = false;
      return;
    }

    try {
      await _player.pause();
      
      if (_adPrefetchedAndReady && _prefetchedAdController != null && _prefetchedAdController!.value.isInitialized) {
        // Fast path: use prefetched ad
        _currentAd = _prefetchedAd;
        _adPlayer?.dispose();
        _adPlayer = _prefetchedAdController;
        
        // Reset prefetch state
        _adPrefetchedAndReady = false;
        _prefetchedAd = null;
        _prefetchedAdController = null;
        
        isAdPlaying = true;
        notifyListeners(); 
        
        try {
          _adPlayer!.addListener(() {
            if (isAdPlaying && _adPlayer!.value.isInitialized && _adPlayer!.value.position >= _adPlayer!.value.duration && _adPlayer!.value.duration.inMilliseconds > 0) {
              _closeAd();
            }
            notifyListeners();
          });
          await _adPlayer!.play();
        } catch (e) {
          debugPrint('Ad playback failed (prefetched): $e');
          _closeAd();
        }
        _isCheckingForAd = false;
        return;
      }
      
      // Fallback: fetch ad if prefetching failed or didn't trigger
      final prefs = await SharedPreferences.getInstance();
      final token = prefs.getString('auth_token') ?? '';
      
      final response = await http.get(
        Uri.parse('https://music-app-api-1.onrender.com/api/ad/random'),
        headers: {
          'Authorization': 'Bearer $token',
        },
      );
      if (response.statusCode == 200) {
        final data = jsonDecode(response.body);
        if (data['status'] == true && data['hasAd'] == true && data['response'] != null) {
          _currentAd = AdModel.fromJson(data['response']);
          
          if (_currentAd!.mediaUrl.isNotEmpty) {
            isAdPlaying = true;
            notifyListeners(); // Update UI immediately to show ad
            
            try {
              _adPlayer?.dispose();
              _adPlayer = VideoPlayerController.networkUrl(Uri.parse(_currentAd!.mediaUrl));
              await _adPlayer!.initialize();
              _adPlayer!.addListener(() {
                if (isAdPlaying && _adPlayer!.value.isInitialized && _adPlayer!.value.position >= _adPlayer!.value.duration && _adPlayer!.value.duration.inMilliseconds > 0) {
                  _closeAd();
                }
                notifyListeners();
              });
              await _adPlayer!.play();
              _isCheckingForAd = false;
              return;
            } catch (e) {
              debugPrint('Ad playback failed: $e');
              _closeAd();
              _isCheckingForAd = false;
              return;
            }
          }
        }
      }
      _isCheckingForAd = false;
      _player.play();
    } catch (e) {
      debugPrint('Ad error: $e');
      _isCheckingForAd = false;
      _player.play();
    }
  }
}
