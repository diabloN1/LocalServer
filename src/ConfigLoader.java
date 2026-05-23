

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import jsonParser.tokenizer.JsonTokenizer;
import jsonParser.tokenizer.Token;

public class ConfigLoader {
    
    public static String read(String path) throws IOException {
        String fileContent = new String(Files.readAllBytes(Paths.get(path)));
        List<Token> item = new JsonTokenizer(fileContent).tokenize();
        item.forEach(i -> System.out.println(i.toString()));
        return fileContent;
    }
    
    public static ServerConfig parse(String json) {
        ServerConfig config = new ServerConfig();
        return config;
    }

}
