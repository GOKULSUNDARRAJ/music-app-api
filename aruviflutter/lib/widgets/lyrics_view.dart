import 'package:flutter/material.dart';

class LyricsView extends StatefulWidget {
  final String lyrics;
  final Duration position;
  final Duration duration;

  const LyricsView({
    super.key,
    required this.lyrics,
    required this.position,
    required this.duration,
  });

  @override
  State<LyricsView> createState() => _LyricsViewState();
}

class _LyricsViewState extends State<LyricsView> {
  final ScrollController _scrollController = ScrollController();
  List<String> _lines = [];
  int _currentLineIndex = 0;

  static const double _itemHeight = 52.0;

  @override
  void initState() {
    super.initState();
    _parseLines();
  }

  void _parseLines() {
    _lines = widget.lyrics
        .split('\n')
        .map((l) => l.trim())
        .toList();
  }

  @override
  void didUpdateWidget(LyricsView oldWidget) {
    super.didUpdateWidget(oldWidget);

    if (oldWidget.lyrics != widget.lyrics) {
      _parseLines();
    }

    // Calculate which line should be active
    if (_lines.isNotEmpty && widget.duration.inMilliseconds > 0) {
      final progress = widget.position.inMilliseconds / widget.duration.inMilliseconds;
      final newIndex = (progress * _lines.length).floor().clamp(0, _lines.length - 1);

      if (newIndex != _currentLineIndex) {
        _currentLineIndex = newIndex;
        _scrollToCurrentLine();
      }
    }
  }

  void _scrollToCurrentLine() {
    if (!_scrollController.hasClients) return;

    final targetOffset = (_currentLineIndex * _itemHeight) -
        (_scrollController.position.viewportDimension / 2) +
        (_itemHeight / 2);

    _scrollController.animateTo(
      targetOffset.clamp(0.0, _scrollController.position.maxScrollExtent),
      duration: const Duration(milliseconds: 400),
      curve: Curves.easeInOut,
    );
  }

  @override
  void dispose() {
    _scrollController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    if (_lines.isEmpty) {
      return const Center(
        child: Text(
          'No lyrics available',
          style: TextStyle(color: Colors.white38, fontSize: 16),
        ),
      );
    }

    return ListView.builder(
      controller: _scrollController,
      padding: const EdgeInsets.symmetric(vertical: 80, horizontal: 24),
      itemCount: _lines.length,
      itemBuilder: (context, index) {
        final isActive = index == _currentLineIndex;
        final isSurrounding = (index - _currentLineIndex).abs() <= 1;
        final isSection = _lines[index].startsWith('Male Part') ||
            _lines[index].startsWith('Female Part') ||
            _lines[index].startsWith('Chorus') ||
            _lines[index].isEmpty;

        if (_lines[index].isEmpty) {
          return const SizedBox(height: 16);
        }

        if (isSection) {
          return Padding(
            padding: const EdgeInsets.symmetric(vertical: 8),
            child: Text(
              _lines[index],
              style: TextStyle(
                color: Colors.white.withValues(alpha: 0.35),
                fontSize: 11,
                fontWeight: FontWeight.w600,
                letterSpacing: 1.5,
              ),
              textAlign: TextAlign.center,
            ),
          );
        }

        return AnimatedDefaultTextStyle(
          duration: const Duration(milliseconds: 300),
          style: TextStyle(
            color: isActive
                ? const Color(0xFFEB1C24)
                : isSurrounding
                    ? Colors.white.withValues(alpha: 0.6)
                    : Colors.white.withValues(alpha: 0.25),
            fontSize: isActive ? 20 : 16,
            fontWeight: isActive ? FontWeight.bold : FontWeight.w400,
            height: 1.5,
          ),
          child: Padding(
            padding: const EdgeInsets.symmetric(vertical: 8),
            child: Text(
              _lines[index],
              textAlign: TextAlign.center,
            ),
          ),
        );
      },
    );
  }
}
