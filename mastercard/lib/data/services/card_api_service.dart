import 'dart:convert';
import 'package:dio/dio.dart';
import '../../core/network/api_config.dart';
import '../models/apply_card_request.dart';
import '../models/apply_card_response.dart';
import '../models/card_details_response.dart';
import '../models/card_response.dart';
import '../models/sub_user.dart';
import '../models/three_ds_response.dart';

class CardApiService {
  static final Dio _dio = Dio(
    BaseOptions(
      baseUrl: ApiConfig.baseUrl,
      connectTimeout: const Duration(seconds: 15),
      receiveTimeout: const Duration(seconds: 30),
      headers: {
        'publickey': ApiConfig.publicKey,
        'secretkey': ApiConfig.secretKey,
        'Content-Type': 'application/json',
      },
    ),
  );

  static Future<ApplyCardResponse?> subuserAdd(SubUser subUser) async {
    try {
      final response = await _dio.post('subuseradd', data: subUser.toJson());
      if (response.data != null) {
        var responseData = response.data;
        if (responseData is String) {
          responseData = jsonDecode(responseData);
        }
        return ApplyCardResponse.fromJson(responseData);
      }
    } catch (e) {
      print("subuseradd error: $e");
    }
    return null;
  }

  static Future<ApplyCardResponse?> applyForNewVirtualCard(ApplyCardRequest request) async {
    try {
      final response = await _dio.post('digitalnewsubusercard', data: request.toJson());
      if (response.data != null) {
        var responseData = response.data;
        if (responseData is String) {
          responseData = jsonDecode(responseData);
        }
        return ApplyCardResponse.fromJson(responseData);
      }
    } catch (e) {
      print("applyForNewVirtualCard error: $e");
    }
    return null;
  }

  static Future<CardResponse?> getAllDigitalCards(String userEmail) async {
    try {
      final data = {'useremail': userEmail};
      print("getAllDigitalCards request: $data");
      final response = await _dio.post('getsubuseralldigital', data: data);
      
      var responseData = response.data;
      print("getAllDigitalCards response type: ${responseData.runtimeType}");
      
      if (responseData is String) {
        try {
          responseData = jsonDecode(responseData);
        } catch (e) {
          print("Error decoding JSON string: $e");
          return null;
        }
      }

      if (responseData != null && responseData is Map<String, dynamic>) {
        return CardResponse.fromJson(responseData);
      } else if (responseData != null) {
        print("Unexpected response data type: ${responseData.runtimeType}");
      }
    } catch (e) {
      print("getAllDigitalCards error: $e");
    }
    return null;
  }

  static Future<CardDetailsResponse?> getDigitalCardDetails(String email, String cardId) async {
    try {
      final response = await _dio.post('getsubuserdigitalcard', data: {
        'useremail': email,
        'cardid': cardId,
      });
      if (response.data != null) {
        var responseData = response.data;
        if (responseData is String) {
          responseData = jsonDecode(responseData);
        }
        return CardDetailsResponse.fromJson(responseData);
      }
    } catch (e) {
      print("getDigitalCardDetails error: $e");
    }
    return null;
  }

  static Future<ThreeDSResponse?> check3ds(String email, String cardId) async {
    try {
      final response = await _dio.post('subusercheck3ds', data: {
        'useremail': email,
        'cardid': cardId,
      });
      if (response.data != null) {
        var responseData = response.data;
        if (responseData is String) {
          responseData = jsonDecode(responseData);
        }
        return ThreeDSResponse.fromJson(responseData);
      }
    } on DioException catch (e) {
      print("check3ds DioException: ${e.response?.statusCode} - ${e.response?.data}");
      if (e.response?.statusCode == 422) {
        return ThreeDSResponse(status: "error", code: "422");
      }
      if (e.response?.data != null) {
        var responseData = e.response!.data;
        if (responseData is String) {
          try {
            responseData = jsonDecode(responseData);
          } catch (_) {}
        }
        if (responseData is Map<String, dynamic>) {
          return ThreeDSResponse.fromJson(responseData);
        }
      }
    } catch (e) {
      print("check3ds error: $e");
    }
    return null;
  }

  static Future<bool> approve3ds(String email, String cardId, String eventId) async {
    try {
      final response = await _dio.post('subuserapprove3ds', data: {
        'useremail': email,
        'cardid': cardId,
        'eventId': eventId,
      });
      return response.statusCode == 200;
    } catch (e) {
      print("approve3ds error: $e");
    }
    return false;
  }

  static Future<ApplyCardResponse?> blockDigitalCard(String email, String cardId) async {
    try {
      final response = await _dio.post('subuserblockdigital', data: {
        'useremail': email,
        'cardid': cardId,
      });
      if (response.data != null) {
        var responseData = response.data;
        if (responseData is String) {
          responseData = jsonDecode(responseData);
        }
        return ApplyCardResponse.fromJson(responseData);
      }
    } catch (e) {
      print("blockDigitalCard error: $e");
    }
    return null;
  }

  static Future<ApplyCardResponse?> unblockDigitalCard(String email, String cardId) async {
    try {
      final response = await _dio.post('subuserunblockdigital', data: {
        'useremail': email,
        'cardid': cardId,
      });
      if (response.data != null) {
        var responseData = response.data;
        if (responseData is String) {
          responseData = jsonDecode(responseData);
        }
        return ApplyCardResponse.fromJson(responseData);
      }
    } catch (e) {
      print("unblockDigitalCard error: $e");
    }
    return null;
  }
}
