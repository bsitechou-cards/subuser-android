class ApplyCardRequest {
  final String useremail;
  final String firstname;
  final String lastname;
  final String dob;
  final String address1;
  final String postalcode;
  final String city;
  final String country;
  final String state;
  final String countrycode;
  final String phone;

  ApplyCardRequest({
    required this.useremail,
    required this.firstname,
    required this.lastname,
    required this.dob,
    required this.address1,
    required this.postalcode,
    required this.city,
    required this.country,
    required this.state,
    required this.countrycode,
    required this.phone,
  });

  Map<String, dynamic> toJson() {
    return {
      'useremail': useremail,
      'firstname': firstname,
      'lastname': lastname,
      'dob': dob,
      'address1': address1,
      'postalcode': postalcode,
      'city': city,
      'country': country,
      'state': state,
      'countrycode': countrycode,
      'phone': phone,
    };
  }
}
