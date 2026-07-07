import 'package:flutter/material.dart';

class HomeArtistTab extends StatelessWidget {
  const HomeArtistTab({super.key});

  @override
  Widget build(BuildContext context) {
    return const Center(
      child: Text(
        'Artist Tab Placeholder\n(Fetching /api/artist in future)',
        textAlign: TextAlign.center,
        style: TextStyle(color: Colors.white, fontSize: 16),
      ),
    );
  }
}
