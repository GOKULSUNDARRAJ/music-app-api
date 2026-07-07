import 'package:flutter/material.dart';
import '../services/audio_service.dart';

class SleepTimerSheet extends StatefulWidget {
  const SleepTimerSheet({super.key});

  @override
  State<SleepTimerSheet> createState() => _SleepTimerSheetState();
}

class _SleepTimerSheetState extends State<SleepTimerSheet> {
  final List<int> _timerOptions = [5, 10, 15, 30, 45, 60, 90, 120];

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: const BoxDecoration(
        color: Color(0xFF1E1E1E),
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          const Padding(
            padding: EdgeInsets.symmetric(vertical: 20),
            child: Text(
              'Sleep Timer',
              style: TextStyle(
                color: Colors.white,
                fontSize: 20,
                fontWeight: FontWeight.bold,
              ),
            ),
          ),
          const Divider(color: Colors.white24, height: 1),
          AnimatedBuilder(
            animation: AudioService(),
            builder: (context, child) {
              final isTimerActive = AudioService().isSleepTimerActive;
              return Flexible(
                child: ListView(
                  shrinkWrap: true,
                  children: [
                    if (isTimerActive)
                      ListTile(
                        leading: const Icon(Icons.timer_off, color: Colors.white),
                        title: const Text('Cancel Timer', style: TextStyle(color: Colors.white)),
                        onTap: () {
                          AudioService().cancelSleepTimer();
                          Navigator.pop(context);
                          ScaffoldMessenger.of(context).showSnackBar(
                            const SnackBar(content: Text('Sleep timer cancelled')),
                          );
                        },
                      ),
                    ..._timerOptions.map((minutes) {
                      return ListTile(
                        leading: const Icon(Icons.access_time, color: Colors.white54),
                        title: Text('$minutes minutes', style: const TextStyle(color: Colors.white)),
                        onTap: () {
                          AudioService().startSleepTimer(Duration(minutes: minutes));
                          Navigator.pop(context);
                          ScaffoldMessenger.of(context).showSnackBar(
                            SnackBar(content: Text('Sleep timer set for $minutes minutes')),
                          );
                        },
                      );
                    }),
                  ],
                ),
              );
            },
          ),
          const SizedBox(height: 10),
        ],
      ),
    );
  }
}
