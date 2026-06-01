package handlers;

import internal.http.ErrorBuilder;
import internal.http.HttpResponse;
import internal.http.requestParser.HttpRequest;
import internal.jsonParser.mapper.ServerConfig;

public class GetHandler {

    public HttpResponse handle(HttpRequest request,
            ServerConfig.Route route,
            ServerConfig.VirtualServer vs,
            ErrorBuilder ErrorBuilder) {
                return new HttpResponse(200);
            }
}