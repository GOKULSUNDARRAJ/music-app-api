import 'package:flutter/material.dart';
import 'package:camera/camera.dart';
import 'package:image_picker/image_picker.dart';
import 'package:http/http.dart' as http;
import 'package:shared_preferences/shared_preferences.dart';
import 'dart:convert';
import 'dart:ui' as ui;
import 'dart:typed_data';
import 'playlist_screen.dart';
import 'models/artist_category.dart';
import 'aruvi_code_generator_sheet.dart' show AruviCodeUtils;


// ─────────────────────────────────────────────────────────────────

// ─────────────────────────────────────────────────────────────────
// Core pixel-level analysis – exact port of ScannerFragment.analyzeImage()
// ─────────────────────────────────────────────────────────────────
List<int>? _analyzeGrayscale(
    Uint8List grayscale, int width, int height, int rowStride, bool isRotated) {
  const int barCount = 23;

  final int scanWidth = isRotated ? height : width;
  final int scanHeight = isRotated ? width : height;
  final int center = scanHeight ~/ 2;

  for (int offset = -50; offset <= 50; offset += 25) {
    final int linePos = center + offset;
    if (linePos < 0 || linePos >= scanHeight) continue;

    // Adaptive threshold
    int sum = 0;
    for (int i = 0; i < scanWidth; i++) {
      final int idx =
          isRotated ? i * rowStride + linePos : linePos * rowStride + i;
      if (idx < grayscale.length) sum += grayscale[idx] & 0xFF;
    }
    final int avg = (sum / scanWidth).round();
    final int threshold = (avg - 35).clamp(40, 255);

    final List<int> barPositions = [];
    bool inBar = false;

    for (int i = 20; i < scanWidth - 20; i++) {
      final int idx =
          isRotated ? i * rowStride + linePos : linePos * rowStride + i;
      if (idx >= grayscale.length) continue;
      final int pixel = grayscale[idx] & 0xFF;

      if (pixel < threshold && !inBar) {
        inBar = true;
        barPositions.add(i);
      } else if (pixel > threshold + 30 && inBar) {
        inBar = false;
      }
    }

    if (barPositions.length >= barCount) {
      final positions = barPositions.length > barCount
          ? barPositions.sublist(barPositions.length - barCount)
          : barPositions;

      final rawHeights = List<int>.filled(barCount, 0);
      int maxH = 0;

      for (int j = 0; j < barCount; j++) {
        final int barX = positions[j];
        int barH = 0;
        for (int dy = -40; dy < 40; dy++) {
          final int pY = linePos + dy;
          if (pY < 0 || pY >= scanHeight) continue;
          final int idx2 =
              isRotated ? barX * rowStride + pY : pY * rowStride + barX;
          if (idx2 < grayscale.length &&
              (grayscale[idx2] & 0xFF) < threshold + 20) {
            barH++;
          }
        }
        rawHeights[j] = barH;
        if (barH > maxH) maxH = barH;
      }

      if (maxH > 15) {
        final levels = List<int>.filled(barCount, 0);
        for (int j = 0; j < barCount; j++) {
          levels[j] = (rawHeights[j] * 9 / maxH - 2).round().clamp(0, 7);
        }
        return levels;
      }
    }
  }
  return null;
}

// Verbose version — only used from _onCameraFrame for debugging
List<int>? _analyzeGrayscaleVerbose(
    Uint8List grayscale, int width, int height, int rowStride, bool isRotated) {
  const int barCount = 23;
  final int scanWidth = isRotated ? height : width;
  final int scanHeight = isRotated ? width : height;
  final int center = scanHeight ~/ 2;

  for (int offset = -50; offset <= 50; offset += 25) {
    final int linePos = center + offset;
    if (linePos < 0 || linePos >= scanHeight) continue;

    int minV = 255;
    int maxV = 0;
    int sum = 0;
    
    for (int i = 20; i < scanWidth - 20; i++) {
      final int idx = isRotated ? i * rowStride + linePos : linePos * rowStride + i;
      if (idx < grayscale.length) {
        final p = grayscale[idx] & 0xFF;
        sum += p;
        if (p < minV) minV = p;
        if (p > maxV) maxV = p;
      }
    }
    final int avg = (sum / (scanWidth - 40)).round();
    
    // Fallback to strict threshold if contrast is terrible (pitch black or pure white)
    int threshold = (avg - 35).clamp(40, 255);
    int hysteresis = 15;
    
    if (maxV - minV >= 30) {
      threshold = minV + (maxV - minV) ~/ 3; // 33% from darkest
    }

    final List<int> rawBarPositions = [];
    bool inBar = false;
    for (int i = 20; i < scanWidth - 20; i++) {
      final int idx = isRotated ? i * rowStride + linePos : linePos * rowStride + i;
      if (idx >= grayscale.length) continue;
      final int pixel = grayscale[idx] & 0xFF;
      if (pixel < threshold && !inBar) {
        inBar = true;
        rawBarPositions.add(i);
      } else if (pixel >= threshold && inBar) {
        inBar = false;
      }
    }

    // Merge bars that are very close to each other (noise split a single bar)
    final List<int> barPositions = [];
    for (int p in rawBarPositions) {
      if (barPositions.isEmpty || (p - barPositions.last) > 8) {
        barPositions.add(p);
      }
    }

    // Always log at least the contrast + bar count for every scan row
    debugPrint('📊 [Analyze] row=$linePos avg=$avg min=$minV max=$maxV threshold=$threshold rawBars=${rawBarPositions.length} mergedBars=${barPositions.length}');

    if (barPositions.length >= barCount) {
      int bestStart = 0;
      double bestVariance = double.infinity;
      for (int i = 0; i <= barPositions.length - barCount; i++) {
        final sublist = barPositions.sublist(i, i + barCount);
        double avgSpacing = (sublist.last - sublist.first) / (barCount - 1);
        double variance = 0;
        for (int j = 1; j < sublist.length; j++) {
          double diff = (sublist[j] - sublist[j-1]).toDouble();
          variance += (diff - avgSpacing) * (diff - avgSpacing);
        }
        if (variance < bestVariance) {
          bestVariance = variance;
          bestStart = i;
        }
      }
      final positions = barPositions.sublist(bestStart, bestStart + barCount);

      final rawHeights = List<int>.filled(barCount, 0);
      int maxH = 0;
        for (int j = 0; j < barCount; j++) {
          final int barX = positions[j];
          int barH = 0;
          
          // Count upwards from the scanline
          for (int dy = 0; dy >= -120; dy--) {
            final int pY = linePos + dy;
            if (pY < 0) break;
            final int idx2 = isRotated ? barX * rowStride + pY : pY * rowStride + barX;
            if (idx2 >= grayscale.length) break;
            if ((grayscale[idx2] & 0xFF) < threshold + 10) {
              barH++;
            } else {
              break;
            }
          }
          
          // Count downwards from the scanline
          for (int dy = 1; dy < 120; dy++) {
            final int pY = linePos + dy;
            if (pY >= scanHeight) break;
            final int idx2 = isRotated ? barX * rowStride + pY : pY * rowStride + barX;
            if (idx2 >= grayscale.length) break;
            if ((grayscale[idx2] & 0xFF) < threshold + 10) {
              barH++;
            } else {
              break;
            }
          }
          
          rawHeights[j] = barH;
        if (barH > maxH) maxH = barH;
      }

      debugPrint('📊 [Analyze] rawHeights=$rawHeights maxH=$maxH');

      if (maxH > 15) {
        final levels = List<int>.filled(barCount, 0);
        for (int j = 0; j < barCount; j++) {
          levels[j] = (rawHeights[j] * 9 / maxH - 2).round().clamp(0, 7);
        }
        debugPrint('📊 [Analyze] levels=$levels');
        return levels;
      }
    }
  }
  return null;
}


// ─────────────────────────────────────────────────────────────────
// Screen
// ─────────────────────────────────────────────────────────────────
class ScannerScreen extends StatefulWidget {
  const ScannerScreen({Key? key}) : super(key: key);

  @override
  State<ScannerScreen> createState() => _ScannerScreenState();
}

class _ScannerScreenState extends State<ScannerScreen>
    with WidgetsBindingObserver {
  CameraController? _controller;
  bool _isInitialized = false;
  bool _isProcessing = false;
  bool _isAnalyzing = false; // throttle: only one frame at a time
  String _statusText = 'Align the Aruvi Code inside the frame';

  final Map<String, ArtistCategory> _playlistMap = {};

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _loadData().then((_) => _initCamera());
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (_controller == null || !_controller!.value.isInitialized) return;
    if (state == AppLifecycleState.inactive) {
      _controller?.stopImageStream();
    } else if (state == AppLifecycleState.resumed) {
      _initCamera();
    }
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    _controller?.stopImageStream();
    _controller?.dispose();
    super.dispose();
  }

  // ── API load ────────────────────────────────────────────────────
  Future<void> _loadData() async {
    debugPrint('🔵 [Scanner] _loadData() started');
    try {
      final prefs = await SharedPreferences.getInstance();
      final token = prefs.getString('access_token') ?? '';
      final auth = token.startsWith('Bearer ') ? token : 'Bearer $token';
      debugPrint('🔵 [Scanner] token present: ${token.isNotEmpty}');

      final response = await http.get(
        Uri.parse('https://music-app-api-1.onrender.com/api/home'),
        headers: {'Authorization': auth, 'Content-Type': 'application/json'},
      );

      debugPrint('🔵 [Scanner] API status: ${response.statusCode}');
      if (response.statusCode == 200) {
        final data = json.decode(response.body);
        final sections = data['sections'] as List?;
        debugPrint('🔵 [Scanner] sections count: ${sections?.length ?? 0}');
        if (sections != null) {
          for (var section in sections) {
            final cats = section['categories'] as List?;
            if (cats != null) {
              for (var cat in cats) {
                final raw = (cat['categoryId'] ?? '').toString().toLowerCase();
                final key = raw.startsWith('cat_') ? raw : 'cat_$raw';
                _playlistMap[key] = ArtistCategory.fromJson(cat);
              }
            }
          }
        }
        debugPrint('✅ [Scanner] Loaded ${_playlistMap.length} playlists into map');
        debugPrint('🔵 [Scanner] First 5 IDs: ${_playlistMap.keys.take(5).toList()}');
      } else {
        debugPrint('❌ [Scanner] API failed: ${response.statusCode} ${response.body.substring(0, 100)}');
      }
    } catch (e, st) {
      debugPrint('❌ [Scanner] _loadData error: $e');
      debugPrint(st.toString());
    }
  }

  // ── Camera init ─────────────────────────────────────────────────
  Future<void> _initCamera() async {
    debugPrint('🔵 [Scanner] _initCamera() started');
    try {
      final cameras = await availableCameras();
      debugPrint('🔵 [Scanner] availableCameras count: ${cameras.length}');
      for (final c in cameras) {
        debugPrint('   cam: ${c.name} dir=${c.lensDirection}');
      }
      if (cameras.isEmpty) {
        debugPrint('❌ [Scanner] No cameras found!');
        return;
      }

      final back = cameras.firstWhere(
        (c) => c.lensDirection == CameraLensDirection.back,
        orElse: () => cameras.first,
      );
      debugPrint('🔵 [Scanner] Using camera: ${back.name}');

      final controller = CameraController(
        back,
        ResolutionPreset.medium,
        enableAudio: false,
        imageFormatGroup: ImageFormatGroup.yuv420,
      );

      debugPrint('🔵 [Scanner] Calling controller.initialize()...');
      await controller.initialize();
      debugPrint('✅ [Scanner] Camera initialized. mounted=$mounted');
      debugPrint('   resolution: ${controller.value.previewSize}');
      debugPrint('   format: ${controller.imageFormatGroup}');

      if (!mounted) {
        debugPrint('⚠️ [Scanner] Widget unmounted after init, aborting');
        return;
      }

      _controller = controller;
      setState(() => _isInitialized = true);

      debugPrint('🔵 [Scanner] Starting image stream...');
      await controller.startImageStream(_onCameraFrame);
      debugPrint('✅ [Scanner] Image stream started');
    } catch (e, st) {
      debugPrint('❌ [Scanner] Camera init error: $e');
      debugPrint(st.toString());
      if (mounted) setState(() => _statusText = 'Camera error: $e');
    }
  }

  int _frameCount = 0;
  int _framesWithBars = 0;

  void _onCameraFrame(CameraImage image) {
    if (_isAnalyzing || _isProcessing) return;
    _isAnalyzing = true;
    _frameCount++;

    // Log every 30th frame so we know frames are arriving
    if (_frameCount % 30 == 1) {
      debugPrint('📷 [Scanner] Frame #$_frameCount received'
          ' size=${image.width}x${image.height}'
          ' planes=${image.planes.length}'
          ' rowStride=${image.planes[0].bytesPerRow}'
          ' format=${image.format.group}');
    }

    try {
      final yPlane = image.planes[0];
      final bytes = yPlane.bytes;
      final rowStride = yPlane.bytesPerRow;
      final width = image.width;
      final height = image.height;

      const bool isRotated = true;

      final levels = _analyzeGrayscaleVerbose(bytes, width, height, rowStride, isRotated);

      if (levels != null) {
        _framesWithBars++;
        debugPrint('🎯 [Scanner] Frame #$_frameCount: bars detected! levels=$levels');
        final matchedId = _findMatchVerbose(levels);
        if (matchedId != null) {
          _controller?.stopImageStream();
          debugPrint('🏆 [Scanner] MATCHED: $matchedId');
          if (mounted) {
            setState(() {
              _isProcessing = true;
              _statusText = 'Found! Loading...';
            });
            _handleMatchedId(matchedId);
          }
          return;
        }
      }
    } catch (e, st) {
      debugPrint('❌ [Scanner] Frame error: $e');
      debugPrint(st.toString());
    } finally {
      _isAnalyzing = false;
    }
  }

  // ── Match levels against all known IDs (same as Android findMatch()) ──
  String? _findMatch(List<int> levels) {
    String? bestMatch;
    int minError = 999;
    for (final id in _playlistMap.keys) {
      final error = AruviCodeUtils.matchError(id, levels);
      if (error < minError) {
        minError = error;
        bestMatch = id;
      }
    }
    return minError <= 15 ? bestMatch : null;
  }

  // Verbose version — logs best match and score every time bars are detected
  String? _findMatchVerbose(List<int> levels) {
    String? bestMatch;
    int minError = 999;
    for (final id in _playlistMap.keys) {
      final error = AruviCodeUtils.matchError(id, levels);
      if (error < minError) {
        minError = error;
        bestMatch = id;
      }
    }
    debugPrint('🔍 [Scanner] Best match: $bestMatch  error=$minError  (threshold≤15)');
    return minError <= 15 ? bestMatch : null;
  }

  // ── Navigate to matched playlist ────────────────────────────────
  Future<void> _handleMatchedId(String matchedId) async {
    final playlist = _playlistMap[matchedId];
    if (playlist != null && mounted) {
      Navigator.pushReplacement(
        context,
        MaterialPageRoute(
          builder: (_) => PlaylistScreen(
            title: playlist.categoryName ?? 'Scanned Playlist',
            subtitle: playlist.songs.isNotEmpty
                ? '${playlist.songs.length} Songs'
                : 'Playlist',
            imageUrl: playlist.categoryImage ?? '',
            categoryId: matchedId,
            songs: playlist.songs,
          ),
        ),
      );
    } else if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Playlist not found. Try again.')),
      );
      setState(() {
        _isProcessing = false;
        _statusText = 'Align the Aruvi Code inside the frame';
      });
      await _controller?.startImageStream(_onCameraFrame);
    }
  }

  // ── Gallery scan ────────────────────────────────────────────────
  Future<void> _pickImageAndScan() async {
    try {
      final picker = ImagePicker();
      final XFile? file = await picker.pickImage(source: ImageSource.gallery);
      if (file == null) return;

      setState(() {
        _isProcessing = true;
        _statusText = 'Analyzing image...';
      });
      await _controller?.stopImageStream();

      final bytes = await file.readAsBytes();
      final codec = await ui.instantiateImageCodec(bytes);
      final frame = await codec.getNextFrame();
      final uiImage = frame.image;

      final byteData =
          await uiImage.toByteData(format: ui.ImageByteFormat.rawRgba);
      if (byteData == null) {
        _resumeAfterFail('Could not decode image');
        return;
      }

      final rgbaBytes = byteData.buffer.asUint8List();
      final matchedId = _analyzeBitmapRgba(rgbaBytes, uiImage.width, uiImage.height);

      if (matchedId != null) {
        await _handleMatchedId(matchedId);
      } else {
        _resumeAfterFail('No Aruvi Code found. Try a clearer photo.');
      }
    } catch (e) {
      _resumeAfterFail('Error: $e');
    }
  }

  void _resumeAfterFail(String msg) {
    if (!mounted) return;
    setState(() {
      _isProcessing = false;
      _statusText = 'Align the Aruvi Code inside the frame';
    });
    _controller?.startImageStream(_onCameraFrame);
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(msg)));
  }

  /// RGBA bitmap analysis — port of ScannerFragment.analyzeBitmap()
  String? _analyzeBitmapRgba(Uint8List rgba, int width, int height) {
    // Convert RGBA → grayscale
    final grayscale = Uint8List(width * height);
    for (int i = 0; i < width * height; i++) {
      final r = rgba[i * 4];
      final g = rgba[i * 4 + 1];
      final b = rgba[i * 4 + 2];
      grayscale[i] = ((r + g + b) ~/ 3);
    }

    for (int rowY = height ~/ 4; rowY < height * 0.95; rowY += 15) {
      int minV = 255, maxV = 0;
      for (int x = width ~/ 4; x < width * 3 ~/ 4; x++) {
        final v = grayscale[rowY * width + x];
        if (v < minV) minV = v;
        if (v > maxV) maxV = v;
      }
      if (maxV - minV < 30) continue;
      final threshold = minV + (maxV - minV) ~/ 3;

      final List<int> rawBarPositions = [];
      bool inBar = false;
      for (int x = (width * 0.15).toInt(); x < (width * 0.9).toInt(); x++) {
        final v = grayscale[rowY * width + x];
        if (v < threshold && !inBar) {
          inBar = true;
          rawBarPositions.add(x);
        } else if (v > threshold + 10 && inBar) {
          inBar = false;
        }
      }

      final List<int> barPositions = [];
      for (int p in rawBarPositions) {
        if (barPositions.isEmpty || (p - barPositions.last) > 8) {
          barPositions.add(p);
        }
      }

      if (barPositions.length >= AruviCodeUtils.barCount) {
        int bestStart = 0;
        double bestVariance = double.infinity;
        for (int i = 0; i <= barPositions.length - AruviCodeUtils.barCount; i++) {
          final sublist = barPositions.sublist(i, i + AruviCodeUtils.barCount);
          double avgSpacing = (sublist.last - sublist.first) / (AruviCodeUtils.barCount - 1);
          double variance = 0;
          for (int j = 1; j < sublist.length; j++) {
            double diff = (sublist[j] - sublist[j-1]).toDouble();
            variance += (diff - avgSpacing) * (diff - avgSpacing);
          }
          if (variance < bestVariance) {
            bestVariance = variance;
            bestStart = i;
          }
        }
        final positions = barPositions.sublist(bestStart, bestStart + AruviCodeUtils.barCount);

        final rawHeights = List<int>.filled(AruviCodeUtils.barCount, 0);
        int maxH = 0;
        for (int i = 0; i < AruviCodeUtils.barCount; i++) {
          final barX = positions[i];
          int barH = 0;
          
          // Count upwards
          for (int dy = 0; dy >= -120; dy--) {
            final y = rowY + dy;
            if (y < 0) break;
            if (grayscale[y * width + barX] < threshold + 10) {
              barH++;
            } else {
              break;
            }
          }
          
          // Count downwards
          for (int dy = 1; dy < 120; dy++) {
            final y = rowY + dy;
            if (y >= height) break;
            if (grayscale[y * width + barX] < threshold + 10) {
              barH++;
            } else {
              break;
            }
          }
          
          rawHeights[i] = barH;
          if (barH > maxH) maxH = barH;
        }

        if (maxH > 10) {
          final levels = List<int>.filled(AruviCodeUtils.barCount, 0);
          for (int i = 0; i < AruviCodeUtils.barCount; i++) {
            levels[i] = (rawHeights[i] * 9 / maxH - 2).round().clamp(0, 7);
          }
          final match = _findMatch(levels);
          if (match != null) return match;
        }
      }
    }
    return null;
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.black,
      body: Stack(
        fit: StackFit.expand,
        children: [
          // ── Camera preview ──────────────────────────────────────
          if (_isInitialized && _controller != null)
            Center(
              child: CameraPreview(_controller!),
            )
          else
            const Center(
              child: CircularProgressIndicator(color: Color(0xFFEB1C24)),
            ),

          // ── Dark overlay with clear cutout ─────────────────────
          CustomPaint(
            size: Size.infinite,
            painter: ScannerOverlayPainter(),
          ),

          // ── Top bar ────────────────────────────────────────────
          SafeArea(
            child: Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
              child: Row(
                children: [
                  GestureDetector(
                    onTap: () => Navigator.pop(context),
                    child: Container(
                      padding: const EdgeInsets.all(8),
                      decoration: BoxDecoration(
                        color: Colors.black45,
                        borderRadius: BorderRadius.circular(8),
                      ),
                      child: const Icon(Icons.arrow_back, color: Colors.white),
                    ),
                  ),
                  const SizedBox(width: 12),
                  const Text(
                    'Scan Aruvi Code',
                    style: TextStyle(
                      color: Colors.white,
                      fontSize: 18,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                ],
              ),
            ),
          ),

          // ── Animated scan line inside the cutout ───────────────
          if (!_isProcessing) _buildScanLine(),

          // ── Bottom status + gallery button ─────────────────────
          Positioned(
            bottom: 50,
            left: 0,
            right: 0,
            child: Column(
              children: [
                Text(
                  _statusText,
                  textAlign: TextAlign.center,
                  style: const TextStyle(
                    color: Colors.white,
                    fontSize: 14,
                    fontWeight: FontWeight.w500,
                  ),
                ),
                const SizedBox(height: 24),
                GestureDetector(
                  onTap: _isProcessing ? null : _pickImageAndScan,
                  child: AnimatedOpacity(
                    opacity: _isProcessing ? 0.5 : 1.0,
                    duration: const Duration(milliseconds: 200),
                    child: Container(
                      padding: const EdgeInsets.symmetric(
                          horizontal: 28, vertical: 14),
                      decoration: BoxDecoration(
                        color: const Color(0xFFEB1C24),
                        borderRadius: BorderRadius.circular(30),
                      ),
                      child: const Row(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          Icon(Icons.image_search, color: Colors.white, size: 20),
                          SizedBox(width: 8),
                          Text(
                            'Scan from Gallery',
                            style: TextStyle(
                              color: Colors.white,
                              fontWeight: FontWeight.bold,
                              fontSize: 15,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                ),
              ],
            ),
          ),

          // ── Processing overlay ──────────────────────────────────
          if (_isProcessing)
            Container(
              color: Colors.black54,
              child: const Center(
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    CircularProgressIndicator(color: Color(0xFFEB1C24)),
                    SizedBox(height: 16),
                    Text(
                      'Loading Playlist...',
                      style:
                          TextStyle(color: Colors.white, fontSize: 16),
                    ),
                  ],
                ),
              ),
            ),
        ],
      ),
    );
  }

  Widget _buildScanLine() {
    return TweenAnimationBuilder<double>(
      tween: Tween(begin: 0.0, end: 1.0),
      duration: const Duration(seconds: 2),
      builder: (context, value, child) {
        return LayoutBuilder(builder: (context, constraints) {
          final double cutoutH = constraints.maxWidth * 0.38;
          final double cutoutTop =
              (constraints.maxHeight - cutoutH) / 2 - 30;
          final double lineY = cutoutTop + cutoutH * value;
          return Positioned(
            top: lineY,
            left: constraints.maxWidth * 0.1 + 4,
            right: constraints.maxWidth * 0.1 + 4,
            child: Container(
              height: 2,
              decoration: BoxDecoration(
                gradient: LinearGradient(colors: [
                  Colors.transparent,
                  const Color(0xFFEB1C24),
                  Colors.transparent,
                ]),
              ),
            ),
          );
        });
      },
      onEnd: () {
        if (mounted) setState(() {}); // restart animation
      },
    );
  }
}

// ─────────────────────────────────────────────────────────────────
// Overlay painter
// ─────────────────────────────────────────────────────────────────
class ScannerOverlayPainter extends CustomPainter {
  @override
  void paint(Canvas canvas, Size size) {
    canvas.saveLayer(Rect.fromLTWH(0, 0, size.width, size.height), Paint());

    final paint = Paint()
      ..color = Colors.black54
      ..style = PaintingStyle.fill;
    canvas.drawRect(Rect.fromLTWH(0, 0, size.width, size.height), paint);

    final double cutoutW = size.width * 0.8;
    final double cutoutH = size.width * 0.38;
    final double dx = (size.width - cutoutW) / 2;
    final double dy = (size.height - cutoutH) / 2 - 30;
    final rect = Rect.fromLTWH(dx, dy, cutoutW, cutoutH);

    paint.blendMode = BlendMode.clear;
    canvas.drawRRect(
      RRect.fromRectAndRadius(rect, const Radius.circular(16)),
      paint,
    );
    canvas.restore();

    final borderPaint = Paint()
      ..color = const Color(0xFFEB1C24)
      ..style = PaintingStyle.stroke
      ..strokeWidth = 3
      ..blendMode = BlendMode.srcOver;

    const double corner = 30;
    // corners
    canvas.drawLine(Offset(dx, dy + corner), Offset(dx, dy), borderPaint);
    canvas.drawLine(Offset(dx, dy), Offset(dx + corner, dy), borderPaint);
    canvas.drawLine(
        Offset(rect.right - corner, dy), Offset(rect.right, dy), borderPaint);
    canvas.drawLine(Offset(rect.right, dy),
        Offset(rect.right, dy + corner), borderPaint);
    canvas.drawLine(Offset(dx, rect.bottom - corner),
        Offset(dx, rect.bottom), borderPaint);
    canvas.drawLine(
        Offset(dx, rect.bottom), Offset(dx + corner, rect.bottom), borderPaint);
    canvas.drawLine(Offset(rect.right - corner, rect.bottom),
        Offset(rect.right, rect.bottom), borderPaint);
    canvas.drawLine(Offset(rect.right, rect.bottom),
        Offset(rect.right, rect.bottom - corner), borderPaint);
  }

  @override
  bool shouldRepaint(covariant CustomPainter old) => false;
}
