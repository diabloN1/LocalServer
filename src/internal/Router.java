package internal;

import handlers.GetHandler;
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

        return dispatch(request, route, vs, errorBuilder);
    }

    private HttpResponse dispatch(HttpRequest request,
                                  ServerConfig.Route route,
                                  ServerConfig.VirtualServer vs,
                                  ErrorBuilder errorBuilder) {
        return switch (request.getMethod()) {

        case "GET"    -> new GetHandler().handle(request, route, vs, errorBuilder);
        default -> errorBuilder.buildError(405);
        };
    }
}
