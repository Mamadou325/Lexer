// Part 1 of project

import AST.*;
import java.util.LinkedList;
import java.util.HashMap;

public class Lexer {
    private final TextManager textManager;
    private final HashMap<String, Token.TokenTypes> keywords;

    public Lexer(String input) {
        this.textManager = new TextManager(input);
        this.keywords = new HashMap<>();
        keywordsMap();
    }

    private void keywordsMap() {    // assign keywords that exist in Token.TokenTypes and nusha
        keywords.put("unique", Token.TokenTypes.UNIQUE);
        keywords.put("var", Token.TokenTypes.VAR);
    }

    public LinkedList<Token> Lex() throws SyntaxErrorException {
        LinkedList<Token> tokens = new LinkedList<>();

        while (!textManager.isAtEnd()) {
            char current = textManager.PeekCharacter();    // just to check next character

            if (Character.isLetter(current)) {
                tokens.add(readWord());
            } else if (current == '\n') {
                textManager.GetCharacter();

                Token newLineToken = new Token(null, 0, 0);  // empty dummy object
                newLineToken.Type = Token.TokenTypes.NEWLINE;
                newLineToken.Value = java.util.Optional.empty();   // will have no values for new line
                newLineToken.LineNumber = 0;
                newLineToken.ColumnNumber = 0;
                tokens.add(newLineToken);

            } else if (Character.isWhitespace(current)) {
                textManager.GetCharacter();
            } else {
                // throw an exception instead of skipping the character
                throw new SyntaxErrorException("Error: Unexpected character: '" + current + "'", 0, 0);
            }
        }

        return tokens;
    }


    private Token readWord() throws SyntaxErrorException {
        StringBuilder wordBuilder = new StringBuilder();

        // will loop until it's a space or punctuation
        while (!textManager.isAtEnd() && Character.isLetterOrDigit(textManager.PeekCharacter())) {
            wordBuilder.append(textManager.GetCharacter());
        }

        String wordStr = wordBuilder.toString();

        Token newToken = new Token(null, 0, 0);  // since Token constructors don't do it

        if (keywords.containsKey(wordStr)) {
            newToken.Type = keywords.get(wordStr);
            newToken.Value = java.util.Optional.empty();
        } else {
            newToken.Type = Token.TokenTypes.IDENTIFIER;
            newToken.Value = java.util.Optional.of(wordStr);
        }

        newToken.LineNumber = 0;
        newToken.ColumnNumber = 0;


        return newToken;
    }
}
