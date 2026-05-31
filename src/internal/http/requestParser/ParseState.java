package internal.http.requestParser;

public enum ParseState {
    HEADERS,
    FIXED_BODY,
    CHUNK_SIZE,
    CHUNK_DATA,
    CHUNK_CRLF,
    TRAILERS
}
