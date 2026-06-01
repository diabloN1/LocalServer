package helpers;
import java.util.HashMap;
import java.util.Map;

public final class MimeTypes {

    public static final Map<String, String> TYPES = new HashMap<>();

    static {
        TYPES.put(".html", "text/html; charset=utf-8");
        TYPES.put(".htm",  "text/html; charset=utf-8");
        TYPES.put(".css",  "text/css");
        TYPES.put(".js",   "application/javascript");
        TYPES.put(".json", "application/json");
        TYPES.put(".xml",  "application/xml");
        TYPES.put(".txt",  "text/plain; charset=utf-8");
        TYPES.put(".png",  "image/png");
        TYPES.put(".jpg",  "image/jpeg");
        TYPES.put(".jpeg", "image/jpeg");
        TYPES.put(".gif",  "image/gif");
        TYPES.put(".svg",  "image/svg+xml");
        TYPES.put(".ico",  "image/x-icon");
        TYPES.put(".pdf",  "application/pdf");
        TYPES.put(".zip",  "application/zip");
        TYPES.put(".mp4",  "video/mp4");
        TYPES.put(".webm", "video/webm");
        TYPES.put(".mp3",  "audio/mpeg");
        TYPES.put(".woff2","font/woff2");
        TYPES.put(".woff", "font/woff");
        TYPES.put(".py",   "text/plain");
        TYPES.put(".sh",   "text/plain");
    }

    private MimeTypes() {
        // prevent instantiation
    }
}