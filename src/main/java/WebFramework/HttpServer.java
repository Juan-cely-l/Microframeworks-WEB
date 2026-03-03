package WebFramework;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class HttpServer {
    private static final Map<String, WebMethod> END_POINTS = new ConcurrentHashMap<>();
    private static final Map<String, String> MIME_TYPES = Map.ofEntries(
            Map.entry("html", "text/html; charset=UTF-8"),
            Map.entry("css", "text/css; charset=UTF-8"),
            Map.entry("js", "application/javascript; charset=UTF-8"),
            Map.entry("json", "application/json; charset=UTF-8"),
            Map.entry("txt", "text/plain; charset=UTF-8"),
            Map.entry("png", "image/png"),
            Map.entry("jpg", "image/jpeg"),
            Map.entry("jpeg", "image/jpeg"),
            Map.entry("gif", "image/gif"),
            Map.entry("svg", "image/svg+xml"),
            Map.entry("ico", "image/x-icon")
    );

    private static volatile boolean running;
    private static volatile int configuredPort = 8080;
    private static volatile int boundPort = -1;
    private static volatile ServerSocket serverSocket;
    private static volatile String routePrefix = "";
    private static volatile String staticFilesRoot = "/webroot";

    public static void main(String[] args) throws IOException {
        start();
    }

    public static synchronized void start() throws IOException {
        if (running) {
            return;
        }

        serverSocket = new ServerSocket(configuredPort);
        boundPort = serverSocket.getLocalPort();
        running = true;

        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                handleClient(clientSocket);
            } catch (SocketException socketException) {
                if (running) {
                    throw socketException;
                }
            }
        }
    }

    public static synchronized Thread startAsync() {
        Thread thread = new Thread(() -> {
            try {
                start();
            } catch (IOException exception) {
                throw new RuntimeException(exception);
            }
        });
        thread.setDaemon(true);
        thread.setName("webframework-http-server");
        thread.start();
        return thread;
    }

    public static synchronized void stop() throws IOException {
        running = false;
        if (serverSocket != null && !serverSocket.isClosed()) {
            serverSocket.close();
        }
        serverSocket = null;
        boundPort = -1;
    }

    public static synchronized void port(int port) {
        configuredPort = port;
    }

    public static int getPort() {
        return boundPort != -1 ? boundPort : configuredPort;
    }

    public static synchronized void staticfiles(String folder) {
        staticFilesRoot = normalizeFolder(folder);
    }

    public static synchronized void setRoutePrefix(String prefix) {
        routePrefix = normalizePrefix(prefix);
    }

    public static synchronized void get(String path, WebMethod wm) {
        END_POINTS.put(buildEndpointPath(path), wm);
    }

    static synchronized void clearRoutesForTests() {
        END_POINTS.clear();
        routePrefix = "";
        staticFilesRoot = "/webroot";
        configuredPort = 8080;
    }

    static boolean isRunning() {
        return running;
    }

    private static void handleClient(Socket clientSocket) {
        try (Socket socket = clientSocket;
             BufferedInputStream inputStream = new BufferedInputStream(socket.getInputStream());
             BufferedOutputStream outputStream = new BufferedOutputStream(socket.getOutputStream())) {
            handleRequest(inputStream, outputStream);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    static byte[] processRawRequestForTests(String rawHttpRequest) throws IOException {
        try (ByteArrayInputStream input = new ByteArrayInputStream(rawHttpRequest.getBytes(StandardCharsets.UTF_8));
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            handleRequest(input, output);
            return output.toByteArray();
        }
    }

    private static void handleRequest(InputStream inputStream, OutputStream outputStream) throws IOException {
        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);
        ParsedRequest parsedRequest = parseRequest(inputStream);
        if (parsedRequest == null) {
            sendResponse(bufferedOutputStream, 400, "text/plain; charset=UTF-8", "Bad Request".getBytes(StandardCharsets.UTF_8), Map.of());
            return;
        }

        Request request = parsedRequest.request();
        String method = request.getMethod();

        if (!"GET".equalsIgnoreCase(method)) {
            sendResponse(bufferedOutputStream, 405, "text/plain; charset=UTF-8", "Method Not Allowed".getBytes(StandardCharsets.UTF_8), Map.of("Allow", "GET"));
            return;
        }

        WebMethod endpoint = END_POINTS.get(normalizePath(request.getPath()));
        if (endpoint != null) {
            serveEndpoint(bufferedOutputStream, request, endpoint);
            return;
        }

        StaticFileResult staticFileResult = resolveStaticFile(request.getPath());
        if (staticFileResult.forbidden()) {
            sendResponse(bufferedOutputStream, 403, "text/plain; charset=UTF-8", "Forbidden".getBytes(StandardCharsets.UTF_8), Map.of());
            return;
        }
        if (staticFileResult.content() == null) {
            sendResponse(bufferedOutputStream, 404, "text/plain; charset=UTF-8", "Not Found".getBytes(StandardCharsets.UTF_8), Map.of());
            return;
        }

        sendResponse(bufferedOutputStream, 200, staticFileResult.contentType(), staticFileResult.content(), Map.of());
    }

    private static void serveEndpoint(BufferedOutputStream outputStream, Request request, WebMethod endpoint) throws IOException {
        Response response = new Response();
        try {
            String result = endpoint.execute(request, response);
            byte[] body = result == null ? new byte[0] : result.getBytes(StandardCharsets.UTF_8);
            sendResponse(outputStream, response.getStatusCode(), response.getContentType(), body, response.getHeaders());
        } catch (Exception exception) {
            sendResponse(outputStream, 500, "text/plain; charset=UTF-8", "Internal Server Error".getBytes(StandardCharsets.UTF_8), Map.of());
        }
    }

    private static ParsedRequest parseRequest(InputStream inputStream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        String requestLine = reader.readLine();
        if (requestLine == null || requestLine.isBlank()) {
            return null;
        }

        String[] tokens = requestLine.split("\\s+");
        if (tokens.length < 3) {
            return null;
        }

        String method = tokens[0];
        String uriValue = tokens[1];
        String protocol = tokens[2];

        URI uri;
        try {
            uri = URI.create(uriValue);
        } catch (IllegalArgumentException exception) {
            return null;
        }

        String path = uri.getPath();
        if (path == null || path.isBlank()) {
            path = "/";
        }

        Map<String, String> headers = new LinkedHashMap<>();
        String headerLine;
        while ((headerLine = reader.readLine()) != null && !headerLine.isEmpty()) {
            int separator = headerLine.indexOf(':');
            if (separator <= 0) {
                continue;
            }
            String name = headerLine.substring(0, separator).trim().toLowerCase();
            String value = headerLine.substring(separator + 1).trim();
            headers.put(name, value);
        }

        Request request = new Request(method, normalizePath(path), protocol, headers, uri.getRawQuery());
        return new ParsedRequest(request);
    }

    private static StaticFileResult resolveStaticFile(String path) throws IOException {
        String normalizedPath = normalizePath(path);
        if ("/".equals(normalizedPath)) {
            normalizedPath = "/index.html";
        }

        if (isPathTraversalAttempt(normalizedPath)) {
            return new StaticFileResult(true, null, null);
        }

        String resourcePath = toClasspathPath(staticFilesRoot + normalizedPath);
        ClassLoader classLoader = HttpServer.class.getClassLoader();
        InputStream resourceStream = classLoader.getResourceAsStream(resourcePath);
        if (resourceStream == null) {
            return new StaticFileResult(false, null, null);
        }

        byte[] content;
        try (InputStream stream = resourceStream) {
            content = readAllBytes(stream);
        }

        String contentType = detectMimeType(normalizedPath);
        return new StaticFileResult(false, content, contentType);
    }

    private static void sendResponse(
            BufferedOutputStream outputStream,
            int statusCode,
            String contentType,
            byte[] body,
            Map<String, String> customHeaders
    ) throws IOException {
        StringBuilder headerBuilder = new StringBuilder();
        headerBuilder.append("HTTP/1.1 ")
                .append(statusCode)
                .append(' ')
                .append(statusText(statusCode))
                .append("\r\n");
        headerBuilder.append("Content-Type: ").append(contentType).append("\r\n");
        headerBuilder.append("Content-Length: ").append(body.length).append("\r\n");
        headerBuilder.append("Connection: close\r\n");
        for (Map.Entry<String, String> header : customHeaders.entrySet()) {
            headerBuilder.append(header.getKey()).append(": ").append(header.getValue()).append("\r\n");
        }
        headerBuilder.append("\r\n");

        outputStream.write(headerBuilder.toString().getBytes(StandardCharsets.UTF_8));
        outputStream.write(body);
        outputStream.flush();
    }

    private static String buildEndpointPath(String path) {
        String normalizedRoute = normalizePath(path);
        if (routePrefix.isEmpty()) {
            return normalizedRoute;
        }
        if ("/".equals(normalizedRoute)) {
            return routePrefix;
        }
        return routePrefix + normalizedRoute;
    }

    private static String normalizePath(String value) {
        if (value == null || value.isBlank()) {
            return "/";
        }
        String normalized = value.replace('\\', '/');
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        normalized = normalized.replaceAll("/+", "/");
        if (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private static String normalizeFolder(String folder) {
        String normalized = normalizePath(folder);
        if ("/".equals(normalized)) {
            return "";
        }
        return normalized;
    }

    private static String normalizePrefix(String prefix) {
        String normalized = normalizePath(prefix);
        if ("/".equals(normalized)) {
            return "";
        }
        return normalized;
    }

    private static String detectMimeType(String path) {
        int dotIndex = path.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == path.length() - 1) {
            return "application/octet-stream";
        }
        String extension = path.substring(dotIndex + 1).toLowerCase();
        return MIME_TYPES.getOrDefault(extension, "application/octet-stream");
    }

    private static boolean isPathTraversalAttempt(String path) {
        String[] segments = path.split("/");
        for (String segment : segments) {
            if ("..".equals(segment)) {
                return true;
            }
        }
        return false;
    }

    private static String toClasspathPath(String value) {
        if (value.startsWith("/")) {
            return value.substring(1);
        }
        return value;
    }

    private static byte[] readAllBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[4096];
        int read;
        while ((read = inputStream.read(chunk)) != -1) {
            buffer.write(chunk, 0, read);
        }
        return buffer.toByteArray();
    }

    private static String statusText(int statusCode) {
        return switch (statusCode) {
            case 200 -> "OK";
            case 400 -> "Bad Request";
            case 403 -> "Forbidden";
            case 404 -> "Not Found";
            case 405 -> "Method Not Allowed";
            case 500 -> "Internal Server Error";
            default -> "Unknown";
        };
    }

    private record ParsedRequest(Request request) { }

    private record StaticFileResult(boolean forbidden, byte[] content, String contentType) { }
}
