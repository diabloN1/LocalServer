import jsonParser.mapper.ServerConfig;

public class Server {
    private final ServerConfig config;

    public Server(ServerConfig config) {
        this.config = config;
    }

    public void start() {
        config.servers.forEach(server -> System.out.println(server));
    }
}
