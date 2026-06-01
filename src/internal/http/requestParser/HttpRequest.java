package internal.http.requestParser;

import java.nio.charset.StandardCharsets;
import java.util.*;

import utils.Cookie;

public class HttpRequest {

    private String method;
    private String uri;
    private String httpVersion;
    private String path;
    private String queryString = "";

    final Map<String, String> headers = new LinkedHashMap<>();
    final Map<String, String> queryParams = new LinkedHashMap<>();
    final Map<String, Cookie> cookies = new LinkedHashMap<>();
    final Map<String, String> trailers = new LinkedHashMap<>();

    private byte[] body = new byte[0];

    private HttpRequest() {
    }

    public static HttpRequest fromHeaderRaw(byte[] headerBytes) {
        HttpRequest req = new HttpRequest();
        String headerRaw = new String(headerBytes, StandardCharsets.US_ASCII);

        String[] lines = headerRaw.split("\r\n", -1);
        if (lines.length == 0 || lines[0].isEmpty()) {
            throw new IllegalArgumentException();
        }

        // Request Line
        String[] reqLine = lines[0].split(" ", 3);
        if (reqLine.length != 3)
            throw new IllegalArgumentException();
        req.method = reqLine[0].toUpperCase();
        req.setUri(reqLine[1]);
        req.httpVersion = reqLine[2];

        // Headers
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i];
            if (line.isEmpty())
                continue;
            int colon = line.indexOf(':');
            if (colon <= 0)
                throw new IllegalArgumentException();

            String name = line.substring(0, colon).trim().toLowerCase();
            String value = line.substring(colon + 1).trim();
            req.headers.put(name, value);
            if (name.equals("cookie"))
                req.parseCookies(value);
        }

        return req;
    }

    // ---- Getters ----

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

    public byte[] getBody() {
        return body;
    }

    public int getContentLength() {
        String v = headers.get("content-length");
        if (v == null)
            return -1;
        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public String getHeader(String name) {
        return headers.get(name.toLowerCase());
    }

    // ---- Setters ----

    private void setUri(String raw) {
        this.uri = raw;
        int q = raw.indexOf('?');
        if (q >= 0) {
            this.path = raw.substring(0, q);
            this.queryString = raw.substring(q + 1);
            parseQueryParams();
        } else {
            this.path = raw;
        }
    }

    public void setBody(byte[] body) {
        this.body = body;
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

    private void parseCookies(String header) {
        cookies.putAll(Cookie.parseCookieHeader(header));
    }

    public static String urlDecode(String s) {
        try {
            return java.net.URLDecoder.decode(s, "UTF-8");
        } catch (Exception e) {
            return s;
        }
    }

    @Override
    public String toString() {
        return method + " " + uri + " " + httpVersion;
    }
}
