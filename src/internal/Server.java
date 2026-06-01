package internal;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;

import internal.jsonParser.mapper.ServerConfig;

public class Server {
    private final ServerConfig config;
    private final Router router;
    private Selector selector;

    public Server(ServerConfig config) {
        this.config = config;
        this.router = new Router(config);
    }

    public void start() throws IOException {
        selector = Selector.open();

        for (ServerConfig.VirtualServer vs : config.servers) {
            for (int port : vs.ports) {
                openServerSocket(vs.host, port);
            }
        }
    }

    private void openServerSocket(String host, int port) throws IOException {
        ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.configureBlocking(false);
        ssc.setOption(StandardSocketOptions.SO_REUSEADDR, true);
        ssc.bind(new InetSocketAddress(host, port));
        ssc.register(selector, SelectionKey.OP_ACCEPT, port);
        System.out.println("[Server] Listening on http://" + host + ":" + port + "/");
    }
}
