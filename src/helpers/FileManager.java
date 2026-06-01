package helpers;

import java.io.File;

import internal.http.ErrorBuilder;
import internal.http.HttpResponse;
import internal.http.requestParser.HttpRequest;

public class FileManager {

    public static HttpResponse serveFile(File file, HttpRequest request, ErrorBuilder errorBuilder) {
        return HttpResponse.ok();
    }
}
