package parser.tokenizer;

public class Token {
    public TokenType type;
    public String value;

    public Token(TokenType type, String value) {
        this.type = type;
        this.value = value;
    }

    @Override
    public String toString() {
        if (value == null)
            return type.toString();

        return type + "(" + value + ")";
    }
}