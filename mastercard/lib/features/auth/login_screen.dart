import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:firebase_auth/firebase_auth.dart';
import '../../core/utils/localization_util.dart';
import '../../data/models/chat_message.dart';
import '../../features/shared/bot_components.dart';
import '../../features/shared/quick_action_item.dart';

class LoginScreen extends StatefulWidget {
  const LoginScreen({super.key});

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  final FirebaseAuth _auth = FirebaseAuth.instance;
  final TextEditingController _inputController = TextEditingController();
  final ScrollController _scrollController = ScrollController();
  
  final List<ChatMessage> _messages = [];
  int _currentStep = 0; // 0: Registered?, 1: Email, 2: Password
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
        context.read<LocalizationUtil>().getString("welcome_registered"), 
        "is_registered",
        translationKey: "welcome_registered"
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

  Future<void> _handleNext({String? customValue}) async {
    final localization = context.read<LocalizationUtil>();
    final finalValue = customValue ?? _inputController.text;
    
    if (finalValue.isEmpty && _currentStep != 0) return;

    final isSensitive = _currentStep == 2;
    setState(() {
      _messages.add(Answer(finalValue, isSensitive: isSensitive));
      _inputController.clear();
    });
    _scrollToBottom();

    switch (_currentStep) {
      case 0:
        final yesTranslated = localization.getString("yes").toLowerCase();
        if (finalValue.toLowerCase() == yesTranslated || 
            finalValue.toLowerCase() == "yes" || 
            finalValue.toLowerCase() == "oui") {
          setState(() {
            _currentStep = 1;
            _isBotTyping = true;
          });
          await Future.delayed(const Duration(seconds: 1));
          setState(() {
            _isBotTyping = false;
            _messages.add(Question(localization.getString("great_email"), "email", translationKey: "great_email"));
          });
        } else {
          Navigator.pushNamed(context, '/register');
        }
        break;
      case 1:
        _email = finalValue;
        setState(() {
          _currentStep = 2;
          _isBotTyping = true;
        });
        await Future.delayed(const Duration(seconds: 1));
        setState(() {
          _isBotTyping = false;
          _messages.add(Question(localization.getString("now_password"), "password", translationKey: "now_password"));
        });
        break;
      case 2:
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
          _performLogin();
        }
        break;
    }
    _scrollToBottom();
  }

  Future<void> _performLogin() async {
    final localization = context.read<LocalizationUtil>();
    setState(() {
      _isLoading = true;
    });

    try {
      await _auth.signInWithEmailAndPassword(email: _email, password: _password);
      if (!mounted) return;
      Navigator.pushReplacementNamed(context, '/home');
    } catch (e) {
      setState(() {
        _isBotTyping = true;
      });
      await Future.delayed(const Duration(seconds: 1));
      if (!mounted) return;
      setState(() {
        _isBotTyping = false;
        _messages.add(Question("${localization.getString("login_failed")}: ${e.toString()}", "email"));
        _currentStep = 1;
      });
    } finally {
      setState(() {
        _isLoading = false;
      });
      _scrollToBottom();
    }
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
            child: _currentStep == 0 
              ? Row(
                  children: [
                    Expanded(
                      child: ElevatedButton(
                        onPressed: () => _handleNext(customValue: localization.getString("yes")),
                        child: Text(localization.getString("yes")),
                      ),
                    ),
                    const SizedBox(width: 8),
                    Expanded(
                      child: OutlinedButton(
                        onPressed: () => _handleNext(customValue: localization.getString("no")),
                        child: Text(localization.getString("no")),
                      ),
                    ),
                  ],
                )
              : Row(
                  children: [
                    Expanded(
                      child: AutofillGroup(
                        child: TextField(
                          controller: _inputController,
                          obscureText: _currentStep == 2,
                          autocorrect: false,
                          enableSuggestions: false,
                          autofillHints: null,
                          keyboardType: _currentStep == 1 ? TextInputType.emailAddress : TextInputType.text,
                          decoration: InputDecoration(
                            hintText: _currentStep == 1 ? localization.getString("enter_email") : localization.getString("enter_password"),
                            border: OutlineInputBorder(borderRadius: BorderRadius.circular(24)),
                            contentPadding: const EdgeInsets.symmetric(horizontal: 16),
                          ),
                        ),
                      ),
                    ),
                    const SizedBox(width: 8),
                    IconButton(
                      onPressed: () => _handleNext(),
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
                  onPressed: () => Navigator.pushNamed(context, '/register'),
                  child: Text(localization.getString("register"), style: const TextStyle(fontSize: 12)),
                ),
                const Text("|", style: TextStyle(color: Colors.grey)),
                TextButton(
                  onPressed: _handleForgotPassword,
                  child: Text(localization.getString("forgot_password"), style: const TextStyle(fontSize: 12)),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  void _handleForgotPassword() {
    // Logic for forgot password
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

class SettingsBottomSheet extends StatelessWidget {
  const SettingsBottomSheet({super.key});

  @override
  Widget build(BuildContext context) {
    final localization = context.watch<LocalizationUtil>();
    
    return Padding(
      padding: EdgeInsets.only(bottom: MediaQuery.of(context).padding.bottom + 32, top: 24, left: 24, right: 24),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            localization.getString("settings"),
            style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 24),
          ),
          const SizedBox(height: 16),
          ListTile(
            leading: const Icon(Icons.language),
            title: Text(localization.getString("language")),
            subtitle: Text(LocalizationUtil.supportedLanguages.firstWhere((l) => l['code'] == localization.selectedLanguage)['label'] ?? "English"),
            onTap: () => _showLanguagePicker(context),
          ),
        ],
      ),
    );
  }

  void _showLanguagePicker(BuildContext context) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: Text(context.read<LocalizationUtil>().getString("select_language")),
        content: SizedBox(
          width: double.maxFinite,
          child: ListView(
            shrinkWrap: true,
            children: LocalizationUtil.supportedLanguages.map((lang) {
              return ListTile(
                leading: Radio<String>(
                  value: lang['code']!,
                  groupValue: context.read<LocalizationUtil>().selectedLanguage,
                  onChanged: (val) {
                    context.read<LocalizationUtil>().saveLanguage(val!);
                    Navigator.pop(context); // Close dialog
                    Navigator.pop(context); // Close bottom sheet
                  },
                ),
                title: Text(lang['label']!),
                onTap: () {
                  context.read<LocalizationUtil>().saveLanguage(lang['code']!);
                  Navigator.pop(context); // Close dialog
                  Navigator.pop(context); // Close bottom sheet
                },
              );
            }).toList(),
          ),
        ),
      ),
    );
  }
}
