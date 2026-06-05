package handlers;

import java.io.File;
import java.io.IOException;
import java.util.List;

import internal.http.ErrorBuilder;
import internal.http.HttpResponse;
import internal.http.MultipartParser;
import internal.http.requestParser.HttpRequest;
import internal.jsonParser.mapper.ServerConfig;

public class UploadHandler {

    public static HttpResponse handle(HttpRequest request,
            ServerConfig.Route route,
            ErrorBuilder errorHandler) {

        String contentType = request.getHeader("content-type");
        if (contentType == null)
            return errorHandler.buildError(400);

        String boundary = null;
        for (String part : contentType.split(";")) {
            part = part.trim();
            if (part.startsWith("boundary=")) {
                boundary = part.substring(9);
                break;
            }
        }

        if (boundary == null)
            return errorHandler.buildError(400);

        try {
            File dir = new File(route.root);
            if (!dir.exists())
                dir.mkdirs();

            List<String> files = MultipartParser.parse(request.getBody(), boundary, dir);

            if (files.isEmpty())
                return errorHandler.buildError(400);

            return HttpResponse.created()
                    .setHtmlBody("<h2>Upload OK</h2>");

        } catch (IOException e) {
            return errorHandler.buildError(500);
        }
    }
}