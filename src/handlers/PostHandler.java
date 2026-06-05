
package handlers;

import internal.CGIExecuter;
import internal.http.ErrorBuilder;
import internal.http.HttpResponse;
import internal.http.requestParser.HttpRequest;
import internal.jsonParser.mapper.ServerConfig;
import helpers.FileManager;

public class PostHandler {

    public HttpResponse handle(HttpRequest request,
            ServerConfig.Route route,
            ServerConfig.VirtualServer vs,
            ErrorBuilder errorHandler) {

        String path = request.getPath();

        if (route.cgiExtension != null && path.endsWith(route.cgiExtension)) {
            String rel = path.substring(route.path.length());
            if (rel.startsWith("/"))
                rel = rel.substring(1);

            String scriptPath = route.root + "/" + rel;
            return CGIExecuter.execute(request, scriptPath, path);
        }

        String contentType = request.getHeader("content-type");

        if (contentType != null && contentType.contains("multipart/form-data")) {
            return UploadHandler.handle(request, route, errorHandler);
        }

        if (request.getBody() != null && request.getBody().length > 0) {
            return FileManager.savePostBody(request, route, errorHandler);
        }

        return HttpResponse.ok().setTextBody("OK");
    }
}