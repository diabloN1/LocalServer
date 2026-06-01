package handlers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import helpers.FileManager;
import internal.http.ErrorBuilder;
import internal.http.HttpResponse;
import internal.http.requestParser.HttpRequest;
import internal.jsonParser.mapper.ServerConfig;

public class GetHandler {

    public HttpResponse handle(HttpRequest request,
            ServerConfig.Route route,
            ServerConfig.VirtualServer vs,
            ErrorBuilder errorBuilder) {
        try {

            // Extract dynamic path
            String dynamicPath = request.getPath().substring(route.path.length());
            if (dynamicPath.startsWith("/")) {
                dynamicPath = dynamicPath.substring(1);
            }

            Path rootPath = Paths.get(route.root).toRealPath();
            Path fullPath = rootPath.resolve(dynamicPath).normalize();

            // Path traversal protection
            if (!fullPath.startsWith(rootPath)) {
                return errorBuilder.buildError(403);
            }

            // serveFile
            File file = fullPath.toFile();
            
            if (!file.exists()) {
                return errorBuilder.buildError(404);
            }

            return FileManager.serveFile(file, request, errorBuilder);

        } catch (IOException e) {
            return errorBuilder.buildError(500);
        }
    }
}