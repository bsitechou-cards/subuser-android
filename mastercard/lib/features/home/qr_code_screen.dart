import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:provider/provider.dart';
import 'package:qr_flutter/qr_flutter.dart';
import '../../core/utils/localization_util.dart';

class QrCodeScreen extends StatelessWidget {
  final String depositAddress;
  final double subuserFee;
  final VoidCallback onClose;

  const QrCodeScreen({
    super.key,
    required this.depositAddress,
    required this.subuserFee,
    required this.onClose,
  });

  @override
  Widget build(BuildContext context) {
    final localization = context.watch<LocalizationUtil>();

    return Scaffold(
      backgroundColor: Colors.white,
      appBar: AppBar(
        backgroundColor: Colors.white,
        elevation: 0,
        leading: IconButton(
          icon: const Icon(Icons.close, color: Colors.black),
          onPressed: onClose,
        ),
        title: Text(
          localization.getString("deposit_crypto"),
          style: const TextStyle(color: Colors.black, fontWeight: FontWeight.bold),
        ),
        centerTitle: true,
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(24.0),
        child: Column(
          children: [
            // Total Amount to Pay
            _buildInfoCard(
              context,
              localization.getString("total_to_pay"),
              "\$${subuserFee.toStringAsFixed(2)}",
              Icons.monetization_on,
            ),
            const SizedBox(height: 24),

            // QR Code
            Container(
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.circular(24),
                boxShadow: [
                  BoxShadow(
                    color: Colors.black.withOpacity(0.05),
                    blurRadius: 10,
                    offset: const Offset(0, 4),
                  ),
                ],
              ),
              child: QrImageView(
                data: depositAddress,
                version: QrVersions.auto,
                size: 200.0,
              ),
            ),
            const SizedBox(height: 24),

            // Deposit Address
            Text(
              localization.getString("deposit_address"),
              style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
            ),
            const SizedBox(height: 12),
            GestureDetector(
              onTap: () {
                Clipboard.setData(ClipboardData(text: depositAddress));
                ScaffoldMessenger.of(context).showSnackBar(
                  SnackBar(content: Text(localization.getString("address_copied"))),
                );
              },
              child: Container(
                padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
                decoration: BoxDecoration(
                  color: const Color(0xFFF1F3F4),
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Row(
                  children: [
                    Expanded(
                      child: Text(
                        depositAddress,
                        style: const TextStyle(fontFamily: 'monospace', fontSize: 12),
                        maxLines: 2,
                        overflow: TextOverflow.ellipsis,
                      ),
                    ),
                    const SizedBox(width: 8),
                    const Icon(Icons.copy, size: 20, color: Colors.grey),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 32),

            // Disclaimers
            _buildDisclaimer(context, localization.getString("disclaimer_non_usdc")),
            const SizedBox(height: 16),
            _buildDisclaimer(context, localization.getString("disclaimer_exact_amount")),
            const SizedBox(height: 48),

            // Action Button
            SizedBox(
              width: double.infinity,
              child: ElevatedButton(
                onPressed: onClose,
                style: ElevatedButton.styleFrom(
                  padding: const EdgeInsets.symmetric(vertical: 16),
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                ),
                child: Text(localization.getString("i_have_paid")),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildInfoCard(BuildContext context, String title, String value, IconData icon) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Theme.of(context).primaryColor.withOpacity(0.1),
        borderRadius: BorderRadius.circular(16),
      ),
      child: Row(
        children: [
          Icon(icon, color: Theme.of(context).primaryColor, size: 24),
          const SizedBox(width: 16),
          Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(title, style: const TextStyle(color: Colors.grey, fontSize: 12)),
              Text(value, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 18, color: Colors.black)),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildDisclaimer(BuildContext context, String text) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Icon(Icons.info_outline, size: 16, color: Colors.orange),
        const SizedBox(width: 8),
        Expanded(
          child: Text(
            text,
            style: const TextStyle(fontSize: 11, color: Colors.grey),
          ),
        ),
      ],
    );
  }
}
