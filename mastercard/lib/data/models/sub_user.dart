class SubUser {
  final String useremail;
  final String userpass;
  final String firebaseUid;

  SubUser({
    required this.useremail,
    required this.userpass,
    required this.firebaseUid,
  });

  Map<String, dynamic> toJson() {
    return {
      'useremail': useremail,
      'userpass': userpass,
      'firebase_uid': firebaseUid,
    };
  }
}
