class ThreeDSResponse {
  final String status;
  final String code;
  final ThreeDSData? data;

  ThreeDSResponse({
    required this.status,
    required this.code,
    this.data,
  });

  factory ThreeDSResponse.fromJson(Map<String, dynamic> json) {
    return ThreeDSResponse(
      status: json['status'] ?? "",
      code: json['code']?.toString() ?? "",
      data: json['data'] != null ? ThreeDSData.fromJson(json['data']) : null,
    );
  }
}

class ThreeDSData {
  final int id;
  final String eventId;
  final String cardId;
  final String merchantName;
  final String maskedPan;
  final String merchantAmount;
  final String merchantCurrency;
  final String eventName;
  final String status;
  final String createdAt;

  ThreeDSData({
    required this.id,
    required this.eventId,
    required this.cardId,
    required this.merchantName,
    required this.maskedPan,
    required this.merchantAmount,
    required this.merchantCurrency,
    required this.eventName,
    required this.status,
    required this.createdAt,
  });

  factory ThreeDSData.fromJson(Map<String, dynamic> json) {
    return ThreeDSData(
      id: json['id'] is int ? json['id'] : int.tryParse(json['id']?.toString() ?? "0") ?? 0,
      eventId: json['eventId'] ?? "",
      cardId: json['cardId']?.toString() ?? "",
      merchantName: json['merchantName'] ?? "",
      maskedPan: json['maskedPan'] ?? "",
      merchantAmount: json['merchantAmount']?.toString() ?? "0",
      merchantCurrency: json['merchantCurrency'] ?? "",
      eventName: json['eventName'] ?? "",
      status: json['status'] ?? "",
      createdAt: json['created_at'] ?? "",
    );
  }
}
