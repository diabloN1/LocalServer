package internal.jsonParser.mapper;

import java.util.*;

public class Mapper {

    public ServerConfig map(Object root) {

        Map<String, Object> rootMap = asMap(root, "root");

        ServerConfig config = new ServerConfig();

        Object serversObj = rootMap.get("servers");
        List<Object> servers = asList(serversObj, "servers");

        if (servers == null)
            throw new RuntimeException("'servers' field is required");

        for (Object s : servers) {
            config.servers.add(
                    mapServer(asMap(s, "server")));
        }

        return config;
    }

    private ServerConfig.VirtualServer mapServer(Map<String, Object> m) {

        ServerConfig.VirtualServer vs = new ServerConfig.VirtualServer();

        // host
        vs.host = asString(m.get("host"), "host");
        if (vs.host == null)
            vs.host = "127.0.0.1";

        // default
        Object def = m.get("default");
        if (def != null && !(def instanceof Boolean)) {
            throw typeError("default", "boolean");
        }
        vs.isDefault = def != null && (Boolean) def;

        // header size
        vs.clientMaxHeaderSize = asLong(
                m.get("client_max_header_size"),
                "client_max_header_size",
                1048576);

        // body size
        vs.clientMaxBodySize = asLong(
                m.get("client_max_body_size"),
                "client_max_body_size",
                1048576);

        // ports
        List<Object> ports = asList(m.get("ports"), "ports");
        if (ports != null) {
            for (Object p : ports) {
                if (!(p instanceof Number)) {
                    throw typeError("ports[]", "number");
                }
                vs.ports.add(((Number) p).intValue());
            }
        }

        // error pages
        Map<String, Object> errors = asMap(m.get("error_pages"), "error_pages");

        if (errors != null) {
            for (Map.Entry<String, Object> e : errors.entrySet()) {

                int code;
                try {
                    code = Integer.parseInt(e.getKey());
                } catch (NumberFormatException ex) {
                    throw new RuntimeException(
                            "Invalid HTTP error code: " + e.getKey());
                }

                if (!(e.getValue() instanceof String)) {
                    throw typeError("error_pages value", "string");
                }

                vs.errorPages.put(code, (String) e.getValue());
            }
        }

        // routes
        List<Object> routes = asList(m.get("routes"), "routes");
        if (routes != null) {
            for (Object r : routes) {
                vs.routes.add(
                        mapRoute(asMap(r, "route")));
            }
        }

        // sort routes (important for matching)
        vs.routes.sort((a, b) -> b.path.length() - a.path.length());

        return vs;
    }

    private ServerConfig.Route mapRoute(Map<String, Object> m) {

        ServerConfig.Route r = new ServerConfig.Route();

        r.path = asString(m.get("path"), "path");
        if (r.path == null)
            r.path = "/";

        r.root = asString(m.get("root"), "root");
        if (r.root == null)
            r.root = "./www";

        r.index = asString(m.get("index"), "index");
        if (r.index == null)
            r.index = "index.html";

        // directory listing
        Object dl = m.get("directory_listing");
        if (dl != null && !(dl instanceof Boolean)) {
            throw typeError("directory_listing", "boolean");
        }
        r.directoryListing = dl != null && (Boolean) dl;

        r.redirect = asString(m.get("redirect"), "redirect");

        r.redirectCode = (int) asLong(
                m.get("redirect_code"),
                "redirect_code",
                301);

        r.cgiExtension = asString(m.get("cgi_extension"), "cgi_extension");

        // methods
        List<Object> methods = asList(m.get("methods"), "methods");
        if (methods != null) {
            for (Object method : methods) {
                if (!(method instanceof String)) {
                    throw typeError("methods[]", "string");
                }
                r.methods.add(((String) method).toUpperCase());
            }
        }

        return r;
    }

    // HELPER
    @SuppressWarnings("unchecked")
    private List<Object> asList(Object o, String field) {
        if (o == null)
            return null;
        if (!(o instanceof List)) {
            throw typeError(field, "array");
        }
        return (List<Object>) o;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object o, String field) {
        if (o == null)
            return null;
        if (!(o instanceof Map)) {
            throw typeError(field, "object");
        }
        return (Map<String, Object>) o;
    }

    private String asString(Object o, String field) {
        if (o == null)
            return null;
        if (!(o instanceof String)) {
            throw typeError(field, "string");
        }
        return (String) o;
    }

    private long asLong(Object o, String field, long def) {
        if (o == null)
            return def;
        if (!(o instanceof Number)) {
            throw typeError(field, "number");
        }
        return ((Number) o).longValue();
    }

    private RuntimeException typeError(String field, String expected) {
        return new RuntimeException(
                "Invalid config: field '" + field +
                        "' must be " + expected);
    }
}