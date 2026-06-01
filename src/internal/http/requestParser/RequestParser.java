package internal.http.requestParser;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

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
                        return ParseResult.COMPLETE;
                    }

                    state = ParseState.FIXED_BODY;
                    continue;
                }
                case FIXED_BODY: {
                    if (in.remaining() < contentLength)
                        return ParseResult.NEED_MORE;

                    byte[] body = new byte[(int) contentLength];
                    in.get(body);
                    req.setBody(body);

                    return ParseResult.COMPLETE;
                }

                case CHUNK_SIZE: {
                    String line = readLineCRLF(in);

                    if (line == null)
                        return ParseResult.NEED_MORE;

                    // Strip chunk extensions
                    String sizePart = line.split(";", 2)[0].trim();
                    if (sizePart.isEmpty())
                        return ParseResult.BAD_REQUEST;

                    int size;

                    try {
                        size = Integer.parseInt(sizePart);
                    } catch (NumberFormatException e) {
                        return ParseResult.BAD_REQUEST;
                    }

                    if (size < 0)
                        return ParseResult.BAD_REQUEST;

                    state = currentChunkSize == 0 ? ParseState.TRAILERS : ParseState.CHUNK_DATA;
                    continue;
                }

                case CHUNK_DATA: {
                    if (in.remaining() < currentChunkSize)
                        return ParseResult.NEED_MORE;

                    if (totalBodyRead + currentChunkSize > maxBodyBytes)
                        return ParseResult.BODY_TOO_LARGE;

                    byte[] chunk = new byte[currentChunkSize];
                    in.get(chunk);
                    chunkOut.write(chunk, 0, chunk.length);

                    totalBodyRead += currentChunkSize;

                    state = ParseState.CHUNK_CRLF;
                    continue;
                }

                case CHUNK_CRLF: {
                    if (in.remaining() < 2)
                        return ParseResult.NEED_MORE;

                    byte b1 = in.get();
                    byte b2 = in.get();

                    if (b1 != '\r' || b2 != '\n')
                        return ParseResult.BAD_REQUEST;

                    currentChunkSize = -1;
                    state = ParseState.CHUNK_SIZE;
                    continue;
                }

                case TRAILERS: {
                    String line = readLineCRLF(in);
                    if (line == null)
                        return ParseResult.NEED_MORE;

                    if (line.isEmpty()) {
                        req.setBody(chunkOut.toByteArray());
                        return ParseResult.COMPLETE;
                    }

                    int colon = line.indexOf(':');
                    if (colon <= 0)
                        return ParseResult.BAD_REQUEST;

                    String name = line.substring(0, colon)
                            .trim()
                            .toLowerCase();

                    String value = line.substring(colon + 1)
                            .trim();

                    req.trailers.put(name, value);

                    continue;
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

    private String readLineCRLF(ByteBuffer in) {
        int pos = in.position();
        int lim = in.limit();

        for (int i = pos; i + 1 < lim; i++) {
            if (in.get(i) == '\r' && in.get(i + 1) == '\n') {
                int len = i - pos;
                byte[] bytes = new byte[len];
                in.get(bytes);
                in.get(); // consume \r
                in.get(); // consume \n
                return new String(bytes, StandardCharsets.US_ASCII);
            }
        }
        return null;
    }

    public HttpRequest takeRequest() {
        HttpRequest out = req;
        reset();
        return out;
    }

    private void reset() {
        state = ParseState.HEADERS;
        req = null;
        contentLength = 0;
        chunked = false;
        currentChunkSize = -1;
        totalBodyRead = 0;
        chunkOut = null;
    }

}
