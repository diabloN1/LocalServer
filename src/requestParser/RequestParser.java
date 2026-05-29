package requestParser;

public class RequestParser {
    private HttpRequest request;
    private byte[] rawBytes;
    private long maxBodySize;
    private int writePos;
    private int headerEnd;

    public RequestParser(long maxBodySize) {
        this.maxBodySize = maxBodySize;
    }

    public HttpRequest getRequest() {
        return request;
    }

    // public boolean parse(byte[] data) {

    // }

    private void parseHeaderSection() {
        String content = new String(rawBytes, 0, writePos);
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

}
