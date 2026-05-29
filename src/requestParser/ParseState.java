package requestParser;

public enum ParseState {
    REQUEST_LINE,
    HEADERS,
    BODY,
    CHUNKED_BODY,
    COMPLETE,
    ERROR
}
