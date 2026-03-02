import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../core/utils/localization_util.dart';

class SettingsBottomSheet extends StatelessWidget {
  const SettingsBottomSheet({super.key});

  @override
  Widget build(BuildContext context) {
    final localization = context.watch<LocalizationUtil>();
    
    return Padding(
      padding: EdgeInsets.only(
        bottom: MediaQuery.of(context).padding.bottom + 32, 
        top: 24, 
        left: 24, 
        right: 24
      ),
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
            subtitle: Text(
              LocalizationUtil.supportedLanguages.firstWhere(
                (l) => l['code'] == localization.selectedLanguage,
                orElse: () => LocalizationUtil.supportedLanguages[0]
              )['label'] ?? "English"
            ),
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
