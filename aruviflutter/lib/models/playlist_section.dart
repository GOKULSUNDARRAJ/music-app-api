import 'artist_category.dart';

class PlaylistSection {
  String? sectionId;
  String? sectionTitle;
  List<ArtistCategory> categories;
  int layoutType;
  int spanCount;

  PlaylistSection({
    this.sectionId,
    this.sectionTitle,
    this.categories = const [],
    this.layoutType = 1,
    this.spanCount = 2,
  });

  factory PlaylistSection.fromJson(Map<String, dynamic> json) {
    var categoriesList = json['categories'] as List? ?? [];
    List<ArtistCategory> parsedCategories = categoriesList.map((c) => ArtistCategory.fromJson(c)).toList();

    return PlaylistSection(
      sectionId: json['sectionId']?.toString(),
      sectionTitle: json['sectionTitle']?.toString() ?? json['sectionName']?.toString(),
      layoutType: json['layoutType'] != null ? int.tryParse(json['layoutType'].toString()) ?? 1 : 1,
      spanCount: json['spanCount'] != null ? int.tryParse(json['spanCount'].toString()) ?? 2 : 2,
      categories: parsedCategories,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'sectionId': sectionId,
      'sectionTitle': sectionTitle,
      'layoutType': layoutType,
      'spanCount': spanCount,
      'categories': categories.map((c) => c.toJson()).toList(),
    };
  }
}
