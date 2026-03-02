import 'dart:convert';
import 'card_item.dart';

class CardResponse {
  final String? code;
  final String status;
  final String message;
  final List<CardItem> data;
  final double? subuserfee;

  CardResponse({
    this.code,
    required this.status,
    required this.message,
    this.data = const [],
    this.subuserfee,
  });

  factory CardResponse.fromJson(dynamic json) {
    if (json is String) {
      try {
        json = jsonDecode(json);
      } catch (e) {
        return CardResponse(status: "error", message: "Invalid JSON");
      }
    }

    if (json is! Map<String, dynamic>) {
      return CardResponse(status: "error", message: "Response is not a Map");
    }

    var dataJson = json['data'];
    if (dataJson is String) {
      try {
        dataJson = jsonDecode(dataJson);
      } catch (e) {
        dataJson = [];
      }
    }

    List<CardItem> cardItems = [];
    if (dataJson is List) {
      for (var item in dataJson) {
        if (item is Map<String, dynamic>) {
          cardItems.add(CardItem.fromJson(item));
        } else if (item is String) {
          try {
            final decodedItem = jsonDecode(item);
            if (decodedItem is Map<String, dynamic>) {
              cardItems.add(CardItem.fromJson(decodedItem));
            }
          } catch (_) {}
        }
      }
    }

    return CardResponse(
      code: json['code']?.toString(),
      status: json['status']?.toString() ?? "",
      message: json['message']?.toString() ?? "",
      data: cardItems,
      subuserfee: (json['subuserfee'] as num?)?.toDouble(),
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'code': code,
      'status': status,
      'message': message,
      'data': data.map((item) => item.toJson()).toList(),
      'subuserfee': subuserfee,
    };
  }
}
