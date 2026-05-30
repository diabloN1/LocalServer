package requestParser;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
public class RequestParser {
    private final int maxHeaderBytes;
    private final long maxBodyBytes;

    private HttpRequest req = null;
    private ParseState state = ParseState.HEADERS;
    private long contentLength = 0;

    // chunked-specific
    private boolean chunked = false;
    private int currentChunkSize = -1;
    private long totalBodyRead = 0;
    private ByteArrayOutputStream chunkOut = null;

    public RequestParser(int maxHeaderBytes, long maxBodyBytes) {
        this.maxHeaderBytes = maxHeaderBytes;
        this.maxBodyBytes = maxBodyBytes;
    }

    public ParseResult feed(ByteBuffer in) {
        in.flip();
        ParseResult result;
        try {
            result = parse(in);
        } catch (Exception e) {
            result = ParseResult.BAD_REQUEST;
        } finally {
            in.compact();
        }
        return result;
    }

    private ParseResult parse(ByteBuffer in) {
        while (true) {
            switch (state) {
                case HEADERS: {
                    ParseResult r = parseHeaders(in);
                    if (r != ParseResult.COMPLETE) {
                        return r;
                    }
                    String te = req.headers.getOrDefault("transfer-encoding", "").toLowerCase();
                    chunked = te.contains("chunked");

                    if (chunked) {
                        chunkOut = new ByteArrayOutputStream();
                        totalBodyRead = 0;
                        currentChunkSize = -1;
                        state = ParseState.CHUNK_SIZE;
                        continue;
                    }

                    // Fixed-length body
                    String clStr = req.headers.getOrDefault("content-length", "0");
                    try {
                        contentLength = Long.parseLong(clStr.trim());
                    } catch (NumberFormatException e) {
                        return ParseResult.BAD_REQUEST;
                    }
                    if (contentLength < 0)
                        return ParseResult.BAD_REQUEST;
                    if (contentLength > maxBodyBytes)
                        return ParseResult.BODY_TOO_LARGE;

                    if (contentLength == 0) {
                        req.setBody(new byte[0]);
                        state = ParseState.DONE;
                        return ParseResult.COMPLETE;
                    }


                }
            }
        }
    }

    private ParseResult parseHeaders(ByteBuffer in) {
        int end = findDoubleCRLF(in);

        if (end == -1) {
            if (in.remaining() > maxHeaderBytes)
                return ParseResult.BODY_TOO_LARGE;
            return ParseResult.NEED_MORE;
        }

        int start = in.position();
        int headerLen = end - start;
        if (headerLen > maxHeaderBytes)
            return ParseResult.BODY_TOO_LARGE;

        byte[] headerBytes = new byte[headerLen];
        in.get(headerBytes);

        try {
            req = HttpRequest.fromHeaderRaw(headerBytes);
        } catch (IllegalArgumentException e) {
            return ParseResult.BAD_REQUEST;
        }

        return ParseResult.COMPLETE;
    }

    private int findDoubleCRLF(ByteBuffer in) {
        int pos = in.position();
        int lim = in.limit();
        for (int i = pos; i + 3 < lim; i++) {
            if (in.get(i) == '\r' && in.get(i + 1) == '\n' &&
                    in.get(i + 2) == '\r' && in.get(i + 3) == '\n') {
                return i + 4;
            }
        }
        return -1;
    }
}
