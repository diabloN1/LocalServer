
package handlers;

import helpers.FileManager;
import internal.http.ErrorBuilder;
import internal.http.HttpResponse;
import internal.http.requestParser.HttpRequest;
import internal.jsonParser.mapper.ServerConfig;

public class DeleteHandler {

    public HttpResponse handle(HttpRequest request,
            ServerConfig.Route route,
            ServerConfig.VirtualServer vs,
            ErrorBuilder errorHandler) {

        return FileManager.deleteFile(request, route, errorHandler);
    }
}