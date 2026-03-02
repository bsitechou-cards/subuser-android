class CardItem {
  final String? cardid;
  final String nameoncard;
  final String useremail;
  final String lastfour;
  final String brand;
  final String type;
  final int paidcard;
  final String? depositaddress;

  CardItem({
    this.cardid,
    required this.nameoncard,
    required this.useremail,
    this.lastfour = "",
    required this.brand,
    required this.type,
    this.paidcard = 1,
    this.depositaddress,
  });

  factory CardItem.fromJson(Map<String, dynamic> json) {
    return CardItem(
      cardid: json['cardid']?.toString(),
      nameoncard: json['nameoncard'] ?? "",
      useremail: json['useremail'] ?? "",
      lastfour: json['lastfour']?.toString() ?? "",
      brand: json['brand'] ?? "",
      type: json['type'] ?? "",
      paidcard: json['paidcard'] is int ? json['paidcard'] : int.tryParse(json['paidcard']?.toString() ?? "1") ?? 1,
      depositaddress: json['depositaddress'],
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'cardid': cardid,
      'nameoncard': nameoncard,
      'useremail': useremail,
      'lastfour': lastfour,
      'brand': brand,
      'type': type,
      'paidcard': paidcard,
      'depositaddress': depositaddress,
    };
  }
}
