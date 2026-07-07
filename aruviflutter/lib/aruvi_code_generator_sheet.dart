import 'package:flutter/material.dart';

// ──────────────────────────────────────────────
// Wave code generator: same algorithm as Android SpotifyCodeView
// ──────────────────────────────────────────────
class AruviCodeUtils {
  static const int barCount = 23;

  /// Produces the 23-level array for a given ID (same formula as Android).
  static List<int> getLevels(String id) {
    final int seed = javaHashCode(id);
    final levels = List<int>.filled(barCount, 0);
    for (int i = 0; i < barCount; i++) {
      if (i == 0 || i == barCount - 1) {
        levels[i] = 3; // guard bars
      } else {
        levels[i] = ((seed ^ (i * 997)).abs() % 8);
      }
    }
    return levels;
  }

  /// Exact replica of Java's String.hashCode() to ensure compatibility 
  /// with Aruvi Codes generated on the Android platform.
  static int javaHashCode(String s) {
    int hash = 0;
    for (int i = 0; i < s.length; i++) {
      hash = (31 * hash + s.codeUnitAt(i)) & 0xFFFFFFFF;
    }
    if ((hash & 0x80000000) != 0) {
      hash = hash - 0x100000000;
    }
    return hash;
  }

  /// Returns the error score between a scanned pattern and a candidate ID.
  static int matchError(String id, List<int> scanned) {
    final expected = getLevels(id);
    int total = 0;
    for (int i = 0; i < barCount; i++) {
      total += (expected[i] - scanned[i]).abs();
    }
    return total;
  }
}

// ──────────────────────────────────────────────
// Custom wave painter – matches Android SpotifyCodeView exactly
// ──────────────────────────────────────────────
class WaveBarcodePainter extends CustomPainter {
  final String categoryId;
  final Color barColor;

  WaveBarcodePainter({required this.categoryId, this.barColor = Colors.black});

  @override
  void paint(Canvas canvas, Size size) {
    final levels = AruviCodeUtils.getLevels(categoryId);
    final paint = Paint()
      ..color = barColor
      ..style = PaintingStyle.fill
      ..isAntiAlias = true;

    const int count = AruviCodeUtils.barCount;
    final double barWidth = size.width / (count * 1.8);
    final double spacing = barWidth * 0.8;
    final double totalWidth = (count * barWidth) + ((count - 1) * spacing);
    final double startX = (size.width - totalWidth) / 2;
    final double centerY = size.height / 2;

    for (int i = 0; i < count; i++) {
      final double x = startX + i * (barWidth + spacing);
      final double heightFactor = 0.2 + (levels[i] * 0.1);
      final double barHeight = size.height * heightFactor;

      final rect = RRect.fromRectAndRadius(
        Rect.fromCenter(
          center: Offset(x + barWidth / 2, centerY),
          width: barWidth,
          height: barHeight,
        ),
        Radius.circular(barWidth / 2),
      );

      canvas.drawRRect(rect, paint);
    }
  }

  @override
  bool shouldRepaint(covariant WaveBarcodePainter old) =>
      old.categoryId != categoryId || old.barColor != barColor;
}

// ──────────────────────────────────────────────
// Bottom sheet UI
// ──────────────────────────────────────────────
class AruviCodeGeneratorSheet extends StatelessWidget {
  final String categoryId;
  final String title;
  final String? imageUrl;

  const AruviCodeGeneratorSheet({
    Key? key,
    required this.categoryId,
    required this.title,
    this.imageUrl,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Container(
      color: const Color(0xFF151515),
      child: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(20.0),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.center,
            children: [
              // Drag handle
              Container(
                width: 40,
                height: 4,
                margin: const EdgeInsets.only(bottom: 20),
                decoration: BoxDecoration(
                  color: Colors.grey[700],
                  borderRadius: BorderRadius.circular(2),
                ),
              ),

              // Header
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Expanded(
                    child: Text(
                      title,
                      style: const TextStyle(
                        color: Colors.white,
                        fontSize: 22,
                        fontWeight: FontWeight.bold,
                      ),
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                    ),
                  ),
                  GestureDetector(
                    onTap: () => Navigator.pop(context),
                    child: Container(
                      padding: const EdgeInsets.all(4),
                      decoration: BoxDecoration(
                        shape: BoxShape.circle,
                        border: Border.all(color: Colors.white, width: 1.5),
                      ),
                      child: const Icon(Icons.close, color: Colors.white, size: 20),
                    ),
                  ),
                ],
              ),

              const SizedBox(height: 30),

              // Card: album art + red wave banner
              Expanded(
                child: Center(
                  child: AspectRatio(
                    aspectRatio: 0.8,
                    child: ClipRRect(
                      borderRadius: BorderRadius.circular(24),
                      child: Stack(
                        children: [
                          // Album art
                          Positioned.fill(
                            child: imageUrl != null && imageUrl!.isNotEmpty
                                ? Image.network(imageUrl!, fit: BoxFit.cover)
                                : Container(color: Colors.grey[800]),
                          ),

                          // Red wave banner at bottom
                          Positioned(
                            bottom: 0,
                            left: 0,
                            right: 0,
                            height: 80,
                            child: Container(
                              color: const Color(0xFFEB1C24),
                              padding: const EdgeInsets.symmetric(horizontal: 16),
                              child: Row(
                                children: [
                                  // Aruvi logo
                                  Image.asset(
                                    'assets/images/arivumusiclogo.png',
                                    height: 30,
                                    width: 30,
                                    color: Colors.white,
                                    errorBuilder: (_, __, ___) =>
                                        const Icon(Icons.music_note, color: Colors.white, size: 30),
                                  ),
                                  const SizedBox(width: 12),
                                  // Wave barcode
                                  Expanded(
                                    child: CustomPaint(
                                      size: const Size(double.infinity, 50),
                                      painter: WaveBarcodePainter(
                                        categoryId: categoryId,
                                        barColor: Colors.black,
                                      ),
                                    ),
                                  ),
                                ],
                              ),
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                ),
              ),

              const SizedBox(height: 30),

              // Action buttons
              Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  ElevatedButton.icon(
                    onPressed: () {
                      ScaffoldMessenger.of(context).showSnackBar(
                        const SnackBar(content: Text('Save Image coming soon!')),
                      );
                    },
                    icon: const Icon(Icons.download, color: Colors.white, size: 18),
                    label: const Text('Save Image', style: TextStyle(color: Colors.white)),
                    style: ElevatedButton.styleFrom(
                      backgroundColor: const Color(0xFFEB1C24),
                      padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 12),
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(30),
                      ),
                    ),
                  ),
                  const SizedBox(width: 16),
                  ElevatedButton.icon(
                    onPressed: () {
                      ScaffoldMessenger.of(context).showSnackBar(
                        const SnackBar(content: Text('Share coming soon!')),
                      );
                    },
                    icon: const Icon(Icons.reply, color: Colors.white, size: 18),
                    label: const Text('Share', style: TextStyle(color: Colors.white)),
                    style: ElevatedButton.styleFrom(
                      backgroundColor: Colors.grey[800],
                      padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 12),
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(30),
                      ),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 40),
            ],
          ),
        ),
      ),
    );
  }
}
