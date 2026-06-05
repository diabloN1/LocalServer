package internal.http;

import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import utils.Cookie;

public class HttpResponse {
    private int statusCode = 200;
    private String statusMessage = "OK";
    private Map<String, String> headers = new LinkedHashMap<>();
    private List<String> setCookies = new ArrayList<>();
    private byte[] body = new byte[0];

    private static final Map<Integer, String> STATUS_MESSAGES = new HashMap<>();

    static {
        STATUS_MESSAGES.put(200, "OK");
        STATUS_MESSAGES.put(201, "Created");
        STATUS_MESSAGES.put(204, "No Content");
        STATUS_MESSAGES.put(301, "Moved Permanently");
        STATUS_MESSAGES.put(302, "Found");
        STATUS_MESSAGES.put(304, "Not Modified");
        STATUS_MESSAGES.put(400, "Bad Request");
        STATUS_MESSAGES.put(403, "Forbidden");
        STATUS_MESSAGES.put(404, "Not Found");
        STATUS_MESSAGES.put(405, "Method Not Allowed");
        STATUS_MESSAGES.put(408, "Request Timeout");
        STATUS_MESSAGES.put(413, "Content Too Large");
        STATUS_MESSAGES.put(500, "Internal Server Error");
        STATUS_MESSAGES.put(501, "Not Implemented");
    }

    public HttpResponse(int statusCode) {
        this.statusCode = statusCode;
        this.statusMessage = STATUS_MESSAGES.getOrDefault(statusCode, "UNKNOWN");
        setHeader("Server", "LocalServer/1.0");
        setHeader("Date", DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH)
                .withZone(ZoneId.of("GMT"))
                .format(ZonedDateTime.now()));
        setHeader("Connection", "keep-alive");
    }

    public HttpResponse setHeader(String name, String value) {
        headers.put(name, value);
        return this;
    }

    public HttpResponse setStatus(int code) {
        this.statusCode = code;
        this.statusMessage = STATUS_MESSAGES.getOrDefault(code, "Unknown");
        return this;
    }

    public HttpResponse setBody(byte[] body, String contentType) {
        this.body = body;
        setHeader("Content-Type", contentType);
        setHeader("Content-Length", String.valueOf(body.length));
        return this;
    }

    public HttpResponse setBody(String body, String contentType) {
        return setBody(body.getBytes(StandardCharsets.UTF_8), contentType);
    }

    public HttpResponse setTextBody(String body) {
        return setBody(body, "text/plain; charset=utf-8");
    }

    public HttpResponse setHtmlBody(String body) {
        return setBody(body, "text/html; charset=utf-8");
    }

    public HttpResponse addCookie(Cookie cookie) {
        setCookies.add(cookie.toHeaderValue());
        return this;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public byte[] toBytes() {
        StringBuilder sb = new StringBuilder();
        sb.append("HTTP/1.1 ").append(statusCode).append(" ").append(statusMessage).append("\r\n");
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("\r\n");
        }

        for (String c : setCookies) {
            sb.append("Set-Cookie: ").append(c).append("\r\n");
        }

        sb.append("\r\n");

        byte[] headerBytes = sb.toString().getBytes(StandardCharsets.UTF_8);
        byte[] result = new byte[headerBytes.length + body.length];
        System.arraycopy(headerBytes, 0, result, 0, headerBytes.length);
        System.arraycopy(body, 0, result, headerBytes.length, body.length);
        return result;
    }

    public static HttpResponse ok() {
        return new HttpResponse((200));
    }

    public static HttpResponse created() {
        return new HttpResponse((201));
    }

    public static HttpResponse noContent() {
        return new HttpResponse((204));
    }

    public static HttpResponse badRequest() {
        return new HttpResponse((400));
    }

    public static HttpResponse forbidden() {
        return new HttpResponse((403));
    }

    public static HttpResponse notFound() {
        return new HttpResponse((404));
    }

    public static HttpResponse methodNotAllowed() {
        return new HttpResponse((405));
    }

    public static HttpResponse tooLarge() {
        return new HttpResponse((413));
    }

    public static HttpResponse internalServerError() {
        return new HttpResponse((500));
    }

    public static HttpResponse redirect(String location, int code) {
        HttpResponse res = new HttpResponse(code);
        res.setHeader("Location", location);
        res.setBody("", "text/html");
        return res;
    }
}