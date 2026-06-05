package helpers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import internal.http.ErrorBuilder;
import internal.http.HttpResponse;
import internal.http.requestParser.HttpRequest;
import internal.jsonParser.mapper.ServerConfig;

public class FileManager {

    public static HttpResponse serveFile(File file,
            HttpRequest request,
            ErrorBuilder errorHandler) {
        try {
            if (!file.canRead())
                return errorHandler.buildError(403);

            byte[] content = Files.readAllBytes(file.toPath());
            String mime = getMimeType(file.getName());

            HttpResponse response = HttpResponse.ok();
            response.setBody(content, mime);
            response.setHeader("Cache-Control", "max-age=3600");
            return response;

        } catch (IOException e) {
            return errorHandler.buildError(500);
        }
    }

    public static HttpResponse savePostBody(HttpRequest request,
            ServerConfig.Route route,
            ErrorBuilder errorHandler) {
        try {
            File dir = new File(route.root);
            if (!dir.exists())
                dir.mkdirs();

            String fileName = "post_" + System.currentTimeMillis() + ".txt";
            File out = new File(dir, fileName);

            Files.write(out.toPath(), request.getBody());

            return HttpResponse.created()
                    .setTextBody("Created: " + fileName);

        } catch (IOException e) {
            return errorHandler.buildError(500);
        }
    }

    public static HttpResponse deleteFile(HttpRequest request,
            ServerConfig.Route route,
            ErrorBuilder errorHandler) {
        String path = request.getPath();
        String rel = path.substring(route.path.length());
        if (rel.startsWith("/"))
            rel = rel.substring(1);

        File file = new File(route.root + "/" + rel);

        if (!file.exists())
            return errorHandler.buildError(404);
        if (file.isDirectory())
            return errorHandler.buildError(403);

        try {
            String root = new File(route.root).getCanonicalPath();
            if (!file.getCanonicalPath().startsWith(root)) {
                return errorHandler.buildError(403);
            }
        } catch (IOException e) {
            return errorHandler.buildError(500);
        }

        return file.delete()
                ? HttpResponse.noContent()
                : errorHandler.buildError(500);
    }

    private static String getMimeType(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot >= 0) {
            String ext = filename.substring(dot).toLowerCase();
            return MimeTypes.TYPES.getOrDefault(ext, "application/octet-stream");
        }
        return "application/octet-stream";
    }
}