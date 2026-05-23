package jsonParser.parser;

import java.util.*;

import jsonParser.tokenizer.*;

public class Parser {

    private List<Token> tokens;
    private int pos = 0;

    public Object parse(List<Token> tokens) {
        this.tokens = tokens;
        this.pos = 0;

        return parseValue();
    }

    private Token peek() {
        return tokens.get(pos);
    }

    private Token consume() {
        return tokens.get(pos++);
    }

    private Object parseValue() {

        Token t = peek();

        switch (t.type) {

            case STRING:
                return consume().value;

            case NUMBER:
                return Long.parseLong(
                        consume().value);

            case TRUE:
                consume();
                return true;

            case FALSE:
                consume();
                return false;

            case NULL:
                consume();
                return null;

            case LBRACE:
                return parseObject();

            case LBRACKET:
                return parseArray();

            default:
                throw new RuntimeException(
                        "Unexpected token: " + t);
        }
    }

    private Map<String, Object> parseObject() {

        consume(); // {

        Map<String, Object> obj = new HashMap<>();

        while (peek().type != TokenType.RBRACE) {

            Token key = consume();

            if (key.type != TokenType.STRING)
                throw new RuntimeException(
                        "Expected key");

            consume(); // :

            Object value = parseValue();

            obj.put(
                    key.value,
                    value);

            if (peek().type == TokenType.COMMA)
                consume();
        }

        consume(); // }

        return obj;
    }

    private List<Object> parseArray() {

        consume(); // [

        List<Object> list = new ArrayList<>();

        while (peek().type != TokenType.RBRACKET) {

            list.add(
                    parseValue());

            if (peek().type == TokenType.COMMA)
                consume();
        }

        consume(); // ]

        return list;
    }
}