import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:shimmer/shimmer.dart';
import '../../core/utils/localization_util.dart';
import '../../data/models/chat_message.dart';

class BotMessageBubble extends StatelessWidget {
  final String text;
  final String? translationKey;
  
  const BotMessageBubble({super.key, required this.text, this.translationKey});

  @override
  Widget build(BuildContext context) {
    final localization = context.watch<LocalizationUtil>();
    final displayText = translationKey != null ? localization.getString(translationKey!) : text;

    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const BotAvatarIcon(),
        const SizedBox(width: 8),
        Flexible(
          child: Container(
            padding: const EdgeInsets.all(12),
            decoration: const BoxDecoration(
              color: Color(0xFFF1F3F4),
              borderRadius: BorderRadius.only(
                topRight: Radius.circular(12),
                bottomLeft: Radius.circular(12),
                bottomRight: Radius.circular(12),
              ),
            ),
            child: Text(displayText, style: Theme.of(context).textTheme.bodyLarge),
          ),
        ),
      ],
    );
  }
}

class UserMessageBubble extends StatelessWidget {
  final String text;
  final bool isSensitive;
  const UserMessageBubble({super.key, required this.text, this.isSensitive = false});

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.end,
      children: [
        Flexible(
          child: Container(
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(
              color: Theme.of(context).primaryColor,
              borderRadius: const BorderRadius.only(
                topLeft: Radius.circular(12),
                topRight: Radius.circular(12),
                bottomLeft: Radius.circular(12),
              ),
            ),
            child: Text(
              isSensitive ? '*' * text.length : text,
              style: Theme.of(context).textTheme.bodyLarge?.copyWith(color: Colors.white),
            ),
          ),
        ),
      ],
    );
  }
}

class BotAvatarIcon extends StatelessWidget {
  const BotAvatarIcon({super.key});

  @override
  Widget build(BuildContext context) {
    return Container(
      width: 40,
      height: 40,
      decoration: BoxDecoration(
        shape: BoxShape.circle,
        color: Colors.white,
        border: Border.all(color: Colors.grey.withOpacity(0.2)),
      ),
      child: Icon(
        Icons.monetization_on,
        color: Theme.of(context).primaryColor,
      ),
    );
  }
}

class BotTypingIndicator extends StatelessWidget {
  const BotTypingIndicator({super.key});

  @override
  Widget build(BuildContext context) {
    return Shimmer.fromColors(
      baseColor: Colors.grey[300]!,
      highlightColor: Colors.grey[100]!,
      child: const Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          BotAvatarIcon(),
          SizedBox(width: 8),
          Flexible(
            child: BotMessageBubble(text: "..."),
          ),
        ],
      ),
    );
  }
}
