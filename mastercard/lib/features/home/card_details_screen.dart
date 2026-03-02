import 'package:firebase_auth/firebase_auth.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:local_auth/local_auth.dart';
import 'package:provider/provider.dart';
import 'package:qr_flutter/qr_flutter.dart';
import 'package:intl/intl.dart';
import '../../core/utils/localization_util.dart';
import '../../data/models/card_details_response.dart';
import '../../data/services/card_api_service.dart';
import '../shared/quick_action_item.dart';
import 'three_ds_bottom_sheet.dart';

class CardDetailsScreen extends StatefulWidget {
  final String cardId;

  const CardDetailsScreen({super.key, required this.cardId});

  @override
  State<CardDetailsScreen> createState() => _CardDetailsScreenState();
}

class _CardDetailsScreenState extends State<CardDetailsScreen> with SingleTickerProviderStateMixin {
  final FirebaseAuth _auth = FirebaseAuth.instance;
  final LocalAuthentication _localAuth = LocalAuthentication();
  CardDetailsResponse? _response;
  bool _isLoading = true;
  bool _isChecking3ds = false;
  bool _isTogglingBlock = false;
  bool _isAuthenticated = false;
  late TabController _tabController;

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 2, vsync: this);
    _authenticate();
  }

  Future<void> _authenticate() async {
    try {
      final bool canAuthenticateWithBiometrics = await _localAuth.canCheckBiometrics;
      final bool canAuthenticate = canAuthenticateWithBiometrics || await _localAuth.isDeviceSupported();

      if (!canAuthenticate) {
        setState(() => _isAuthenticated = true);
        _fetchDetails();
        return;
      }

      final bool didAuthenticate = await _localAuth.authenticate(
        localizedReason: 'Please authenticate to view card details',
        options: const AuthenticationOptions(
          stickyAuth: true,
          biometricOnly: false,
        ),
      );

      if (didAuthenticate) {
        setState(() => _isAuthenticated = true);
        _fetchDetails();
      } else {
        if (mounted) Navigator.pop(context);
      }
    } on PlatformException catch (e) {
      print("Authentication error: $e");
      setState(() => _isAuthenticated = true);
      _fetchDetails();
    }
  }

  Future<void> _fetchDetails() async {
    final user = _auth.currentUser;
    if (user == null || user.email == null) return;

    setState(() => _isLoading = true);
    final res = await CardApiService.getDigitalCardDetails(user.email!, widget.cardId);
    
    if (mounted) {
      setState(() {
        _response = res;
        _isLoading = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final localization = context.watch<LocalizationUtil>();
    final card = _response?.data;

    if (!_isAuthenticated) {
      return const Scaffold(
        body: Center(child: CircularProgressIndicator()),
      );
    }

    return Scaffold(
      backgroundColor: const Color(0xFFF8F9FA),
      body: SafeArea(
        child: Column(
          children: [
            // Top Bar
            _buildTopBar(localization),

            Expanded(
              child: _isLoading && _response == null
                  ? const Center(child: CircularProgressIndicator())
                  : card == null
                      ? const Center(child: Text("Could not load details"))
                      : RefreshIndicator(
                          onRefresh: _fetchDetails,
                          child: Column(
                            children: [
                              Padding(
                                padding: const EdgeInsets.symmetric(horizontal: 24),
                                child: Column(
                                  children: [
                                    const SizedBox(height: 24),
                                    _FlippableCard(card: card),
                                    const SizedBox(height: 32),
                                    _buildBalanceSection(card, localization),
                                    const SizedBox(height: 24),
                                  ],
                                ),
                              ),
                              _buildTabs(localization),
                              Expanded(
                                child: _buildTabContent(card, localization),
                              ),
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
        boxShadow: [BoxShadow(color: Colors.black.withOpacity(0.05), blurRadius: 2, offset: const Offset(0, 2))],
      ),
      child: Row(
        children: [
          Image.asset('assets/images/logo.png', width: 32, height: 32, errorBuilder: (_, __, ___) => const Icon(Icons.wallet, color: Colors.purple)),
          const SizedBox(width: 10),
          Text(localization.getString("app_name"), style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 18)),
          const Spacer(),
          QuickActionItem(
            icon: Icons.arrow_back,
            label: localization.getString("back"),
            size: 40,
            onClick: () => Navigator.pop(context),
          ),
        ],
      ),
    );
  }

  Widget _buildBalanceSection(CardDetails card, LocalizationUtil localization) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(localization.getString("current_balance"), style: const TextStyle(color: Colors.grey, fontSize: 14)),
            Text("\$${card.balance.toStringAsFixed(2)}", style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 28)),
          ],
        ),
        Row(
          children: [
            // Plus Icon for Deposit Addresses
            IconButton(
              icon: const Icon(Icons.add),
              onPressed: () => _showDepositSheet(context, card, localization),
            ),
            _isChecking3ds 
                ? const SizedBox(width: 48, height: 48, child: Center(child: SizedBox(width: 24, height: 24, child: CircularProgressIndicator(strokeWidth: 2))))
                : IconButton(
                    icon: const Icon(Icons.shield_outlined),
                    onPressed: _handleCheck3ds,
                  ),
            // Block and Unblock toggle switch
            _isTogglingBlock
                ? const SizedBox(width: 48, height: 48, child: Center(child: SizedBox(width: 24, height: 24, child: CircularProgressIndicator(strokeWidth: 2))))
                : Switch(
                    value: card.status == "active",
                    onChanged: (val) => _handleToggleBlock(card),
                    activeColor: const Color(0xFF006400),
                  ),
          ],
        ),
      ],
    );
  }

  Widget _buildTabs(LocalizationUtil localization) {
    return TabBar(
      controller: _tabController,
      indicatorColor: Theme.of(context).primaryColor,
      labelColor: Colors.black,
      unselectedLabelColor: Colors.grey,
      labelStyle: const TextStyle(fontWeight: FontWeight.bold),
      tabs: [
        Tab(text: localization.getString("transactions")),
        Tab(text: localization.getString("deposits")),
      ],
    );
  }

  Widget _buildTabContent(CardDetails card, LocalizationUtil localization) {
    return TabBarView(
      controller: _tabController,
      children: [
        _TransactionList(transactions: card.transactions),
        _DepositList(deposits: card.deposits),
      ],
    );
  }

  void _showDepositSheet(BuildContext context, CardDetails card, LocalizationUtil localization) {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.white,
      shape: const RoundedRectangleBorder(borderRadius: BorderRadius.vertical(top: Radius.circular(24))),
      builder: (context) => _PremiumDepositSheet(card: card),
    );
  }

  Future<void> _handleCheck3ds() async {
    final user = _auth.currentUser;
    if (user == null || user.email == null) return;

    setState(() => _isChecking3ds = true);
    final apiResponse = await CardApiService.check3ds(user.email!, widget.cardId);
    setState(() => _isChecking3ds = false);

    if (!mounted) return;

    if (apiResponse == null) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text("An error occurred.")));
    } else if (apiResponse.code == "422") {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text("No 3DS Request")));
    } else if (apiResponse.code == "200") {
      showModalBottomSheet(
        context: context,
        isScrollControlled: true,
        backgroundColor: Colors.transparent,
        builder: (context) => ThreeDSBottomSheet(
          response: apiResponse,
          cardId: widget.cardId,
          userEmail: user.email!,
          onDismiss: () => _fetchDetails(),
        ),
      );
    }
  }

  Future<void> _handleToggleBlock(CardDetails card) async {
    final user = _auth.currentUser;
    if (user == null || user.email == null) return;

    setState(() => _isTogglingBlock = true);
    final response = card.status == "active"
        ? await CardApiService.blockDigitalCard(user.email!, widget.cardId)
        : await CardApiService.unblockDigitalCard(user.email!, widget.cardId);
    
    if (mounted) {
      setState(() => _isTogglingBlock = false);
      if (response != null) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(response.message)));
        _fetchDetails();
      } else {
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text("An error occurred.")));
      }
    }
  }
}

class _FlippableCard extends StatefulWidget {
  final CardDetails card;
  const _FlippableCard({required this.card});

  @override
  State<_FlippableCard> createState() => _FlippableCardState();
}

class _FlippableCardState extends State<_FlippableCard> with SingleTickerProviderStateMixin {
  late AnimationController _controller;
  late Animation<double> _animation;
  bool _isFront = true;

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(duration: const Duration(milliseconds: 500), vsync: this);
    _animation = Tween<double>(begin: 0, end: 1).animate(_controller);
  }

  void _toggleCard() {
    if (_isFront) {
      _controller.forward();
    } else {
      _controller.reverse();
    }
    setState(() => _isFront = !_isFront);
  }

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: _toggleCard,
      child: AnimatedBuilder(
        animation: _animation,
        builder: (context, child) {
          final double angle = _animation.value * 3.14159;
          return Transform(
            transform: Matrix4.identity()
              ..setEntry(3, 2, 0.001)
              ..rotateY(angle),
            alignment: Alignment.center,
            child: angle <= 1.5708 
                ? _buildFront() 
                : Transform(
                    transform: Matrix4.identity()..rotateY(3.14159),
                    alignment: Alignment.center,
                    child: _buildBack(),
                  ),
          );
        },
      ),
    );
  }

  Widget _buildFront() {
    return Container(
      height: 220,
      padding: const EdgeInsets.all(24),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(16),
        gradient: const LinearGradient(colors: [Color(0xFF2B2B2B), Color(0xFF000000)]),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Container(width: 40, height: 30, decoration: BoxDecoration(color: Colors.yellow[700], borderRadius: BorderRadius.circular(4))),
              Image.asset('assets/images/mastercard_logo.png', width: 40, height: 40, errorBuilder: (_, __, ___) => const Icon(Icons.credit_card, color: Colors.orange, size: 40)),
            ],
          ),
          Text(
            widget.card.cardNumber.replaceAllMapped(RegExp(r".{4}"), (match) => "${match.group(0)} "),
            style: const TextStyle(color: Colors.white, fontSize: 24, fontWeight: FontWeight.bold, letterSpacing: 2),
          ),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text("CARDHOLDER", style: TextStyle(color: Colors.white70, fontSize: 10)),
                  Text(widget.card.nameoncard.toUpperCase(), style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold)),
                ],
              ),
              Column(
                crossAxisAlignment: CrossAxisAlignment.end,
                children: [
                  const Text("EXPIRY", style: TextStyle(color: Colors.white70, fontSize: 10)),
                  Text("${widget.card.expiryMonth}/${widget.card.expiryYear}", style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold)),
                ],
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildBack() {
    return Container(
      height: 220,
      padding: const EdgeInsets.all(24),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(16),
        gradient: const LinearGradient(colors: [Color(0xFF2B2B2B), Color(0xFF4A4A4A)]),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(height: 40, width: double.infinity, color: Colors.white70, alignment: Alignment.centerRight, padding: const EdgeInsets.only(right: 8), child: const Text("Authorized Signature", style: TextStyle(fontSize: 8, color: Colors.black))),
          const Spacer(),
          Align(alignment: Alignment.centerRight, child: Text("CVV: ${widget.card.cvv}", style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 16))),
          const SizedBox(height: 16),
          const Text("BILLING ADDRESS", style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 10)),
          Text("${widget.card.address1 ?? ""}, ${widget.card.city ?? ""}, ${widget.card.state ?? ""}, UK, ${widget.card.postalCode ?? ""}", style: const TextStyle(color: Colors.white, fontSize: 12)),
        ],
      ),
    );
  }
}

class _PremiumDepositSheet extends StatelessWidget {
  final CardDetails card;
  const _PremiumDepositSheet({required this.card});

  @override
  Widget build(BuildContext context) {
    final localization = context.watch<LocalizationUtil>();
    
    return Padding(
      padding: EdgeInsets.only(bottom: MediaQuery.of(context).padding.bottom + 32, top: 24, left: 24, right: 24),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(localization.getString("deposit_crypto"), style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 24)),
          const SizedBox(height: 8),
          Text(localization.getString("disclaimer_non_usdc"), style: const TextStyle(color: Colors.red, fontSize: 12)),
          const SizedBox(height: 24),
          SizedBox(
            height: 400,
            child: ListView(
              children: [
                _PremiumDepositCard(currency: "USDC", network: "Polygon Network", address: card.depositaddress?.replaceFirst("USDC-POLYGON-", ""), color: const Color(0xFF8247E5)),
                _PremiumDepositCard(currency: "BTC", network: "Bitcoin Network", address: card.btcdepositaddress?.replaceFirst("BTC-", ""), color: const Color(0xFFF7931A)),
                _PremiumDepositCard(currency: "ETH", network: "Ethereum Network", address: card.ethdepositaddress?.replaceFirst("ETH-", ""), color: const Color(0xFF627EEA)),
                _PremiumDepositCard(currency: "USDT", network: "BSC | BEP20", address: card.usdtdepositaddress?.replaceFirst("USDT-BSC|BEP20-", ""), color: const Color(0xFF26A17B)),
                _PremiumDepositCard(currency: "SOL", network: "Solana Network", address: card.soldepositaddress?.replaceFirst("SOL-", ""), color: const Color(0xFF14F195)),
                _PremiumDepositCard(currency: "BNB", network: "Binance Smart Chain", address: card.bnbdepositaddress?.replaceFirst("BNB-BSC-", ""), color: const Color(0xFFF3BA2F)),
                _PremiumDepositCard(currency: "XRP", network: "Ripple Network", address: card.xrpdepositaddress?.replaceFirst("XRP-BSC-", ""), color: const Color(0xFF23292F)),
                _PremiumDepositCard(currency: "PAXG", network: "Pax Gold Network", address: card.paxgdepositaddress?.replaceFirst("PAXG-", ""), color: const Color(0xFFE6B34B)),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _PremiumDepositCard extends StatefulWidget {
  final String currency;
  final String network;
  final String? address;
  final Color color;

  const _PremiumDepositCard({required this.currency, required this.network, this.address, required this.color});

  @override
  State<_PremiumDepositCard> createState() => _PremiumDepositCardState();
}

class _PremiumDepositCardState extends State<_PremiumDepositCard> {
  bool _isExpanded = false;

  @override
  Widget build(BuildContext context) {
    if (widget.address == null || widget.address!.isEmpty) return const SizedBox.shrink();

    return Card(
      margin: const EdgeInsets.only(bottom: 12),
      color: const Color(0xFFF5F5F5),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      child: Column(
        children: [
          ListTile(
            leading: CircleAvatar(backgroundColor: widget.color.withOpacity(0.1), child: Text(widget.currency[0], style: TextStyle(color: widget.color, fontWeight: FontWeight.bold))),
            title: Text(widget.currency, style: const TextStyle(fontWeight: FontWeight.bold)),
            subtitle: Text(widget.network, style: TextStyle(color: widget.color, fontSize: 10)),
            trailing: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                IconButton(icon: const Icon(Icons.copy, size: 20), onPressed: () {
                  Clipboard.setData(ClipboardData(text: widget.address!));
                  ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text("Address copied")));
                }),
                Icon(_isExpanded ? Icons.expand_less : Icons.expand_more),
              ],
            ),
            onTap: () => setState(() => _isExpanded = !_isExpanded),
          ),
          if (_isExpanded)
            Padding(
              padding: const EdgeInsets.all(16.0),
              child: Column(
                children: [
                  Text(widget.address!, style: const TextStyle(fontSize: 12, fontFamily: 'monospace'), textAlign: TextAlign.center),
                  const SizedBox(height: 16),
                  QrImageView(data: widget.address!, size: 150),
                  const SizedBox(height: 8),
                  Text("Send only ${widget.currency} via ${widget.network}", style: const TextStyle(color: Colors.red, fontSize: 10)),
                ],
              ),
            ),
        ],
      ),
    );
  }
}

class _TransactionList extends StatelessWidget {
  final List<TransactionItem> transactions;
  const _TransactionList({required this.transactions});

  @override
  Widget build(BuildContext context) {
    if (transactions.isEmpty) return const Center(child: Text("No transactions yet"));
    
    final grouped = _groupItemsByDate(transactions, (t) => t.paymentDateTime, context);

    return ListView.builder(
      itemCount: grouped.length,
      itemBuilder: (context, index) {
        final entry = grouped[index];
        if (entry is String) {
          return _DateHeader(date: entry);
        } else {
          final tx = entry as TransactionItem;
          return Column(
            children: [
              _TransactionRow(transaction: tx),
              const Divider(height: 1, indent: 16, endIndent: 16),
            ],
          );
        }
      },
    );
  }
}

class _TransactionRow extends StatelessWidget {
  final TransactionItem transaction;
  const _TransactionRow({required this.transaction});

  @override
  Widget build(BuildContext context) {
    final isPayment = transaction.type.toLowerCase() == "payment";
    final amountColor = isPayment ? Colors.black : const Color(0xFF34A853);
    final prefix = isPayment ? "-" : "+";

    return Container(
      color: Colors.white,
      padding: const EdgeInsets.all(16),
      child: Row(
        children: [
          Container(
            width: 44,
            height: 44,
            decoration: const BoxDecoration(shape: BoxShape.circle, color: Color(0xFFF1F3F4)),
            alignment: Alignment.center,
            child: Text(
              transaction.merchant.name.substring(0, 1).toUpperCase(),
              style: const TextStyle(fontWeight: FontWeight.bold, color: Colors.black54),
            ),
          ),
          const SizedBox(width: 16),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  transaction.merchant.name,
                  style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 16),
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                ),
                Text(
                  DateFormat('hh:mm a').format(DateTime.parse(transaction.paymentDateTime)),
                  style: const TextStyle(color: Colors.grey, fontSize: 12),
                ),
              ],
            ),
          ),
          Text(
            "$prefix\$${transaction.amount.toStringAsFixed(2)}",
            style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16, color: amountColor),
          ),
        ],
      ),
    );
  }
}

class _DepositList extends StatelessWidget {
  final List<Deposit> deposits;
  const _DepositList({required this.deposits});

  @override
  Widget build(BuildContext context) {
    if (deposits.isEmpty) return const Center(child: Text("No deposits yet"));
    
    final grouped = _groupItemsByDate(deposits, (d) => d.createdAt, context);

    return ListView.builder(
      itemCount: grouped.length,
      itemBuilder: (context, index) {
        final entry = grouped[index];
        if (entry is String) {
          return _DateHeader(date: entry);
        } else {
          final deposit = entry as Deposit;
          return Column(
            children: [
              _DepositRow(deposit: deposit),
              const Divider(height: 1, indent: 16, endIndent: 16),
            ],
          );
        }
      },
    );
  }
}

class _DepositRow extends StatelessWidget {
  final Deposit deposit;
  const _DepositRow({required this.deposit});

  @override
  Widget build(BuildContext context) {
    final localization = context.watch<LocalizationUtil>();
    return Container(
      color: Colors.white,
      padding: const EdgeInsets.all(16),
      child: Row(
        children: [
          Container(
            width: 44,
            height: 44,
            decoration: const BoxDecoration(shape: BoxShape.circle, color: Color(0xFFE8F5E9)),
            alignment: Alignment.center,
            child: const Icon(Icons.arrow_downward, color: Color(0xFF34A853), size: 24),
          ),
          const SizedBox(width: 16),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  localization.getString("crypto_deposit"),
                  style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 16),
                ),
                Text(
                  "${deposit.transactionHash.substring(0, 8)}...${deposit.transactionHash.substring(deposit.transactionHash.length - 8)}",
                  style: const TextStyle(color: Colors.grey, fontSize: 12),
                ),
              ],
            ),
          ),
          Text(
            "+\$${deposit.amount.toStringAsFixed(2)}",
            style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16, color: Color(0xFF34A853)),
          ),
        ],
      ),
    );
  }
}

class _DateHeader extends StatelessWidget {
  final String date;
  const _DateHeader({required this.date});

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      color: const Color(0xFFF8F9FA),
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
      child: Text(
        date.toUpperCase(),
        style: const TextStyle(fontWeight: FontWeight.bold, color: Colors.grey, letterSpacing: 1, fontSize: 12),
      ),
    );
  }
}

List<dynamic> _groupItemsByDate<T>(List<T> items, String Function(T) dateSelector, BuildContext context) {
  final localization = context.read<LocalizationUtil>();
  final grouped = <dynamic>[];
  final sorted = List<T>.from(items)..sort((a, b) => dateSelector(b).compareTo(dateSelector(a)));
  
  String? lastDate;
  for (var item in sorted) {
    final dateStr = dateSelector(item);
    final dateTime = DateTime.parse(dateStr).toLocal();
    final now = DateTime.now();
    final today = DateTime(now.year, now.month, now.day);
    final yesterday = today.subtract(const Duration(days: 1));
    final itemDate = DateTime(dateTime.year, dateTime.month, dateTime.day);

    String displayDate;
    if (itemDate == today) {
      displayDate = localization.getString("today");
    } else if (itemDate == yesterday) {
      displayDate = localization.getString("yesterday");
    } else {
      displayDate = DateFormat('dd MMMM yyyy').format(dateTime);
    }

    if (displayDate != lastDate) {
      grouped.add(displayDate);
      lastDate = displayDate;
    }
    grouped.add(item);
  }
  return grouped;
}
