class AudioModel {
  String? songId;
  String? audioName;
  String? audioUrl;
  String? categoryName;
  String? categoryId;
  String? imageUrl;
  String? downloadPath;
  bool isDownloaded;
  int? fileSize;
  String? duration;
  int? durationInMillis;
  String? playlistId;

  AudioModel({
    this.songId,
    this.audioName,
    this.audioUrl,
    this.categoryName,
    this.categoryId,
    this.imageUrl,
    this.downloadPath,
    this.isDownloaded = false,
    this.fileSize,
    this.duration,
    this.durationInMillis,
    this.playlistId,
  });

  factory AudioModel.fromJson(Map<String, dynamic> json) {
    return AudioModel(
      songId: json['songId']?.toString() ?? json['id']?.toString(),
      audioName: json['audioName'] ?? json['song_name'] ?? json['name'],
      audioUrl: json['audioUrl'] ?? json['song_url'] ?? json['url'],
      categoryName: json['categoryName'] ?? json['artist'] ?? json['artist_name'],
      categoryId: json['categoryId']?.toString() ?? json['category_id']?.toString(),
      imageUrl: json['imageUrl'] ?? json['song_image'] ?? json['image'],
      isDownloaded: json['isDownloaded'] ?? false,
      fileSize: json['fileSize'] != null ? int.tryParse(json['fileSize'].toString()) : null,
      duration: json['duration'],
      playlistId: json['playlistId']?.toString(),
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'songId': songId,
      'audioName': audioName,
      'audioUrl': audioUrl,
      'categoryName': categoryName,
      'categoryId': categoryId,
      'imageUrl': imageUrl,
      'isDownloaded': isDownloaded,
      'downloadPath': downloadPath,
      'fileSize': fileSize,
      'duration': duration,
      'playlistId': playlistId,
    };
  }
}
