package requestParser;

import java.util.Arrays;

public class RequestParser {
    private HttpRequest request;
    private byte[] rawBytes;
    private long maxHeaderSize;
    private long maxBodySize;
    private int readPos;
    private int headerEnd;

    public RequestParser(long maxHeaderSize, long maxBodySize) {
        this.maxBodySize = maxBodySize;
        this.maxHeaderSize = maxHeaderSize;
    }

    public HttpRequest getRequest() {
        return request;
    }

    public boolean feed(byte[] data, int length) {
        if (readPos + length > rawBytes.length) {
            rawBytes = Arrays.copyOf(rawBytes, rawBytes.length * 2 + length);
        }

        System.arraycopy(data, 0, rawBytes, readPos, length);
        readPos += length;

        if (!request.isHeadersComplete() && readPos + length > maxHeaderSize) {
            request.setState(ParseState.ERROR);
            return false;
        }

        if (request.getState() == ParseState.REQUEST_LINE ||
                request.getState() == ParseState.HEADERS) {
            parseHeaderSection();
        }

        if (request.isHeadersComplete()) {
            if (request.isChunked()) {
                parseChunkedBody();
            } else {
                parseBody();
            }
        }

        return request.isComplete();
    }

    private void parseHeaderSection() {
        String content = new String(rawBytes, 0, readPos);
        int end = content.indexOf("\r\n\r\n");
        if (end < 0)
            return;

        headerEnd = end + 4;
        String headerContent = content.substring(0, end);
        String[] lines = headerContent.split("\r\n");

        String[] parts = lines[0].split(" ", 3);
        if (parts.length != 3) {
            request.setState(ParseState.ERROR);
            return;
        }

        request.setMethod(parts[0]);
        request.setUri(parts[1]);
        request.setHttpVersion(parts[2]);
        request.setState(ParseState.HEADERS);

        for (int i = 1; i < lines.length; i++) {
            int colonIdx = lines[i].indexOf(':');
            if (colonIdx > 0) {
                String name = lines[i].substring(0, colonIdx).trim();
                String value = lines[i].substring(colonIdx + 1).trim();
                request.addHeader(name, value);
            }
        }

        request.setHeadersComplete(true);

        if (!request.isChunked() && request.getContentLength() <= 0) {
            request.setState(ParseState.COMPLETE);
        }
    }

    private void parseBody() {
        int contentLength = request.getContentLength();
        if (contentLength <= 0) {
            request.setState(ParseState.COMPLETE);
            return;
        }

        if (contentLength > maxBodySize) {
            request.setState(ParseState.ERROR);
            return;
        }

        int available = readPos - headerEnd;

        if (available >= contentLength) {
            byte[] body = new byte[contentLength];
            System.arraycopy(rawBytes, headerEnd, body, 0, contentLength);
            request.setBody(body);
            request.setState(ParseState.COMPLETE);
        }
    }

    private void parseChunkedBody() {
    }
}
