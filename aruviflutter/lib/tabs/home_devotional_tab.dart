import 'package:flutter/material.dart';

class HomeDevotionalTab extends StatelessWidget {
  const HomeDevotionalTab({super.key});

  @override
  Widget build(BuildContext context) {
    return const Center(
      child: Text(
        'Devotional Tab Placeholder\n(Fetching /api/devotional in future)',
        textAlign: TextAlign.center,
        style: TextStyle(color: Colors.white, fontSize: 16),
      ),
    );
  }
}
