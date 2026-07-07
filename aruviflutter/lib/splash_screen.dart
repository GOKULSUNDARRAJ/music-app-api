import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:http/http.dart' as http;
import 'dart:convert';
import 'onboarding_screen.dart';
import 'sign_up_screen.dart';
import 'main_activity.dart';

class SplashScreen extends StatefulWidget {
  const SplashScreen({super.key});

  @override
  State<SplashScreen> createState() => _SplashScreenState();
}

class _SplashScreenState extends State<SplashScreen> {
  @override
  void initState() {
    super.initState();
    _checkInitialNavigation();
  }

  Future<void> _checkInitialNavigation() async {
    final prefs = await SharedPreferences.getInstance();
    final bool onboardingComplete = prefs.getBool('onboarding_complete') ?? false;
    final String accessToken = prefs.getString('access_token') ?? '';

    // Always show splash for at least 1.5 seconds so user sees the logo
    final minDelay = Future.delayed(const Duration(milliseconds: 1500));

    if (accessToken.isNotEmpty) {
      Future<void> fetchMenu() async {
        try {
          final authHeader = accessToken.startsWith('Bearer ') ? accessToken : 'Bearer $accessToken';
          final response = await http.post(
            Uri.parse('https://music-app-api-1.onrender.com/api/secure/appMenuList'),
            headers: {'Authorization': authHeader},
          ).timeout(const Duration(seconds: 5)); // 5s max — don't block UI

          if (response.statusCode == 200) {
            final data = json.decode(response.body);
            if (data['status'] == true) {
              final topMenuJson = json.encode(data['topMenu']);
              final bottomMenuJson = json.encode(data['bottomMenu']);
              await prefs.setString('top_navigation', topMenuJson);
              await prefs.setString('bottom_navigation', bottomMenuJson);
              debugPrint('Successfully saved app menu lists');
            }
          }
        } catch (e) {
          debugPrint('Error fetching app menu (will use cached if available): $e');
        }
      }

      // Always fire fetch in background; wait for both fetch and min delay to finish
      await Future.wait([fetchMenu(), minDelay]);

      if (!mounted) return;
      Navigator.pushReplacement(context, MaterialPageRoute(builder: (_) => const MainActivity()));
    } else {
      // Wait at least 1.5s so user sees the splash branding
      await minDelay;
      if (!mounted) return;

      if (onboardingComplete) {
        Navigator.pushReplacement(context, MaterialPageRoute(builder: (_) => const SignUpScreen()));
      } else {
        Navigator.pushReplacement(context, MaterialPageRoute(builder: (_) => const OnboardingScreen()));
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.black, // Match home screen background
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            FractionallySizedBox(
              widthFactor: 0.6,
              child: AspectRatio(
                aspectRatio: 1.0,
                child: Image.asset(
                  'assets/images/arivumusiclogo.png',
                  fit: BoxFit.contain,
                ),
              ),
            ),
            const SizedBox(height: 20),
            const CircularProgressIndicator(color: Color(0xFFEB1C24)),
          ],
        ),
      ),
    );
  }
}
