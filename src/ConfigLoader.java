import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ConfigLoader {
    
    public static String read(String path) throws IOException {
        byte[] fileBytes = Files.readAllBytes(Paths.get(path));
        return new String(fileBytes);
    }
    
    public static ServerConfig parse(String json) {
        ServerConfig config = new ServerConfig();
        return config;
    }

}
