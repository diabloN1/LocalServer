package helpers;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

import internal.http.HttpResponse;

public class DirectoryListingBuilder {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter
            .ofPattern("dd-MMM-yyyy HH:mm")
            .withZone(ZoneId.of("GMT"));

    public static HttpResponse build(File dir, String urlPath) {
        File[] files = dir.listFiles();

        String headerHtml = """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Index of %1$s</title>
                    <style>
                        :root { --bg: #0f172a; --surface: #1e293b; --text: #e2e8f0; --muted: #94a3b8; --accent: #3b82f6; --border: #334155; }
                        * { box-sizing: border-box; margin: 0; padding: 0; }
                        body { font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace; background: var(--bg); color: var(--text); padding: 2rem; line-height: 1.5; }
                        .container { max-width: 1000px; margin: 0 auto; background: var(--surface); border: 1px solid var(--border); border-radius: 8px; overflow: hidden; box-shadow: 0 4px 6px -1px rgb(0 0 0 / 0.1); }
                        h1 { padding: 1.5rem; font-size: 1.25rem; color: var(--text); background: #0b1120; border-bottom: 1px solid var(--border); word-break: break-all; }
                        .path-accent { color: var(--accent); }
                        table { width: 100%%; border-collapse: collapse; text-align: left; }
                        th, td { padding: 0.75rem 1.5rem; white-space: nowrap; }
                        th { background: #0f172a; color: var(--muted); font-weight: 600; font-size: 0.875rem; text-transform: uppercase; letter-spacing: 0.05em; border-bottom: 1px solid var(--border); }
                        tr { border-bottom: 1px solid var(--border); transition: background-color 0.15s; }
                        tr:hover { background: rgba(59, 130, 246, 0.1); } /* Subtle blue hover effect */
                        tr:last-child { border-bottom: none; }
                        a { color: var(--text); text-decoration: none; display: flex; align-items: center; gap: 0.6rem; }
                        a:hover { color: var(--accent); }
                        .icon { font-size: 1.2rem; }
                        .size, .date { color: var(--muted); font-size: 0.9rem; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <h1>Index of <span class="path-accent">%1$s</span></h1>
                        <table>
                            <thead>
                                <tr>
                                    <th style="width: 60%%">Name</th>
                                    <th style="width: 20%%">Size</th>
                                    <th style="width: 20%%">Last Modified</th>
                                </tr>
                            </thead>
                            <tbody>
                """
                .formatted(escapeHtml(urlPath));

        StringBuilder html = new StringBuilder(headerHtml);

        if (!urlPath.equals("/")) {
            html.append("""
                    <tr>
                        <td><a href="../"><span class="icon">📁</span> ..</a></td>
                        <td class="size">-</td>
                        <td class="date">-</td>
                    </tr>
                    """);
        }

        if (files != null) {
            Arrays.sort(files, (a, b) -> {
                if (a.isDirectory() != b.isDirectory()) {
                    return a.isDirectory() ? -1 : 1;
                }
                return a.getName().compareToIgnoreCase(b.getName());
            });

            for (File f : files) {
                boolean isDir = f.isDirectory();
                String name = f.getName() + (isDir ? "/" : "");
                String href = urlEncode(f.getName()) + (isDir ? "/" : "");
                String size = isDir ? "-" : formatSize(f.length());
                String modified = formatFileDate(f.lastModified());
                String icon = isDir ? "📁" : "📄";

                html.append(String.format("""
                        <tr>
                            <td><a href="%s"><span class="icon">%s</span> %s</a></td>
                            <td class="size">%s</td>
                            <td class="date">%s</td>
                        </tr>
                        """, href, icon, escapeHtml(name), size, modified));
            }
        }

        html.append("""
                            </tbody>
                        </table>
                    </div>
                </body>
                </html>
                """);

        return HttpResponse.ok().setHtmlBody(html.toString());
    }

    private static String escapeHtml(String s) {
        if (s == null)
            return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }

    private static String urlEncode(String s) {
        try {
            return URLEncoder.encode(s, StandardCharsets.UTF_8.toString()).replace("+", "%20");
        } catch (Exception e) {
            return s;
        }
    }

    private static String formatFileDate(long millis) {
        return DATE_FORMATTER.format(Instant.ofEpochMilli(millis));
    }

    private static String formatSize(long bytes) {
        if (bytes < 1024)
            return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "B";
        return String.format(java.util.Locale.US, "%.1f %s", bytes / Math.pow(1024, exp), pre);
    }
}