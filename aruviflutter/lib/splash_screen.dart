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

    if (accessToken.isNotEmpty) {
      final hasLocalMenu = prefs.getString('top_navigation') != null && prefs.getString('bottom_navigation') != null;

      Future<void> fetchMenu() async {
        try {
          final authHeader = accessToken.startsWith('Bearer ') ? accessToken : 'Bearer $accessToken';
          final response = await http.post(
            Uri.parse('https://music-app-api-1.onrender.com/api/secure/appMenuList'),
            headers: {'Authorization': authHeader},
          ).timeout(const Duration(seconds: 10));

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
          debugPrint('Error fetching app menu: $e');
        }
      }

      if (hasLocalMenu) {
        // Fire and forget so we load FAST
        fetchMenu();
        await Future.delayed(const Duration(milliseconds: 300)); // Tiny delay for smooth transition
      } else {
        // Wait for it if we don't have it
        await fetchMenu();
      }

      if (!mounted) return;
      Navigator.pushReplacement(context, MaterialPageRoute(builder: (_) => const MainActivity()));
    } else {
      await Future.delayed(const Duration(seconds: 1)); // Small delay for branding
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
      appBar: AppBar(
        backgroundColor: Colors.black,
        elevation: 0,
        toolbarHeight: 60,
        titleSpacing: 0,
        automaticallyImplyLeading: false,
        title: Container(
          width: 150, 
          height: 40,
          margin: const EdgeInsets.only(left: 16),
          decoration: const BoxDecoration(
            image: DecorationImage(
              image: AssetImage('assets/images/arivumusiclogo.png'),
              fit: BoxFit.contain,
              alignment: Alignment.centerLeft,
            ),
          ),
        ),
        actions: [
          IconButton(
            icon: const Icon(Icons.qr_code_scanner, color: Colors.white),
            onPressed: () {}, // Disabled on splash screen
          ),
          Container(
            width: 32,
            height: 32,
            margin: const EdgeInsets.only(right: 15, left: 10),
            decoration: const BoxDecoration(
              shape: BoxShape.circle,
            ),
            child: Image.asset('assets/images/profilemusic.png', fit: BoxFit.contain),
          ),
        ],
      ),
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
