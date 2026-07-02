package handlers;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import helpers.DirectoryListingBuilder;
import helpers.FileManager;
import internal.CGIExecuter;
import internal.http.ErrorBuilder;
import internal.http.HttpResponse;
import internal.http.requestParser.HttpRequest;
import internal.jsonParser.mapper.ServerConfig;

public class GetHandler {

    public HttpResponse handle(HttpRequest request,
            ServerConfig.Route route,
            ServerConfig.VirtualServer vs,
            ErrorBuilder errorHandler) {

        try {
            String path = request.getPath();

            String routeRelPath = path.substring(route.path.length());
            if (routeRelPath.startsWith("/")) {
                routeRelPath = routeRelPath.substring(1);
            }

            Path rootPath = Paths.get(route.root).toRealPath();
            Path fullPath = rootPath.resolve(routeRelPath).normalize();

            // (path traversal protection)
            if (!fullPath.startsWith(rootPath)) {
                return errorHandler.buildError(403);
            }

            String finalPathStr = fullPath.toString();

            if (route.cgiExtension != null && path.endsWith(route.cgiExtension)) {
                return CGIExecuter.execute(request, finalPathStr, path);
            }

            File file = fullPath.toFile();

            if (file.isDirectory()) {

                if (!path.endsWith("/")) {
                    return HttpResponse.redirect(path + "/", 301);
                }

                // index file
                if (route.index != null && !route.index.isEmpty()) {
                    Path indexPath = fullPath.resolve(route.index).normalize();

                    if (indexPath.startsWith(rootPath) && indexPath.toFile().exists()) {
                        
                        // Test sessions
                        // Session session = request.getSession();
                        // var helloValue = session.attributes.getOrDefault("hello", "");
                        // if (helloValue != "") {
                        //     System.out.println(helloValue);
                        // }
                        // session.attributes.put("hello", "123");
                        
                        return FileManager.serveFile(indexPath.toFile(), request, errorHandler);
                    }
                }

                if (route.directoryListing) {
                    return DirectoryListingBuilder.build(file, path);
                }

                return errorHandler.buildError(403);
            }

            if (!file.exists()) {
                return errorHandler.buildError(404);
            }

            return FileManager.serveFile(file, request, errorHandler);

        } catch (Exception e) {
            return errorHandler.buildError(500);
        }
    }
}