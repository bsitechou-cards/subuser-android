import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../core/utils/localization_util.dart';
import '../../data/models/three_ds_response.dart';
import '../../data/services/card_api_service.dart';

class ThreeDSBottomSheet extends StatefulWidget {
  final ThreeDSResponse response;
  final String cardId;
  final String userEmail;
  final VoidCallback onDismiss;

  const ThreeDSBottomSheet({
    super.key,
    required this.response,
    required this.cardId,
    required this.userEmail,
    required this.onDismiss,
  });

  @override
  State<ThreeDSBottomSheet> createState() => _ThreeDSBottomSheetState();
}

class _ThreeDSBottomSheetState extends State<ThreeDSBottomSheet> {
  bool _isApproving = false;
  bool _isApproved = false;

  @override
  Widget build(BuildContext context) {
    final localization = context.watch<LocalizationUtil>();
    final data = widget.response.data;

    return Container(
      padding: EdgeInsets.only(
        left: 24,
        right: 24,
        top: 16,
        bottom: MediaQuery.of(context).padding.bottom + 32,
      ),
      decoration: const BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          // Header
          Row(
            children: [
              Container(
                width: 40,
                height: 40,
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  color: Theme.of(context).primaryColor.withOpacity(0.1),
                ),
                child: Icon(Icons.shield, color: Theme.of(context).primaryColor),
              ),
              const SizedBox(width: 12),
              Text(
                localization.getString("3ds_auth"),
                style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 20),
              ),
            ],
          ),
          const SizedBox(height: 24),

          // Transaction Details Card
          Container(
            width: double.infinity,
            padding: const EdgeInsets.all(20),
            decoration: BoxDecoration(
              color: const Color(0xFFF5F5F5),
              borderRadius: BorderRadius.circular(16),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                if (data != null) ...[
                  _DetailRow(label: localization.getString("merchant"), value: data.merchantName),
                  const SizedBox(height: 12),
                  _DetailRow(
                    label: localization.getString("amount"),
                    value: "${data.merchantAmount} ${data.merchantCurrency}",
                  ),
                ],
              ],
            ),
          ),
          const SizedBox(height: 32),

          // Action Buttons
          Row(
            children: [
              // Reject Button
              Expanded(
                child: OutlinedButton.icon(
                  onPressed: (_isApproving || _isApproved) ? null : () {
                    Navigator.pop(context);
                    widget.onDismiss();
                  },
                  icon: const Icon(Icons.close),
                  label: Text(localization.getString("reject")),
                  style: OutlinedButton.styleFrom(
                    minimumSize: const Size(0, 56),
                    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                  ),
                ),
              ),
              const SizedBox(width: 16),
              // Approve Button
              Expanded(
                child: ElevatedButton.icon(
                  onPressed: (_isApproving || _isApproved) ? null : _handleApprove,
                  icon: _isApproved ? const Icon(Icons.check) : null,
                  label: _isApproving
                      ? const SizedBox(
                          width: 24,
                          height: 24,
                          child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white),
                        )
                      : Text(_isApproved ? localization.getString("approved") : localization.getString("approve")),
                  style: ElevatedButton.styleFrom(
                    backgroundColor: _isApproved ? Colors.green : Theme.of(context).primaryColor,
                    foregroundColor: Colors.white,
                    minimumSize: const Size(0, 56),
                    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                    disabledBackgroundColor: _isApproved ? Colors.green : null,
                    disabledForegroundColor: _isApproved ? Colors.white : null,
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 16),
          Text(
            localization.getString("verify_details"),
            textAlign: TextAlign.center,
            style: const TextStyle(color: Colors.grey, fontSize: 12),
          ),
        ],
      ),
    );
  }

  Future<void> _handleApprove() async {
    final data = widget.response.data;
    if (data == null) return;

    setState(() => _isApproving = true);
    final success = await CardApiService.approve3ds(widget.userEmail, widget.cardId, data.eventId);
    
    if (mounted) {
      if (success) {
        setState(() {
          _isApproving = false;
          _isApproved = true;
        });
        await Future.delayed(const Duration(milliseconds: 1500));
        if (mounted) {
          Navigator.pop(context);
          widget.onDismiss();
        }
      } else {
        setState(() => _isApproving = false);
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(context.read<LocalizationUtil>().getString("approval_failed"))),
        );
      }
    }
  }
}

class _DetailRow extends StatelessWidget {
  final String label;
  final String value;
  const _DetailRow({required this.label, required this.value});

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(label, style: const TextStyle(color: Colors.grey, fontSize: 12)),
        Text(value, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
      ],
    );
  }
}
