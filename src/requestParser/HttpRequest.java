package requestParser;

public class HttpRequest {

    public enum ParserState {
        REQUEST_LINE,
        HEADERS,
        BODY,
        CHUNKED_BODY,
        COMPLETE,
        ERROR
    }
    
    
}
