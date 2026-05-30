import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class HttpResponse {
    private int statusCode = 200;
    private String statusMessage = "OK";
    private Map<String, String> headers = new LinkedHashMap<>();
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
}