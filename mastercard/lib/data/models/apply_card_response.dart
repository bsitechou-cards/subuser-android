class ApplyCardResponse {
  final String? code;
  final String status;
  final String message;
  final String? depositaddress;
  final double? subuserfee;

  ApplyCardResponse({
    this.code,
    this.status = "",
    this.message = "",
    this.depositaddress,
    this.subuserfee,
  });

  factory ApplyCardResponse.fromJson(Map<String, dynamic> json) {
    return ApplyCardResponse(
      code: json['code']?.toString(),
      status: json['status'] ?? "",
      message: json['message'] ?? "",
      depositaddress: json['depositaddress'],
      subuserfee: (json['subuserfee'] as num?)?.toDouble(),
    );
  }
}
