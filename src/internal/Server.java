package internal;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
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
            int readyCount = selector.select(1000); // max 1s tick

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

    private void handleAccept(SelectionKey key) throws IOException {
        ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
        SocketChannel clientChannel = ssc.accept();
        if (clientChannel == null)
            return;

        clientChannel.configureBlocking(false);
        clientChannel.setOption(StandardSocketOptions.TCP_NODELAY, true);

        int port = (int) key.attachment();
        // ServerConfig.VirtualServer vs = 
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
