import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        String configPath = "config.json";
        if (args.length > 0) {
            configPath = args[0];
        }

        try {
            System.out.println(ConfigLoader.read(configPath));
        } catch (IOException err) {
            System.err.println(configPath + " file do not have correct format!");
        }
    }
}