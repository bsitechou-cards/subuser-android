import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:intl/intl.dart';
import '../../core/utils/localization_util.dart';
import '../../core/utils/country_data.dart';
import '../../data/models/chat_message.dart';
import '../../data/models/apply_card_request.dart';
import '../../data/services/card_api_service.dart';
import '../shared/bot_components.dart';

class ApplyForCardBottomSheet extends StatefulWidget {
  final String userEmail;
  final double subuserFee;
  final VoidCallback onCardApplied;
  final Function(String, String) onShowQrCode;

  const ApplyForCardBottomSheet({
    super.key,
    required this.userEmail,
    required this.subuserFee,
    required this.onCardApplied,
    required this.onShowQrCode,
  });

  @override
  State<ApplyForCardBottomSheet> createState() => _ApplyForCardBottomSheetState();
}

class _ApplyForCardBottomSheetState extends State<ApplyForCardBottomSheet> {
  final TextEditingController _inputController = TextEditingController();
  final ScrollController _scrollController = ScrollController();
  final List<ChatMessage> _messages = [];
  
  int _currentStep = 0;
  bool _isBotTyping = false;
  bool _isSubmitting = false;

  // Form Data
  String _firstName = "";
  String _lastName = "";
  String _dob = "";
  String _address1 = "";
  String _postalCode = "";
  String _city = "";
  String _country = "";
  String _state = "";
  String _countryCode = "";
  String _phone = "";

  late List<String> _steps;
  final List<String> _fields = [
    "feeConfirmation", "firstName", "lastName", "dob", "countryCode", "phone", 
    "address1", "city", "state", "country", "postalCode"
  ];

  @override
  void initState() {
    super.initState();
    _initSteps();
    _startBot();
  }

  void _initSteps() {
    final loc = context.read<LocalizationUtil>();
    _steps = [
      loc.getString("step_fee").replaceAll("%s", (widget.subuserFee + 5).toStringAsFixed(2)),
      loc.getString("step_first_name"),
      loc.getString("step_last_name"),
      loc.getString("step_dob"),
      loc.getString("step_country_code"),
      loc.getString("step_phone"),
      loc.getString("step_address"),
      loc.getString("step_city"),
      loc.getString("step_state"),
      loc.getString("step_country"),
      loc.getString("step_postal"),
    ];
  }

  void _startBot() async {
    setState(() => _isBotTyping = true);
    await Future.delayed(const Duration(milliseconds: 1500));
    if (!mounted) return;
    setState(() {
      _isBotTyping = false;
      _messages.add(Question(_steps[0], _fields[0]));
    });
  }

  void _scrollToBottom() {
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (_scrollController.hasClients) {
        _scrollController.animateTo(
          _scrollController.position.maxScrollExtent,
          duration: const Duration(milliseconds: 300),
          curve: Curves.easeOut,
        );
      }
    });
  }

  Future<void> _handleNext({String? customValue}) async {
    final finalValue = customValue ?? _inputController.text;
    if (finalValue.isEmpty && ![0, 3, 4, 9].contains(_currentStep)) return;

    if (_currentStep == 5 && !RegExp(r'^[0-9]+$').hasMatch(finalValue)) {
      setState(() {
        _messages.add(Answer(finalValue));
        _isBotTyping = true;
      });
      await Future.delayed(const Duration(seconds: 1));
      setState(() {
        _isBotTyping = false;
        _messages.add(Question(context.read<LocalizationUtil>().getString("invalid_phone"), _fields[_currentStep]));
      });
      _scrollToBottom();
      return;
    }

    setState(() {
      _messages.add(Answer(_getDisplayValue(finalValue)));
      _inputController.clear();
    });
    _scrollToBottom();

    _saveStepData(finalValue);

    if (_currentStep < _steps.length - 1) {
      setState(() {
        _currentStep++;
        _isBotTyping = true;
      });
      await Future.delayed(const Duration(milliseconds: 1500));
      setState(() {
        _isBotTyping = false;
        _messages.add(Question(_steps[_currentStep], _fields[_currentStep]));
      });
    } else {
      _submitApplication();
    }
    _scrollToBottom();
  }

  String _getDisplayValue(String val) {
    if (_currentStep == 0) return context.read<LocalizationUtil>().getString("confirm");
    if (_currentStep == 4) {
      final country = CountryData.countries.firstWhere((c) => c['dial_code'] == val, orElse: () => {});
      return country.isNotEmpty ? "${country['name']} ($val)" : val;
    }
    if (_currentStep == 9) {
      final country = CountryData.countries.firstWhere((c) => c['code'] == val, orElse: () => {});
      return country.isNotEmpty ? country['name']! : val;
    }
    return val;
  }

  void _saveStepData(String val) {
    switch (_currentStep) {
      case 1: _firstName = val; break;
      case 2: _lastName = val; break;
      case 3: _dob = val; break;
      case 4: _countryCode = val; break;
      case 5: _phone = val; break;
      case 6: _address1 = val; break;
      case 7: _city = val; break;
      case 8: _state = val; break;
      case 9: _country = val; break;
      case 10: _postalCode = val; break;
    }
  }

  Future<void> _submitApplication() async {
    setState(() => _isBotTyping = true);
    await Future.delayed(const Duration(seconds: 1));
    setState(() {
      _isBotTyping = false;
      _messages.add(Question(context.read<LocalizationUtil>().getString("thank_you_apply"), "done"));
    });
    await Future.delayed(const Duration(milliseconds: 1500));
    setState(() => _isSubmitting = true);

    final request = ApplyCardRequest(
      useremail: widget.userEmail,
      firstname: _firstName,
      lastname: _lastName,
      dob: _dob,
      address1: _address1,
      postalcode: _postalCode,
      city: _city,
      country: _country,
      state: _state,
      countrycode: _countryCode,
      phone: _phone.replaceAll(RegExp(r'[^0-9]'), ''),
    );

    final response = await CardApiService.applyForNewVirtualCard(request);
    
    if (!mounted) return;
    if (response?.status == "success" && response?.depositaddress != null) {
      widget.onShowQrCode(response!.depositaddress!, response.subuserfee.toString());
    } else if (response?.status == "success") {
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(response?.message ?? "Success")));
      widget.onCardApplied();
    } else {
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(response?.message ?? "Failed")));
      Navigator.pop(context);
    }
    setState(() => _isSubmitting = false);
  }

  @override
  Widget build(BuildContext context) {
    final localization = context.watch<LocalizationUtil>();
    return Padding(
      padding: EdgeInsets.only(bottom: MediaQuery.of(context).viewInsets.bottom),
      child: Container(
        height: MediaQuery.of(context).size.height * 0.8,
        decoration: const BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
        ),
        child: Column(
          children: [
            Padding(
              padding: const EdgeInsets.all(16.0),
              child: Text(
                localization.getString("apply_for_card"),
                style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 20),
              ),
            ),
            Expanded(
              child: ListView.builder(
                controller: _scrollController,
                padding: const EdgeInsets.symmetric(horizontal: 16),
                itemCount: _messages.length + (_isBotTyping ? 1 : 0) + (_isSubmitting ? 1 : 0),
                itemBuilder: (context, index) {
                  if (index < _messages.length) {
                    final msg = _messages[index];
                    return Padding(
                      padding: const EdgeInsets.only(bottom: 8),
                      child: msg is Question 
                        ? BotMessageBubble(text: msg.text)
                        : UserMessageBubble(text: msg.text),
                    );
                  } else if (_isBotTyping) {
                    return const BotTypingIndicator();
                  } else if (_isSubmitting) {
                    return const Center(child: Padding(padding: EdgeInsets.all(16), child: CircularProgressIndicator()));
                  }
                  return const SizedBox.shrink();
                },
              ),
            ),
            if (!_isSubmitting) _buildInput(localization),
          ],
        ),
      ),
    );
  }

  Widget _buildInput(LocalizationUtil localization) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white,
        boxShadow: [BoxShadow(color: Colors.black.withOpacity(0.1), blurRadius: 8, offset: const Offset(0, -2))],
      ),
      child: _isBotTyping 
        ? const SizedBox(height: 48, child: Center(child: Text("Bot is typing...", style: TextStyle(color: Colors.grey, fontSize: 12))))
        : _currentStep == 0 
          ? Row(
              children: [
                Expanded(child: ElevatedButton(onPressed: () => Navigator.pop(context), style: ElevatedButton.styleFrom(backgroundColor: Colors.grey), child: Text(localization.getString("cancel")))),
                const SizedBox(width: 8),
                Expanded(child: ElevatedButton(onPressed: () => _handleNext(customValue: "apply"), child: Text(localization.getString("apply")))),
              ],
            )
          : _currentStep == 3 
            ? ElevatedButton(
                onPressed: () async {
                  final date = await showDatePicker(
                    context: context,
                    initialDate: DateTime.now().subtract(const Duration(days: 365 * 18)),
                    firstDate: DateTime(1900),
                    lastDate: DateTime.now(),
                  );
                  if (date != null) {
                    _handleNext(customValue: DateFormat('yyyy-MM-dd').format(date));
                  }
                },
                child: Text(localization.getString("select_dob")),
              )
            : _currentStep == 4
              ? ElevatedButton(
                  onPressed: () => _showCountryPicker(isDialCode: true),
                  child: Text(localization.getString("search_country_code")),
                )
              : _currentStep == 9
                ? ElevatedButton(
                    onPressed: () => _showCountryPicker(isDialCode: false),
                    child: Text(localization.getString("search_country")),
                  )
                : Row(
                    children: [
                      Expanded(
                        child: TextField(
                          controller: _inputController,
                          keyboardType: [5].contains(_currentStep) ? TextInputType.number : TextInputType.text,
                          decoration: InputDecoration(
                            hintText: "Type your answer...",
                            border: OutlineInputBorder(borderRadius: BorderRadius.circular(24)),
                            contentPadding: const EdgeInsets.symmetric(horizontal: 16),
                          ),
                          onSubmitted: (_) => _handleNext(),
                        ),
                      ),
                      const SizedBox(width: 8),
                      IconButton(
                        onPressed: () => _handleNext(),
                        style: IconButton.styleFrom(backgroundColor: Theme.of(context).primaryColor, foregroundColor: Colors.white),
                        icon: const Icon(Icons.arrow_forward),
                      ),
                    ],
                  ),
    );
  }

  void _showCountryPicker({required bool isDialCode}) {
    String searchQuery = "";
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.white,
      shape: const RoundedRectangleBorder(borderRadius: BorderRadius.vertical(top: Radius.circular(24))),
      builder: (context) {
        return StatefulBuilder(
          builder: (context, setModalState) {
            final filteredCountries = CountryData.countries.where((country) {
              final name = country['name']!.toLowerCase();
              final code = country['code']!.toLowerCase();
              final dialCode = country['dial_code']!.toLowerCase();
              final query = searchQuery.toLowerCase();
              return name.contains(query) || code.contains(query) || dialCode.contains(query);
            }).toList();

            return Container(
              height: MediaQuery.of(context).size.height * 0.7,
              padding: const EdgeInsets.all(16),
              child: Column(
                children: [
                  TextField(
                    decoration: InputDecoration(
                      hintText: "Search...",
                      prefixIcon: const Icon(Icons.search),
                      border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
                    ),
                    onChanged: (val) {
                      setModalState(() {
                        searchQuery = val;
                      });
                    },
                  ),
                  const SizedBox(height: 16),
                  Expanded(
                    child: ListView.builder(
                      itemCount: filteredCountries.length,
                      itemBuilder: (context, index) {
                        final country = filteredCountries[index];
                        final name = country['name']!;
                        final code = country['code']!;
                        final dialCode = country['dial_code']!;
                        
                        return ListTile(
                          title: Text(name),
                          subtitle: Text(isDialCode ? dialCode : code),
                          onTap: () {
                            Navigator.pop(context);
                            _handleNext(customValue: isDialCode ? dialCode : code);
                          },
                        );
                      },
                    ),
                  ),
                ],
              ),
            );
          }
        );
      },
    );
  }
}
