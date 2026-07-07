import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'sign_up_screen.dart';

class OnboardingData {
  final String title;
  final String description;
  final String imagePath;

  OnboardingData({
    required this.title,
    required this.description,
    required this.imagePath,
  });
}

class OnboardingScreen extends StatefulWidget {
  const OnboardingScreen({super.key});

  @override
  State<OnboardingScreen> createState() => _OnboardingScreenState();
}

class _OnboardingScreenState extends State<OnboardingScreen> {
  final PageController _pageController = PageController();
  int _currentPage = 0;

  final List<OnboardingData> _pages = [
    OnboardingData(
      title: "All the Music You Love, One Place",
      description: "Stream millions of songs and discover new music made just for you.",
      imagePath: "assets/images/onboarding1.png",
    ),
    OnboardingData(
      title: "Find Your Sound",
      description: "Explore songs, albums, and playlists across every genre and mood.",
      imagePath: "assets/images/onboarding2.png",
    ),
    OnboardingData(
      title: "Music Made for You",
      description: "Enjoy personalized playlists based on your listening habits and favorites.",
      imagePath: "assets/images/onboarding3.png",
    ),
    OnboardingData(
      title: "Stay Connected to Artists",
      description: "Explore songs, albums, and playlists across every genre and mood",
      imagePath: "assets/images/onboarding4.png",
    ),
    OnboardingData(
      title: "Listen Anytime, Anywhere",
      description: "Play music while working, driving, or relaxing - even offline",
      imagePath: "assets/images/onboarding5.png",
    ),
  ];

  void _finishOnboarding() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool('onboarding_complete', true);
    if (!mounted) return;
    Navigator.pushReplacement(context, MaterialPageRoute(builder: (_) => const SignUpScreen()));
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFF181A20), // @color/darkGray
      body: SafeArea(
        child: Column(
          children: [
            Expanded(
              child: PageView.builder(
                controller: _pageController,
                itemCount: _pages.length,
                onPageChanged: (index) {
                  setState(() {
                    _currentPage = index;
                  });
                },
                itemBuilder: (context, index) {
                  return Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 32.0, vertical: 32.0),
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        const Spacer(),
                        Image.asset(
                          _pages[index].imagePath,
                          width: 200,
                          height: 200,
                          fit: BoxFit.contain,
                        ),
                        const SizedBox(height: 60),
                        Text(
                          _pages[index].title,
                          textAlign: TextAlign.center,
                          style: const TextStyle(
                            fontSize: 32,
                            fontWeight: FontWeight.bold,
                            color: Colors.white,
                          ),
                        ),
                        const SizedBox(height: 16),
                        Text(
                          _pages[index].description,
                          textAlign: TextAlign.center,
                          style: const TextStyle(
                            fontSize: 24,
                            color: Color(0xFF8592A2), // @color/textplaceholder
                            height: 1.2,
                          ),
                        ),
                        const Spacer(),
                      ],
                    ),
                  );
                },
              ),
            ),
            
            // Dots indicator
            Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: List.generate(
                _pages.length,
                (index) => _buildDot(index == _currentPage),
              ),
            ),
            const SizedBox(height: 32),
            
            // Bottom button
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16.0).copyWith(bottom: 40.0),
              child: InkWell(
                onTap: _finishOnboarding,
                borderRadius: BorderRadius.circular(100),
                child: Container(
                  width: double.infinity,
                  height: 42, // from Android layout dp_42
                  decoration: BoxDecoration(
                    color: const Color(0xFFEB1C24), // @color/yellow
                    borderRadius: BorderRadius.circular(100),
                  ),
                  child: Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: const [
                      Text(
                        'Get Start',
                        style: TextStyle(
                          fontSize: 22, 
                          fontWeight: FontWeight.w500, // circularstdmedium equivalent
                          color: Colors.white,
                          decoration: TextDecoration.none,
                        ),
                      ),
                      SizedBox(width: 8),
                      Icon(Icons.arrow_forward, color: Colors.white, size: 24),
                    ],
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildDot(bool isActive) {
    return AnimatedContainer(
      duration: const Duration(milliseconds: 300),
      margin: const EdgeInsets.symmetric(horizontal: 4),
      width: isActive ? 10 : 8,
      height: isActive ? 10 : 8,
      decoration: BoxDecoration(
        color: isActive ? const Color(0xFFEB1C24) : Colors.white, // In Android it's yellow or white
        shape: BoxShape.circle,
      ),
    );
  }
}
