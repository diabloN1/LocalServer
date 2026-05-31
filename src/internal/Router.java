package internal;

import internal.http.ErrorBuilder;
import internal.http.HttpResponse;
import internal.http.requestParser.HttpRequest;
import internal.jsonParser.mapper.ServerConfig;

public class Router {
    private final ServerConfig config;

    public Router(ServerConfig serverConfig) {
        this.config = serverConfig;
    }

    public HttpResponse handle(HttpRequest request, ServerConfig.VirtualServer vs) {

        String path = request.getPath();
        ErrorBuilder errorBuilder = new ErrorBuilder(vs);

        if (path.contains("..")) {
            return errorBuilder.buildError(403);
        }

        ServerConfig.Route route = config.findRoute(vs, path);

        if (route == null) {
            return errorBuilder.buildError(404);
        }

        if (route.redirect != null && !route.redirect.isEmpty()) {
            return HttpResponse.redirect(route.redirect, route.redirectCode);
        }

        if (!route.methods.isEmpty() && !route.methods.contains(request.getMethod())) {
            HttpResponse r = errorBuilder.buildError(405);
            r.setHeader("Allow", String.join(", ", route.methods));
            return r;
        }
        
        return dispatch();
    }

    private HttpResponse dispatch() {
        return HttpResponse.ok();
    }
}
