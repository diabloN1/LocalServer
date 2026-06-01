package internal;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.Iterator;
import java.util.Set;

import internal.jsonParser.mapper.ServerConfig;

public class Server {
    private final ServerConfig config;
    private final Router router;
    private Selector selector;
    private volatile boolean running = true;

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
        System.out.println("[Server] Event loop started. Press Ctrl+C to stop.");
        installShutdownHook();

        // Main event loop
        while (running) {
                int readyCount = selector.select(1000); // 1s timeout for maintenance tasks

                if (readyCount > 0) {
                    Set<SelectionKey> selectedKeys = selector.selectedKeys();
                    Iterator<SelectionKey> keyIterator = selectedKeys.iterator();

                    while (keyIterator.hasNext()) {
                        SelectionKey key = keyIterator.next();
                        keyIterator.remove();

                        if (!key.isValid())
                            continue;

                        try {
                            if (key.isAcceptable()) {
                                // handleAccept(key);
                            }
                        } catch (CancelledKeyException e) {
                            
                        } catch (Exception e) {
                            
                        }
                    }
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

    public void installShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            running = false;
            selector.wakeup();
        }));
    }
}
