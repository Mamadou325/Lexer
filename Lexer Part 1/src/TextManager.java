public class TextManager {
    private final String text;
    private int position;

    public TextManager(String input) {
        this.text = input;
        this.position = 0;
    }

    public boolean isAtEnd() {
        return position >= text.length();
    }

    public char PeekCharacter() {
        if (isAtEnd()) {
            return '\0';
        }
        return text.charAt(position);
    }

    public char PeekCharacter(int dist) {
        int targetIndex = position + dist; // avoid going out of bounds
        if (targetIndex < 0 || targetIndex >= text.length()) {
            return '\0';
        }
        return text.charAt(targetIndex);
    }

    public char GetCharacter() {
        if (isAtEnd()) {
            return '\0';
        }
        // get current char and then move to next position
        return text.charAt(position++);
    }
}

