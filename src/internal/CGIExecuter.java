package internal;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import internal.http.HttpResponse;
import internal.http.requestParser.HttpRequest;

public class CGIExecuter {

    private static final int CGI_TIMEOUT_MS = 10000; // 10s timeout
    private static final Map<String, String> INTERPRETERS = new HashMap<>();

    static {
        INTERPRETERS.put(".py", "python3");
        INTERPRETERS.put(".sh", "bash");
        INTERPRETERS.put(".rb", "ruby");
        INTERPRETERS.put(".pl", "perl");
        INTERPRETERS.put(".php", "php");
    }

    // Execute a CGI script and return the HTTP response.

    public static HttpResponse execute(HttpRequest request, String scriptPath, String pathInfo) {
        File scriptFile = new File(scriptPath);

        if (!scriptFile.exists() || !scriptFile.isFile()) {
            return HttpResponse.notFound().setHtmlBody(errorPage("CGI script not found: " + scriptPath));
        }

        String ext = getExtension(scriptPath);
        String interpreter = INTERPRETERS.getOrDefault(ext, "python3");

        try {
            ProcessBuilder pb = new ProcessBuilder(interpreter, scriptFile.getAbsolutePath());
            pb.directory(scriptFile.getParentFile());
            pb.redirectErrorStream(false);

            Map<String, String> env = pb.environment();
            env.put("REQUEST_METHOD", request.getMethod());
            env.put("PATH_INFO", pathInfo != null ? pathInfo : "");
            env.put("SCRIPT_NAME", request.getPath());
            env.put("SCRIPT_FILENAME", scriptFile.getAbsolutePath());
            env.put("QUERY_STRING", request.getQueryString() != null ? request.getQueryString() : "");
            env.put("SERVER_PROTOCOL", "HTTP/1.1");
            env.put("SERVER_SOFTWARE", "LocalServer/1.0");
            env.put("GATEWAY_INTERFACE", "CGI/1.1");
            env.put("DOCUMENT_ROOT", scriptFile.getParent());

            String contentType = request.getHeader("content-type");
            String host = request.getHeader("host");
            String accept = request.getHeader("accept");
            String userAgent = request.getHeader("user-agent");

            if (contentType != null)
                env.put("CONTENT_TYPE", contentType);

            byte[] body = request.getBody();
            env.put("CONTENT_LENGTH", String.valueOf(body != null ? body.length : 0));

            if (host != null)
                env.put("HTTP_HOST", host);
            if (accept != null)
                env.put("HTTP_ACCEPT", accept);
            if (userAgent != null)
                env.put("HTTP_USER_AGENT", userAgent);

            env.entrySet().removeIf(e -> e.getKey() == null || e.getValue() == null ||
                    e.getKey().contains("=") || e.getValue().contains("\n"));

            Process process = pb.start();

            if (body != null && body.length > 0) {
                try (OutputStream stdin = process.getOutputStream()) {
                    stdin.write(body);
                    stdin.flush();
                }
            } else {
                process.getOutputStream().close();
            }

            CompletableFuture<byte[]> outputFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    return process.getInputStream().readAllBytes();
                } catch (IOException e) {
                    return new byte[0];
                }
            });

            CompletableFuture<byte[]> errorFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    return process.getErrorStream().readAllBytes();
                } catch (IOException e) {
                    return new byte[0];
                }
            });

            boolean finished = process.waitFor(CGI_TIMEOUT_MS, TimeUnit.MILLISECONDS);

            if (!finished) {
                process.destroyForcibly();
                System.err.println("[CGI] Timeout exceeded for: " + scriptPath);
                return HttpResponse.internalServerError().setHtmlBody(errorPage("CGI script timed out"));
            }

            // Retrieve data from futures safely
            byte[] output = outputFuture.join();
            byte[] errorOutput = errorFuture.join();

            if (errorOutput.length > 0) {
                System.err.println("[CGI] stderr: " + new String(errorOutput, StandardCharsets.UTF_8));
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                System.err.println("[CGI] Exit code " + exitCode + " for: " + scriptPath);
            }

            return parseCGIOutput(output);

        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            System.err.println("[CGI] Error executing script: " + e.getMessage());
            return HttpResponse.internalServerError().setHtmlBody(errorPage("CGI execution failed: " + e.getMessage()));
        }
    }

    private static HttpResponse parseCGIOutput(byte[] output) {
        HttpResponse response = HttpResponse.ok();

        byte[] crlfCrlf = { '\r', '\n', '\r', '\n' };
        byte[] lflf = { '\n', '\n' };

        int headerEnd = findSequence(output, crlfCrlf);
        int headerLength = 4;

        if (headerEnd < 0) {
            headerEnd = findSequence(output, lflf);
            headerLength = 2;
        }

        if (headerEnd >= 0) {
            String headerSection = new String(output, 0, headerEnd, StandardCharsets.UTF_8);
            byte[] body = Arrays.copyOfRange(output, headerEnd + headerLength, output.length);
            return parseCGIHeaders(headerSection, body, response);
        } else {
            // No headers, treat as HTML body
            return response.setBody(output, "text/html; charset=utf-8");
        }
    }

    private static HttpResponse parseCGIHeaders(String headerSection, byte[] body, HttpResponse response) {
        String contentType = "text/html; charset=utf-8";
        for (String line : headerSection.split("\r?\n")) {
            int colonIdx = line.indexOf(':');
            if (colonIdx > 0) {
                String name = line.substring(0, colonIdx).trim();
                String value = line.substring(colonIdx + 1).trim();
                if (name.equalsIgnoreCase("Content-Type")) {
                    contentType = value;
                } else if (name.equalsIgnoreCase("Status")) {
                    try {
                        int code = Integer.parseInt(value.split(" ")[0]);
                        response.setStatus(code);
                    } catch (NumberFormatException ignored) {
                    }
                } else if (name.equalsIgnoreCase("Location")) {
                    response.setStatus(302);
                    response.setHeader("Location", value);
                } else {
                    response.setHeader(name, value);
                }
            }
        }
        response.setBody(body, contentType);
        return response;
    }

    /**
     * Utility method to find a byte sequence within a byte array.
     */
    private static int findSequence(byte[] data, byte[] sequence) {
        if (data == null || sequence == null || data.length < sequence.length)
            return -1;
        for (int i = 0; i <= data.length - sequence.length; i++) {
            boolean match = true;
            for (int j = 0; j < sequence.length; j++) {
                if (data[i + j] != sequence[j]) {
                    match = false;
                    break;
                }
            }
            if (match)
                return i;
        }
        return -1;
    }

    private static String getExtension(String path) {
        int dot = path.lastIndexOf('.');
        return dot >= 0 ? path.substring(dot).toLowerCase() : "";
    }

    private static String errorPage(String message) {
        return "<html><body><h1>CGI Error</h1><p>" + message + "</p></body></html>";
    }
}