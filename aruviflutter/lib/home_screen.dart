import 'package:flutter/material.dart';
import 'tabs/home_all_tab.dart';
import 'tabs/home_artist_tab.dart';
import 'tabs/home_devotional_tab.dart';

import 'dart:convert';
import 'package:shared_preferences/shared_preferences.dart';
import 'models/top_nav_item.dart';
import 'scanner_screen.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> with TickerProviderStateMixin {
  TabController? _tabController;

  List<TopNavItem> _tabs = [
    TopNavItem(topmenuId: 1, topmenuName: 'ALL'),
    TopNavItem(topmenuId: 3, topmenuName: 'ARTIST'),
    TopNavItem(topmenuId: 4, topmenuName: 'DIVOTIONAL'),
  ];
  int _selectedIndex = 0;
  bool _isLoading = false; // We can set this to false initially since we have default tabs, but let's keep it true if we want to show loading indicator for the body.

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: _tabs.length, vsync: this);
    _tabController!.addListener(() {
      if (!_tabController!.indexIsChanging) {
        setState(() {
          _selectedIndex = _tabController!.index;
        });
      }
    });
    _loadTabs();
  }

  Future<void> _loadTabs() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final topNavJson = prefs.getString('top_navigation');
      
      if (topNavJson != null && topNavJson.isNotEmpty) {
        final List<dynamic> parsed = json.decode(topNavJson);
        final loadedTabs = parsed.map((e) => TopNavItem.fromJson(e)).toList();
        if (loadedTabs.isNotEmpty) {
          setState(() {
            _tabs = loadedTabs;
            // Only reinitialize tab controller if length changed or we want to be safe
            if (_tabController?.length != _tabs.length) {
              _tabController?.dispose();
              _tabController = TabController(length: _tabs.length, vsync: this);
              _tabController!.addListener(() {
                if (!_tabController!.indexIsChanging) {
                  setState(() {
                    _selectedIndex = _tabController!.index;
                  });
                }
              });
            }
          });
        }
      }
    } catch (e) {
      debugPrint('Error loading tabs: $e');
    }
  }

  @override
  void dispose() {
    _tabController?.dispose();
    super.dispose();
  }


  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.black,
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
            onPressed: () {
              Navigator.push(
                context,
                MaterialPageRoute(builder: (context) => const ScannerScreen()),
              );
            },
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
        bottom: PreferredSize(
          preferredSize: const Size.fromHeight(32),
          child: Padding(
            padding: const EdgeInsets.only(bottom: 0, left: 15, right: 15),
            child: SizedBox(
              height: 32,
              child: ListView.builder(
                scrollDirection: Axis.horizontal,
                itemCount: _tabs.length,
                itemBuilder: (context, index) {
                  bool isSelected = _selectedIndex == index;
                  return GestureDetector(
                    onTap: () {
                      _tabController?.animateTo(index);
                      setState(() {
                        _selectedIndex = index;
                      });
                    },
                    child: Container(
                      margin: const EdgeInsets.only(right: 12),
                      padding: const EdgeInsets.symmetric(horizontal: 20),
                      alignment: Alignment.center,
                      decoration: BoxDecoration(
                        color: Colors.black,
                        border: Border.all(
                          color: isSelected ? const Color(0xFFF7B500) : Colors.white,
                          width: 1,
                        ),
                        borderRadius: BorderRadius.circular(20),
                      ),
                      child: Text(
                        _tabs[index].topmenuName.toUpperCase(),
                        style: TextStyle(
                          color: isSelected ? const Color(0xFFF7B500) : Colors.white,
                          fontSize: 11,
                          fontWeight: FontWeight.bold,
                          letterSpacing: 1.0,
                        ),
                      ),
                    ),
                  );
                },
              ),
            ),
          ),
        ),
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator(color: Color(0xFFEB1C24)))
          : TabBarView(
              controller: _tabController,
              children: _tabs.map((tab) {
                final tabName = tab.topmenuName.toLowerCase();
                if (tabName == 'all') {
                  return const HomeAllTab();
                } else if (tabName == 'artist') {
                  return const HomeArtistTab();
                } else if (tabName == 'divotional' || tabName == 'devotional') {
                  return const HomeDevotionalTab();
                } else {
                  return Center(child: Text('${tab.topmenuName.toUpperCase()} TAB', style: const TextStyle(color: Colors.white)));
                }
              }).toList(),
            ),
    );
  }
}
