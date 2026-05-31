import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import jsonParser.mapper.ServerConfig;

public class ErrorHandler {
  private ServerConfig.VirtualServer virtualServer;

  private static final Map<Integer, String[]> DEFAULT_ERRORS = new HashMap<>();

  static {
    DEFAULT_ERRORS.put(400, new String[] { "400 Bad Request", "The server could not understand the request" });
    DEFAULT_ERRORS.put(403, new String[] { "403 Frobidden", "Access to this resource is denied" });
    DEFAULT_ERRORS.put(404, new String[] { "404 Not Found", "The request resource could not be found" });
    DEFAULT_ERRORS.put(405,
        new String[] { "405 Methode Not Allowed", "The HTTP Methode used is not allowed for this resource" });
    DEFAULT_ERRORS.put(408, new String[] { "408 Request Timout", "The server timed out waiting for the request" });
    DEFAULT_ERRORS.put(413,
        new String[] { "413 Content Too large", "The request body exeeds the maximim allowed size" });
    DEFAULT_ERRORS.put(500,
        new String[] { "500 Internal Server Error", "The server ecountred an unexpected error." });
  }

  public ErrorHandler(ServerConfig.VirtualServer virtualServer) {
    this.virtualServer = virtualServer;
  }

  public HttpResponse buildError(int code) {
    if (virtualServer != null && virtualServer.errorPages.containsKey(code)) {
      String pagePath = virtualServer.errorPages.get(code);
      try {
        Path filePath = Paths.get(pagePath.startsWith("/") ? pagePath.substring(1) : pagePath);
        if (Files.exists(filePath)) {
          byte[] content = Files.readAllBytes(filePath);
          return new HttpResponse(code).setBody(content, "text/html; charset=utf-8");
        }
      } catch (IOException e) {

      }
    }
    String[] info = DEFAULT_ERRORS.getOrDefault(code, new String[] { code + " Error", "An error occurred." });
    String html = buildDefaultErrorPage(info[0], info[1]);
    return new HttpResponse(code).setHtmlBody(html);
  }

  private String buildDefaultErrorPage(String title, String message) {
    // Safely parss the status code and text to privent Exceptions if a title
    // doesn't contain a space
    String statusCode = "";
    String statusText = title;

    if (title != null) {
      int spaceIdx = title.indexOf(' ');
      if (spaceIdx > 0) {
        statusCode = title.substring(0, spaceIdx);
        statusText = title.substring(spaceIdx + 1);
      } else if (title.matches("^\\d+$")) {
        statusCode = title;
        statusText = "An Error Occurred";
      }
    }

    String htmlTemplate = """
        <!DOCTYPE html>
        <html lang="en">
        <head>
          <meta charset="UTF-8">
          <meta name="viewport" content="width=device-width, initial-scale=1.0">
          <title>%s</title>
          <style>
            :root {
              --bg-dark: #0f172a;
              --bg-light: #1e293b;
              --accent: #3b82f6;
              --accent-hover: #2563eb;
              --text-main: #f1f5f9;
              --text-muted: #94a3b8;
            }
            * { margin: 0; padding: 0; box-sizing: border-box; }
            body {
              font-family: system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
              background: radial-gradient(circle at center, var(--bg-light) 0%%, var(--bg-dark) 100%%);
              color: var(--text-main);
              min-height: 100vh;
              display: flex;
              align-items: center;
              justify-content: center;
              padding: 1rem;
            }
            .container {
              text-align: center;
              padding: 2rem;
              animation: slideUp 0.6s ease-out forwards;
              opacity: 0;
              transform: translateY(20px);
            }
            .code {
              /* clamp makes it responsive: 5rem on mobile, 8rem on desktop */
              font-size: clamp(5rem, 15vw, 8rem);
              font-weight: 900;
              color: var(--accent);
              line-height: 1;
              letter-spacing: -0.05em;
              text-shadow: 0 0 40px rgba(59, 130, 246, 0.3);
            }
            .title {
              font-size: 1.75rem;
              font-weight: 600;
              margin: 1.5rem 0 0.5rem;
              letter-spacing: -0.025em;
            }
            .message {
              color: var(--text-muted);
              font-size: 1.1rem;
              max-width: 450px;
              margin: 0 auto;
              line-height: 1.6;
            }
            .back {
              display: inline-block;
              margin-top: 2.5rem;
              padding: 0.75rem 1.75rem;
              background: var(--accent);
              color: white;
              text-decoration: none;
              border-radius: 9999px; /* Pill shape */
              font-size: 0.95rem;
              font-weight: 500;
              transition: all 0.2s ease;
              box-shadow: 0 4px 14px 0 rgba(59, 130, 246, 0.39);
            }
            .back:hover {
              background: var(--accent-hover);
              transform: translateY(-2px);
              box-shadow: 0 6px 20px rgba(59, 130, 246, 0.23);
            }
            .back:focus-visible {
              outline: 2px solid white;
              outline-offset: 2px;
            }
            .divider {
              width: 40px;
              height: 4px;
              background: var(--accent);
              margin: 1.5rem auto 0;
              border-radius: 2px;
            }
            @keyframes slideUp {
              to { opacity: 1; transform: translateY(0); }
            }
          </style>
        </head>
        <body>
          <div class="container">
            <div class="code">%s</div>
            <div class="divider"></div>
            <div class="title">%s</div>
            <div class="message">%s</div>
            <a href="/" class="back">&larr; Back to Home</a>
          </div>
        </body>
        </html>
        """;

    return String.format(
        htmlTemplate,
        escapeHtml(title), // Page <title>
        escapeHtml(statusCode), // Huge Number (e.g. 404)
        escapeHtml(statusText), // Text below number (e.g. Not Found)
        escapeHtml(message) // Description message
    );
  }

  private String escapeHtml(String s) {
    if (s == null)
      return "";
    return s.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#x27;");
  }
}
