import 'package:flutter/material.dart';
import 'package:palette_generator/palette_generator.dart';
import 'package:cached_network_image/cached_network_image.dart';
import 'models/audio_model.dart';
import 'services/audio_service.dart';
import 'widgets/song_options_sheet.dart';

class BottomSheetPlayer extends StatefulWidget {
  final AudioModel currentSong;
  
  const BottomSheetPlayer({super.key, required this.currentSong});

  @override
  State<BottomSheetPlayer> createState() => _BottomSheetPlayerState();
}

class _BottomSheetPlayerState extends State<BottomSheetPlayer> {
  Color _dominantColor = const Color(0xFF1E1E1E); // Fallback color
  double _sliderValue = 0.0;
  
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
        
        // Ensure slider value is between 0 and 1
        double sliderVal = 0.0;
        if (duration.inMilliseconds > 0 && position.inMilliseconds > 0) {
          sliderVal = position.inMilliseconds / duration.inMilliseconds;
          if (sliderVal > 1.0) sliderVal = 1.0;
        }

        // Generate a background gradient that matches Android behavior
        final backgroundGradient = LinearGradient(
          colors: [
            _dominantColor,
            _dominantColor.withOpacity(0.5),
            const Color(0xFF121212), // Dark grey / almost black
          ],
          begin: Alignment.topCenter,
          end: Alignment.bottomCenter,
          stops: const [0.0, 0.5, 1.0],
        );

        return Container(
          height: MediaQuery.of(context).size.height,
          decoration: BoxDecoration(
            gradient: backgroundGradient,
          ),
          padding: const EdgeInsets.only(top: 40, left: 16, right: 16, bottom: 20),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // 1. Close Button (X)
              IconButton(
                icon: const Icon(Icons.close, color: Colors.white, size: 28),
                padding: EdgeInsets.zero,
                alignment: Alignment.centerLeft,
                onPressed: () {
                  Navigator.pop(context);
                },
              ),
              
              const Spacer(flex: 1),
              
              // 2. Album Art
              Center(
                child: Container(
                  width: 300,
                  height: 300,
                  decoration: BoxDecoration(
                    borderRadius: BorderRadius.circular(16),
                    boxShadow: [
                      BoxShadow(
                        color: Colors.black.withOpacity(0.3),
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
              
              const Spacer(flex: 1),
              
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
                          style: const TextStyle(
                            color: Colors.white70,
                            fontSize: 18,
                          ),
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                        ),
                      ],
                    ),
                  ),
                  // Actions (Timer, Stopwatch, Menu)
                  Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      IconButton(
                        icon: const Icon(Icons.access_time, color: Colors.white, size: 24),
                        onPressed: () {},
                        padding: EdgeInsets.zero,
                        constraints: const BoxConstraints(),
                      ),
                      const SizedBox(width: 16),
                      IconButton(
                        icon: const Icon(Icons.timer_outlined, color: Colors.white, size: 24),
                        onPressed: () {},
                        padding: EdgeInsets.zero,
                        constraints: const BoxConstraints(),
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
              SliderTheme(
                data: SliderThemeData(
                  trackHeight: 2.0,
                  thumbShape: const RoundSliderThumbShape(enabledThumbRadius: 6.0),
                  overlayShape: const RoundSliderOverlayShape(overlayRadius: 14.0),
                  activeTrackColor: const Color(0xFFEB1C24),
                  inactiveTrackColor: Colors.grey.shade800,
                  thumbColor: const Color(0xFFEB1C24),
                  overlayColor: const Color(0xFFEB1C24).withOpacity(0.2),
                ),
                child: Slider(
                  value: sliderVal,
                  onChanged: (value) {
                    final newPosition = Duration(
                      milliseconds: (value * duration.inMilliseconds).round(),
                    );
                    audioService.seek(newPosition);
                  },
                ),
              ),
              
              // Time Labels
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 16.0),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Text(
                      _formatDuration(position),
                      style: const TextStyle(color: Colors.white70, fontSize: 12),
                    ),
                    Text(
                      _formatDuration(duration),
                      style: const TextStyle(color: Colors.white70, fontSize: 12),
                    ),
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

  String _formatDuration(Duration d) {
    String twoDigits(int n) => n.toString().padLeft(2, "0");
    String twoDigitMinutes = twoDigits(d.inMinutes.remainder(60));
    String twoDigitSeconds = twoDigits(d.inSeconds.remainder(60));
    return "$twoDigitMinutes:$twoDigitSeconds";
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
