package internal;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

import internal.http.ErrorBuilder;
import internal.http.HttpResponse;
import internal.http.requestParser.HttpRequest;
import internal.http.requestParser.ParseResult;
import internal.http.requestParser.RequestParser;
import internal.jsonParser.mapper.ServerConfig;

public class Server {
    private static final int BUFFER_SIZE = 64 * 1024;
    private static final long REQUEST_TIMEOUT_MS = 30_000; // 30s
    private static final long SESSION_PURGE_INTERVAL_MS = 5 * 60_000; // 5 min

    private final ServerConfig config;
    private final Router router;
    private Selector selector;
    private volatile boolean running = true;
    private long lastSessionPurge = System.currentTimeMillis();

    private static class ClientContext {
        SocketChannel channel;
        RequestParser parser;
        ByteBuffer in;
        ByteBuffer writeBuffer;
        int port;
        long lastActivity = System.currentTimeMillis();
        boolean closeAfterWrite = false;

        ClientContext(SocketChannel ch, long maxHeaderSize, long maxBodySize, int port) {
            this.channel = ch;
            this.in = ByteBuffer.allocate(BUFFER_SIZE);
            this.parser = new RequestParser(maxHeaderSize, maxBodySize);
            this.port = port;
        }

        void touch() {
            lastActivity = System.currentTimeMillis();
        }

        boolean isTimedOut() {
            return System.currentTimeMillis() - lastActivity > REQUEST_TIMEOUT_MS;
        }
    }

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
                            handleAccept(key);
                        } else if (key.isReadable()) {
                            handleRead(key);
                        } else if (key.isWritable()) {
                            handleWrite(key);
                        }
                    } catch (CancelledKeyException e) {
                        closeKey(key);
                    } catch (Exception e) {
                        System.err.println("[Server] Error on key: " + e.getMessage());
                        closeKey(key);
                    }
                }
            }

            maybePurgeSessions();
        }

        System.out.println("[Server] Shutting down...");
        selector.close();
    }

    private void openServerSocket(String host, int port) throws IOException {
        ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.configureBlocking(false);
        ssc.setOption(StandardSocketOptions.SO_REUSEADDR, true);
        ssc.bind(new InetSocketAddress(host, port));
        ssc.register(selector, SelectionKey.OP_ACCEPT, port);
        System.out.println("[Server] Listening on http://" + host + ":" + port + "/");
    }

    private void handleAccept(SelectionKey key) throws IOException {
        ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
        SocketChannel clientChannel = ssc.accept();
        if (clientChannel == null)
            return;

        clientChannel.configureBlocking(false);
        clientChannel.setOption(StandardSocketOptions.TCP_NODELAY, true);

        int port = (int) key.attachment();
        ServerConfig.VirtualServer vs = config.findServer(null, port);

        if (vs == null) {
            throw new IllegalStateException("No virtual server found");
        }

        clientChannel.register(selector, SelectionKey.OP_READ, port);

        ClientContext ctx = new ClientContext(
                clientChannel,
                vs.clientMaxHeaderSize,
                vs.clientMaxBodySize,
                port);

        clientChannel.register(selector, SelectionKey.OP_READ, ctx);

        System.out.println("[Server] Accepted connection from " +
                clientChannel.getRemoteAddress() + " on port " + port);
    }

    private void handleRead(SelectionKey key) throws IOException {
        ClientContext ctx = (ClientContext) key.attachment();
        int bytesRead = ctx.channel.read(ctx.in);

        if (bytesRead < 0) {
            closeKey(key);
            return;
        }
        if (bytesRead == 0)
            return;

        ctx.touch();

        ParseResult result = ctx.parser.feed(ctx.in);

        ServerConfig.VirtualServer vs = config.findServer(null, ctx.port);
        ErrorBuilder eh = new ErrorBuilder(vs);
        switch (result) {
            case COMPLETE:
                processRequest(key, ctx);
                break;

            case BAD_REQUEST:
                sendResponse(key, ctx, eh.buildError(400), true);
                break;

            case BODY_TOO_LARGE:
                sendResponse(key, ctx, eh.buildError(413), true);
                break;

            case NEED_MORE:
                break;
        }

    }

    private void processRequest(SelectionKey key, ClientContext ctx) {

        HttpRequest request = ctx.parser.takeRequest();

        // Get requested virtual server
        String hostHeader = request.getHeader("host");
        String host = hostHeader != null ? hostHeader.split(":")[0] : null;
        ServerConfig.VirtualServer vs = config.findServer(host, ctx.port);

        HttpResponse response;

        if (vs == null) {
            response = new ErrorBuilder(null).buildError(500);
        } else {
            try {
                response = router.handle(request, vs);
            } catch (Exception e) {
                System.err.println("[Server] Router error: " + e.getMessage());
                response = new ErrorBuilder(vs).buildError(500);
            }
        }

        // Set Keep-alive
        String connHeader = request.getHeader("connection");
        boolean keepAlive = !"close".equalsIgnoreCase(connHeader) &&
                "HTTP/1.1".equalsIgnoreCase(request.getHttpVersion());

        if (keepAlive) {
            response.setHeader("Connection", "keep-alive");
            response.setHeader("Keep-Alive", "timeout=30, max=100");
        } else {
            response.setHeader("Connection", "close");
        }

        System.out.printf("[%s] %s %s -> %d%n",
                new java.util.Date(), request.getMethod(),
                request.getUri(), response.getStatusCode());

        sendResponse(key, ctx, response, !keepAlive);
    }

    private void sendResponse(SelectionKey key, ClientContext ctx, HttpResponse response,
            boolean closeAfter) {

        byte[] responseBytes = response.toBytes();
        ctx.writeBuffer = ByteBuffer.wrap(responseBytes);
        ctx.closeAfterWrite = closeAfter;

        key.interestOps(SelectionKey.OP_WRITE);
    }

    private void handleWrite(SelectionKey key) throws IOException {
        ClientContext ctx = (ClientContext) key.attachment();
        SocketChannel channel = ctx.channel;

        channel.write(ctx.writeBuffer);
        ctx.touch();

        if (!ctx.writeBuffer.hasRemaining()) {
            if (ctx.closeAfterWrite) {
                closeKey(key);
            } else {
                ctx.writeBuffer = null;
                key.interestOps(SelectionKey.OP_READ);
            }
        }
    }

    private void maybePurgeSessions() {
        long now = System.currentTimeMillis();
        if (now - lastSessionPurge > SESSION_PURGE_INTERVAL_MS) {
            int purged = utils.Session.purgeExpired();
            if (purged > 0)
                System.out.println("[Server] Purged " + purged + " expired sessions");
            lastSessionPurge = now;
        }
    }

    private void closeKey(SelectionKey key) {
        try {
            key.cancel();
            key.channel().close();
        } catch (IOException ignored) {
        }
    }

    public void installShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            running = false;
            selector.wakeup();
        }));
    }
}
