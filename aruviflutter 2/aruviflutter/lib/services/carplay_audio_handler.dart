import 'package:audio_service/audio_service.dart';
import 'package:just_audio/just_audio.dart';
import 'audio_service.dart' as app_audio;
import 'database_service.dart';
import 'dart:async';
import 'package:flutter/foundation.dart';

class CarPlayAudioHandler extends BaseAudioHandler with QueueHandler, SeekHandler {
  final app_audio.AudioService _appAudioService;
  late final AudioPlayer _player;
  
  bool _initialized = false;

  CarPlayAudioHandler(this._appAudioService) {
    _player = _appAudioService.player;
    _init();
  }

  void _init() {
    if (_initialized) return;
    _initialized = true;

    // Broadcast playback state changes from just_audio
    _player.playbackEventStream.listen(_broadcastState);
    _player.playerStateStream.listen((_) => _broadcastState(_player.playbackEvent));
    
    // Broadcast current media item changes
    _player.sequenceStateStream.listen((sequenceState) {
      if (sequenceState == null) return;
      final currentItem = sequenceState.currentSource;
      if (currentItem != null && currentItem.tag is MediaItem) {
        mediaItem.add(currentItem.tag as MediaItem);
      }
    });

    // Update queue when appAudioService updates it
    _appAudioService.addListener(_updateQueue);
    _updateQueue();
  }

  void _updateQueue() {
    final queueItems = _appAudioService.queue.map((song) => MediaItem(
      id: song.songId ?? '',
      album: song.categoryName ?? 'Unknown Album',
      title: song.audioName ?? 'Unknown Title',
      artUri: song.imageUrl != null ? Uri.parse(song.imageUrl!) : null,
      extras: {'url': song.audioUrl},
    )).toList();
    queue.add(queueItems);
  }

  void _broadcastState(PlaybackEvent event) {
    final playing = _player.playing;
    playbackState.add(playbackState.value.copyWith(
      controls: [
        MediaControl.skipToPrevious,
        if (playing) MediaControl.pause else MediaControl.play,
        MediaControl.stop,
        MediaControl.skipToNext,
      ],
      systemActions: const {
        MediaAction.seek,
        MediaAction.seekForward,
        MediaAction.seekBackward,
      },
      androidCompactActionIndices: const [0, 1, 3],
      processingState: const {
        ProcessingState.idle: AudioProcessingState.idle,
        ProcessingState.loading: AudioProcessingState.loading,
        ProcessingState.buffering: AudioProcessingState.buffering,
        ProcessingState.ready: AudioProcessingState.ready,
        ProcessingState.completed: AudioProcessingState.completed,
      }[_player.processingState] ?? AudioProcessingState.idle,
      playing: playing,
      updatePosition: _player.position,
      bufferedPosition: _player.bufferedPosition,
      speed: _player.speed,
      queueIndex: event.currentIndex,
    ));
  }

  @override
  Future<void> play() => _appAudioService.play();

  @override
  Future<void> pause() => _appAudioService.pause();

  @override
  Future<void> seek(Duration position) => _player.seek(position);

  @override
  Future<void> stop() => _appAudioService.pause();

  @override
  Future<void> skipToNext() => _appAudioService.skipToNext();

  @override
  Future<void> skipToPrevious() => _appAudioService.skipToPrevious();

  // Implement getChildren for CarPlay/Android Auto browsable menu
  @override
  Future<List<MediaItem>> getChildren(String parentMediaId, [Map<String, dynamic>? options]) async {
    try {
      if (parentMediaId == AudioService.browsableRootId) {
        // Root Menu
        return [
          const MediaItem(
            id: 'recent',
            title: 'Recently Played',
            playable: false,
          ),
          const MediaItem(
            id: 'playlists',
            title: 'My Playlists',
            playable: false,
          ),
          const MediaItem(
            id: 'favorites',
            title: 'Favorites',
            playable: false,
          ),
        ];
      } else if (parentMediaId == 'recent') {
        // Fetch recently played songs
        final recentSongs = await DatabaseService().getRecentlyPlayedSongs();
        return recentSongs.map((song) => MediaItem(
          id: 'play_recent_${song.songId}',
          title: song.audioName ?? 'Unknown Title',
          album: song.categoryName ?? 'Recent',
          artUri: song.imageUrl != null ? Uri.parse(song.imageUrl!) : null,
          playable: true,
        )).toList();
      } else if (parentMediaId == 'favorites') {
        // Fetch liked songs
        final favoriteSongs = await DatabaseService().getLikedSongs();
        return favoriteSongs.map((song) => MediaItem(
          id: 'play_favorite_${song.songId}',
          title: song.audioName ?? 'Unknown Title',
          album: song.categoryName ?? 'Favorites',
          artUri: song.imageUrl != null ? Uri.parse(song.imageUrl!) : null,
          playable: true, // User taps to play
        )).toList();
      } else if (parentMediaId == 'playlists') {
        // Fetch saved playlists
        final localPlaylists = await DatabaseService().getCustomPlaylists();
        return localPlaylists.map((p) => MediaItem(
          id: 'playlist_${p['id']}',
          title: p['name'] ?? 'Playlist',
          playable: false,
        )).toList();
      } else if (parentMediaId.startsWith('playlist_')) {
        // Fetch songs for a specific playlist
        final pId = parentMediaId.replaceFirst('playlist_', '');
        final songs = await DatabaseService().getCustomPlaylistSongs(pId);
        return songs.map((song) => MediaItem(
          id: 'play_pl_${song.songId}',
          title: song.audioName ?? 'Unknown Title',
          album: song.categoryName,
          artUri: song.imageUrl != null ? Uri.parse(song.imageUrl!) : null,
          playable: true,
        )).toList();
      }
      return [];
    } catch (e) {
      debugPrint('CarPlay getChildren Error: $e');
      return [];
    }
  }

  @override
  Future<void> playFromMediaId(String mediaId, [Map<String, dynamic>? extras]) async {
    // Tapped a playable item in CarPlay
    try {
      if (mediaId.startsWith('play_favorite_')) {
        final favoriteSongs = await DatabaseService().getLikedSongs();
        final songId = mediaId.replaceFirst('play_favorite_', '');
        final index = favoriteSongs.indexWhere((s) => s.songId == songId);
        if (index >= 0) {
          await _appAudioService.playSongs(favoriteSongs, initialIndex: index, playlistName: 'Favorites');
        }
      } else if (mediaId.startsWith('play_recent_')) {
        final recentSongs = await DatabaseService().getRecentlyPlayedSongs();
        final songId = mediaId.replaceFirst('play_recent_', '');
        final index = recentSongs.indexWhere((s) => s.songId == songId);
        if (index >= 0) {
          await _appAudioService.playSongs(recentSongs, initialIndex: index, playlistName: 'Recently Played');
        }
      } else if (mediaId.startsWith('play_pl_')) {
        // We might just load the whole queue from favorites if we clicked it.
      }
    } catch (e) {
      debugPrint('CarPlay playFromMediaId Error: $e');
    }
  }
}
