import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) {
        String configPath = "config.json";
        if (args.length > 0) {
            configPath = args[0];
        }
        try {
            String fileContent = new String(Files.readAllBytes(Paths.get(configPath)));

            new Server().run(fileContent);
        } catch (IOException err) {
            System.err.println(configPath + " file do not have correct format!");
        }

    }
}