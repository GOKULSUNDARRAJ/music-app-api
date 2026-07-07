import 'package:flutter/material.dart';
import 'package:palette_generator/palette_generator.dart';
import 'package:cached_network_image/cached_network_image.dart';
import 'models/audio_model.dart';
import 'services/audio_service.dart';
import 'widgets/song_options_sheet.dart';
import 'widgets/sleep_timer_sheet.dart';
import 'widgets/bluetooth_devices_sheet.dart';
import 'widgets/lyrics_view.dart';
import 'services/bluetooth_service.dart';

class BottomSheetPlayer extends StatefulWidget {
  final AudioModel currentSong;
  
  const BottomSheetPlayer({super.key, required this.currentSong});

  @override
  State<BottomSheetPlayer> createState() => _BottomSheetPlayerState();
}

class _BottomSheetPlayerState extends State<BottomSheetPlayer> {
  Color _dominantColor = const Color(0xFF1E1E1E);
  bool _showLyrics = false;
  
  @override
  void initState() {
    super.initState();
    _updatePalette();
  }
  
  Future<void> _updatePalette() async {
    if (widget.currentSong.imageUrl == null || widget.currentSong.imageUrl!.isEmpty) return;

    try {
      final PaletteGenerator generator = await PaletteGenerator.fromImageProvider(
        CachedNetworkImageProvider(widget.currentSong.imageUrl!),
        maximumColorCount: 10,
      );
      
      if (generator.dominantColor != null && mounted) {
        setState(() {
          _dominantColor = generator.dominantColor!.color;
        });
      }
    } catch (e) {
      debugPrint('Failed to extract palette: $e');
    }
  }

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: AudioService(),
      builder: (context, child) {
        final audioService = AudioService();
        final song = audioService.currentSong ?? widget.currentSong;
        
        final duration = audioService.duration;
        final position = audioService.position;
        
        // Reset lyrics view when song changes
        final hasLyrics = song.lyrics != null && song.lyrics!.trim().isNotEmpty;
        if (!hasLyrics && _showLyrics) {
          WidgetsBinding.instance.addPostFrameCallback((_) {
            if (mounted) setState(() => _showLyrics = false);
          });
        }

        double sliderVal = 0.0;
        double secondaryVal = 0.0;
        if (duration.inMilliseconds > 0) {
          sliderVal = position.inMilliseconds / duration.inMilliseconds;
          if (sliderVal > 1.0) sliderVal = 1.0;
          
          if (audioService.isClipModeActive) {
            final clipEndMs = audioService.clipStartPosition.inMilliseconds + audioService.clipDuration.inMilliseconds;
            secondaryVal = clipEndMs / duration.inMilliseconds;
          } else {
            secondaryVal = audioService.bufferedPosition.inMilliseconds / duration.inMilliseconds;
          }
          if (secondaryVal > 1.0) secondaryVal = 1.0;
        }

        final backgroundGradient = LinearGradient(
          colors: [
            _dominantColor.withValues(alpha: 1.0),
            Color.lerp(_dominantColor, const Color(0xFF121212), 0.5)!.withValues(alpha: 1.0),
            const Color(0xFF121212),
          ],
          begin: Alignment.topCenter,
          end: Alignment.bottomCenter,
          stops: const [0.0, 0.5, 1.0],
        );

        return Container(
          height: MediaQuery.of(context).size.height,
          decoration: BoxDecoration(gradient: backgroundGradient),
          padding: const EdgeInsets.only(top: 40, left: 16, right: 16, bottom: 20),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // 1. Top Bar: Close + Lyrics Toggle
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  IconButton(
                    icon: const Icon(Icons.close, color: Colors.white, size: 28),
                    padding: EdgeInsets.zero,
                    onPressed: () => Navigator.pop(context),
                  ),
                  if (hasLyrics)
                    GestureDetector(
                      onTap: () => setState(() => _showLyrics = !_showLyrics),
                      child: Container(
                        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 6),
                        decoration: BoxDecoration(
                          color: _showLyrics
                              ? const Color(0xFFEB1C24)
                              : Colors.white.withValues(alpha: 0.15),
                          borderRadius: BorderRadius.circular(20),
                          border: Border.all(
                            color: _showLyrics
                                ? const Color(0xFFEB1C24)
                                : Colors.white.withValues(alpha: 0.3),
                            width: 1,
                          ),
                        ),
                        child: Row(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            Icon(
                              Icons.lyrics_outlined,
                              color: _showLyrics ? Colors.white : Colors.white70,
                              size: 16,
                            ),
                            const SizedBox(width: 6),
                            Text(
                              'LYRICS',
                              style: TextStyle(
                                color: _showLyrics ? Colors.white : Colors.white70,
                                fontSize: 11,
                                fontWeight: FontWeight.bold,
                                letterSpacing: 1.2,
                              ),
                            ),
                          ],
                        ),
                      ),
                    ),
                ],
              ),

              // 2. Main area: Album Art OR Lyrics
              Expanded(
                child: AnimatedSwitcher(
                  duration: const Duration(milliseconds: 350),
                  transitionBuilder: (child, animation) => FadeTransition(
                    opacity: animation,
                    child: child,
                  ),
                  child: _showLyrics && hasLyrics
                      ? LyricsView(
                          key: ValueKey('lyrics_${song.songId}'),
                          lyrics: song.lyrics!,
                          position: position,
                          duration: duration,
                        )
                      : _buildAlbumArtSection(song, audioService),
                ),
              ),

              // 3. Song Info & Actions Row
              Row(
                crossAxisAlignment: CrossAxisAlignment.end,
                children: [
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          song.audioName ?? 'Unknown Song',
                          style: const TextStyle(
                            color: Colors.white,
                            fontSize: 20,
                            fontWeight: FontWeight.bold,
                          ),
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                        ),
                        const SizedBox(height: 8),
                        Text(
                          AudioService().currentPlaylistName ?? widget.currentSong.categoryName ?? 'Unknown Artist',
                          style: const TextStyle(color: Colors.white70, fontSize: 18),
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                        ),

                        // Bluetooth Device Chip
                        FutureBuilder<Map<String, String>?>(
                          future: BluetoothService().getConnectedDevice(),
                          builder: (context, snapshot) {
                            if (!snapshot.hasData || snapshot.data == null) {
                              return const SizedBox.shrink();
                            }
                            final deviceName = snapshot.data!['name'] ?? 'Connected';
                            return GestureDetector(
                              onTap: () {
                                showModalBottomSheet(
                                  context: context,
                                  backgroundColor: Colors.transparent,
                                  isScrollControlled: true,
                                  builder: (context) => const BluetoothDevicesSheet(),
                                );
                              },
                              child: Container(
                                margin: const EdgeInsets.only(top: 8),
                                padding: const EdgeInsets.symmetric(horizontal: 0, vertical: 4),
                                child: Row(
                                  mainAxisSize: MainAxisSize.min,
                                  children: [
                                    const Icon(Icons.headset, color: Colors.red, size: 16),
                                    const SizedBox(width: 8),
                                    Text(
                                      deviceName,
                                      style: const TextStyle(color: Colors.red, fontSize: 12),
                                    ),
                                  ],
                                ),
                              ),
                            );
                          },
                        ),
                      ],
                    ),
                  ),
                  // Actions (Timer, Clip, Menu)
                  Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      AnimatedBuilder(
                        animation: audioService,
                        builder: (context, child) {
                          final isSleepTimerActive = audioService.isSleepTimerActive;
                          return IconButton(
                            icon: Icon(
                              isSleepTimerActive ? Icons.timer : Icons.access_time,
                              color: isSleepTimerActive ? const Color(0xFFEB1C24) : Colors.white,
                              size: 24,
                            ),
                            onPressed: () {
                              showModalBottomSheet(
                                context: context,
                                backgroundColor: Colors.transparent,
                                builder: (context) => const SleepTimerSheet(),
                              );
                            },
                            padding: EdgeInsets.zero,
                            constraints: const BoxConstraints(),
                          );
                        },
                      ),
                      const SizedBox(width: 16),
                      AnimatedBuilder(
                        animation: audioService,
                        builder: (context, child) {
                          final isClipModeActive = audioService.isClipModeActive;
                          return IconButton(
                            icon: Icon(
                              Icons.timer_outlined,
                              color: isClipModeActive ? const Color(0xFFEB1C24) : Colors.white,
                              size: 24,
                            ),
                            onPressed: () {
                              audioService.toggleClipMode();
                              ScaffoldMessenger.of(context).showSnackBar(
                                SnackBar(
                                  content: Text(audioService.isClipModeActive
                                      ? 'Clip Mode Enabled'
                                      : 'Clip Mode Disabled'),
                                  duration: const Duration(seconds: 2),
                                ),
                              );
                            },
                            padding: EdgeInsets.zero,
                            constraints: const BoxConstraints(),
                          );
                        },
                      ),
                      const SizedBox(width: 16),
                      IconButton(
                        icon: const Icon(Icons.more_vert, color: Colors.white, size: 24),
                        onPressed: () {
                          showModalBottomSheet(
                            context: context,
                            backgroundColor: Colors.transparent,
                            isScrollControlled: true,
                            builder: (context) => SongOptionsSheet(song: song),
                          );
                        },
                        padding: EdgeInsets.zero,
                        constraints: const BoxConstraints(),
                      ),
                    ],
                  ),
                ],
              ),

              const SizedBox(height: 30),

              // 4. Progress Slider
              Stack(
                alignment: Alignment.center,
                children: [
                  SliderTheme(
                    data: SliderThemeData(
                      trackHeight: 2.0,
                      thumbShape: const RoundSliderThumbShape(enabledThumbRadius: 6.0),
                      overlayShape: const RoundSliderOverlayShape(overlayRadius: 14.0),
                      activeTrackColor: const Color(0xFFEB1C24),
                      inactiveTrackColor: const Color(0xFF1E1E1E),
                      secondaryActiveTrackColor: audioService.isClipModeActive
                          ? Colors.green
                          : Colors.grey.shade600,
                      thumbColor: const Color(0xFFEB1C24),
                      overlayColor: const Color(0xFFEB1C24).withValues(alpha: 0.2),
                    ),
                    child: Slider(
                      value: sliderVal,
                      secondaryTrackValue: secondaryVal,
                      onChanged: (value) {
                        final newPosition = Duration(
                          milliseconds: (value * duration.inMilliseconds).round(),
                        );
                        audioService.seek(newPosition);
                      },
                    ),
                  ),
                ],
              ),

              // Time Labels
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 16.0),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Text(_formatDuration(position),
                        style: const TextStyle(color: Colors.white70, fontSize: 12)),
                    Text(_formatDuration(duration),
                        style: const TextStyle(color: Colors.white70, fontSize: 12)),
                  ],
                ),
              ),

              const SizedBox(height: 20),

              // 5. Playback Controls
              Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  IconButton(
                    icon: const Icon(Icons.skip_previous, color: Colors.white, size: 36),
                    onPressed: () => audioService.skipToPrevious(),
                    padding: const EdgeInsets.all(12),
                  ),
                  const SizedBox(width: 24),
                  Container(
                    width: 74,
                    height: 74,
                    decoration: const BoxDecoration(
                      color: Color(0xFFEB1C24),
                      shape: BoxShape.circle,
                    ),
                    child: IconButton(
                      icon: Icon(
                        audioService.isPlaying ? Icons.pause : Icons.play_arrow,
                        color: Colors.white,
                        size: 38,
                      ),
                      onPressed: () => audioService.togglePlayPause(),
                    ),
                  ),
                  const SizedBox(width: 24),
                  IconButton(
                    icon: const Icon(Icons.skip_next, color: Colors.white, size: 36),
                    onPressed: () => audioService.skipToNext(),
                    padding: const EdgeInsets.all(12),
                  ),
                ],
              ),

              const SizedBox(height: 40),
            ],
          ),
        );
      },
    );
  }

  Widget _buildAlbumArtSection(AudioModel song, AudioService audioService) {
    return Column(
      key: const ValueKey('album_art'),
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        Center(
          child: Container(
            width: 300,
            height: 300,
            decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(16),
              boxShadow: [
                BoxShadow(
                  color: Colors.black.withValues(alpha: 0.3),
                  spreadRadius: 2,
                  blurRadius: 10,
                  offset: const Offset(0, 5),
                ),
              ],
            ),
            child: ClipRRect(
              borderRadius: BorderRadius.circular(16),
              child: song.imageUrl != null && song.imageUrl!.isNotEmpty
                  ? CachedNetworkImage(
                      imageUrl: song.imageUrl!,
                      fit: BoxFit.cover,
                      errorWidget: (context, url, error) => _buildPlaceholder(),
                    )
                  : _buildPlaceholder(),
            ),
          ),
        ),
      ],
    );
  }

  String _formatDuration(Duration d) {
    String twoDigits(int n) => n.toString().padLeft(2, "0");
    return "${twoDigits(d.inMinutes.remainder(60))}:${twoDigits(d.inSeconds.remainder(60))}";
  }

  Widget _buildPlaceholder() {
    return Container(
      color: Colors.grey.shade800,
      child: const Center(
        child: Icon(Icons.music_note, size: 100, color: Colors.white24),
      ),
    );
  }
}
