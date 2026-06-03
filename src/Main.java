import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import internal.Server;
import internal.jsonParser.Parser;
import internal.jsonParser.mapper.ServerConfig;

public class Main {
    public static void main(String[] args) {
        String configPath = "config.json";
        if (args.length > 0) {
            configPath = args[0];
        }
        try {
            String fileContent = new String(Files.readAllBytes(Paths.get(configPath)));
            ServerConfig config = new Parser().parse(fileContent);

            new Server(config).start();
        } catch (IOException err) {
            System.err.println(configPath + " file do not have correct format!");
        } catch (Exception e) {
            System.err.println("[Main] Fatal: Server failed to start: " + e.getMessage());
            e.printStackTrace();
        }
    }
}