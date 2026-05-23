import jsonParser.Parser;
import jsonParser.mapper.ServerConfig;

public class Server {

    public void run(String configContent) {
        ServerConfig config = new Parser().parse(configContent);
        config.servers.forEach(server -> System.out.println(server));
    }

}
