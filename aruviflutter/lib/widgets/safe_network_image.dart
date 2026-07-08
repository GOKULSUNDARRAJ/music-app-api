import 'package:flutter/material.dart';
import 'package:cached_network_image/cached_network_image.dart';

/// A safe network image widget that:
/// - Uses CachedNetworkImage to avoid repeated downloads
/// - Gracefully handles AVIF/WebP decode failures on Android < 12
/// - Shows a placeholder while loading and a fallback icon on error
class SafeNetworkImage extends StatelessWidget {
  final String? url;
  final double? width;
  final double? height;
  final BoxFit fit;
  final Widget? placeholder;
  final BorderRadius? borderRadius;
  final int? memCacheWidth;
  final int? memCacheHeight;

  const SafeNetworkImage({
    Key? key,
    required this.url,
    this.width,
    this.height,
    this.fit = BoxFit.cover,
    this.placeholder,
    this.borderRadius,
    this.memCacheWidth,
    this.memCacheHeight,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    final Widget fallback = placeholder ??
        Container(
          width: width,
          height: height,
          color: const Color(0xFF2B2B2B),
          child: const Icon(Icons.music_note, color: Colors.white54, size: 32),
        );

    if (url == null || url!.isEmpty) return _wrap(fallback);

    final Widget image = CachedNetworkImage(
      imageUrl: url!,
      width: width,
      height: height,
      fit: fit,
      memCacheWidth: memCacheWidth,
      memCacheHeight: memCacheHeight,
      placeholder: (context, url) => _wrap(
        Container(
          width: width,
          height: height,
          color: const Color(0xFF2B2B2B),
          child: const Center(
            child: SizedBox(
              width: 20,
              height: 20,
              child: CircularProgressIndicator(
                strokeWidth: 2,
                color: Color(0xFFEB1C24),
              ),
            ),
          ),
        ),
      ),
      errorWidget: (context, url, error) {
        debugPrint('SafeNetworkImage: Failed to load $url — $error');
        return _wrap(fallback);
      },
    );

    return _wrap(image);
  }

  Widget _wrap(Widget child) {
    if (borderRadius != null) {
      return ClipRRect(borderRadius: borderRadius!, child: child);
    }
    return child;
  }
}
