abstract class ChatMessage {
  final String text;
  ChatMessage(this.text);
}

class Question extends ChatMessage {
  final String field;
  final String? translationKey;
  Question(String text, this.field, {this.translationKey}) : super(text);
}

class Answer extends ChatMessage {
  final bool isSensitive;
  Answer(String text, {this.isSensitive = false}) : super(text);
}
