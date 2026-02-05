// Part 2 of the Lexer class

import AST.*;
import java.util.LinkedList;
import java.util.HashMap;

public class Lexer {
    private final TextManager textManager;
    private final HashMap<String, Token.TokenTypes> keywords;
    private final HashMap<String, Token.TokenTypes> punctuationMap;
    private int currentLine;
    private int currentColumn;
    private int currentIndentLevel;
    private boolean startOfLine;

    public Lexer(String input) {     // handle empty inputs first
        if (input == null || input.isEmpty()) {
            input = "\n";
        } else if (input.charAt(input.length() - 1) != '\n') {
            input = input + "\n";
        }

        this.textManager = new TextManager(input);
        this.keywords = new HashMap<>();
        this.punctuationMap = new HashMap<>();
        this.currentLine = 1;
        this.currentColumn = 1;
        this.currentIndentLevel = 0;
        this.startOfLine = true;

        setupKeywordsMap();
        setupPunctuationMap();
    }

    private void setupKeywordsMap() {
        keywords.put("unique", Token.TokenTypes.UNIQUE);
        keywords.put("var", Token.TokenTypes.VAR);
    }


    private void setupPunctuationMap() {     // from enum in token class
        punctuationMap.put("=", Token.TokenTypes.EQUAL);
        punctuationMap.put("=>", Token.TokenTypes.YIELDS);
        punctuationMap.put("!=", Token.TokenTypes.NOTEQUAL);
        punctuationMap.put("{", Token.TokenTypes.LEFTCURLY);
        punctuationMap.put("}", Token.TokenTypes.RIGHTCURLY);
        punctuationMap.put("[", Token.TokenTypes.LEFTBRACE);
        punctuationMap.put("]", Token.TokenTypes.RIGHTBRACE);
        punctuationMap.put(",", Token.TokenTypes.COMMA);
        punctuationMap.put(":", Token.TokenTypes.COLON);
        punctuationMap.put(".", Token.TokenTypes.DOT);
    }


    private char getNextChar() {
        char c = textManager.GetCharacter();
        if (c == '\n') {
            currentLine++;
            currentColumn = 1;     // reset the column to 1 (because new line)
        } else {
            currentColumn++;
        }
        return c;
    }


    public LinkedList<Token> Lex() throws SyntaxErrorException {
        LinkedList<Token> tokens = new LinkedList<>();

        while (!textManager.isAtEnd()) {
            char current = textManager.PeekCharacter();
            int tokenStartLine = currentLine;
            int tokenStartColumn = currentColumn;

            if (current == '\n') {
                getNextChar();
                tokens.add(new Token(Token.TokenTypes.NEWLINE, tokenStartLine, tokenStartColumn));
                startOfLine = true;
            } else if (startOfLine && (current == ' ' || current == '\t')) {
                processIndentation(tokens);
            } else if (Character.isWhitespace(current)) {
                getNextChar(); // skip space like between words
            } else {
                // have to handle dedent when at start of line
                if (startOfLine && currentIndentLevel > 0) {
                    for (int i = currentIndentLevel; i > 0; i--) {
                        tokens.add(new Token(Token.TokenTypes.DEDENT, tokenStartLine, 1));
                    }
                    currentIndentLevel = 0;
                }
                startOfLine = false;  // this will no longer be start of new line

                if (Character.isLetter(current)) {
                    tokens.add(readWord());
                } else if (Character.isDigit(current)) {
                    tokens.add(readNumber());
                } else {
                    tokens.add(readPunctuation());
                }
            }
        }

        addFinalDedent(tokens);

        tokens.add(new Token(Token.TokenTypes.NEWLINE, currentLine, currentColumn));

        return tokens;
    }


    private void processIndentation(LinkedList<Token> tokens) throws SyntaxErrorException {
        int spaces = 0;
        int startLine = currentLine;
        int startColumn = currentColumn;

        while (!textManager.isAtEnd()) {
            char c = textManager.PeekCharacter();
            if (c == ' ') {
                spaces++;
                getNextChar();
            } else if (c == '\t') {
                spaces += 4;        // handle tabs as 4 spaces
                getNextChar();
            } else {
                break;
            }
        }


        if (textManager.isAtEnd() || textManager.PeekCharacter() == '\n') {   // whitespace or not??
            startOfLine = true;
            return;
        }


        if (spaces % 4 != 0) {
            throw new SyntaxErrorException("Indentation MUST be multiple of 4 spaces", startLine, startColumn);
        }

        int newIndentLevel = spaces / 4;


        if (newIndentLevel > currentIndentLevel) {       // create indent or dedent based on punctuation
            for (int i = currentIndentLevel; i < newIndentLevel; i++) {
                tokens.add(new Token(Token.TokenTypes.INDENT, startLine, 1));
            }
        } else if (newIndentLevel < currentIndentLevel) {
            for (int i = currentIndentLevel; i > newIndentLevel; i--) {
                tokens.add(new Token(Token.TokenTypes.DEDENT, startLine, 1));
            }
        }
        currentIndentLevel = newIndentLevel;
        startOfLine = false;
    }



    private void addFinalDedent(LinkedList<Token> tokens) {     // handling tokens at end of file
        while (currentIndentLevel > 0) {
            tokens.add(new Token(Token.TokenTypes.DEDENT, currentLine, 1));
            currentIndentLevel--;
        }
    }



    private Token readWord() {
        int startLine = currentLine;
        int startColumn = currentColumn;
        StringBuilder builder = new StringBuilder();

        //read all consecutive letters & digits
        while (!textManager.isAtEnd() && Character.isLetterOrDigit(textManager.PeekCharacter())) {
            builder.append(getNextChar());
        }

        String word = builder.toString();  // keyword or regular identifier??
        if (keywords.containsKey(word)) {
            return new Token(keywords.get(word), startLine, startColumn);
        } else {
            return new Token(Token.TokenTypes.IDENTIFIER, startLine, startColumn, word);
        }
    }



    private Token readNumber() throws SyntaxErrorException {
        int startLine = currentLine;
        int startColumn = currentColumn;
        StringBuilder builder = new StringBuilder();
        boolean hasDecimal = false;

        while (!textManager.isAtEnd()) {
            char c = textManager.PeekCharacter();
            if (Character.isDigit(c)) {
                builder.append(getNextChar());
            } else if (c == '.' && !hasDecimal) {
                builder.append(getNextChar());
                hasDecimal = true;
            } else if (c == '.' && hasDecimal) {  // checking for second decimal point
                throw new SyntaxErrorException("Number cannot have multiple decimal points", currentLine, currentColumn);
            } else {
                break;
            }
        }

        String numberStr = builder.toString();
        if (numberStr.endsWith(".")) {
            throw new SyntaxErrorException("Number cannot end with a decimal point", currentLine, currentColumn);
        }

        return new Token(Token.TokenTypes.NUMBER, startLine, startColumn, numberStr);
    }



    private Token readPunctuation() throws SyntaxErrorException {
        int startLine = currentLine;
        int startColumn = currentColumn;

        char firstChar = textManager.PeekCharacter();  // use peek instead of get
        String singleCharSymbol = String.valueOf(firstChar);

        if (!textManager.isAtEnd()) {           // checking for two-character symbols
            char secondChar = textManager.PeekCharacter(1);

            if (secondChar != '\0') {
                String twoCharSymbol = singleCharSymbol + secondChar;

                if (punctuationMap.containsKey(twoCharSymbol)) {
                    getNextChar();
                    getNextChar();
                    return new Token(punctuationMap.get(twoCharSymbol), startLine, startColumn);
                }
            }
        }

        // checking for single-character symbols
        if (punctuationMap.containsKey(singleCharSymbol)) {
            getNextChar();
            return new Token(punctuationMap.get(singleCharSymbol), startLine, startColumn);
        }

        // has to be in map or invalid
        throw new SyntaxErrorException("Invalid character: '" + singleCharSymbol + "'", startLine, startColumn);
    }
}
