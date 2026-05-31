package internal.jsonParser.mapper;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServerConfig {
    public static class Route {
        public String path;
        public List<String> methods = new ArrayList<>();
        public String root;
        public String index;
        public boolean directoryListing = false;
        public String redirect;
        public int redirectCode = 301;
        public String cgiExtension;

        @Override
        public String toString() {
            return "Route{path='" + path + "', methods=" + methods +
                    ", root='" + root + "', cgi='" + cgiExtension + "'}";
        }
    }

    public static class VirtualServer {
        public String host;
        public List<Integer> ports = new ArrayList<>();
        public boolean isDefault = false;
        public long clientMaxHeaderSize = 1048576; // 1MB default
        public long clientMaxBodySize = 1048576;
        public Map<Integer, String> errorPages = new HashMap<>();
        public List<Route> routes = new ArrayList<>();

        @Override
        public String toString() {
            return "VirtualServer{host='" + host + "', ports=" + ports + "}";
        }
    }

    public Route findRoute(VirtualServer vs, String path) {
        Route bestMatch = null;
        int bestLen = -1;

        for (Route route : vs.routes) {
            if (path.startsWith(route.path)) {
                if (route.path.length() > bestLen) {
                    bestLen = route.path.length();
                    bestMatch = route;
                }
            }
        }
        return bestMatch;
    }

    public List<VirtualServer> servers = new ArrayList<>();
}
