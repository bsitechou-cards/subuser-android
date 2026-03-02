import 'package:firebase_auth/firebase_auth.dart';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:shimmer/shimmer.dart';
import '../../core/utils/localization_util.dart';
import '../../data/models/card_item.dart';
import '../../data/models/card_response.dart';
import '../../data/services/card_api_service.dart';
import '../shared/quick_action_item.dart';
import 'apply_card_bottom_sheet.dart';
import 'qr_code_screen.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  final FirebaseAuth _auth = FirebaseAuth.instance;
  CardResponse? _cardResponse;
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _fetchCards();
  }

  Future<void> _fetchCards() async {
    final user = _auth.currentUser;
    if (user == null || user.email == null) return;

    setState(() {
      _isLoading = true;
    });

    final response = await CardApiService.getAllDigitalCards(user.email!);
    
    if (mounted) {
      if (response?.code == "401") {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(context.read<LocalizationUtil>().getString("user_not_found"))),
        );
        _handleLogout();
      } else {
        setState(() {
          _cardResponse = response;
          _isLoading = false;
        });
      }
    }
  }

  void _handleLogout() async {
    await _auth.signOut();
    if (!mounted) return;
    Navigator.pushReplacementNamed(context, '/');
  }

  @override
  Widget build(BuildContext context) {
    final localization = context.watch<LocalizationUtil>();
    final cards = _cardResponse?.data ?? [];
    final issuedCards = cards.where((c) => c.cardid != null).toList();
    final pendingCards = cards.where((c) => c.cardid == null).toList();

    return Scaffold(
      backgroundColor: const Color(0xFFF8F9FA),
      body: SafeArea(
        child: Column(
          children: [
            // Top Bar
            _buildTopBar(localization),
            
            Expanded(
              child: RefreshIndicator(
                onRefresh: _fetchCards,
                child: ListView(
                  padding: const EdgeInsets.only(bottom: 32),
                  children: [
                    // Hero Section: My Cards
                    Padding(
                      padding: const EdgeInsets.only(left: 24, top: 24, bottom: 12),
                      child: Text(
                        localization.getString("my_cards"),
                        style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 18),
                      ),
                    ),

                    if (_isLoading && _cardResponse == null)
                      const Padding(
                        padding: EdgeInsets.symmetric(horizontal: 24),
                        child: ShimmerCardPlaceholder(),
                      )
                    else if (issuedCards.isEmpty)
                      _EmptyCardDesign(onApplyClick: () => _showApplySheet(context))
                    else
                      _CardPager(
                        cards: issuedCards,
                        onCardClick: (card) {
                          Navigator.pushNamed(context, '/cardDetails', arguments: card.cardid);
                        },
                      ),

                    // Pending Actions Section
                    if (pendingCards.isNotEmpty) ...[
                      Padding(
                        padding: const EdgeInsets.only(left: 24, top: 32, bottom: 12),
                        child: Text(
                          localization.getString("action_required"),
                          style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 18),
                        ),
                      ),
                      ...pendingCards.map((card) => _PendingCardItem(
                        card: card,
                        onPayNowClick: () => _showQrCode(card.depositaddress ?? "", (_cardResponse?.subuserfee ?? 0.0) + 5.0),
                      )),
                    ],
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildTopBar(LocalizationUtil localization) {
    return Container(
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
          Row(
            children: [
              QuickActionItem(
                icon: Icons.add,
                label: localization.getString("apply_new"),
                size: 38,
                onClick: () => _showApplySheet(context),
              ),
              const SizedBox(width: 12),
              QuickActionItem(
                icon: Icons.settings,
                label: localization.getString("settings"),
                size: 38,
                onClick: () => _showSettings(context),
              ),
            ],
          ),
        ],
      ),
    );
  }

  void _showApplySheet(BuildContext context) {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.white,
      builder: (context) => ApplyForCardBottomSheet(
        userEmail: _auth.currentUser?.email ?? "",
        subuserFee: _cardResponse?.subuserfee ?? 0.0,
        onCardApplied: () {
          Navigator.pop(context);
          _fetchCards();
        },
        onShowQrCode: (address, fee) {
          Navigator.pop(context);
          _showQrCode(address, (double.tryParse(fee) ?? 0.0) + 5.0);
        },
      ),
    );
  }

  void _showQrCode(String address, double fee) {
    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (context) => Dialog.fullscreen(
        child: QrCodeScreen(
          depositAddress: address,
          subuserFee: fee,
          onClose: () {
            Navigator.pop(context);
            _fetchCards();
          },
        ),
      ),
    );
  }

  void _showSettings(BuildContext context) {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.white,
      builder: (context) => HomeSettingsBottomSheet(onLogout: _handleLogout),
    );
  }
}

class _CardPager extends StatefulWidget {
  final List<CardItem> cards;
  final Function(CardItem) onCardClick;

  const _CardPager({required this.cards, required this.onCardClick});

  @override
  State<_CardPager> createState() => _CardPagerState();
}

class _CardPagerState extends State<_CardPager> {
  final PageController _pageController = PageController(viewportFraction: 0.85);
  int _currentPage = 0;

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        SizedBox(
          height: 200,
          child: PageView.builder(
            controller: _pageController,
            onPageChanged: (idx) => setState(() => _currentPage = idx),
            itemCount: widget.cards.length,
            itemBuilder: (context, index) {
              return Padding(
                padding: const EdgeInsets.symmetric(horizontal: 8),
                child: _CardDesign(
                  card: widget.cards[index],
                  onClick: () => widget.onCardClick(widget.cards[index]),
                ),
              );
            },
          ),
        ),
        const SizedBox(height: 12),
        Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: List.generate(widget.cards.length, (index) {
            return Container(
              margin: const EdgeInsets.symmetric(horizontal: 2),
              width: _currentPage == index ? 12 : 8,
              height: 4,
              decoration: BoxDecoration(
                borderRadius: BorderRadius.circular(2),
                color: _currentPage == index ? Theme.of(context).primaryColor : Colors.grey[300],
              ),
            );
          }),
        ),
      ],
    );
  }
}

class _CardDesign extends StatelessWidget {
  final CardItem card;
  final VoidCallback onClick;

  const _CardDesign({required this.card, required this.onClick});

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onClick,
      child: Container(
        padding: const EdgeInsets.all(24),
        decoration: BoxDecoration(
          borderRadius: BorderRadius.circular(24),
          gradient: const LinearGradient(
            colors: [Color(0xFF2B2B2B), Color(0xFF000000)],
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
          ),
          boxShadow: [
            BoxShadow(
              color: Colors.black.withOpacity(0.3),
              blurRadius: 8,
              offset: const Offset(0, 4),
            ),
          ],
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                const Text("Virtual Card", style: TextStyle(color: Colors.white70, fontSize: 14)),
                const Icon(Icons.remove_red_eye, color: Colors.white70, size: 20),
              ],
            ),
            Text(
              "**** **** **** ${card.lastfour}",
              style: const TextStyle(color: Colors.white, fontSize: 24, letterSpacing: 2, fontWeight: FontWeight.bold),
            ),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              crossAxisAlignment: CrossAxisAlignment.end,
              children: [
                Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(card.nameoncard.toUpperCase(), style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold)),
                    Text(card.type.toUpperCase(), style: const TextStyle(color: Colors.white70, fontSize: 12)),
                  ],
                ),
                Image.asset(
                  'assets/images/mastercard_logo.png',
                  width: 44,
                  height: 44,
                  errorBuilder: (_, __, ___) => const Icon(Icons.credit_card, color: Colors.orange, size: 44),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

class _PendingCardItem extends StatelessWidget {
  final CardItem card;
  final VoidCallback onPayNowClick;

  const _PendingCardItem({required this.card, required this.onPayNowClick});

  @override
  Widget build(BuildContext context) {
    final localization = context.watch<LocalizationUtil>();
    return Container(
      margin: const EdgeInsets.symmetric(horizontal: 24, vertical: 8),
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: Colors.grey[300]!),
      ),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                  decoration: BoxDecoration(
                    color: const Color(0xFFFFF3E0),
                    borderRadius: BorderRadius.circular(4),
                  ),
                  child: Text(
                    localization.getString("awaiting_payment"),
                    style: const TextStyle(color: Color(0xFFE65100), fontSize: 10, fontWeight: FontWeight.bold),
                  ),
                ),
                const SizedBox(height: 4),
                Text(
                  "${localization.getString("virtual_card_for")} ${card.nameoncard}",
                  style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 16),
                ),
              ],
            ),
          ),
          ElevatedButton(
            onPressed: onPayNowClick,
            style: ElevatedButton.styleFrom(
              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
            ),
            child: Text(localization.getString("pay_now"), style: const TextStyle(fontSize: 12)),
          ),
        ],
      ),
    );
  }
}

class _EmptyCardDesign extends StatelessWidget {
  final VoidCallback onApplyClick;
  const _EmptyCardDesign({required this.onApplyClick});

  @override
  Widget build(BuildContext context) {
    final localization = context.watch<LocalizationUtil>();
    return Column(
      children: [
        const SizedBox(height: 48),
        Icon(Icons.credit_card, size: 64, color: Colors.grey[300]),
        const SizedBox(height: 16),
        const Text("No active cards yet", style: TextStyle(color: Colors.grey)),
        const SizedBox(height: 24),
        ElevatedButton(
          onPressed: onApplyClick,
          child: Text(localization.getString("apply_new")),
        ),
      ],
    );
  }
}

class ShimmerCardPlaceholder extends StatelessWidget {
  const ShimmerCardPlaceholder({super.key});

  @override
  Widget build(BuildContext context) {
    return Shimmer.fromColors(
      baseColor: Colors.grey[300]!,
      highlightColor: Colors.grey[100]!,
      child: Container(
        height: 180,
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(16),
        ),
      ),
    );
  }
}

class HomeSettingsBottomSheet extends StatelessWidget {
  final VoidCallback onLogout;
  const HomeSettingsBottomSheet({super.key, required this.onLogout});

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
          const Divider(),
          ListTile(
            leading: const Icon(Icons.power_settings_new, color: Colors.red),
            title: Text(localization.getString("logout"), style: const TextStyle(color: Colors.red)),
            onTap: () {
              Navigator.pop(context);
              onLogout();
            },
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
                    Navigator.pop(context);
                  },
                ),
                title: Text(lang['label']!),
                onTap: () {
                  context.read<LocalizationUtil>().saveLanguage(lang['code']!);
                  Navigator.pop(context);
                },
              );
            }).toList(),
          ),
        ),
      ),
    );
  }
}
