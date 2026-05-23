package parser.tokenizer;

import java.util.ArrayList;
import java.util.List;

public class JsonTokenizer {

    private final String input;
    private int pos = 0;

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