package utils;

import java.net.URLEncoder;
import java.net.URLDecoder;

public class Cookie {
    private String name, value, domain, sameSite, path = "/";
    private int maxAge = -1;
    private boolean httpOnly = false, secure = false;

    public Cookie(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String toHeaderValue() {
        StringBuilder sb = new StringBuilder();
        sb.append(encode(name)).append("=").append(encode(value));
        if (path != null)
            sb.append("; Path=").append(path);
        if (domain != null)
            sb.append("; Domain=").append(domain);
        if (maxAge >= 0)
            sb.append("; Max-Age=").append(maxAge);
        if (httpOnly)
            sb.append("; HttpOnly");
        if (secure)
            sb.append("; Secure");
        if (sameSite != null)
            sb.append("; SameSite=").append(sameSite);
        return sb.toString();
    }

    public Cookie path(String p) {
        this.path = p;
        return this;
    }

    public Cookie domain(String d) {
        this.domain = d;
        return this;
    }

    public Cookie maxAge(int s) {
        this.maxAge = s;
        return this;
    }

    public Cookie httpOnly() {
        this.httpOnly = true;
        return this;
    }

    public Cookie secure() {
        this.secure = true;
        return this;
    }

    // Possible values : Strict - Lax - None
    public Cookie sameSite(String s) {
        this.sameSite = s;
        return this;
    }

    public Cookie delete() {
        this.maxAge = 0;
        this.value = "";
        return this;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    private static String encode(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8").replace("+", "%20");
        } catch (Exception e) {
            return s;
        }
    }

    private static String decode(String s) {
        try {
            return URLDecoder.decode(s, "UTF-8");
        } catch (Exception e) {
            return s;
        }
    }
}
