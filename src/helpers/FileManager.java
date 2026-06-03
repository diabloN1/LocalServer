package helpers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import internal.http.ErrorBuilder;
import internal.http.HttpResponse;
import internal.http.requestParser.HttpRequest;

public class FileManager {

    public static HttpResponse serveFile(File file, HttpRequest request, ErrorBuilder errorBuilder) {
        try {
            byte[] content = Files.readAllBytes(file.toPath());
            String mime = MimeTypes.getMimeType(file.getName());

            return HttpResponse
                    .ok()
                    .setHeader("Cache-Control", "max-age=3600")
                    .setBody(content, mime);
        } catch (IOException e) {
            System.out.println(file.toPath());
            return errorBuilder.buildError(500);
        }
    }
}
