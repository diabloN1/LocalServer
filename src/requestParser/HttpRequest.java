package requestParser;

import java.util.*;

/**
 * Represents a parsed HTTP/1.1 request.
 */
public class HttpRequest {

    public enum ParseState {
        REQUEST_LINE,
        HEADERS,
        BODY,
        CHUNKED_BODY,
        COMPLETE,
        ERROR
    }

    private String method;
    private String uri;
    private String httpVersion;
    private String path;
    private String queryString = "";
    private Map<String, String> headers = new LinkedHashMap<>();
    private Map<String, String> queryParams = new LinkedHashMap<>();
    private Map<String, String> cookies = new LinkedHashMap<>();
    private byte[] body = new byte[0];
    private ParseState state = ParseState.REQUEST_LINE;
    private StringBuilder rawBuffer = new StringBuilder();
    private boolean headersComplete = false;
    private int contentLength = -1;
    private boolean chunked = false;

    public void appendRaw(String data) {
        rawBuffer.append(data);
    }

    public void appendRawBytes(byte[] data, int len) {
        for (int i = 0; i < len; i++) {
            rawBuffer.append((char)(data[i] & 0xFF));
        }
    }

    // ---- Getters ----

    public String getMethod() { return method; }
    public String getUri() { return uri; }
    public String getHttpVersion() { return httpVersion; }
    public String getPath() { return path; }
    public String getQueryString() { return queryString; }
    public Map<String, String> getHeaders() { return headers; }
    public Map<String, String> getQueryParams() { return queryParams; }
    public Map<String, String> getCookies() { return cookies; }
    public byte[] getBody() { return body; }
    public ParseState getState() { return state; }
    public boolean isChunked() { return chunked; }
    public int getContentLength() { return contentLength; }
    public String getRawBuffer() { return rawBuffer.toString(); }

    public String getHeader(String name) {
        return headers.getOrDefault(name.toLowerCase(), null);
    }

    // ---- Setters (used by parser) ----

    public void setMethod(String method) { this.method = method.toUpperCase(); }
    public void setUri(String uri) {
        this.uri = uri;
        int qIdx = uri.indexOf('?');
        if (qIdx >= 0) {
            this.path = uri.substring(0, qIdx);
            this.queryString = uri.substring(qIdx + 1);
            // parseQueryParams();
        } else {
            this.path = uri;
        }
    }
    public void setHttpVersion(String version) { this.httpVersion = version; }
    public void setState(ParseState state) { this.state = state; }
    public void setBody(byte[] body) { this.body = body; }
    public void setContentLength(int len) { this.contentLength = len; }
    public void setChunked(boolean chunked) { this.chunked = chunked; }
    public void setHeadersComplete(boolean complete) { this.headersComplete = complete; }
    public boolean isHeadersComplete() { return headersComplete; }

    
}
