import 'package:flutter/material.dart';
import 'package:cached_network_image/cached_network_image.dart';

class MiniPlayer extends StatelessWidget {
  final bool isBluetoothConnected;
  final bool isPlaying;
  final double progress;
  final String songTitle;
  final String artistName;
  final String? imageUrl;
  final VoidCallback onPlayPause;
  final VoidCallback onAdd;
  final VoidCallback onTap;

  const MiniPlayer({
    super.key,
    this.isBluetoothConnected = false,
    this.isPlaying = false,
    this.progress = 0.0,
    this.songTitle = 'Song Name',
    this.artistName = 'Artist',
    this.imageUrl,
    required this.onPlayPause,
    required this.onAdd,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        height: 55, // 54dp
        decoration: BoxDecoration(
          color: const Color(0xFF828993), // gray (Wait, the android layout says background @color/gray for miniaudioplayer)
          borderRadius: BorderRadius.circular(10), // cardMiniPlayer cornerRadius 10dp
        ),
        margin: const EdgeInsets.symmetric(horizontal: 10, vertical: 5), // Added margin to match CardView style
        child: Column(
          children: [
            Expanded(
              child: Row(
                children: [
                  // Album Art
                  Container(
                    width: 42,
                    height: 42,
                    margin: const EdgeInsets.only(left: 10),
                    decoration: BoxDecoration(
                      color: Colors.black,
                      borderRadius: BorderRadius.circular(4),
                      image: DecorationImage(
                        image: imageUrl != null && imageUrl!.isNotEmpty
                            ? CachedNetworkImageProvider(imageUrl!) as ImageProvider
                            : const AssetImage('assets/images/video_placholder.png'),
                        fit: BoxFit.cover,
                        onError: (e, s) => debugPrint('MiniPlayer image error: $e'),
                      ),
                    ),
                    child: imageUrl == null || imageUrl!.isEmpty
                        ? const Icon(Icons.music_note, color: Colors.white54)
                        : null,
                  ),
                  
                  const SizedBox(width: 10),

                  // Song Info
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Text(
                          songTitle,
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          style: const TextStyle(
                            color: Colors.white,
                            fontSize: 16,
                            fontWeight: FontWeight.w500,
                          ),
                        ),
                        Text(
                          artistName,
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          style: const TextStyle(
                            color: Colors.white,
                            fontSize: 14,
                          ),
                        ),
                      ],
                    ),
                  ),

                  // Bluetooth Icon
                  if (isBluetoothConnected)
                    const Padding(
                      padding: EdgeInsets.only(right: 10),
                      child: Icon(
                        Icons.headphones, // closest to headphones1
                        color: Color(0xFFEB1C24), // bgred
                        size: 24,
                      ),
                    ),

                  // Add Button
                  GestureDetector(
                    onTap: onAdd,
                    child: const Padding(
                      padding: EdgeInsets.only(right: 15),
                      child: Icon(
                        Icons.add_circle_outline,
                        color: Colors.white,
                        size: 30,
                      ),
                    ),
                  ),

                  // Play/Pause Button
                  GestureDetector(
                    onTap: onPlayPause,
                    child: Padding(
                      padding: const EdgeInsets.only(right: 15),
                      child: Icon(
                        isPlaying ? Icons.pause_circle_filled : Icons.play_circle_filled,
                        color: Colors.white,
                        size: 35,
                      ),
                    ),
                  ),
                ],
              ),
            ),
            
            // Progress Bar
            LinearProgressIndicator(
              value: progress,
              backgroundColor: const Color(0xFF828993), // gray
              color: const Color(0xFFEB1C24),
              minHeight: 2,
            ),
          ],
        ),
      ),
    );
  }
}
