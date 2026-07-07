import 'audio_model.dart';

class ArtistCategory {
  String? categoryId;
  String? categoryName;
  List<AudioModel> songs;
  String? categoryImage;
  int adapterType;

  ArtistCategory({
    this.categoryId,
    this.categoryName,
    this.songs = const [],
    this.categoryImage,
    this.adapterType = 1,
  });

  factory ArtistCategory.fromJson(Map<String, dynamic> json) {
    var songsList = json['songs'] as List? ?? [];
    List<AudioModel> parsedSongs = songsList.map((s) => AudioModel.fromJson(s)).toList();

    return ArtistCategory(
      categoryId: json['categoryId']?.toString(),
      categoryName: json['categoryName']?.toString() ?? json['artistName']?.toString(),
      categoryImage: json['categoryImage']?.toString() ?? json['artistImageUrl']?.toString(),
      adapterType: json['adapterType'] != null ? int.tryParse(json['adapterType'].toString()) ?? 1 : 1,
      songs: parsedSongs,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'categoryId': categoryId,
      'categoryName': categoryName,
      'categoryImage': categoryImage,
      'adapterType': adapterType,
      'songs': songs.map((s) => s.toJson()).toList(),
    };
  }
}
