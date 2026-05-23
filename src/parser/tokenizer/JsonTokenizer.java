package parser.tokenizer;

import java.util.ArrayList;
import java.util.List;

public class JsonTokenizer {

    private final String input;
    private int pos = 0;

    public JsonTokenizer(String input) {
        this.input = input;
    }

    public List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();

        while (pos < input.length()) {

            char c = input.charAt(pos);

            switch (c) {

                case '{':
                    tokens.add(new Token(TokenType.LBRACE, null));
                    pos++;
                    break;

                case '}':
                    tokens.add(new Token(TokenType.RBRACE, null));
                    pos++;
                    break;

                case '[':
                    tokens.add(new Token(TokenType.LBRACKET, null));
                    pos++;
                    break;

                case ']':
                    tokens.add(new Token(TokenType.RBRACKET, null));
                    pos++;
                    break;

                case ':':
                    tokens.add(new Token(TokenType.COLON, null));
                    pos++;
                    break;

                case ',':
                    tokens.add(new Token(TokenType.COMMA, null));
                    pos++;
                    break;

                case '"':
                    tokens.add(readString());
                    break;

                default:

                    if (Character.isWhitespace(c)) {
                        pos++;
                    } else if (Character.isDigit(c) || c == '-') {
                        tokens.add(readNumber());
                    } else if (Character.isLetter(c)) {
                        tokens.add(readKeyword());
                    } else {
                        throw new RuntimeException(
                                "Unexpected character: " + c);
                    }
            }
        }

        tokens.add(new Token(TokenType.EOF, null));

        return tokens;
    }

    private Token readString() {

        pos++; // skip "

        StringBuilder sb = new StringBuilder();

        while (pos < input.length()
                && input.charAt(pos) != '"') {

            sb.append(input.charAt(pos));
            pos++;
        }

        pos++; // closing "

        return new Token(
                TokenType.STRING,
                sb.toString());
    }

    private Token readNumber() {

        StringBuilder sb = new StringBuilder();

        while (pos < input.length()) {

            char c = input.charAt(pos);

            if (!Character.isDigit(c)
                    && c != '-'
                    && c != '.') {
                break;
            }

            sb.append(c);
            pos++;
        }

        return new Token(
                TokenType.NUMBER,
                sb.toString());
    }

    private Token readKeyword() {

        StringBuilder sb = new StringBuilder();

        while (pos < input.length()
                && Character.isLetter(input.charAt(pos))) {

            sb.append(input.charAt(pos));
            pos++;
        }

        String word = sb.toString();

        switch (word) {

            case "true":
                return new Token(TokenType.TRUE, null);

            case "false":
                return new Token(TokenType.FALSE, null);

            case "null":
                return new Token(TokenType.NULL, null);

            default:
                throw new RuntimeException(
                        "Unknown keyword: " + word);
        }
    }
}