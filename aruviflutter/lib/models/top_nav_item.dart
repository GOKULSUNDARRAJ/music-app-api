class TopNavItem {
  final int topmenuId;
  final String topmenuName;
  final int? topmenuStatusId;
  final String? topmenuStatus;
  final String? topmenuActiveIcon;
  final String? topmenuInActiveIcon;
  final bool? isActive;

  TopNavItem({
    required this.topmenuId,
    required this.topmenuName,
    this.topmenuStatusId,
    this.topmenuStatus,
    this.topmenuActiveIcon,
    this.topmenuInActiveIcon,
    this.isActive,
  });

  factory TopNavItem.fromJson(Map<String, dynamic> json) {
    return TopNavItem(
      topmenuId: json['topmenuId'] is int ? json['topmenuId'] : int.tryParse(json['topmenuId']?.toString() ?? '0') ?? 0,
      topmenuName: json['topmenuName']?.toString() ?? '',
      topmenuStatusId: json['topmenuStatusId'] is int ? json['topmenuStatusId'] : int.tryParse(json['topmenuStatusId']?.toString() ?? ''),
      topmenuStatus: json['topmenuStatus']?.toString(),
      topmenuActiveIcon: json['topmenuActiveIcon']?.toString(),
      topmenuInActiveIcon: json['topmenuInActiveIcon']?.toString(),
      isActive: json['is_active'] is bool ? json['is_active'] : json['is_active']?.toString().toLowerCase() == 'true',
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'topmenuId': topmenuId,
      'topmenuName': topmenuName,
      'topmenuStatusId': topmenuStatusId,
      'topmenuStatus': topmenuStatus,
      'topmenuActiveIcon': topmenuActiveIcon,
      'topmenuInActiveIcon': topmenuInActiveIcon,
      'is_active': isActive,
    };
  }
}
