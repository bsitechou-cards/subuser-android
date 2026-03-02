import 'package:firebase_auth/firebase_auth.dart';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../core/utils/localization_util.dart';
import '../../data/models/chat_message.dart';
import '../../data/models/sub_user.dart';
import '../../data/services/card_api_service.dart';
import '../shared/bot_components.dart';
import '../shared/quick_action_item.dart';
import 'login_screen.dart';

class RegisterScreen extends StatefulWidget {
  const RegisterScreen({super.key});

  @override
  State<RegisterScreen> createState() => _RegisterScreenState();
}

class _RegisterScreenState extends State<RegisterScreen> {
  final FirebaseAuth _auth = FirebaseAuth.instance;
  final TextEditingController _inputController = TextEditingController();
  final ScrollController _scrollController = ScrollController();

  final List<ChatMessage> _messages = [];
  int _currentStep = 0; // 0: Email, 1: Password
  String _email = "";
  String _password = "";
  bool _isLoading = false;
  bool _isBotTyping = false;

  @override
  void initState() {
    super.initState();
    _startInitialBotMessage();
  }

  void _startInitialBotMessage() async {
    setState(() {
      _messages.clear();
      _isBotTyping = true;
    });
    await Future.delayed(const Duration(seconds: 1));
    if (!mounted) return;
    setState(() {
      _isBotTyping = false;
      _messages.add(Question(
        context.read<LocalizationUtil>().getString("let_create_account"),
        "email",
        translationKey: "let_create_account"
      ));
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

  Future<void> _handleNext() async {
    final localization = context.read<LocalizationUtil>();
    final finalValue = _inputController.text;

    if (finalValue.isEmpty) return;

    final isSensitive = _currentStep == 1;
    setState(() {
      _messages.add(Answer(finalValue, isSensitive: isSensitive));
      _inputController.clear();
    });
    _scrollToBottom();

    switch (_currentStep) {
      case 0:
        _email = finalValue;
        setState(() {
          _currentStep = 1;
          _isBotTyping = true;
        });
        await Future.delayed(const Duration(seconds: 1));
        setState(() {
          _isBotTyping = false;
          _messages.add(Question(localization.getString("choose_password"), "password", translationKey: "choose_password"));
        });
        break;
      case 1:
        _password = finalValue;
        if (_password.length != 6 || !RegExp(r'^[a-zA-Z0-9]+$').hasMatch(_password)) {
          setState(() {
            _isBotTyping = true;
          });
          await Future.delayed(const Duration(seconds: 1));
          setState(() {
            _isBotTyping = false;
            _messages.add(Question(localization.getString("invalid_password_format"), "password", translationKey: "invalid_password_format"));
          });
        } else {
          _performRegistration();
        }
        break;
    }
    _scrollToBottom();
  }

  Future<void> _performRegistration() async {
    final localization = context.read<LocalizationUtil>();
    setState(() {
      _isLoading = true;
    });

    try {
      final userCredential = await _auth.createUserWithEmailAndPassword(
        email: _email,
        password: _password,
      );
      final firebaseUid = userCredential.user?.uid ?? "";
      
      final response = await CardApiService.subuserAdd(SubUser(
        useremail: _email,
        userpass: _password,
        firebaseUid: firebaseUid,
      ));

      if (response?.status == "success") {
        if (!mounted) return;
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(response?.message ?? "Registration Successful")),
        );
        Navigator.pushReplacementNamed(context, '/home');
      } else {
        final errorMsg = response?.message ?? "Backend registration failed.";
        _showBotError("${localization.getString("registration_failed")}: $errorMsg");
      }
    } catch (e) {
      _showBotError("${localization.getString("registration_failed")}: ${e.toString()}");
    } finally {
      setState(() {
        _isLoading = false;
      });
      _scrollToBottom();
    }
  }

  void _showBotError(String errorText) async {
    setState(() {
      _isBotTyping = true;
    });
    await Future.delayed(const Duration(seconds: 1));
    if (!mounted) return;
    setState(() {
      _isBotTyping = false;
      _messages.add(Question(errorText, "email"));
      _currentStep = 0;
    });
  }

  @override
  Widget build(BuildContext context) {
    final localization = context.watch<LocalizationUtil>();

    return Scaffold(
      body: SafeArea(
        child: Column(
          children: [
            // Top Bar
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 16),
              decoration: BoxDecoration(
                color: Colors.white,
                boxShadow: [
                  BoxShadow(
                    color: Colors.black.withOpacity(0.05),
                    blurRadius: 2,
                    offset: const Offset(0, 2),
                  ),
                ],
              ),
              child: Row(
                children: [
                  Image.asset('assets/images/logo.png', width: 32, height: 32, errorBuilder: (_, __, ___) => const Icon(Icons.wallet, color: Colors.purple)),
                  const SizedBox(width: 10),
                  Text(
                    localization.getString("app_name"),
                    style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 18),
                  ),
                  const Spacer(),
                  QuickActionItem(
                    icon: Icons.settings,
                    label: localization.getString("settings"),
                    size: 38,
                    onClick: () => _showSettings(context),
                  ),
                ],
              ),
            ),

            // Chat Area
            Expanded(
              child: ListView.builder(
                controller: _scrollController,
                padding: const EdgeInsets.all(16),
                itemCount: _messages.length + (_isBotTyping ? 1 : 0) + (_isLoading ? 1 : 0),
                itemBuilder: (context, index) {
                  if (index < _messages.length) {
                    final message = _messages[index];
                    if (message is Question) {
                      return BotMessageBubble(text: message.text, translationKey: message.translationKey);
                    } else if (message is Answer) {
                      return UserMessageBubble(text: message.text, isSensitive: message.isSensitive);
                    }
                  } else if (index == _messages.length && _isBotTyping) {
                    return const BotTypingIndicator();
                  } else if (_isLoading) {
                    return const Center(child: Padding(padding: EdgeInsets.all(16), child: CircularProgressIndicator()));
                  }
                  return const SizedBox.shrink();
                },
              ),
            ),

            // Input Area
            if (!_isLoading && !_isBotTyping) _buildInputArea(localization),
          ],
        ),
      ),
    );
  }

  Widget _buildInputArea(LocalizationUtil localization) {
    return Container(
      decoration: BoxDecoration(
        color: Colors.white,
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.1),
            blurRadius: 8,
            offset: const Offset(0, -2),
          ),
        ],
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Padding(
            padding: const EdgeInsets.all(16.0),
            child: Row(
              children: [
                Expanded(
                  child: AutofillGroup(
                    child: TextField(
                      controller: _inputController,
                      obscureText: _currentStep == 1,
                      autocorrect: false,
                      enableSuggestions: false,
                      autofillHints: null,
                      keyboardType: _currentStep == 0 ? TextInputType.emailAddress : TextInputType.text,
                      decoration: InputDecoration(
                        hintText: _currentStep == 0 ? localization.getString("enter_email") : localization.getString("enter_password"),
                        border: OutlineInputBorder(borderRadius: BorderRadius.circular(24)),
                        contentPadding: const EdgeInsets.symmetric(horizontal: 16),
                      ),
                    ),
                  ),
                ),
                const SizedBox(width: 8),
                IconButton(
                  onPressed: _handleNext,
                  style: IconButton.styleFrom(
                    backgroundColor: Theme.of(context).primaryColor,
                    foregroundColor: Colors.white,
                  ),
                  icon: const Icon(Icons.arrow_forward),
                ),
              ],
            ),
          ),
          Padding(
            padding: const EdgeInsets.only(bottom: 8.0),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                TextButton(
                  onPressed: () => Navigator.pop(context),
                  child: Text(localization.getString("already_have_account"), style: const TextStyle(fontSize: 12)),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  void _showSettings(BuildContext context) {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.white,
      builder: (context) => const SettingsBottomSheet(),
    );
  }
}
