package requestParser;

public enum ParseResult {
    NEED_MORE,
    COMPLETE,
    BAD_REQUEST,
    BODY_TOO_LARGE
}