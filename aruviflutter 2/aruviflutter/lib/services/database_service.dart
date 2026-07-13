import 'package:sqflite/sqflite.dart';
import 'package:path/path.dart';
import 'dart:convert';
import 'package:http/http.dart' as http;
import 'package:shared_preferences/shared_preferences.dart';
import '../models/audio_model.dart';
import '../models/artist_category.dart';

class DatabaseService {
  static final DatabaseService _instance = DatabaseService._internal();
  factory DatabaseService() => _instance;
  DatabaseService._internal();

  Database? _database;

  Future<Database> get database async {
    if (_database != null) return _database!;
    _database = await _initDB();
    return _database!;
  }

  Future<Database> _initDB() async {
    final dbPath = await getDatabasesPath();
    final path = join(dbPath, 'aruvi_downloads.db');

    return await openDatabase(
      path,
      version: 6,
      onCreate: _createDB,
      onUpgrade: _upgradeDB,
    );
  }

  Future<void> _upgradeDB(Database db, int oldVersion, int newVersion) async {
    if (oldVersion < 2) {
      await db.execute('''
        CREATE TABLE custom_playlists (
          id INTEGER PRIMARY KEY AUTOINCREMENT,
          playlistId TEXT UNIQUE,
          name TEXT,
          imageUrl TEXT,
          createdAt INTEGER
        )
      ''');
      await db.execute('''
        CREATE TABLE custom_playlist_songs (
          id INTEGER PRIMARY KEY AUTOINCREMENT,
          playlistId TEXT,
          songId TEXT,
          audioModelJson TEXT
        )
      ''');
    }
    if (oldVersion < 3) {
      await db.execute('''
        CREATE TABLE recently_played (
          id INTEGER PRIMARY KEY AUTOINCREMENT,
          songId TEXT UNIQUE,
          audioModelJson TEXT,
          playedAt INTEGER
        )
      ''');
    }
    if (oldVersion < 4) {
      await db.execute('''
        CREATE TABLE liked_songs (
          id INTEGER PRIMARY KEY AUTOINCREMENT,
          songId TEXT UNIQUE,
          audioModelJson TEXT,
          likedAt INTEGER
        )
      ''');
    }
    if (oldVersion < 5) {
      await db.execute('ALTER TABLE downloads ADD COLUMN isSingle INTEGER DEFAULT 0');
    }
    if (oldVersion < 6) {
      await db.execute('ALTER TABLE downloads ADD COLUMN isPlaylist INTEGER DEFAULT 0');
      // For existing downloads from before version 6, assume they are playlist downloads if they aren't single downloads, 
      // or just set all existing non-single to playlist to be safe.
      await db.execute('UPDATE downloads SET isPlaylist = 1 WHERE isSingle = 0');
    }
  }

  Future<void> _createDB(Database db, int version) async {
    await db.execute('''
      CREATE TABLE downloads (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        songId TEXT,
        audioName TEXT,
        audioUrl TEXT,
        categoryName TEXT,
        categoryId TEXT,
        imageUrl TEXT,
        downloadPath TEXT,
        duration TEXT,
        durationInMillis INTEGER,
        playlistId TEXT,
        isSingle INTEGER DEFAULT 0,
        isPlaylist INTEGER DEFAULT 0
      )
    ''');
    await db.execute('''
      CREATE TABLE custom_playlists (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        playlistId TEXT UNIQUE,
        name TEXT,
        imageUrl TEXT,
        createdAt INTEGER
      )
    ''');
    await db.execute('''
      CREATE TABLE custom_playlist_songs (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        playlistId TEXT,
        songId TEXT,
        audioModelJson TEXT
      )
    ''');
    await db.execute('''
      CREATE TABLE recently_played (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        songId TEXT UNIQUE,
        audioModelJson TEXT,
        playedAt INTEGER
      )
    ''');
    await db.execute('''
      CREATE TABLE liked_songs (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        songId TEXT UNIQUE,
        audioModelJson TEXT,
        likedAt INTEGER
      )
    ''');
  }

  Future<void> insertDownload(AudioModel song, String localPath, {bool isSingle = false, bool isPlaylist = false}) async {
    final db = await database;
    
    // Check if exists
    final existing = await db.query(
      'downloads',
      where: 'songId = ?',
      whereArgs: [song.songId],
    );

    if (existing.isEmpty) {
      await db.insert('downloads', {
        'songId': song.songId,
        'audioName': song.audioName,
        'audioUrl': song.audioUrl,
        'categoryName': song.categoryName,
        'categoryId': song.categoryId,
        'imageUrl': song.imageUrl,
        'downloadPath': localPath,
        'duration': song.duration,
        'durationInMillis': song.durationInMillis,
        'playlistId': song.playlistId,
        'isSingle': isSingle ? 1 : 0,
        'isPlaylist': isPlaylist ? 1 : 0,
      });
    } else {
      final existingData = existing.first;
      final currentIsSingle = (existingData['isSingle'] as int?) ?? 0;
      final currentIsPlaylist = (existingData['isPlaylist'] as int?) ?? 0;

      await db.update('downloads', {
        'downloadPath': localPath,
        'isSingle': isSingle ? 1 : currentIsSingle,
        'isPlaylist': isPlaylist ? 1 : currentIsPlaylist,
      }, where: 'songId = ?', whereArgs: [song.songId]);
    }
  }

  Future<void> removeDownload(String songId) async {
    final db = await database;
    await db.delete('downloads', where: 'songId = ?', whereArgs: [songId]);
  }

  Future<void> updateDownloadCategory(String songId, String categoryId, String categoryName) async {
    final db = await database;
    await db.update(
      'downloads',
      {
        'categoryId': categoryId,
        'categoryName': categoryName,
        'isPlaylist': 1,
      },
      where: 'songId = ?',
      whereArgs: [songId],
    );
  }

  Future<bool> isDownloaded(String songId) async {
    final db = await database;
    final maps = await db.query(
      'downloads',
      columns: ['songId'],
      where: 'songId = ?',
      whereArgs: [songId],
    );
    return maps.isNotEmpty;
  }

  Future<Set<String>> getAllDownloadedSongIds() async {
    final db = await database;
    final maps = await db.query('downloads', columns: ['songId']);
    return maps.map((e) => e['songId'] as String).toSet();
  }

  Future<bool> isSingleDownloaded(String songId) async {
    final db = await database;
    final maps = await db.query(
      'downloads',
      columns: ['songId'],
      where: 'songId = ? AND isSingle = ?',
      whereArgs: [songId, 1],
    );
    return maps.isNotEmpty;
  }

  Future<void> markAsSingleDownload(String songId) async {
    final db = await database;
    // We only set isSingle to 1, we preserve isPlaylist as is.
    await db.update(
      'downloads',
      {'isSingle': 1},
      where: 'songId = ?',
      whereArgs: [songId],
    );
  }

  Future<bool> unmarkSingleDownload(String songId) async {
    final db = await database;
    
    // Check if it's also a playlist download
    final maps = await db.query('downloads', where: 'songId = ?', whereArgs: [songId]);
    if (maps.isNotEmpty) {
      final isPlaylist = (maps.first['isPlaylist'] as int?) ?? 0;
      if (isPlaylist == 1) {
        // Just unmark single, keep it in DB for the playlist
        await db.update('downloads', {'isSingle': 0}, where: 'songId = ?', whereArgs: [songId]);
        return false; // Not completely removed
      } else {
        // Not in a playlist, safe to remove completely
        await removeDownload(songId);
        return true; // Completely removed
      }
    }
    return false;
  }

  Future<AudioModel?> getDownload(String songId) async {
    final db = await database;
    final maps = await db.query(
      'downloads',
      where: 'songId = ?',
      whereArgs: [songId],
    );

    if (maps.isNotEmpty) {
      final data = maps.first;
      return AudioModel(
        songId: data['songId'] as String?,
        audioName: data['audioName'] as String?,
        audioUrl: data['audioUrl'] as String?,
        categoryName: data['categoryName'] as String?,
        categoryId: data['categoryId'] as String?,
        imageUrl: data['imageUrl'] as String?,
        downloadPath: data['downloadPath'] as String?,
        isDownloaded: true,
        duration: data['duration'] as String?,
        durationInMillis: data['durationInMillis'] as int?,
        playlistId: data['playlistId'] as String?,
      );
    }
    return null;
  }

  Future<List<AudioModel>> getAllDownloads() async {
    final db = await database;
    // Only return single downloads for the Songs tab
    final maps = await db.query(
      'downloads',
      where: 'isSingle = ?',
      whereArgs: [1],
    );
    return maps.map((data) => AudioModel(
      songId: data['songId'] as String?,
      audioName: data['audioName'] as String?,
      audioUrl: data['audioUrl'] as String?,
      categoryName: data['categoryName'] as String?,
      categoryId: data['categoryId'] as String?,
      imageUrl: data['imageUrl'] as String?,
      downloadPath: data['downloadPath'] as String?,
      isDownloaded: true,
      duration: data['duration'] as String?,
      durationInMillis: data['durationInMillis'] as int?,
      playlistId: data['playlistId'] as String?,
    )).toList();
  }

  Future<List<ArtistCategory>> getDownloadedPlaylists() async {
    final db = await database;
    // Only fetch songs downloaded as part of a playlist
    final maps = await db.query(
      'downloads',
      where: 'isPlaylist = ?',
      whereArgs: [1],
    );
    
    final songs = maps.map((data) => AudioModel(
      songId: data['songId'] as String?,
      audioName: data['audioName'] as String?,
      audioUrl: data['audioUrl'] as String?,
      categoryName: data['categoryName'] as String?,
      categoryId: data['categoryId'] as String?,
      imageUrl: data['imageUrl'] as String?,
      downloadPath: data['downloadPath'] as String?,
      isDownloaded: true,
      duration: data['duration'] as String?,
      durationInMillis: data['durationInMillis'] as int?,
      playlistId: data['playlistId'] as String?,
    )).toList();

    final Map<String, ArtistCategory> categoryMap = {};

    for (var song in songs) {
      final String catId = song.categoryId ?? 'unknown';
      if (!categoryMap.containsKey(catId)) {
        categoryMap[catId] = ArtistCategory(
          categoryId: catId,
          categoryName: song.categoryName ?? 'Unknown Playlist',
          categoryImage: song.imageUrl ?? '',
          songs: [],
        );
      }
      categoryMap[catId]!.songs.add(song);
    }

    return categoryMap.values.toList();
  }

  // --- CUSTOM PLAYLISTS ---

  Future<String> createCustomPlaylist(String name) async {
    final db = await database;
    final playlistId = 'custom_${DateTime.now().millisecondsSinceEpoch}';
    await db.insert('custom_playlists', {
      'playlistId': playlistId,
      'name': name,
      'createdAt': DateTime.now().millisecondsSinceEpoch,
    });
    return playlistId;
  }

  Future<List<Map<String, dynamic>>> getCustomPlaylists() async {
    final db = await database;
    return await db.query('custom_playlists', orderBy: 'createdAt DESC');
  }

  Future<void> addSongToCustomPlaylist(String playlistId, AudioModel song) async {
    final db = await database;
    // Check if song already in playlist
    final existing = await db.query(
      'custom_playlist_songs',
      where: 'playlistId = ? AND songId = ?',
      whereArgs: [playlistId, song.songId],
    );

    if (existing.isEmpty) {
      await db.insert('custom_playlist_songs', {
        'playlistId': playlistId,
        'songId': song.songId,
        'audioModelJson': jsonEncode(song.toJson()),
      });
      
      // Update playlist image if it doesn't have one
      final playlist = await db.query('custom_playlists', where: 'playlistId = ?', whereArgs: [playlistId]);
      if (playlist.isNotEmpty) {
        final img = playlist.first['imageUrl'] as String?;
        if (img == null || img.isEmpty) {
          if (song.imageUrl != null && song.imageUrl!.isNotEmpty) {
            await db.update('custom_playlists', {'imageUrl': song.imageUrl}, where: 'playlistId = ?', whereArgs: [playlistId]);
          }
        }
      }
    }
  }

  Future<void> addMultipleSongsToCustomPlaylist(String playlistId, List<AudioModel> songs) async {
    final db = await database;
    final batch = db.batch();
    bool hasSetImage = false;
    
    final existingMap = await db.query(
      'custom_playlist_songs',
      columns: ['songId'],
      where: 'playlistId = ?',
      whereArgs: [playlistId],
    );
    final existingIds = existingMap.map((e) => e['songId'] as String).toSet();

    for (var song in songs) {
      if (!existingIds.contains(song.songId)) {
        batch.insert('custom_playlist_songs', {
          'playlistId': playlistId,
          'songId': song.songId,
          'audioModelJson': jsonEncode(song.toJson()),
        });
        
        if (!hasSetImage && song.imageUrl != null && song.imageUrl!.isNotEmpty) {
          batch.update(
            'custom_playlists', 
            {'imageUrl': song.imageUrl}, 
            where: 'playlistId = ? AND (imageUrl IS NULL OR imageUrl = "")', 
            whereArgs: [playlistId]
          );
          hasSetImage = true;
        }
      }
    }
    
    await batch.commit(noResult: true);
  }
  
  Future<List<AudioModel>> getCustomPlaylistSongs(String playlistId) async {
    final db = await database;
    final maps = await db.query(
      'custom_playlist_songs',
      where: 'playlistId = ?',
      whereArgs: [playlistId],
    );
    
    return maps.map((data) {
      final jsonStr = data['audioModelJson'] as String;
      return AudioModel.fromJson(jsonDecode(jsonStr));
    }).toList();
  }

  Future<List<String>> getPlaylistsContainingSong(String songId) async {
    final db = await database;
    final maps = await db.query(
      'custom_playlist_songs',
      columns: ['playlistId'],
      where: 'songId = ?',
      whereArgs: [songId],
    );
    return maps.map((e) => e['playlistId'] as String).toList();
  }

  Future<void> addToRecentlyPlayed(AudioModel song) async {
    if (song.songId == null) return;
    final db = await database;
    await db.insert(
      'recently_played',
      {
        'songId': song.songId,
        'audioModelJson': jsonEncode(song.toJson()),
        'playedAt': DateTime.now().millisecondsSinceEpoch,
      },
      conflictAlgorithm: ConflictAlgorithm.replace,
    );
  }

  Future<List<AudioModel>> getRecentlyPlayedSongs() async {
    final db = await database;
    final maps = await db.query(
      'recently_played',
      orderBy: 'playedAt DESC',
      limit: 100, // Keep a reasonable history limit
    );
    return maps.map((data) {
      final jsonStr = data['audioModelJson'] as String;
      return AudioModel.fromJson(jsonDecode(jsonStr));
    }).toList();
  }

  Future<void> syncRecentlyPlayedFromApi() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final token = prefs.getString('access_token') ?? '';
      if (token.isEmpty) return;

      final authHeader = token.startsWith('Bearer ') ? token : 'Bearer $token';

      final response = await http.get(
        Uri.parse('https://music-app-api-1.onrender.com/api/user/getRecentPlays'),
        headers: {'Authorization': authHeader},
      );

      if (response.statusCode == 200) {
        final data = json.decode(response.body);
        final List<dynamic> songsData = data['data'] ?? data['songs'] ?? data['recentPlays'] ?? [];
        
        final db = await database;
        await db.delete('recently_played'); // Clear local cache

        for (var item in songsData) {
          final song = AudioModel.fromJson(item);
          await addToRecentlyPlayed(song);
        }
      }
    } catch (e) {
      print('Failed to sync recently played: $e');
    }
  }

  // --- LIKED SONGS ---

  Future<void> likeSong(AudioModel song) async {
    if (song.songId == null) return;
    
    // Save locally
    final db = await database;
    await db.insert(
      'liked_songs',
      {
        'songId': song.songId,
        'audioModelJson': jsonEncode(song.toJson()),
        'likedAt': DateTime.now().millisecondsSinceEpoch,
      },
      conflictAlgorithm: ConflictAlgorithm.replace,
    );

    // Sync with backend
    await _toggleBackendLike(song.songId!, 'like');
  }

  Future<void> unlikeSong(String songId) async {
    // Delete locally
    final db = await database;
    await db.delete(
      'liked_songs',
      where: 'songId = ?',
      whereArgs: [songId],
    );

    // Sync with backend
    await _toggleBackendLike(songId, 'unlike');
  }

  Future<void> _toggleBackendLike(String songId, String action) async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final token = prefs.getString('auth_token');
      if (token == null) return;

      final url = Uri.parse('https://music-app-api-1.onrender.com/api/likes/song/toggle');
      await http.post(
        url,
        headers: {
          'Authorization': 'Bearer $token',
          'Content-Type': 'application/json',
        },
        body: jsonEncode({'songId': songId, 'action': action}),
      );
    } catch (e) {
      print('Failed to sync song like to backend: $e');
    }
  }

  Future<bool> isSongLiked(String songId) async {
    final db = await database;
    final maps = await db.query(
      'liked_songs',
      columns: ['songId'],
      where: 'songId = ?',
      whereArgs: [songId],
    );
    return maps.isNotEmpty;
  }

  Future<List<AudioModel>> getLikedSongs() async {
    final db = await database;
    final maps = await db.query(
      'liked_songs',
      orderBy: 'likedAt DESC',
    );
    return maps.map((data) {
      final jsonStr = data['audioModelJson'] as String;
      return AudioModel.fromJson(jsonDecode(jsonStr));
    }).toList();
  }

  // --- Search API ---
  Future<Map<String, dynamic>> searchApi(String query, {int page = 1, int limit = 20}) async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final token = prefs.getString('auth_token');

      final url = Uri.parse('https://music-app-api-1.onrender.com/api/search?q=$query&page=$page&limit=$limit');
      
      final headers = {
        'Content-Type': 'application/json',
      };
      if (token != null) {
        headers['Authorization'] = 'Bearer $token';
      }

      final response = await http.get(url, headers: headers);
      if (response.statusCode == 200) {
        final data = jsonDecode(response.body);
        if (data['status'] == true) {
          final List<AudioModel> songs = (data['songs'] as List?)
                  ?.map((s) => AudioModel.fromJson(s))
                  .toList() ??
              [];
          final List<ArtistCategory> playlists = (data['playlists'] as List?)
                  ?.map((p) => ArtistCategory.fromJson(p))
                  .toList() ??
              [];
          return {
            'songs': songs,
            'playlists': playlists,
            'hasMore': data['hasMore'] ?? false,
          };
        }
      }
      return {'songs': <AudioModel>[], 'playlists': <ArtistCategory>[], 'hasMore': false};
    } catch (e) {
      print('Error searching API: $e');
      return {'songs': <AudioModel>[], 'playlists': <ArtistCategory>[], 'hasMore': false};
    }
  }

  Future<List<AudioModel>> getCategorySongs(String categoryId) async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final token = prefs.getString('auth_token');

      final url = Uri.parse('https://music-app-api-1.onrender.com/api/search/category/$categoryId/songs');
      
      final headers = {
        'Content-Type': 'application/json',
      };
      if (token != null) {
        headers['Authorization'] = 'Bearer $token';
      }

      final response = await http.get(url, headers: headers);
      if (response.statusCode == 200) {
        final data = jsonDecode(response.body);
        if (data['status'] == true) {
          return (data['songs'] as List?)
                  ?.map((s) => AudioModel.fromJson(s))
                  .toList() ??
              [];
        }
      }
      return [];
    } catch (e) {
      print('Error fetching category songs: $e');
      return [];
    }
  }
}
