package internal.http;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * Parses multipart/form-data bodies and saves uploaded files to disk.
 */
public class MultipartParser {

    /**
     * Parse a multipart body and save any file parts to uploadDir.
     * Returns a list of saved file names.
     */
    public static List<String> parse(byte[] body, String boundary, File uploadDir) throws IOException {
        List<String> savedFiles = new ArrayList<>();

        if (body == null || body.length == 0) {
            return savedFiles;
        }

        // Ensure the upload directory exists to prevent NoSuchFileException
        if (!uploadDir.exists() && !uploadDir.mkdirs()) {
            System.err.println("[Upload] Warning: Could not create upload directory.");
        }

        byte[] delimiter = ("--" + boundary).getBytes(StandardCharsets.UTF_8);
        byte[] finalDelimiter = ("--" + boundary + "--").getBytes(StandardCharsets.UTF_8);

        List<int[]> partRanges = splitParts(body, delimiter, finalDelimiter);

        for (int[] range : partRanges) {
            processPart(body, range[0], range[1], uploadDir, savedFiles);
        }

        return savedFiles;
    }

    private static List<int[]> splitParts(byte[] body, byte[] delimiter, byte[] finalDelimiter) {
        List<int[]> ranges = new ArrayList<>();
        int currentDelim = indexOf(body, delimiter, 0);

        while (currentDelim >= 0) {
            // Check if this is the final delimiter
            if (startsWith(body, finalDelimiter, currentDelim)) {
                break;
            }

            int partStart = currentDelim + delimiter.length;

            // Skip CRLF after delimiter
            if (partStart + 1 < body.length && body[partStart] == '\r' && body[partStart + 1] == '\n') {
                partStart += 2;
            }

            // Find next delimiter
            int nextDelim = indexOf(body, delimiter, partStart);
            if (nextDelim < 0) {
                break;
            }

            // Remove trailing CRLF before next delimiter
            int partEnd = nextDelim;
            if (partEnd >= 2 && body[partEnd - 2] == '\r' && body[partEnd - 1] == '\n') {
                partEnd -= 2;
            }

            ranges.add(new int[] { partStart, partEnd });

            // Advance to the next delimiter directly without skipping parts
            currentDelim = nextDelim;
        }

        return ranges;
    }

    private static void processPart(byte[] body, int start, int end, File uploadDir,
            List<String> savedFiles) throws IOException {

        // Find end of part headers
        byte[] headerEnd = "\r\n\r\n".getBytes(StandardCharsets.UTF_8);
        int headerEndPos = indexOf(body, headerEnd, start);

        if (headerEndPos < 0 || headerEndPos > end) {
            return;
        }

        String headers = new String(body, start, headerEndPos - start, StandardCharsets.UTF_8);
        int dataStart = headerEndPos + 4;

        // Parse Content-Disposition
        String filename = null;

        for (String line : headers.split("\r\n")) {
            if (line.toLowerCase().startsWith("content-disposition:")) {
                for (String part : line.split(";")) {
                    part = part.trim();
                    if (part.startsWith("filename=")) {
                        filename = part.substring("filename=".length()).replaceAll("\"", "").trim();
                    }
                }
            }
        }

        if (filename != null && !filename.isEmpty()) {
            // Sanitize filename
            filename = sanitizeFilename(filename);

            if (!filename.isEmpty()) {
                File outFile = new File(uploadDir, filename);
                int fileLength = end - dataStart;

                // Write directly from the original array buffer to prevent massive memory
                // duplication
                try (OutputStream out = Files.newOutputStream(outFile.toPath())) {
                    out.write(body, dataStart, fileLength);
                }

                savedFiles.add(filename);
                System.out.println("[Upload] Saved file: " + filename + " (" + fileLength + " bytes)");
            }
        }
    }

    private static String sanitizeFilename(String filename) {
        // Remove path components and dangerous characters
        filename = filename.replace('\\', '/');
        int lastSlash = filename.lastIndexOf('/');
        if (lastSlash >= 0) {
            filename = filename.substring(lastSlash + 1);
        }
        filename = filename.replaceAll("[^a-zA-Z0-9._\\-]", "_");

        // Prevent hidden files
        while (filename.startsWith(".")) {
            filename = filename.substring(1);
        }
        return filename.trim();
    }

    private static int indexOf(byte[] haystack, byte[] needle, int from) {
        if (needle.length == 0)
            return from;

        outer: for (int i = from; i <= haystack.length - needle.length; i++) {
            // Fast first-byte check optimization
            if (haystack[i] == needle[0]) {
                for (int j = 1; j < needle.length; j++) {
                    if (haystack[i + j] != needle[j]) {
                        continue outer;
                    }
                }
                return i;
            }
        }
        return -1;
    }

    private static boolean startsWith(byte[] data, byte[] prefix, int at) {
        if (at + prefix.length > data.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (data[at + i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }
}