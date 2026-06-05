package internal.jsonParser.tokenizer;

public enum TokenType {
    LBRACE, // {
    RBRACE, // }
    LBRACKET, // [
    RBRACKET, // ]
    COLON, // :
    COMMA, // ,

    STRING,
    NUMBER,

    TRUE,
    FALSE,
    NULL,

    EOF
}
