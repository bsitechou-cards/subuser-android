class CardDetailsResponse {
  final int code;
  final String status;
  final String message;
  final CardDetails data;

  CardDetailsResponse({
    required this.code,
    required this.status,
    required this.message,
    required this.data,
  });

  factory CardDetailsResponse.fromJson(Map<String, dynamic> json) {
    return CardDetailsResponse(
      code: json['code'] is int ? json['code'] : int.tryParse(json['code']?.toString() ?? "0") ?? 0,
      status: json['status'] ?? "",
      message: json['message'] ?? "",
      data: CardDetails.fromJson(json['data'] ?? {}),
    );
  }
}

class CardDetails {
  final String cardNumber;
  final String expiryMonth;
  final String expiryYear;
  final String cvv;
  final String nameoncard;
  final double balance;
  final String status;
  final List<TransactionItem> transactions;
  final List<Deposit> deposits;
  final String? depositaddress;
  final String? btcdepositaddress;
  final String? ethdepositaddress;
  final String? usdtdepositaddress;
  final String? soldepositaddress;
  final String? bnbdepositaddress;
  final String? xrpdepositaddress;
  final String? paxgdepositaddress;
  final String? address1;
  final String? city;
  final String? state;
  final String? country;
  final String? postalCode;

  CardDetails({
    required this.cardNumber,
    required this.expiryMonth,
    required this.expiryYear,
    required this.cvv,
    required this.nameoncard,
    required this.balance,
    required this.status,
    this.transactions = const [],
    this.deposits = const [],
    this.depositaddress,
    this.btcdepositaddress,
    this.ethdepositaddress,
    this.usdtdepositaddress,
    this.soldepositaddress,
    this.bnbdepositaddress,
    this.xrpdepositaddress,
    this.paxgdepositaddress,
    this.address1,
    this.city,
    this.state,
    this.country,
    this.postalCode,
  });

  factory CardDetails.fromJson(Map<String, dynamic> json) {
    var transWrapper = json['transactions'];
    List<TransactionItem> transList = [];
    if (transWrapper != null && transWrapper['response'] != null && transWrapper['response']['items'] != null) {
      transList = (transWrapper['response']['items'] as List)
          .map((i) => TransactionItem.fromJson(i))
          .toList();
    }

    return CardDetails(
      cardNumber: json['card_number'] ?? "",
      expiryMonth: json['expiry_month'] ?? "",
      expiryYear: json['expiry_year'] ?? "",
      cvv: json['cvv'] ?? "",
      nameoncard: json['nameoncard'] ?? "",
      balance: (json['balance'] as num?)?.toDouble() ?? 0.0,
      status: json['status'] ?? "",
      transactions: transList,
      deposits: (json['deposits'] as List? ?? [])
          .map((i) => Deposit.fromJson(i))
          .toList(),
      depositaddress: json['depositaddress'],
      btcdepositaddress: json['btcdepositaddress'],
      ethdepositaddress: json['ethdepositaddress'],
      usdtdepositaddress: json['usdtdepositaddress'],
      soldepositaddress: json['soldepositaddress'],
      bnbdepositaddress: json['bnbdepositaddress'],
      xrpdepositaddress: json['xrpdepositaddress'],
      paxgdepositaddress: json['paxgdepositaddress'],
      address1: json['address1'],
      city: json['city'],
      state: json['state'],
      country: json['country'],
      postalCode: json['postalCode'],
    );
  }
}

class Deposit {
  final String id;
  final double amount;
  final String transactionHash;
  final String createdAt;

  Deposit({
    required this.id,
    required this.amount,
    required this.transactionHash,
    required this.createdAt,
  });

  factory Deposit.fromJson(Map<String, dynamic> json) {
    return Deposit(
      id: json['id']?.toString() ?? "",
      amount: (json['amount'] as num?)?.toDouble() ?? 0.0,
      transactionHash: json['transactionHash'] ?? "",
      createdAt: json['createdAt'] ?? "",
    );
  }
}

class TransactionItem {
  final String id;
  final double amount;
  final String currency;
  final String status;
  final String paymentDateTime;
  final Merchant merchant;
  final String type;

  TransactionItem({
    required this.id,
    required this.amount,
    required this.currency,
    required this.status,
    required this.paymentDateTime,
    required this.merchant,
    required this.type,
  });

  factory TransactionItem.fromJson(Map<String, dynamic> json) {
    return TransactionItem(
      id: json['id']?.toString() ?? "",
      amount: (json['amount'] as num?)?.toDouble() ?? 0.0,
      currency: json['currency'] ?? "",
      status: json['status'] ?? "",
      paymentDateTime: json['paymentDateTime'] ?? "",
      merchant: Merchant.fromJson(json['merchant'] ?? {}),
      type: json['type'] ?? "",
    );
  }
}

class Merchant {
  final String name;
  final String city;
  final String country;

  Merchant({
    required this.name,
    required this.city,
    required this.country,
  });

  factory Merchant.fromJson(Map<String, dynamic> json) {
    return Merchant(
      name: json['name'] ?? "",
      city: json['city'] ?? "",
      country: json['country'] ?? "",
    );
  }
}
