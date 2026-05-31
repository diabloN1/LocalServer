import java.util.LinkedHashMap;
import java.util.Map;

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

    private int contentLength = -1;

    private boolean headersComplete = false;
    private boolean chunked = false;

    public void appendRaw(String data) {
        rawBuffer.append(data);
    }

    public void appendRawBytes(byte[] data, int length) {
        for (int i = 0; i < length; i++) {
            rawBuffer.append((char) (data[i] & 0xFF));
        }
    }

    // GETTERS
    public String getMethod() {
        return method;
    }

    public String getUri() {
        return uri;
    }

    public String getHttpVersion() {
        return httpVersion;
    }

    public String getPath() {
        return path;
    }

    public String getQueryString() {
        return queryString;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public Map<String, String> getQueryParams() {
        return queryParams;
    }

    public Map<String, String> getCookies() {
        return cookies;
    }

    public byte[] getBody() {
        return body;
    }

    public ParseState getState() {
        return state;
    }

    public boolean isChunked() {
        return chunked;
    }

    public boolean isHeaderComplete() {
        return headersComplete;
    }

    public int getContentLength() {
        return contentLength;
    }

    public String getRawBuffer() {
        return rawBuffer.toString();
    }

    public String getHeader(String name) {
        return headers.getOrDefault(name.toLowerCase(), null);
    }

    // SETTERS
    public void setMethod(String method) {
        this.method = method.toUpperCase();
    }

    public void setUri(String uri) {
        this.uri = uri;
        int qIdx = uri.indexOf('?');
        if (qIdx >= 0) {
            this.path = uri.substring(0, qIdx);
            this.queryString = uri.substring(qIdx + 1);
            parseQueryParams();
        } else {
            this.path = uri;
        }
    }

    public void setHttpVersion(String version) {
        this.httpVersion = version;
    }

    public void setState(ParseState state) {
        this.state = state;
    }

    public void setBody(byte[] body) {
        this.body = body;
    }

    public void setContentLength(int len) {
        this.contentLength = len;
    }

    public void setHeadersComplete(boolean complete) {
        this.headersComplete = complete;
    }

    public boolean isHeadersComplete() {
        return headersComplete;
    }

    public void addHeader(String name, String value) {
        String lower = name.trim().toLowerCase();
        headers.put(lower, value.trim());
        if (lower.equals("content-length")) {
            try {
                contentLength = Integer.parseInt(value.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        if (lower.equals("transfer-encoding") && value.trim().toLowerCase().contains("chunked")) {
            chunked = true;
        }
        if (lower.equals("cookie")) {
            parseCookies(value);
        }
    }

    private void parseCookies(String cookieHeader) {
        for (String part : cookieHeader.split(";")) {
            int eq = part.indexOf('=');
            if (eq >= 0) {
                cookies.put(part.substring(0, eq).trim(), part.substring(eq + 1).trim());
            }
        }
    }

    private void parseQueryParams() {
        if (queryString == null || queryString.isEmpty())
            return;
        for (String pair : queryString.split("&")) {
            int eq = pair.indexOf('=');
            if (eq >= 0) {
                queryParams.put(urlDecode(pair.substring(0, eq)), urlDecode(pair.substring(eq + 1)));
            } else {
                queryParams.put(urlDecode(pair), "");
            }
        }
    }

    public static String urlDecode(String s) {
        try {
            return java.net.URLDecoder.decode(s, "UTF-8");
        } catch (Exception e) {
            return s;
        }
    }

    public boolean isComplete() {
        return state == ParseState.COMPLETE;
    }

    public boolean hasError() {
        return state == ParseState.ERROR;
    }

    @Override
    public String toString() {
        return method + " " + uri + " " + httpVersion;
    }
}