import 'playlist_section.dart';

class HomeResponse {
  bool status;
  String? message;
  List<PlaylistSection> sections;

  HomeResponse({
    this.status = false,
    this.message,
    this.sections = const [],
  });

  factory HomeResponse.fromJson(Map<String, dynamic> json) {
    var sectionsList = json['sections'] as List? ?? [];
    List<PlaylistSection> parsedSections = sectionsList.map((s) => PlaylistSection.fromJson(s)).toList();

    return HomeResponse(
      status: json['status'] == true || json['status'] == 'true',
      message: json['message']?.toString(),
      sections: parsedSections,
    );
  }
}
